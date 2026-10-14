-- Удаляем триггер и функцию, они больше не нужны
DROP TRIGGER trg_sync_course_description_summary ON courses;
DROP FUNCTION sync_course_description_summary();

-- Удаляем старую колонку
ALTER TABLE courses DROP COLUMN description;