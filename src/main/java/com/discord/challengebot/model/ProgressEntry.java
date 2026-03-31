package com.discord.challengebot.model;

import java.time.LocalDateTime;

/**
 * Модель записи о прогрессе.
 * Фиксирует отдельный факт добавления прогресса пользователем в рамках конкретного испытания.
 */
public record ProgressEntry(
        String id,
        String userId,
        String challengeId,
        long amount,
        LocalDateTime timestamp,
        String note
) {

    /**
     * Создаёт запись о прогрессе с текущим временем в качестве временной метки.
     *
     * @param id          уникальный идентификатор записи
     * @param userId      идентификатор пользователя в Discord
     * @param challengeId идентификатор испытания
     * @param amount      добавленное количество прогресса
     * @param note        произвольный комментарий к записи
     */
    public ProgressEntry(String id, String userId, String challengeId, long amount, String note) {
        this(id, userId, challengeId, amount, LocalDateTime.now(), note);
    }
}
