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
    participant_progress TEXT NOT NULL DEFAULT '{}',
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
