-- Заменяем функцию триггера на корректную версию,
-- которая умеет синхронизировать поля не только при INSERT, но и при UPDATE.
CREATE OR REPLACE FUNCTION sync_course_description_summary()
RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        -- При вставке: если одно поле пустое, заполняем его из другого
        IF NEW.summary IS NULL AND NEW.description IS NOT NULL THEN
            NEW.summary := NEW.description;
        ELSIF NEW.description IS NULL AND NEW.summary IS NOT NULL THEN
            NEW.description := NEW.summary;
END IF;

    ELSIF TG_OP = 'UPDATE' THEN
        -- При обновлении: если изменился только description, тянем за ним summary
        IF NEW.description IS DISTINCT FROM OLD.description
           AND NEW.summary IS NOT DISTINCT FROM OLD.summary THEN
            NEW.summary := NEW.description;
        -- И наоборот: если изменился только summary, тянем за ним description
        ELSIF NEW.summary IS DISTINCT FROM OLD.summary
              AND NEW.description IS NOT DISTINCT FROM OLD.description THEN
            NEW.description := NEW.summary;
END IF;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;