-- PostgreSQL schema for ChallengeDiscordBot
CREATE TABLE IF NOT EXISTS challenges (
    id                   TEXT PRIMARY KEY,
    name                 TEXT NOT NULL,
    target_value         BIGINT NOT NULL DEFAULT 0,
    current_value        BIGINT NOT NULL DEFAULT 0,
    chal_type            TEXT NOT NULL DEFAULT 'INDIVIDUAL',
    start_date           TIMESTAMP,
    end_date             TIMESTAMP,
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
    join_date             TIMESTAMP,
    registered_challenges TEXT NOT NULL DEFAULT '[]',
    current_streak        INTEGER NOT NULL DEFAULT 0,
    longest_streak        INTEGER NOT NULL DEFAULT 0,
    last_activity_date    DATE,
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

-- Состояние участия хранится отдельно для каждого испытания. Обновляем старые строки
-- только один раз: повторный запуск схемы не должен переносить старую историю на повторное вступление.
ALTER TABLE challenge_progress ADD COLUMN IF NOT EXISTS joined_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE challenge_progress ADD COLUMN IF NOT EXISTS last_progress_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE challenge_progress ADD COLUMN IF NOT EXISTS first_inactivity_warning_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE challenge_progress ADD COLUMN IF NOT EXISTS second_inactivity_warning_at TIMESTAMP WITH TIME ZONE;
UPDATE challenge_progress cp SET joined_at = NOW(), last_progress_at = (
    SELECT MAX(recorded_at) FROM progress_history ph
    WHERE ph.challenge_id = cp.challenge_id AND ph.user_id = cp.user_id AND ph.amount > 0
) WHERE cp.joined_at IS NULL;
ALTER TABLE challenge_progress ALTER COLUMN joined_at SET DEFAULT NOW();
ALTER TABLE challenge_progress ALTER COLUMN joined_at SET NOT NULL;

CREATE TABLE IF NOT EXISTS challenge_archive (
    id            TEXT PRIMARY KEY,
    name          TEXT,
    target_value  BIGINT,
    current_value BIGINT,
    chal_type     TEXT,
    start_date    TIMESTAMP,
    end_date      TIMESTAMP,
    description   TEXT,
    unit          TEXT,
    archived_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Наследие Ignite: колонка не читается и не пишется ни одной строкой кода (удалена 02.09.2026)
ALTER TABLE challenges DROP COLUMN IF EXISTS participant_progress;

-- Даты хранились строками (LocalDateTime.toString()), из-за чего фильтрация и сортировка
-- по датам были возможны только в Java. Перевод в нативные типы; ISO-строки Postgres
-- разбирает сам, повторный запуск ничего не делает (колонка уже нужного типа).
ALTER TABLE challenges        ALTER COLUMN start_date        TYPE TIMESTAMP USING start_date::timestamp;
ALTER TABLE challenges        ALTER COLUMN end_date          TYPE TIMESTAMP USING end_date::timestamp;
ALTER TABLE challenge_archive ALTER COLUMN start_date        TYPE TIMESTAMP USING start_date::timestamp;
ALTER TABLE challenge_archive ALTER COLUMN end_date          TYPE TIMESTAMP USING end_date::timestamp;
ALTER TABLE challenge_participants ALTER COLUMN join_date          TYPE TIMESTAMP USING join_date::timestamp;
ALTER TABLE challenge_participants ALTER COLUMN last_activity_date TYPE DATE      USING last_activity_date::date;
