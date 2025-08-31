package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для управления испытаниями
 */
@Service
public class ChallengeService {
    private static final Logger logger = LoggerFactory.getLogger(ChallengeService.class);
    
    @Autowired
    private DataStorageService dataStorageService;

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
        
        // Сохраняем испытание
        dataStorageService.saveChallenge(challenge);
        
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
        
        // Добавляем участника в список, если его там нет
        challenge.addParticipant(userId);
        
        // Сохраняем обновленное испытание
        dataStorageService.saveChallenge(challenge);
        
        logger.info("Прогресс успешно добавлен. Текущий общий прогресс: {}", challenge.getCurrentValue());
        return challenge;
    }

    /**
     * Получить испытание по имени
     */
    public Challenge getChallenge(String name) {
        return dataStorageService.getChallenge(name);
    }

    /**
     * Получить все испытания
     */
    public List<Challenge> getAllChallenges() {
        return dataStorageService.getAllChallenges();
    }

    /**
     * Получить статистику по испытанию
     */
    public ChallengeStats getChallengeStats(Challenge challenge) {
        logger.info("Расчет статистики для испытания: {}", challenge.getName());
        
        long remaining = challenge.getTargetValue() - challenge.getCurrentValue();
        double percentage = challenge.getTargetValue() > 0 ? 
                           (double) challenge.getCurrentValue() / challenge.getTargetValue() * 100 : 0;
        
        // Расчет дней до окончания
        LocalDateTime now = LocalDateTime.now();
        long daysRemaining = java.time.Duration.between(now, challenge.getEndDate()).toDays();
        double dailyTarget = daysRemaining > 0 ? (double) remaining / daysRemaining : 0;
        
        return new ChallengeStats(
            challenge.getName(),
            challenge.getTargetValue(),
            challenge.getCurrentValue(),
            remaining,
            percentage,
            dailyTarget,
            (int) daysRemaining
        );
    }

    /**
     * Получить статистику по всем испытаниям
     */
    public Map<String, ChallengeStats> getAllChallengesStats() {
        List<Challenge> challenges = getAllChallenges();
        new java.util.HashMap<String, ChallengeStats>();
        for (Challenge challenge : challenges) {
            ChallengeStats stats = getChallengeStats(challenge);
        }
        // В реальной реализации здесь будет обращение к Ignite
        logger.info("Получение статистики по всем испытаниям");
        return new java.util.HashMap<>();
    }

    /**
     * Получить испытания пользователя
     */
    public List<Challenge> getUserChallenges(String userId) {
        List<Challenge> allChallenges = getAllChallenges();
        return allChallenges.stream()
                .filter(challenge -> challenge.hasParticipant(userId))
                .collect(Collectors.toList());
    }

    /**
     * Удалить испытание
     */
    public boolean deleteChallenge(String challengeName) {
        logger.info("Удаление испытания: {}", challengeName);
        return dataStorageService.deleteChallenge(challengeName);
    }

    /**
     * Обновить статус испытания
     */
    public Challenge updateChallengeStatus(Challenge challenge, boolean active) {
        logger.info("Обновление статуса испытания {}: {}", challenge.getName(), active ? "активно" : "остановлено");
        challenge.setActive(active);
        dataStorageService.saveChallenge(challenge);
        return challenge;
    }

    /**
     * Обновить цель испытания
     */
    public Challenge updateChallengeTarget(Challenge challenge, long newTarget) {
        logger.info("Обновление цели испытания {} с {} на {}", 
                   challenge.getName(), challenge.getTargetValue(), newTarget);
        challenge.setTargetValue(newTarget);
        dataStorageService.saveChallenge(challenge);
        return challenge;
    }

    /**
     * Установить прогресс участника в испытании
     */
    public Challenge setParticipantProgress(Challenge challenge, String userId, long progress) {
        logger.info("Установка прогресса {} для пользователя {} в испытании {}", 
                   progress, userId, challenge.getName());
        
        // Устанавливаем прогресс участника
        challenge.getParticipantProgress().put(userId, progress);
        
        // Добавляем участника в список, если его там нет
        challenge.addParticipant(userId);
        
        // Пересчитываем общий прогресс
        long totalProgress = challenge.getParticipantProgress().values().stream().mapToLong(Long::longValue).sum();
        challenge.setCurrentValue(totalProgress);
        
        // Сохраняем обновленное испытание
        dataStorageService.saveChallenge(challenge);
        
        return challenge;
    }

    /**
     * Удалить участника из испытания
     */
    public Challenge removeParticipant(Challenge challenge, String userId) {
        logger.info("Удаление участника {} из испытания {}", userId, challenge.getName());
        
        // Удаляем прогресс участника
        challenge.getParticipantProgress().remove(userId);
        
        // Удаляем участника из списка
        challenge.removeParticipant(userId);
        
        // Пересчитываем общий прогресс
        long totalProgress = challenge.getParticipantProgress().values().stream().mapToLong(Long::longValue).sum();
        challenge.setCurrentValue(totalProgress);
        
        // Сохраняем обновленное испытание
        dataStorageService.saveChallenge(challenge);
        
        return challenge;
    }

    /**
     * Добавить участника в испытание
     */
    public Challenge addParticipant(Challenge challenge, String userId) {
        logger.info("Добавление участника {} в испытание {}", userId, challenge.getName());
        
        // Добавляем участника в список
        challenge.addParticipant(userId);
        
        // Если у участника еще нет прогресса, устанавливаем 0
        if (!challenge.getParticipantProgress().containsKey(userId)) {
            challenge.getParticipantProgress().put(userId, 0L);
        }
        
        // Сохраняем обновленное испытание
        dataStorageService.saveChallenge(challenge);
        
        return challenge;
    }

    /**
     * Получить топ участников по прогрессу в испытании
     */
    public List<Map.Entry<String, Long>> getTopParticipants(Challenge challenge, int limit) {
        return challenge.getParticipantProgress().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Завершить испытание и отправить уведомление
     */
    public void completeChallenge(Challenge challenge) {
        logger.info("Завершение испытания: {}", challenge.getName());
        challenge.setActive(false);
        dataStorageService.saveChallenge(challenge);
        // Отправка уведомления будет выполнена в другом сервисе
    }
}