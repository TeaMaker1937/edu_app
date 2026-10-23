CREATE TABLE lessons (
                         id        BIGSERIAL PRIMARY KEY,
                         course_id BIGINT NOT NULL,
                         title     VARCHAR(255) NOT NULL,
                         content   TEXT,
                         version   BIGINT NOT NULL DEFAULT 0,
                         CONSTRAINT fk_lessons_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE INDEX idx_lessons_course ON lessons(course_id);