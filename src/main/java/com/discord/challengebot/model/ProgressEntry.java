package com.discord.challengebot.model;

import java.time.LocalDateTime;

/**
 * Модель записи о прогрессе
 */
public class ProgressEntry {
    private String id;
    private String userId;
    private String challengeId;
    private long amount;
    private LocalDateTime timestamp;
    private String note;

    public ProgressEntry() {
    }

    public ProgressEntry(String id, String userId, String challengeId, long amount, String note) {
        this.id = id;
        this.userId = userId;
        this.challengeId = challengeId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.note = note;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}