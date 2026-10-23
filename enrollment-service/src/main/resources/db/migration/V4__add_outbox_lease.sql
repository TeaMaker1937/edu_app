-- Lease: до какого момента событие залочено relay'ем.
-- Если relay упал, после истечения locked_until другой relay подхватит событие.
ALTER TABLE enrollment.outbox_events
    ADD COLUMN locked_until TIMESTAMP NULL;

-- Индекс для быстрого поиска свободных событий
CREATE INDEX ix_outbox_lease ON enrollment.outbox_events (locked_until, created_at)
    WHERE published_at IS NULL;