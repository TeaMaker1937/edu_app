-- 1. Цена курса
ALTER TABLE courses
    ADD COLUMN price NUMERIC(10, 2) NOT NULL DEFAULT 0;

-- 2. Записи на курс
CREATE TABLE enrollments (
                             id        BIGSERIAL PRIMARY KEY,
                             user_id   BIGINT NOT NULL,
                             course_id BIGINT NOT NULL,
                             status    VARCHAR(20) NOT NULL,
                             CONSTRAINT fk_enrollments_user   FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE,
                             CONSTRAINT fk_enrollments_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
                             CONSTRAINT chk_enrollments_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
                             CONSTRAINT uq_enrollment_user_course UNIQUE (user_id, course_id)
);

CREATE INDEX idx_enrollments_course_status ON enrollments(course_id, status);