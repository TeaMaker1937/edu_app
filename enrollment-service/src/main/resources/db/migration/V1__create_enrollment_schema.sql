-- Схема для модуля enrollment. Хранит записи на курсы и ключи идемпотентности.
CREATE SCHEMA IF NOT EXISTS enrollment;

-- Ключи идемпотентности: одна строка = один обработанный Idempotency-Key.
CREATE TABLE enrollment.idempotency_keys (
                                             idempotency_key VARCHAR(255) PRIMARY KEY,
                                             request_hash    VARCHAR(64)  NOT NULL,
                                             response_status INTEGER      NOT NULL,
                                             response_body   TEXT         NOT NULL,
                                             created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

-- Сами записи на курсы. Ссылается на ключ идемпотентности.
CREATE TABLE enrollment.enrollments (
                                        id              BIGSERIAL PRIMARY KEY,
                                        idempotency_key VARCHAR(255)  NOT NULL,
                                        user_id         BIGINT        NOT NULL,
                                        course_id       BIGINT        NOT NULL,
                                        course_title    VARCHAR(255)  NOT NULL,
                                        price           NUMERIC(10,2) NOT NULL,
                                        status          VARCHAR(32)   NOT NULL,
                                        created_at      TIMESTAMP     NOT NULL DEFAULT now(),

                                        CONSTRAINT fk_enr_key FOREIGN KEY (idempotency_key)
                                            REFERENCES enrollment.idempotency_keys(idempotency_key) ON DELETE CASCADE,
                                        CONSTRAINT ux_enr_key UNIQUE (idempotency_key)
);

CREATE INDEX ix_enr_user   ON enrollment.enrollments(user_id);
CREATE INDEX ix_enr_course ON enrollment.enrollments(course_id);