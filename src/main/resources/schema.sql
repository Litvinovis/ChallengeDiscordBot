-- DDL для таблиц ChallengeDiscordBot в Apache Ignite 3.x (3.0.0 / 3.1.0)
-- Все типы совместимы с 3.1.0: VARCHAR, BIGINT, BOOLEAN, INT
-- Если CREATE ZONE упадёт с ошибкой на 3.1.0, SchemaInitializer пропустит и продолжит

-- Зона хранения данных ChallengeBot
CREATE ZONE IF NOT EXISTS challengebot WITH STORAGE_PROFILES='default', REPLICAS=1, PARTITIONS=25;

-- Таблица испытаний (challenges)
-- participant_progress хранится как JSON: {"userId": значение, ...}
-- participants хранится как JSON: ["userId1", "userId2", ...]
-- chal_type вместо challenge_type (Ignite 3 Tuple API не поддерживает quoted identifiers)
CREATE TABLE IF NOT EXISTS challenges (
    id                   VARCHAR PRIMARY KEY,
    name                 VARCHAR NOT NULL,
    target_value         BIGINT NOT NULL DEFAULT 0,
    current_value        BIGINT NOT NULL DEFAULT 0,
    chal_type            VARCHAR NOT NULL DEFAULT 'INDIVIDUAL',
    start_date           VARCHAR,
    end_date             VARCHAR,
    active               BOOLEAN NOT NULL DEFAULT true,
    description          VARCHAR,
    unit                 VARCHAR,
    participant_progress VARCHAR NOT NULL DEFAULT '{}',
    participants         VARCHAR NOT NULL DEFAULT '[]'
) ZONE challengebot;

-- Нормализованная таблица прогресса участников (challenge_progress)
-- Заменяет JSON-колонку participant_progress в таблице challenges
CREATE TABLE IF NOT EXISTS challenge_progress (
    challenge_id VARCHAR NOT NULL,
    user_id      VARCHAR NOT NULL,
    progress     BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (challenge_id, user_id)
) ZONE challengebot;

-- Таблица участников (participants)
-- registered_challenges хранится как JSON: ["challengeName1", ...]
-- awarded_achievements хранится как JSON: ["key1", ...]
CREATE TABLE IF NOT EXISTS challenge_participants (
    user_id               VARCHAR PRIMARY KEY,
    username              VARCHAR,
    join_date             VARCHAR,
    registered_challenges VARCHAR NOT NULL DEFAULT '[]',
    current_streak        INT NOT NULL DEFAULT 0,
    longest_streak        INT NOT NULL DEFAULT 0,
    last_activity_date    VARCHAR,
    awarded_achievements  VARCHAR NOT NULL DEFAULT '[]'
) ZONE challengebot;
