CREATE TABLE twin (
    id           BIGSERIAL PRIMARY KEY,
    platform     VARCHAR(50)  NOT NULL DEFAULT 'github',
    username     VARCHAR(100) NOT NULL,
    display_name VARCHAR(255),
    profile      JSONB        NOT NULL,
    ai_summary   TEXT,
    built_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_twin UNIQUE (platform, username)
);

CREATE TABLE skill_claim (
    id         BIGSERIAL PRIMARY KEY,
    twin_id    BIGINT       NOT NULL REFERENCES twin(id) ON DELETE CASCADE,
    tech_name  VARCHAR(100) NOT NULL,
    category   VARCHAR(50)  NOT NULL,
    repo_count INT          NOT NULL,
    first_used DATE,
    last_used  DATE,

    CONSTRAINT uq_skill_claim UNIQUE (twin_id, tech_name)
);

CREATE TABLE evidence (
    id             BIGSERIAL PRIMARY KEY,
    skill_claim_id BIGINT       NOT NULL REFERENCES skill_claim(id) ON DELETE CASCADE,
    evidence_type  VARCHAR(50)  NOT NULL,
    repo_full_name VARCHAR(255) NOT NULL,
    detail         TEXT         NOT NULL,
    url            TEXT
);

CREATE INDEX idx_evidence_claim ON evidence (skill_claim_id);
