CREATE TABLE raw_data (
    id          BIGSERIAL PRIMARY KEY,
    platform    VARCHAR(50)  NOT NULL,
    username    VARCHAR(100) NOT NULL,
    data_type   VARCHAR(50)  NOT NULL,
    ref         VARCHAR(500) NOT NULL DEFAULT '',
    payload     JSONB        NOT NULL,
    fetched_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_raw_data UNIQUE (platform, username, data_type, ref)
);

CREATE INDEX idx_raw_data_user ON raw_data (platform, username);
