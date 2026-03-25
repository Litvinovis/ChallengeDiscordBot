package com.discord.challengebot.model;

import java.time.LocalDateTime;

/**
 * Модель записи о прогрессе.
 * Фиксирует отдельный факт добавления прогресса пользователем в рамках конкретного испытания.
 */
public class ProgressEntry {
    private String id;
    private String userId;
    private String challengeId;
    private long amount;
    private LocalDateTime timestamp;
    private String note;

    /**
     * Конструктор по умолчанию.
     */
    public ProgressEntry() {
    }

    /**
     * Конструктор с параметрами. Временная метка устанавливается в текущее время.
     *
     * @param id          уникальный идентификатор записи
     * @param userId      идентификатор пользователя в Discord
     * @param challengeId идентификатор испытания
     * @param amount      добавленное количество прогресса
     * @param note        произвольный комментарий к записи
     */
    public ProgressEntry(String id, String userId, String challengeId, long amount, String note) {
        this.id = id;
        this.userId = userId;
        this.challengeId = challengeId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.note = note;
    }

    // Getters and setters
    /**
     * Возвращает уникальный идентификатор записи о прогрессе.
     *
     * @return идентификатор записи
     */
    public String getId() {
        return id;
    }

    /**
     * Устанавливает уникальный идентификатор записи.
     *
     * @param id идентификатор записи
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Возвращает идентификатор пользователя, добавившего прогресс.
     *
     * @return идентификатор пользователя
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Устанавливает идентификатор пользователя.
     *
     * @param userId идентификатор пользователя в Discord
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Возвращает идентификатор испытания.
     *
     * @return идентификатор испытания
     */
    public String getChallengeId() {
        return challengeId;
    }

    /**
     * Устанавливает идентификатор испытания.
     *
     * @param challengeId идентификатор испытания
     */
    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    /**
     * Возвращает добавленное количество прогресса.
     *
     * @return добавленное количество
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Устанавливает добавленное количество прогресса.
     *
     * @param amount добавленное количество
     */
    public void setAmount(long amount) {
        this.amount = amount;
    }

    /**
     * Возвращает дату и время создания записи.
     *
     * @return временная метка записи
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Устанавливает дату и время создания записи.
     *
     * @param timestamp временная метка записи
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Возвращает произвольный комментарий к записи о прогрессе.
     *
     * @return комментарий
     */
    public String getNote() {
        return note;
    }

    /**
     * Устанавливает произвольный комментарий к записи.
     *
     * @param note комментарий к записи
     */
    public void setNote(String note) {
        this.note = note;
    }
}