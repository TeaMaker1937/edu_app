-- Inbox-паттерн: дедупликация обработанных событий по паре (eventId, handlerId).
CREATE TABLE inbox_events (
                              id            BIGSERIAL   PRIMARY KEY,
                              event_id      VARCHAR(64) NOT NULL,
                              handler_id    VARCHAR(64) NOT NULL,
                              event_payload TEXT        NOT NULL,
                              processed_at  TIMESTAMP   NOT NULL DEFAULT now(),
                              CONSTRAINT ux_inbox_event_handler UNIQUE (event_id, handler_id)
);

CREATE INDEX ix_inbox_event_id ON inbox_events(event_id);