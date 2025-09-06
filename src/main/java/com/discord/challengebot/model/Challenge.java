package com.discord.challengebot.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

/**
 * Модель испытания
 */
public class Challenge implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(Challenge.class);
    
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
        logger.debug("Создан новый экземпляр Challenge");
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
        logger.debug("Создан новый экземпляр Challenge с параметрами: id={}, name={}", id, name);
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        logger.debug("Установка ID испытания: {}", id);
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        logger.debug("Установка названия испытания: {}", name);
        this.name = name;
    }

    public long getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(long targetValue) {
        logger.debug("Установка целевого значения испытания '{}': {}", name, targetValue);
        this.targetValue = targetValue;
    }

    public long getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(long currentValue) {
        logger.debug("Установка текущего значения испытания '{}': {}", name, currentValue);
        this.currentValue = currentValue;
    }

    public ChallengeType getType() {
        return type;
    }

    public void setType(ChallengeType type) {
        logger.debug("Установка типа испытания '{}': {}", name, type);
        this.type = type;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        logger.debug("Установка даты начала испытания '{}': {}", name, startDate);
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        logger.debug("Установка даты окончания испытания '{}': {}", name, endDate);
        this.endDate = endDate;
    }

    public Map<String, Long> getParticipantProgress() {
        return participantProgress;
    }

    public void setParticipantProgress(Map<String, Long> participantProgress) {
        logger.debug("Установка прогресса участников испытания '{}'", name);
        this.participantProgress = participantProgress;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        logger.debug("Установка статуса активности испытания '{}': {}", name, active);
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        logger.debug("Установка описания испытания '{}'", name);
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        logger.debug("Установка единицы измерения испытания '{}': {}", name, unit);
        this.unit = unit;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        logger.debug("Установка списка участников испытания '{}'", name);
        this.participants = participants;
    }

    // Helper methods for participant management
    public void addParticipant(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка добавить участника с пустым ID в испытание '{}'", name);
                return;
            }
            
            if (!participants.contains(userId)) {
                participants.add(userId);
                logger.debug("Участник '{}' добавлен в испытание '{}'", userId, name);
            } else {
                logger.debug("Участник '{}' уже присутствует в испытании '{}'", userId, name);
            }
        } catch (Exception e) {
            logger.error("Ошибка при добавлении участника '{}' в испытание '{}'", userId, name, e);
        }
    }

    public void removeParticipant(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка удалить участника с пустым ID из испытания '{}'", name);
                return;
            }
            
            if (participants.remove(userId)) {
                logger.debug("Участник '{}' удален из испытания '{}'", userId, name);
            } else {
                logger.debug("Участник '{}' не найден в испытании '{}'", userId, name);
            }
        } catch (Exception e) {
            logger.error("Ошибка при удалении участника '{}' из испытания '{}'", userId, name, e);
        }
    }

    public boolean hasParticipant(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка проверить наличие участника с пустым ID в испытании '{}'", name);
                return false;
            }
            
            boolean hasParticipant = participants.contains(userId);
            logger.debug("Проверка наличия участника '{}' в испытании '{}': {}", userId, name, hasParticipant);
            return hasParticipant;
        } catch (Exception e) {
            logger.error("Ошибка при проверке наличия участника '{}' в испытании '{}'", userId, name, e);
            return false;
        }
    }
}