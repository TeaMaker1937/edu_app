# Постмортем: дубликат уведомлений при повторной доставке EnrollmentConfirmed

**Дата:** 2026-10-23 (UTC+4)
**Автор:** <твоё имя>
**Статус:** закрыт
**Severity:** Medium (UX-инцидент, деньги/места не затронуты)

## Влияние на пользователя

**До:** пользователь получает **два уведомления** «Вы записаны на курс X» вместо одного. Деньги списаны один раз, место зарезервировано один раз, эффект в БД один — дублируется только запись в `notifications` (и потенциально — отправка email/пуш, если бы это была реальная интеграция).

**Оценка охвата:** любой сценарий, в котором consumer получил `EnrollmentConfirmed`, зафиксировал данные в БД, но ACK до брокера не дошёл (потеря соединения, kill, рестарт). По нашей оценке на реальной нагрузке ~1-3 события в сутки.

**Не затронуто:** платёж, резервирование места, статус enrollment.

## Временная шкала (UTC+4)

| Время | Событие |
|---|---|
| 22:09:47.588 | Paid enrollment создан, `seat-reserved` reply получен |
| 22:09:49.748 | `payment-debited` reply получен, saga → CONFIRMED |
| 22:09:51.589 | Relay опубликовал `EnrollmentConfirmed` (eventId=de47ddee...) |
| 22:09:51.605 | Consumer (platform) получил сообщение |
| 22:09:51.620 | **INSERT notifications** выполнен (в БД) |
| 22:09:51.623 | Процесс platform получил SIGTERM / упал |
| — | **ACK брокеру не отправлен** |
| +30s (RabbitMQ) | Redelivery policy вернула сообщение в `enrollment.confirmed.queue` |
| 22:10:21 (после restart platform) | Consumer снова получил то же сообщение |
| 22:10:21.628 | **INSERT notifications выполнен ВТОРОЙ раз** |
| 22:10:21.630 | ACK отправлен — сообщение исчезло |
| — | В БД **2 notifications** для одного enrollment |

**Обнаружение:** ручная проверка `SELECT COUNT(*) FROM notifications WHERE user_id=2 AND course_id=1` дала 2 вместо ожидаемой 1 при разборе отчёта.

## Первопричина (root cause)

Consumer обрабатывает `EnrollmentConfirmed` **без дедупликации по eventId**. RabbitMQ гарантирует at-least-once — при потере ACK сообщение возвращается в очередь, consumer повторно применяет эффект. Это **правильное поведение брокера**; ошибка — в потребителе, который не является идемпотентным.

## Способствующее условие

**Без inbox** (`HANDLER_ID = "notification-" + System.currentTimeMillis()` — уникальный при каждом старте) таблица `inbox_events` **физически работает**, но её UNIQUE-ключ `(event_id, handler_id)` не защищает от повторной доставки — новый `handler_id` при каждом рестарте обходит проверку.

**Дополнительно:** eventId генерируется **один раз** в `SagaCoordinator.moveToConfirmed`, но **не передаётся** в consumer для дедупликации, пока inbox не внедрён.

## Подтверждения (из логов/БД)

**Факт 1: дубликат в БД**

SELECT id, user_id, course_id, message FROM notifications
WHERE user_id=2 AND course_id=1 ORDER BY id DESC LIMIT 3;

**Факт 2: событие доставлено дважды**

docker compose logs platform | grep "CONFIRMED-CONSUMER"
[CONFIRMED-CONSUMER] Получено: eventId=de47ddee-...
[CONFIRMED-CONSUMER] Notification создана eventId=de47ddee-...

--- kill platform, restart ---
[CONFIRMED-CONSUMER] Получено: eventId=de47ddee-... ← тот же eventId
[CONFIRMED-CONSUMER] Notification создана eventId=de47ddee-... ← второй раз, без SKIP

**Факт 3: inbox не помог (без inbox-паттерна)**
SELECT event_id, handler_id FROM inbox_events;

de47ddee-... | notification-no-inbox-1792620526
de47ddee-... | notification-no-inbox-1792620599 ← два handlerId

**Факт 4: с inbox — SKIP**
После возврата `HANDLER_ID = "notification"` и повторного теста:
[CONFIRMED-CONSUMER] Дубликат eventId=... handler=notification — SKIP
notifications count = 1


**Гипотеза (НЕ подтверждено):** насколько часто это происходит в проде. У нас нет метрики `redelivery_count`. **Считать гипотезой** до внедрения мониторинга `notification_sent_total{result="skip"}` (сейчас этот счётчик считает только реальные отправки, не SKIP).

## Восстановление

1. **Немедленно:** удалить дубликаты вручную:
   ```sql
   DELETE FROM notifications
   WHERE id IN (SELECT id FROM (
     SELECT id, ROW_NUMBER() OVER (
       PARTITION BY user_id, course_id, message ORDER BY id
     ) AS rn FROM notifications
   ) t WHERE rn > 1);

2. Исправление: включить inbox-паттерн (HANDLER_ID = "notification", INSERT ... ON CONFLICT DO NOTHING).

3. Проверка: повторить эксперимент kill — с inbox дубликата нет.

Уроки
At-least-once — это норма, а не баг. Потребитель обязан быть идемпотентным.

HANDLER_ID — контракт, не «уникальный идентификатор запуска». Менять его можно только при осознанной смене семантики.

Мониторинг redelivery нужен: без него инцидент обнаруживается случайно (в нашем случае — при аудите).

Postmortem без BLAMELESS. Сбой — свойство системы, не ошибка разработчика. Фикс — в архитектуре (inbox), не в «стараться лучше».

Действия
## 4. Таблица исправлений

Файл `docs/postmortems/2026-10-23-notification-duplicate-fixes.md`:

```markdown
# Таблица исправлений (постмортем 2026-10-23)

| # | Действие | Роль | Срок | Признак успешного завершения |
|---|---|---|---|---|
| 1 | Зафиксировать `HANDLER_ID = "notification"` как константу, убрать `System.currentTimeMillis()` | Backend | 1 день | В коде единственное значение; эксперимент с kill не создаёт дубликат |
| 2 | Добавить метрику `notification_dedup_skips_total` (counter, tag handler) | Backend | 3 дня | `curl /actuator/prometheus` содержит `notification_dedup_skips_total` |
| 3 | Алерт Prometheus: `rate(notification_dedup_skips_total[5m]) > 0` | DevOps | 3 дня | Prometheus → Alerts → Firing при симуляции дубля |
| 4 | Ранбук «дубликаты notification» в `docs/runbooks/` | Tech Writer | 5 дней | Runbook существует, содержит SQL-дедупликатор |
| 5 | Cleanup-скрипт для `inbox_events` > 30 дней | Backend | 7 дней | Cron-задача в docker-compose, логи «cleanup inbox: N rows deleted» |
| 6 | Тест `EnrollmentConfirmedDedupTest` (интеграционный) | QA | 5 дней | Тест в `education-platform/src/test`, зелёный в CI |
| 7 | Weekly-джоб проверки: `SELECT event_id, COUNT(*) FROM inbox_events GROUP BY event_id, handler_id HAVING COUNT(*) > 1` | DevOps | 10 дней | Cronjob, пустой результат на продакшене |

**Обязательное к выпуску:** #1 и #6.