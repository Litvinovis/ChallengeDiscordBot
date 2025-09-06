package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.cache.Cache;
import java.util.*;

/**
 * Сервис для работы с хранилищем данных (Apache Ignite)
 */
@Service
public class DataStorageService {
    private static final Logger logger = LoggerFactory.getLogger(DataStorageService.class);
    
    @Value("${ignite.addresses:127.0.0.1:11800}")
    private List<String> igniteAddresses;
    
    @Value("${ignite.client-mode:true}")
    private boolean clientMode;
    
    private Ignite ignite;
    private IgniteCache<String, Challenge> challengesCache;
    private IgniteCache<String, Participant> participantsCache;
    private boolean isTestMode = false;
    
    /**
     * Инициализация подключения к Apache Ignite
     */
    @PostConstruct
    public void init() {
        try {
            logger.info("Инициализация подключения к Apache Ignite");
            
            // Проверяем, запущены ли тесты
            if (isTestMode) {
                logger.info("Работаем в тестовом режиме с embedded Ignite");
                // Используем встроенный режим для тестов
                IgniteConfiguration cfg = new IgniteConfiguration();
                cfg.setClientMode(false); // Режим сервера для тестов
                cfg.setIgniteInstanceName("test-ignite-instance");
                
                ignite = Ignition.start(cfg);
            } else {
                // Инициализация клиента Apache Ignite для production
                IgniteConfiguration cfg = new IgniteConfiguration();
                cfg.setClientMode(clientMode);
                
                // Настройка адресов серверов (из application.yml)
                cfg.setDiscoverySpi(new org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi()
                    .setIpFinder(new org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder()
                        .setAddresses(igniteAddresses)));
                
                ignite = Ignition.start(cfg);
            }
            
            // Создание или получение кэша для испытаний
            CacheConfiguration<String, Challenge> challengeCacheCfg = new CacheConfiguration<>("challenges");
            challengeCacheCfg.setCacheMode(CacheMode.PARTITIONED);
            challengesCache = ignite.getOrCreateCache(challengeCacheCfg);
            
            // Создание или получение кэша для участников
            CacheConfiguration<String, Participant> participantCacheCfg = new CacheConfiguration<>("participants");
            participantCacheCfg.setCacheMode(CacheMode.PARTITIONED);
            participantsCache = ignite.getOrCreateCache(participantCacheCfg);
            
            logger.info("Подключение к Apache Ignite успешно инициализировано");
        } catch (Exception e) {
            logger.error("Ошибка инициализации подключения к Apache Ignite", e);
            // Не прерываем инициализацию приложения из-за ошибки Ignite
        }
    }
    
    /**
     * Установить режим тестирования
     */
    public void setTestMode(boolean testMode) {
        this.isTestMode = testMode;
    }
    
    /**
     * Закрытие подключения к Apache Ignite
     */
    @PreDestroy
    public void destroy() {
        try {
            if (ignite != null) {
                logger.info("Закрытие подключения к Apache Ignite");
                // Безопасное закрытие Ignite
                try {
                    ignite.close();
                } catch (Exception e) {
                    logger.warn("Ошибка при закрытии подключения к Apache Ignite (игнорируется)", e);
                }
                logger.info("Подключение к Apache Ignite успешно закрыто");
            }
        } catch (Exception e) {
            logger.error("Ошибка при закрытии подключения к Apache Ignite", e);
        }
    }
    
    /**
     * Сохранить испытание
     */
    public void saveChallenge(Challenge challenge) {
        try {
            // Проверяем, что сервис инициализирован
            if (challengesCache == null) {
                logger.warn("Кэш испытаний не инициализирован, пропускаем сохранение");
                return;
            }
            
            logger.debug("Сохранение испытания в Apache Ignite: {}", challenge != null ? challenge.getName() : "null");
            if (challenge == null) {
                logger.warn("Попытка сохранить null испытание");
                return;
            }
            
            // Сохранение испытания в кэш Ignite
            challengesCache.put(challenge.getId(), challenge);
            
            logger.info("Испытание '{}' успешно сохранено в Apache Ignite", challenge.getName());
        } catch (Exception e) {
            logger.error("Ошибка при сохранении испытания в Apache Ignite: {}", challenge != null ? challenge.getName() : "null", e);
        }
    }
    
    /**
     * Получить испытание по имени
     */
    public Challenge getChallenge(String name) {
        try {
            // Проверяем, что сервис инициализирован
            if (challengesCache == null) {
                logger.warn("Кэш испытаний не инициализирован, возвращаем null");
                return null;
            }
            
            logger.debug("Получение испытания из Apache Ignite: {}", name);
            if (name == null || name.isEmpty()) {
                logger.warn("Попытка получить испытание с пустым именем");
                return null;
            }
            
            // Поиск испытания по имени в кэше Ignite
            for (javax.cache.Cache.Entry<String, Challenge> entry : challengesCache) {
                Challenge challenge = entry.getValue();
                if (name.equals(challenge.getName())) {
                    logger.debug("Испытание '{}' успешно получено из Apache Ignite", name);
                    return challenge;
                }
            }
            
            logger.debug("Испытание '{}' не найдено в Apache Ignite", name);
            return null;
        } catch (Exception e) {
            logger.error("Ошибка при получении испытания из Apache Ignite: {}", name, e);
            return null;
        }
    }
    
