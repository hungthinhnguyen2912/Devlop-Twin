CREATE TABLE normalized_repo (
    id               BIGSERIAL PRIMARY KEY,
    platform         VARCHAR(50)  NOT NULL,
    username         VARCHAR(100) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    full_name        VARCHAR(255) NOT NULL,
    description      TEXT,
    primary_language VARCHAR(100),
    is_fork          BOOLEAN      NOT NULL DEFAULT FALSE,
    stars            INT          NOT NULL DEFAULT 0,
    repo_created_at  TIMESTAMPTZ,
    last_pushed_at   TIMESTAMPTZ,
    analyzed_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_normalized_repo UNIQUE (platform, full_name)
);

CREATE INDEX idx_normalized_repo_user ON normalized_repo (platform, username);

CREATE TABLE detected_tech (
    id        BIGSERIAL PRIMARY KEY,
    repo_id   BIGINT       NOT NULL REFERENCES normalized_repo(id) ON DELETE CASCADE,
    tech_name VARCHAR(100) NOT NULL,
    category  VARCHAR(50)  NOT NULL,
    source    VARCHAR(50)  NOT NULL,
    detail    TEXT,

    CONSTRAINT uq_detected_tech UNIQUE (repo_id, tech_name, source)
);
