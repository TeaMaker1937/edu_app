CREATE TABLE enrollment.saga_instances (
                                           id                 BIGSERIAL PRIMARY KEY,
                                           enrollment_id      BIGINT       NOT NULL,
                                           course_id          BIGINT       NOT NULL,
                                           user_id            BIGINT       NOT NULL,
                                           state              VARCHAR(32)  NOT NULL,
                                           current_command_id VARCHAR(64),
                                           deadline_at        TIMESTAMPTZ,
                                           retry_count        INT          NOT NULL DEFAULT 0,
                                           created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                           updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_saga_enrollment ON enrollment.saga_instances(enrollment_id);
CREATE INDEX ix_saga_active ON enrollment.saga_instances(state)
    WHERE state IN ('RESERVING','DEBITING','COMPENSATING');

CREATE TABLE enrollment.saga_transitions (
                                             id          BIGSERIAL PRIMARY KEY,
                                             saga_id     BIGINT       NOT NULL,
                                             from_state  VARCHAR(32),
                                             to_state    VARCHAR(32)  NOT NULL,
                                             reason      VARCHAR(255),
                                             command_id  VARCHAR(64),
                                             created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_transitions_saga ON enrollment.saga_transitions(saga_id);