    /**
     * Получить все испытания
     */
    public List<Challenge> getAllChallenges() {
        try {
            // Проверяем, что сервис инициализирован
            if (challengesCache == null) {
                logger.warn("Кэш испытаний не инициализирован, возвращаем пустой список");
                return new ArrayList<>();
            }
            
            logger.debug("Получение всех испытаний из Apache Ignite");
            
            // Получение всех испытаний из кэша Ignite
            List<Challenge> challenges = new ArrayList<>();
            for (javax.cache.Cache.Entry<String, Challenge> entry : challengesCache) {
                challenges.add(entry.getValue());
            }
            
            logger.debug("Получено {} испытаний из Apache Ignite", challenges.size());
            return challenges;
        } catch (Exception e) {
            logger.error("Ошибка при получении всех испытаний из Apache Ignite", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Удалить испытание
     */
    public boolean deleteChallenge(String challengeName) {
        try {
            // Проверяем, что сервис инициализирован
            if (challengesCache == null) {
                logger.warn("Кэш испытаний не инициализирован, возвращаем false");
                return false;
            }
            
            logger.debug("Удаление испытания из Apache Ignite: {}", challengeName);
            if (challengeName == null || challengeName.isEmpty()) {
                logger.warn("Попытка удалить испытание с пустым именем");
                return false;
            }
            
            // Поиск и удаление испытания по имени
            for (javax.cache.Cache.Entry<String, Challenge> entry : challengesCache) {
                Challenge challenge = entry.getValue();
                if (challengeName.equals(challenge.getName())) {
                    challengesCache.remove(entry.getKey());
                    logger.info("Испытание '{}' успешно удалено из Apache Ignite", challengeName);
                    return true;
                }
            }
            
            logger.warn("Испытание '{}' не найдено для удаления в Apache Ignite", challengeName);
            return false;
        } catch (Exception e) {
            logger.error("Ошибка при удалении испытания из Apache Ignite: {}", challengeName, e);
            return false;
        }
    }
    
    /**
     * Сохранить участника
     */
    public void saveParticipant(Participant participant) {
        try {
            // Проверяем, что сервис инициализирован
            if (participantsCache == null) {
                logger.warn("Кэш участников не инициализирован, пропускаем сохранение");
                return;
            }
            
            logger.debug("Сохранение участника в Apache Ignite: {}", participant != null ? participant.getUsername() : "null");
            if (participant == null) {
                logger.warn("Попытка сохранить null участника");
                return;
            }
            
            // Сохранение участника в кэш Ignite
            participantsCache.put(participant.getUserId(), participant);
            
            logger.info("Участник '{}' успешно сохранен в Apache Ignite", participant.getUsername());
        } catch (Exception e) {
            logger.error("Ошибка при сохранении участника в Apache Ignite: {}", participant != null ? participant.getUsername() : "null", e);
        }
    }
    
    /**
     * Получить участника по ID
     */
    public Participant getParticipant(String userId) {
        try {
            // Проверяем, что сервис инициализирован
            if (participantsCache == null) {
                logger.warn("Кэш участников не инициализирован, возвращаем null");
                return null;
            }
            
            logger.debug("Получение участника из Apache Ignite по ID: {}", userId);
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка получить участника с пустым ID");
                return null;
            }
            
            // Получение участника из кэша Ignite
            Participant participant = participantsCache.get(userId);
            
            if (participant != null) {
                logger.debug("Участник '{}' успешно получен из Apache Ignite", userId);
            } else {
                logger.debug("Участник с ID '{}' не найден в Apache Ignite", userId);
            }
            
            return participant;
        } catch (Exception e) {
            logger.error("Ошибка при получении участника из Apache Ignite по ID: {}", userId, e);
            return null;
        }
    }
    
    /**
     * Получить всех участников
     */
    public List<Participant> getAllParticipants() {
        try {
            // Проверяем, что сервис инициализирован
            if (participantsCache == null) {
                logger.warn("Кэш участников не инициализирован, возвращаем пустой список");
                return new ArrayList<>();
            }
            
            logger.debug("Получение всех участников из Apache Ignite");
            
            // Получение всех участников из кэша Ignite
            List<Participant> participants = new ArrayList<>();
            for (javax.cache.Cache.Entry<String, Participant> entry : participantsCache) {
                participants.add(entry.getValue());
            }
            
            logger.debug("Получено {} участников из Apache Ignite", participants.size());
            return participants;
        } catch (Exception e) {
            logger.error("Ошибка при получении всех участников из Apache Ignite", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Удалить участника
     */
    public boolean deleteParticipant(String userId) {
        try {
            // Проверяем, что сервис инициализирован
            if (participantsCache == null) {
                logger.warn("Кэш участников не инициализирован, возвращаем false");
                return false;
            }
            
            logger.debug("Удаление участника из Apache Ignite по ID: {}", userId);
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка удалить участника с пустым ID");
                return false;
            }
            
            // Удаление участника из кэша Ignite
            boolean removed = participantsCache.remove(userId);
            
            if (removed) {
                logger.info("Участник с ID '{}' успешно удален из Apache Ignite", userId);
            } else {
                logger.warn("Участник с ID '{}' не найден для удаления в Apache Ignite", userId);
            }
            
            return removed;
        } catch (Exception e) {
            logger.error("Ошибка при удалении участника из Apache Ignite по ID: {}", userId, e);
            return false;
        }
    }
}