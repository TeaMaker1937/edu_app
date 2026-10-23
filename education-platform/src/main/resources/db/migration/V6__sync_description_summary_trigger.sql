CREATE OR REPLACE FUNCTION sync_course_description_summary()
RETURNS trigger AS $$
BEGIN
    -- Если запись пришла от старой версии (только description заполнен)
    IF NEW.summary IS NULL AND NEW.description IS NOT NULL THEN
        NEW.summary := NEW.description;
END IF;
    -- Если запись пришла от новой версии (только summary заполнен)
    IF NEW.description IS NULL AND NEW.summary IS NOT NULL THEN
        NEW.description := NEW.summary;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_course_description_summary
    BEFORE INSERT OR UPDATE ON courses
                         FOR EACH ROW EXECUTE FUNCTION sync_course_description_summary();