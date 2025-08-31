package com.discord.challengebot.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Модель испытания
 */
public class Challenge {
    private String id;
    private String name;
    private long targetValue;
    private long currentValue;
    private ChallengeType type;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Map<String, Long> participantProgress;
    private boolean active;
    private String description;
    private String unit;
    private List<String> participants; // List of participant user IDs

    public Challenge() {
        this.participantProgress = new HashMap<>();
        this.participants = new ArrayList<>();
    }

    public Challenge(String id, String name, long targetValue, ChallengeType type, 
                     LocalDateTime startDate, LocalDateTime endDate, String description, String unit) {
        this.id = id;
        this.name = name;
        this.targetValue = targetValue;
        this.currentValue = 0;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.participantProgress = new HashMap<>();
        this.active = true;
        this.description = description;
        this.unit = unit;
        this.participants = new ArrayList<>();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(long targetValue) {
        this.targetValue = targetValue;
    }

    public long getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(long currentValue) {
        this.currentValue = currentValue;
    }

    public ChallengeType getType() {
        return type;
    }

    public void setType(ChallengeType type) {
        this.type = type;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Map<String, Long> getParticipantProgress() {
        return participantProgress;
    }

    public void setParticipantProgress(Map<String, Long> participantProgress) {
        this.participantProgress = participantProgress;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    // Helper methods for participant management
    public void addParticipant(String userId) {
        if (!participants.contains(userId)) {
            participants.add(userId);
        }
    }

    public void removeParticipant(String userId) {
        participants.remove(userId);
    }

    public boolean hasParticipant(String userId) {
        return participants.contains(userId);
    }
}