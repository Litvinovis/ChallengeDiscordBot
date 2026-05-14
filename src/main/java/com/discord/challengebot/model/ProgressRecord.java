package com.discord.challengebot.model;

import java.time.LocalDateTime;

/**
 * Запись истории прогресса участника по испытанию.
 */
public record ProgressRecord(
        String challengeId,
        String userId,
        String username,
        long amount,
        LocalDateTime recordedAt
) {}
