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

    /**
     * Зарегистрировать пользователя на испытание
     */
    public boolean registerForChallenge(String userId, String username, String challengeName) {
        logger.info("Регистрация пользователя {} на испытание {}", username, challengeName);
        
        // В реальной реализации здесь будет обращение к Ignite
        return true;
    }

    /**
     * Отменить регистрацию пользователя на испытание
     */
    public boolean unregisterFromChallenge(String userId, String challengeName) {
        logger.info("Отмена регистрации пользователя {} на испытание {}", userId, challengeName);
        
        // В реальной реализации здесь будет обращение к Ignite
        return true;
    }

    /**
     * Получить информацию об участнике
     */
    public Participant getParticipant(String userId) {
        logger.info("Получение информации об участнике: {}", userId);
        
        // В реальной реализации здесь будет обращение к Ignite
        return null;
    }

    /**
     * Получить испытания, на которые зарегистрирован пользователь
     */
    public java.util.List<com.discord.challengebot.model.Challenge> getRegisteredChallenges(String userId) {
        logger.info("Получение зарегистрированных испытаний для пользователя: {}", userId);
        
        // В реальной реализации здесь будет обращение к Ignite
        return null;
    }

    /**
     * Проверить, является ли пользователь администратором
     */
    public boolean isAdminUser(String userId) {
        boolean isAdmin = discordConfig.getAdminUserId().equals(userId);
        logger.info("Проверка прав администратора для пользователя {}: {}", userId, isAdmin);
        return isAdmin;
    }
}