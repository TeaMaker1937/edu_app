CREATE SCHEMA IF NOT EXISTS catalog;

-- Таблица идемпотентности: одна строка на (commandId, action).
-- action = RESERVE | RELEASE.
-- Возвращаем сохранённый result для повторов.
CREATE TABLE catalog.seat_operations (
                                         id            BIGSERIAL PRIMARY KEY,
                                         command_id    VARCHAR(64) NOT NULL,
                                         action        VARCHAR(16) NOT NULL,
                                         enrollment_id BIGINT      NOT NULL,
                                         course_id     BIGINT      NOT NULL,
                                         result        VARCHAR(32) NOT NULL,
                                         reason        VARCHAR(255),
                                         created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                         CONSTRAINT ux_seat_ops_cmd_action UNIQUE (command_id, action)
);

CREATE INDEX ix_seat_ops_course ON catalog.seat_operations(course_id);