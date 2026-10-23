-- Делаем FK отложенным: проверка переносится на момент COMMIT,
-- поэтому порядок INSERT (enrollments → idempotency_keys) становится неважен.
ALTER TABLE enrollment.enrollments
    DROP CONSTRAINT fk_enr_key;

ALTER TABLE enrollment.enrollments
    ADD CONSTRAINT fk_enr_key
        FOREIGN KEY (idempotency_key)
            REFERENCES enrollment.idempotency_keys(idempotency_key)
            ON DELETE CASCADE
            DEFERRABLE INITIALLY DEFERRED;