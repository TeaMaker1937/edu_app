CREATE SCHEMA IF NOT EXISTS payment;

CREATE TABLE payment.payments (
                                  id             BIGSERIAL PRIMARY KEY,
                                  command_id     VARCHAR(64)   NOT NULL UNIQUE,
                                  enrollment_id  BIGINT        NOT NULL,
                                  user_id        BIGINT        NOT NULL,
                                  amount         NUMERIC(10,2) NOT NULL,
                                  status         VARCHAR(16)   NOT NULL,
                                  created_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX ix_payments_enrollment ON payment.payments(enrollment_id);