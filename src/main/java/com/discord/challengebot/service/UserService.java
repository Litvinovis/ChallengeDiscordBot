package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Participant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Сервис для управления пользователями
 */
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private DiscordConfig discordConfig;
    @Autowired
    private DataStorageService dataStorageService;

    /**
     * Зарегистрировать пользователя на испытание
     */
    public boolean registerForChallenge(String userId, String username, String challengeName) {
        try {
            logger.info("Регистрация пользователя {} на испытание {}", username, challengeName);
            
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка регистрации с пустым ID пользователя");
                return false;
            }
            
            if (username == null || username.isEmpty()) {
                logger.warn("Попытка регистрации с пустым именем пользователя");
                return false;
            }
            
            if (challengeName == null || challengeName.isEmpty()) {
                logger.warn("Попытка регистрации на испытание с пустым названием");
                return false;
            }
            
            // Получаем информацию об участнике или создаем новую
            Participant participant = dataStorageService.getParticipant(userId);
            if (participant == null) {
                participant = new Participant(userId, username);
            }
            
            // Добавляем испытание в список участника
            participant.addChallenge(challengeName);
            
            // Сохраняем обновленную информацию об участнике
            dataStorageService.saveParticipant(participant);
            
            logger.info("Пользователь {} успешно зарегистрирован на испытание {}", username, challengeName);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при регистрации пользователя {} на испытание {}", username, challengeName, e);
            return false;
        }
    }

    /**
     * Отменить регистрацию пользователя на испытание
     */
    public boolean unregisterFromChallenge(String userId, String challengeName) {
        try {
            logger.info("Отмена регистрации пользователя {} на испытание {}", userId, challengeName);
            
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка отмены регистрации с пустым ID пользователя");
                return false;
            }
            
            if (challengeName == null || challengeName.isEmpty()) {
                logger.warn("Попытка отмены регистрации на испытание с пустым названием");
                return false;
            }
            
            // Получаем информацию об участнике
            Participant participant = dataStorageService.getParticipant(userId);
            if (participant == null) {
                logger.warn("Участник с ID {} не найден", userId);
                return false;
            }
            
            // Удаляем испытание из списка участника
            participant.removeChallenge(challengeName);
            
            // Сохраняем обновленную информацию об участнике
            dataStorageService.saveParticipant(participant);
            
            logger.info("Регистрация пользователя {} на испытание {} успешно отменена", userId, challengeName);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при отмене регистрации пользователя {} на испытание {}", userId, challengeName, e);
            return false;
        }
    }

    /**
     * Получить информацию об участнике
     */
    public Participant getParticipant(String userId) {
        try {
            logger.info("Получение информации об участнике: {}", userId);
            
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка получить информацию об участнике с пустым ID");
                return null;
            }
            
            Participant participant = dataStorageService.getParticipant(userId);
            
            if (participant != null) {
                logger.debug("Информация об участнике {} успешно получена", userId);
            } else {
                logger.debug("Информация об участнике {} не найдена", userId);
            }
            
            return participant;
        } catch (Exception e) {
            logger.error("Ошибка при получении информации об участнике: {}", userId, e);
            return null;
        }
    }

    /**
     * Получить испытания, на которые зарегистрирован пользователь
     */
    public java.util.List<com.discord.challengebot.model.Challenge> getRegisteredChallenges(String userId) {
        try {
            logger.info("Получение зарегистрированных испытаний для пользователя: {}", userId);
            
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка получить зарегистрированные испытания для пользователя с пустым ID");
                return new java.util.ArrayList<>();
            }
            
            // Получаем информацию об участнике
            Participant participant = dataStorageService.getParticipant(userId);
            if (participant == null) {
                logger.debug("Участник с ID {} не найден", userId);
                return new java.util.ArrayList<>();
            }
            
            // Получаем все испытания
            java.util.List<com.discord.challengebot.model.Challenge> allChallenges = dataStorageService.getAllChallenges();
            
            // Фильтруем только те испытания, на которые зарегистрирован пользователь
            java.util.List<com.discord.challengebot.model.Challenge> registeredChallenges = new java.util.ArrayList<>();
            for (com.discord.challengebot.model.Challenge challenge : allChallenges) {
                if (participant.isRegisteredForChallenge(challenge.getName())) {
                    registeredChallenges.add(challenge);
                }
            }
            
            logger.debug("Получено {} зарегистрированных испытаний для пользователя {}", registeredChallenges.size(), userId);
            return registeredChallenges;
        } catch (Exception e) {
            logger.error("Ошибка при получении зарегистрированных испытаний для пользователя: {}", userId, e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Проверить, является ли пользователь администратором
     */
    public boolean isAdminUser(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка проверки прав администратора для пользователя с пустым ID");
                return false;
            }
            
            boolean isAdmin = discordConfig.getAdminUserId().equals(userId);
            logger.debug("Проверка прав администратора для пользователя {}: {}", userId, isAdmin);
            return isAdmin;
        } catch (Exception e) {
            logger.error("Ошибка при проверке прав администратора для пользователя: {}", userId, e);
            return false;
        }
    }
}