CREATE TABLE media_files (
                             id            BIGSERIAL PRIMARY KEY,
                             original_name VARCHAR(255) NOT NULL,
                             stored_name   VARCHAR(255) NOT NULL UNIQUE,
                             content_type  VARCHAR(64)  NOT NULL,
                             size_bytes    BIGINT       NOT NULL,
                             uploaded_by   BIGINT       NOT NULL,
                             created_at    TIMESTAMP    NOT NULL DEFAULT now(),
                             CONSTRAINT fk_media_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_media_uploader ON media_files(uploaded_by);