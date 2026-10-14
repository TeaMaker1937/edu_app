CREATE TABLE notifications (
                               id         BIGSERIAL PRIMARY KEY,
                               user_id    BIGINT NOT NULL,
                               course_id  BIGINT NOT NULL,
                               message    TEXT   NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT now(),
                               CONSTRAINT fk_notifications_user   FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE,
                               CONSTRAINT fk_notifications_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_course ON notifications(course_id);
CREATE INDEX idx_notifications_user   ON notifications(user_id);