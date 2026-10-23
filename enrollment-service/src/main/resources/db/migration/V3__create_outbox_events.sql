CREATE TABLE enrollment.outbox_events (
                                          id           BIGSERIAL PRIMARY KEY,
                                          event_id     VARCHAR(64) NOT NULL UNIQUE,
                                          event_type   VARCHAR(64) NOT NULL,
                                          aggregate_id BIGINT      NOT NULL,
                                          payload      TEXT        NOT NULL,
                                          created_at   TIMESTAMP   NOT NULL DEFAULT now(),
                                          published_at TIMESTAMP   NULL,
                                          attempts     INT         NOT NULL DEFAULT 0,
                                          last_error   TEXT        NULL
);

-- Индекс для быстрого поиска неопубликованных событий
CREATE INDEX ix_outbox_unpublished ON enrollment.outbox_events (created_at)
    WHERE published_at IS NULL;