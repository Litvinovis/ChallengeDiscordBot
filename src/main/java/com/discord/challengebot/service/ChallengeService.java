package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
        try {
            logger.info("Создание нового испытания: {}", name);
            
            if (name == null || name.isEmpty()) {
                logger.warn("Попытка создать испытание с пустым именем");
                return null;
            }
            
            if (targetValue <= 0) {
                logger.warn("Попытка создать испытание с недопустимой целью: {}", targetValue);
                return null;
            }
            
            if (endDate == null) {
                logger.warn("Попытка создать испытание с пустой датой окончания");
                return null;
            }
            
            if (type == null) {
                logger.warn("Попытка создать испытание с пустым типом");
                return null;
            }
            
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
            
            logger.info("Испытание '{}' успешно создано", name);
            return challenge;
        } catch (Exception e) {
            logger.error("Ошибка при создании испытания: {}", name, e);
            return null;
        }
    }

    /**
     * Добавить прогресс к испытанию
     */
    public Challenge addProgress(Challenge challenge, String userId, String username, long amount) {
        try {
            // Получаем текущий прогресс пользователя до обновления
            long currentUserProgress = challenge.getParticipantProgress().getOrDefault(userId, 0L);
            logger.info("Добавление прогресса {} для пользователя {} в испытание {}. Текущий прогресс пользователя: {}", 
                       amount, username, challenge != null ? challenge.getName() : "null", currentUserProgress);
            
            if (challenge == null) {
                logger.warn("Попытка добавить прогресс к null испытанию");
                return null;
            }
            
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка добавить прогресс с пустым ID пользователя");
                return challenge;
            }
            
            if (username == null || username.isEmpty()) {
                logger.warn("Попытка добавить прогресс с пустым именем пользователя");
                return challenge;
            }
            
            if (amount < 0) {
                logger.warn("Попытка добавить отрицательный прогресс: {}", amount);
                return challenge;
            }
            
            // Обновляем общий прогресс
            challenge.setCurrentValue(challenge.getCurrentValue() + amount);
            
            // Обновляем прогресс участника
            long userProgress = challenge.getParticipantProgress().getOrDefault(userId, 0L);
            challenge.getParticipantProgress().put(userId, userProgress + amount);
            
            // Добавляем участника в список, если его там нет
            challenge.addParticipant(userId);
            
            // Сохраняем обновленное испытание
            dataStorageService.saveChallenge(challenge);
            
            // Получаем общий прогресс пользователя после обновления
            long updatedUserProgress = challenge.getParticipantProgress().getOrDefault(userId, 0L);
            logger.info("Прогресс успешно добавлен. Текущий общий прогресс: {}. Общий прогресс пользователя после обновления: {}", 
                       challenge.getCurrentValue(), updatedUserProgress);
            return challenge;
        } catch (Exception e) {
            logger.error("Ошибка при добавлении прогресса к испытанию: {}", 
                        challenge != null ? challenge.getName() : "null", e);
            return challenge;
        }
    }

    /**
     * Получить испытание по имени
     */
    public Challenge getChallenge(String name) {
        try {
            logger.debug("Получение испытания: {}", name);
            return dataStorageService.getChallenge(name);
        } catch (Exception e) {
            logger.error("Ошибка при получении испытания: {}", name, e);
            return null;
        }
    }

    /**
     * Получить все испытания
     */
    public List<Challenge> getAllChallenges() {
        try {
            logger.debug("Получение всех испытаний");
            return dataStorageService.getAllChallenges();
        } catch (Exception e) {
            logger.error("Ошибка при получении всех испытаний", e);
            return new ArrayList<>();
        }
    }

    /**
     * Получить все активные испытания
     */
    public List<Challenge> getActiveChallenges() {
        try {
            logger.debug("Получение всех активных испытаний");
            List<Challenge> allChallenges = getAllChallenges();
            List<Challenge> activeChallenges = allChallenges.stream()
                    .filter(Challenge::isActive)
                    .collect(Collectors.toList());
            logger.debug("Получено {} активных испытаний", activeChallenges.size());
            return activeChallenges;
        } catch (Exception e) {
            logger.error("Ошибка при получении активных испытаний", e);
            return new ArrayList<>();
        }
    }

    /**
     * Получить статистику по испытанию
     */
    public ChallengeStats getChallengeStats(Challenge challenge) {
        try {
            logger.debug("Расчет статистики для испытания: {}", challenge != null ? challenge.getName() : "null");
            
            if (challenge == null) {
                logger.warn("Попытка получить статистику для null испытания");
                return null;
            }
            
            long remaining = challenge.getTargetValue() - challenge.getCurrentValue();
            double percentage = challenge.getTargetValue() > 0 ? 
                               (double) challenge.getCurrentValue() / challenge.getTargetValue() * 100 : 0;
            
            // Расчет дней до окончания
            LocalDateTime now = LocalDateTime.now();
            long daysRemaining = java.time.Duration.between(now, challenge.getEndDate()).toDays();
            
            // Расчет ежедневной цели с распределением между участниками
            double dailyTarget = 0;
            if (daysRemaining > 0) {
                // Получаем количество участников
                int participantCount = challenge.getParticipants().size();
                
                // Если нет участников, распределяем на одного участника
                if (participantCount <= 0) {
                    participantCount = 1;
                }
                
                // Распределяем оставшуюся цель среди участников и делим на количество дней
                dailyTarget = (double) remaining / participantCount / daysRemaining;
            }
            
            ChallengeStats stats = new ChallengeStats(
                challenge.getName(),
                challenge.getTargetValue(),
                challenge.getCurrentValue(),
                remaining,
                percentage,
                dailyTarget,
                (int) daysRemaining
            );
            
            logger.debug("Статистика для испытания '{}' успешно рассчитана", challenge.getName());
            return stats;
        } catch (Exception e) {
            logger.error("Ошибка при расчете статистики для испытания: {}", 
                        challenge != null ? challenge.getName() : "null", e);
            return null;
        }
    }

    /**
     * Получить статистику по всем испытаниям
     */
    public Map<String, ChallengeStats> getAllChallengesStats() {
        try {
            logger.debug("Получение статистики по всем испытаниям");
            List<Challenge> challenges = getAllChallenges();
            Map<String, ChallengeStats> statsMap = new java.util.HashMap<>();
            for (Challenge challenge : challenges) {
                ChallengeStats stats = getChallengeStats(challenge);
                if (stats != null) {
                    statsMap.put(challenge.getName(), stats);
                }
            }
            logger.debug("Получена статистика по {} испытаниям", statsMap.size());
            return statsMap;
        } catch (Exception e) {
            logger.error("Ошибка при получении статистики по всем испытаниям", e);
            return new java.util.HashMap<>();
        }
    }

    /**
     * Получить испытания пользователя
     */
    public List<Challenge> getUserChallenges(String userId) {
        try {
            logger.debug("Получение испытаний пользователя: {}", userId);
            
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка получить испытания для пользователя с пустым ID");
                return new ArrayList<>();
            }
            
            List<Challenge> allChallenges = getAllChallenges();
            List<Challenge> userChallenges = allChallenges.stream()
                    .filter(challenge -> challenge.hasParticipant(userId))
                    .collect(Collectors.toList());
            
            logger.debug("Пользователь '{}' участвует в {} испытаниях", userId, userChallenges.size());
            return userChallenges;
        } catch (Exception e) {
            logger.error("Ошибка при получении испытаний пользователя: {}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Удалить испытание
     */
    public boolean deleteChallenge(String challengeName) {
        try {
            logger.info("Удаление испытания: {}", challengeName);
            return dataStorageService.deleteChallenge(challengeName);
        } catch (Exception e) {
            logger.error("Ошибка при удалении испытания: {}", challengeName, e);
            return false;
        }
    }

    /**
     * Обновить статус испытания
     */
    public Challenge updateChallengeStatus(Challenge challenge, boolean active) {
        try {
            logger.info("Обновление статуса испытания {}: {}", 
                       challenge != null ? challenge.getName() : "null", 
                       active ? "активно" : "остановлено");
            
            if (challenge == null) {
                logger.warn("Попытка обновить статус null испытания");
                return null;
            }
            
            challenge.setActive(active);
            dataStorageService.saveChallenge(challenge);
            
            logger.info("Статус испытания '{}' успешно обновлен", challenge.getName());
            return challenge;
        } catch (Exception e) {
            logger.error("Ошибка при обновлении статуса испытания: {}", 
                        challenge != null ? challenge.getName() : "null", e);
            return challenge;
        }
    }

    /**
     * Обновить цель испытания
     */
    public Challenge updateChallengeTarget(Challenge challenge, long newTarget) {
        try {
            logger.info("Обновление цели испытания {} с {} на {}", 
                       challenge != null ? challenge.getName() : "null", 
                       challenge != null ? challenge.getTargetValue() : 0, 
                       newTarget);
            
            if (challenge == null) {
                logger.warn("Попытка обновить цель null испытания");
                return null;
            }
            
            if (newTarget <= 0) {
                logger.warn("Попытка установить недопустимую цель: {}", newTarget);
                return challenge;
            }
            
            challenge.setTargetValue(newTarget);
            dataStorageService.saveChallenge(challenge);
            
            logger.info("Цель испытания '{}' успешно обновлена", challenge.getName());
            return challenge;
        } catch (Exception e) {
            logger.error("Ошибка при обновлении цели испытания: {}", 
                        challenge != null ? challenge.getName() : "null", e);
            return challenge;
        }
    }

    /**
     * Обновить дату окончания испытания
     */
    public Challenge updateChallengeEndDate(Challenge challenge, LocalDateTime newEndDate) {
        try {
            logger.info("Обновление даты окончания испытания {} с {} на {}", 
                       challenge != null ? challenge.getName() : "null", 
                       challenge != null ? challenge.getEndDate() : null, 
                       newEndDate);
            
            if (challenge == null) {
                logger.warn("Попытка обновить дату окончания null испытания");
                return null;
            }
            
            if (newEndDate == null) {
                logger.warn("Попытка установить пустую дату окончания");
                return challenge;
            }
            
            challenge.setEndDate(newEndDate);
            dataStorageService.saveChallenge(challenge);
            
            logger.info("Дата окончания испытания '{}' успешно обновлена", challenge.getName());
            return challenge;
        } catch (Exception e) {
            logger.error("Ошибка при обновлении даты окончания испытания: {}", 
                        challenge != null ? challenge.getName() : "null", e);
            return challenge;
        }
    }

    /**
     * Установить прогресс участника в испытании
     */
    public Challenge setParticipantProgress(Challenge challenge, String userId, long progress) {
        try {
            // Получаем текущий прогресс пользователя до обновления
            long currentUserProgress = challenge.getParticipantProgress().getOrDefault(userId, 0L);
            logger.info("Установка прогресса {} для пользователя {} в испытании {}. Текущий прогресс пользователя: {}", 
                       progress, userId, challenge != null ? challenge.getName() : "null", currentUserProgress);
            
            if (challenge == null) {
                logger.warn("Попытка установить прогресс для null испытания");
                return null;
            }
            
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка установить прогресс для пользователя с пустым ID");
                return challenge;
            }
            
            if (progress < 0) {
                logger.warn("Попытка установить отрицательный прогресс: {}", progress);
                return challenge;
            }
            
            // Устанавливаем прогресс участника
            challenge.getParticipantProgress().put(userId, progress);
            
            // Добавляем участника в список, если его там нет
            challenge.addParticipant(userId);
            
            // Пересчитываем общий прогресс
            long totalProgress = challenge.getParticipantProgress().values().stream().mapToLong(Long::longValue).sum();
            challenge.setCurrentValue(totalProgress);
            
            // Сохраняем обновленное испытание
            dataStorageService.saveChallenge(challenge);
            
            logger.info("Прогресс участника '{}' в испытании '{}' успешно установлен. Общий прогресс после обновления: {}", 
                       userId, challenge.getName(), totalProgress);
            return challenge;
        } catch (Exception e) {
            logger.error("Ошибка при установке прогресса участника '{}' в испытании '{}'", 
                        userId, challenge != null ? challenge.getName() : "null", e);
            return challenge;
        }
    }

    /**
     * Удалить участника из испытания
     */
    public Challenge removeParticipant(Challenge challenge, String userId) {
        try {
            logger.info("Удаление участника {} из испытания {}", userId, challenge != null ? challenge.getName() : "null");
            
            if (challenge == null) {
                logger.warn("Попытка удалить участника из null испытания");
                return null;
            }
            
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка удалить участника с пустым ID");
                return challenge;
            }
            
            // Удаляем прогресс участника
            challenge.getParticipantProgress().remove(userId);
            
            // Удаляем участника из списка
            challenge.removeParticipant(userId);
            
            // Пересчитываем общий прогресс
            long totalProgress = challenge.getParticipantProgress().values().stream().mapToLong(Long::longValue).sum();
            challenge.setCurrentValue(totalProgress);
            
            // Сохраняем обновленное испытание
            dataStorageService.saveChallenge(challenge);
            
            logger.info("Участник '{}' успешно удален из испытания '{}'", userId, challenge.getName());
            return challenge;
        } catch (Exception e) {
            logger.error("Ошибка при удалении участника '{}' из испытания '{}'", 
                        userId, challenge != null ? challenge.getName() : "null", e);
            return challenge;
        }
    }

    /**
     * Добавить участника в испытание
     */
    public Challenge addParticipant(Challenge challenge, String userId) {
        try {
            logger.info("Добавление участника {} в испытание {}", userId, challenge != null ? challenge.getName() : "null");
            
            if (challenge == null) {
                logger.warn("Попытка добавить участника в null испытание");
                return null;
            }
            
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка добавить участника с пустым ID");
                return challenge;
            }
            
            // Добавляем участника в список
            challenge.addParticipant(userId);
            
            // Если у участника еще нет прогресса, устанавливаем 0
            if (!challenge.getParticipantProgress().containsKey(userId)) {
                challenge.getParticipantProgress().put(userId, 0L);
            }
            
            // Сохраняем обновленное испытание
            dataStorageService.saveChallenge(challenge);
            
            logger.info("Участник '{}' успешно добавлен в испытание '{}'", userId, challenge.getName());
            return challenge;
        } catch (Exception e) {
            logger.error("Ошибка при добавлении участника '{}' в испытание '{}'", 
                        userId, challenge != null ? challenge.getName() : "null", e);
            return challenge;
        }
    }

    /**
     * Получить топ участников по прогрессу в испытании
     */
    public List<Map.Entry<String, Long>> getTopParticipants(Challenge challenge, int limit) {
        try {
            logger.debug("Получение топ-{} участников по испытанию {}", limit, challenge != null ? challenge.getName() : "null");
            
            if (challenge == null) {
                logger.warn("Попытка получить топ участников для null испытания");
                return new ArrayList<>();
            }
            
            if (limit <= 0) {
                logger.warn("Попытка получить топ с недопустимым лимитом: {}", limit);
                return new ArrayList<>();
            }
            
            List<Map.Entry<String, Long>> topParticipants = challenge.getParticipantProgress().entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
            
            logger.debug("Получено {} топ участников для испытания '{}'", topParticipants.size(), challenge.getName());
            return topParticipants;
        } catch (Exception e) {
            logger.error("Ошибка при получении топ участников по испытанию: {}", 
                        challenge != null ? challenge.getName() : "null", e);
            return new ArrayList<>();
        }
    }

    /**
     * Завершить испытание и отправить уведомление
     */
    public void completeChallenge(Challenge challenge) {
        try {
            logger.info("Завершение испытания: {}", challenge != null ? challenge.getName() : "null");
            
            if (challenge == null) {
                logger.warn("Попытка завершить null испытание");
                return;
            }
            
            challenge.setActive(false);
            dataStorageService.saveChallenge(challenge);
            
            logger.info("Испытание '{}' успешно завершено", challenge.getName());
        } catch (Exception e) {
            logger.error("Ошибка при завершении испытания: {}", 
                        challenge != null ? challenge.getName() : "null", e);
        }
    }
}