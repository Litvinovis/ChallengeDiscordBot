-- PostgreSQL schema for ChallengeDiscordBot
CREATE TABLE IF NOT EXISTS challenges (
    id                   TEXT PRIMARY KEY,
    name                 TEXT NOT NULL,
    target_value         BIGINT NOT NULL DEFAULT 0,
    current_value        BIGINT NOT NULL DEFAULT 0,
    chal_type            TEXT NOT NULL DEFAULT 'INDIVIDUAL',
    start_date           TEXT,
    end_date             TEXT,
    active               BOOLEAN NOT NULL DEFAULT true,
    description          TEXT,
    unit                 TEXT,
    participants         TEXT NOT NULL DEFAULT '[]'
);

CREATE TABLE IF NOT EXISTS challenge_progress (
    challenge_id TEXT NOT NULL,
    user_id      TEXT NOT NULL,
    progress     BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (challenge_id, user_id)
);

CREATE TABLE IF NOT EXISTS challenge_participants (
    user_id               TEXT PRIMARY KEY,
    username              TEXT,
    join_date             TEXT,
    registered_challenges TEXT NOT NULL DEFAULT '[]',
    current_streak        INTEGER NOT NULL DEFAULT 0,
    longest_streak        INTEGER NOT NULL DEFAULT 0,
    last_activity_date    TEXT,
    awarded_achievements  TEXT NOT NULL DEFAULT '[]'
);

CREATE TABLE IF NOT EXISTS progress_history (
    id           BIGSERIAL PRIMARY KEY,
    challenge_id TEXT NOT NULL,
    user_id      TEXT NOT NULL,
    username     TEXT,
    amount       BIGINT NOT NULL,
    recorded_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_progress_history_challenge_user ON progress_history(challenge_id, user_id);
CREATE INDEX IF NOT EXISTS idx_progress_history_recorded_at ON progress_history(recorded_at);

CREATE TABLE IF NOT EXISTS challenge_archive (
    id            TEXT PRIMARY KEY,
    name          TEXT,
    target_value  BIGINT,
    current_value BIGINT,
    chal_type     TEXT,
    start_date    TEXT,
    end_date      TEXT,
    description   TEXT,
    unit          TEXT,
    archived_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Наследие Ignite: колонка не читается и не пишется ни одной строкой кода (удалена 02.09.2026)
ALTER TABLE challenges DROP COLUMN IF EXISTS participant_progress;
