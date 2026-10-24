-- Новое поле: уровень курса (BASIC / ADVANCED). Дефолт — BASIC.
-- Обратная совместимость: старые клиенты не знают про поле и его игнорируют.
ALTER TABLE courses ADD COLUMN level VARCHAR(32) NOT NULL DEFAULT 'BASIC';