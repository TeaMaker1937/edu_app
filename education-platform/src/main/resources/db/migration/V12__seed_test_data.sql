-- ==========================================================================
-- V12: тестовые данные для отладки и демонстрации
-- ==========================================================================
-- Пароль у всех пользователей: password123
-- Пароли захэшированы BCrypt (значения идентичны тем, что создаёт
-- приложение при регистрации через POST /api/auth/register).
--
-- Миграция рассчитана на чистую БД (только что применены V1..V11).
-- Если нужно пересоздать данные: docker compose down -v && docker compose up
-- ==========================================================================
TRUNCATE notifications, lessons, enrollments, courses, users RESTART IDENTITY CASCADE;
-- 1. Пользователи -----------------------------------------------------------
INSERT INTO users (id, email, password, full_name, role) VALUES
                                                             (1, 'teacher@example.com',
                                                              '$2a$10$r/0vDS84wzB5Y7z/9jqzs.JoZlhqJjcbCtQl3SxcgeJUrvn6OKovC',
                                                              'Иван Преподаватель', 'TEACHER'),
                                                             (2, 'student@example.com',
                                                              '$2a$10$izd.cG.lRtwb8wzLdPo4XuxtBYUSOl8jCcwuedGBOKn477Cd8fx5C',
                                                              'Пётр Студент', 'STUDENT'),
                                                             (3, 'max@example.com',
                                                              '$2a$10$izd.cG.lRtwb8wzLdPo4XuxtBYUSOl8jCcwuedGBOKn477Cd8fx5C',
                                                              'Максим Студент', 'STUDENT'),
                                                             (4, 'newstudent@example.com',
                                                              '$2a$10$izd.cG.lRtwb8wzLdPo4XuxtBYUSOl8jCcwuedGBOKn477Cd8fx5C',
                                                              'Новый Студент', 'STUDENT');

-- 2. Курсы ------------------------------------------------------------------
INSERT INTO courses (id, title, summary, price, version, teacher_id) VALUES
                                                                         (1, 'C# Course',        'Базовый курс по Spring',                     1000, 0, 1),
                                                                         (2, 'Java Spring Boot', 'Базовый курс по Spring Boot для начинающих', 1500, 0, 1),
                                                                         (3, 'Java Spring Boot', 'Базовый курс по Spring Boot для начинающих', 1500, 0, 1),
                                                                         (4, 'Новый курс',       'Тестовый курс',                              2000, 0, 1),
                                                                         (5, 'Java Spring',      'Курс по Spring Boot',                        5000, 0, 1);

-- 3. Записи на курсы --------------------------------------------------------
-- Проверяем крайние случаи отчёта:
--   курс 1 → 2 ACTIVE
--   курс 2 → 1 ACTIVE + 1 CANCELLED
--   курс 3 → без записей (activeCount = 0)
--   курс 4 → только CANCELLED (activeCount = 0)
--   курс 5 → 2 ACTIVE
INSERT INTO enrollments (id, user_id, course_id, status) VALUES
                                                             (1, 2, 1, 'ACTIVE'),     -- student → курс 1
                                                             (2, 3, 1, 'ACTIVE'),     -- max     → курс 1
                                                             (3, 2, 2, 'ACTIVE'),     -- student → курс 2
                                                             (4, 3, 2, 'CANCELLED'),  -- max     → курс 2 (не считается)
                                                             (5, 2, 4, 'CANCELLED'),  -- student → курс 4 (не считается)
                                                             (6, 3, 5, 'ACTIVE'),     -- max     → курс 5
                                                             (7, 4, 5, 'ACTIVE');     -- newstudent → курс 5

-- 4. Урок -------------------------------------------------------------------
INSERT INTO lessons (id, course_id, title, content, version) VALUES
    (1, 1, 'Введение в Spring', 'Что такое IoC и DI', 0);

-- 5. Уведомление ------------------------------------------------------------
-- Даёт notificationCount = 1 для курса 1 в отчёте.
INSERT INTO notifications (id, user_id, course_id, message, created_at) VALUES
    (1, 2, 1, 'Вы записаны на курс C# Course', now());

-- 6. Сброс последовательностей ---------------------------------------------
-- Без этого следующий INSERT через приложение попытается взять id=1 и упадёт.
SELECT setval(pg_get_serial_sequence('users', 'id'),         (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('courses', 'id'),       (SELECT MAX(id) FROM courses));
SELECT setval(pg_get_serial_sequence('enrollments', 'id'),   (SELECT MAX(id) FROM enrollments));
SELECT setval(pg_get_serial_sequence('lessons', 'id'),       (SELECT MAX(id) FROM lessons));
SELECT setval(pg_get_serial_sequence('notifications', 'id'), (SELECT MAX(id) FROM notifications));