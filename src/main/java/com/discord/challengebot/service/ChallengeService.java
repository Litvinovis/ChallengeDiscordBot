package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Сервис для управления испытаниями
 */
@Service
public class ChallengeService {
    private static final Logger logger = LoggerFactory.getLogger(ChallengeService.class);

    /**
     * Создать новое испытание
     */
    public Challenge createChallenge(String name, long targetValue, LocalDateTime endDate, 
                                   ChallengeType type, String description, String unit) {
        logger.info("Создание нового испытания: {}", name);
        
        Challenge challenge = new Challenge();
        challenge.setId(name.toLowerCase().replace(" ", "_"));
        challenge.setName(name);
        challenge.setTargetValue(targetValue);
        challenge.setCurrentValue(0);
        challenge.setType(type);
        challenge.setStartDate(LocalDateTime.now());
        challenge.setEndDate(endDate);
        challenge.setActive(true);
        challenge.setDescription(description);
        challenge.setUnit(unit);
        
        logger.info("Испытание {} успешно создано", name);
        return challenge;
    }

    /**
     * Добавить прогресс к испытанию
     */
    public Challenge addProgress(Challenge challenge, String userId, String username, long amount) {
        logger.info("Добавление прогресса {} для пользователя {} в испытание {}", 
                   amount, username, challenge.getName());
        
        // Обновляем общий прогресс
        challenge.setCurrentValue(challenge.getCurrentValue() + amount);
        
        // Обновляем прогресс участника
        long userProgress = challenge.getParticipantProgress().getOrDefault(userId, 0L);
        challenge.getParticipantProgress().put(userId, userProgress + amount);
        
        logger.info("Прогресс успешно добавлен. Текущий общий прогресс: {}", challenge.getCurrentValue());
        return challenge;
    }

    /**
     * Получить испытание по имени
     */
    public Challenge getChallenge(String name) {
        // В реальной реализации здесь будет обращение к Ignite
        logger.info("Получение испытания: {}", name);
        return null;
    }

    /**
     * Получить все испытания
     */
    public List<Challenge> getAllChallenges() {
        // В реальной реализации здесь будет обращение к Ignite
        logger.info("Получение всех испытаний");
        return null;
    }

    /**
     * Получить статистику по испытанию
     */
    public ChallengeStats getChallengeStats(Challenge challenge) {
        logger.info("Расчет статистики для испытания: {}", challenge.getName());
        
        long remaining = challenge.getTargetValue() - challenge.getCurrentValue();
        double percentage = challenge.getTargetValue() > 0 ? 
                           (double) challenge.getCurrentValue() / challenge.getTargetValue() * 100 : 0;
        
        // В реальной реализации здесь будет расчет дней до окончания
        int daysRemaining = 10;
        double dailyTarget = daysRemaining > 0 ? (double) remaining / daysRemaining : 0;
        
        return new ChallengeStats(
            challenge.getName(),
            challenge.getTargetValue(),
            challenge.getCurrentValue(),
            remaining,
            percentage,
            dailyTarget,
            daysRemaining
        );
    }

    /**
     * Получить статистику по всем испытаниям
     */
    public Map<String, ChallengeStats> getAllChallengesStats() {
        // В реальной реализации здесь будет обращение к Ignite
        logger.info("Получение статистики по всем испытаниям");
        return null;
    }

    /**
     * Получить испытания пользователя
     */
    public List<Challenge> getUserChallenges(String userId) {
        // В реальной реализации здесь будет обращение к Ignite
        logger.info("Получение испытаний пользователя: {}", userId);
        return null;
    }

    /**
     * Удалить испытание
     */
    public boolean deleteChallenge(String challengeName) {
        logger.info("Удаление испытания: {}", challengeName);
        // В реальной реализации здесь будет обращение к Ignite
        return true;
    }

    /**
     * Обновить статус испытания
     */
    public Challenge updateChallengeStatus(Challenge challenge, boolean active) {
        logger.info("Обновление статуса испытания {}: {}", challenge.getName(), active ? "активно" : "остановлено");
        challenge.setActive(active);
        return challenge;
    }

    /**
     * Обновить цель испытания
     */
    public Challenge updateChallengeTarget(Challenge challenge, long newTarget) {
        logger.info("Обновление цели испытания {} с {} на {}", 
                   challenge.getName(), challenge.getTargetValue(), newTarget);
        challenge.setTargetValue(newTarget);
        return challenge;
    }
}