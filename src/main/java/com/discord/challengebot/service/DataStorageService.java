package com.discord.challengebot.service;

import com.discord.challengebot.config.IgniteConfig;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.cache.CacheMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.cache.Cache;
import java.util.*;

/**
 * Сервис для работы с хранилищем данных (Apache Ignite)
 */
@Service
public class DataStorageService implements IDataStorageService {
    private static final Logger logger = LoggerFactory.getLogger(DataStorageService.class);
    
    @Autowired
    private Ignite ignite;
    
    @Autowired
    private IgniteConfig igniteConfig;
    
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
            
            // Создание или получение кэша для испытаний с включенной персистентностью
            CacheConfiguration<String, Challenge> challengeCacheCfg = new CacheConfiguration<>("challenges");
            challengeCacheCfg.setBackups(1); // Резервные копии для надежности
            challengeCacheCfg.setCacheMode(CacheMode.PARTITIONED); // Режим кэширования
            challengesCache = ignite.getOrCreateCache(challengeCacheCfg);
            
            // Создание или получение кэша для участников с включенной персистентностью
            CacheConfiguration<String, Participant> participantCacheCfg = new CacheConfiguration<>("participants");
            participantCacheCfg.setBackups(1); // Резервные копии для надежности
            participantCacheCfg.setCacheMode(CacheMode.PARTITIONED); // Режим кэширования
            participantsCache = ignite.getOrCreateCache(participantCacheCfg);
            
            logger.info("Подключение к Apache Ignite успешно инициализировано");
            logger.debug("Кэш испытаний инициализирован: {}", challengesCache != null);
            logger.debug("Кэш участников инициализирован: {}", participantsCache != null);
            
            // Проверяем, есть ли уже участники в кэше
            if (participantsCache != null) {
                try {
                    int participantCount = 0;
                    for (javax.cache.Cache.Entry<String, Participant> entry : participantsCache) {
                        participantCount++;
                    }
                    logger.debug("При инициализации в кэше участников найдено: {} записей", participantCount);
                } catch (Exception e) {
                    logger.debug("Ошибка при проверке содержимого кэша участников при инициализации: {}", e.getMessage());
                }
            }
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
            logger.info("Закрытие подключения к Apache Ignite");
            // Закрываем экземпляр Ignite для корректного сохранения данных на диск
            igniteConfig.closeIgnite();
            logger.info("Подключение к Apache Ignite успешно закрыто");
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

            // Bug fix #4: O(1) direct key lookup instead of O(n) iteration.
            // Challenge id is always derived as name.toLowerCase().replace(" ","_")
            String id = name.toLowerCase().replace(" ", "_");
            Challenge challenge = challengesCache.get(id);
            if (challenge != null) {
                logger.debug("Испытание '{}' успешно получено из Apache Ignite по ключу '{}'", name, id);
                return challenge;
            }

            // Fallback: O(n) scan for challenges saved with legacy/different id convention
            for (javax.cache.Cache.Entry<String, Challenge> entry : challengesCache) {
                Challenge c = entry.getValue();
                if (name.equals(c.getName())) {
                    logger.debug("Испытание '{}' найдено через полный перебор (legacy id)", name);
                    return c;
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

            // Bug fix #4: O(1) direct key removal first
            String id = challengeName.toLowerCase().replace(" ", "_");
            boolean removed = challengesCache.remove(id);
            if (removed) {
                logger.info("Испытание '{}' успешно удалено из Apache Ignite по ключу '{}'", challengeName, id);
                return true;
            }

            // Fallback: O(n) scan for legacy id conventions
            for (javax.cache.Cache.Entry<String, Challenge> entry : challengesCache) {
                Challenge challenge = entry.getValue();
                if (challengeName.equals(challenge.getName())) {
                    challengesCache.remove(entry.getKey());
                    logger.info("Испытание '{}' успешно удалено из Apache Ignite (legacy id)", challengeName);
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
            
            logger.debug("Сохраняем участника с ID: {}, имя: {}, количество зарегистрированных испытаний: {}", 
                        participant.getUserId(), participant.getUsername(), participant.getRegisteredChallenges().size());
            
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
                logger.debug("Участник '{}' успешно получен из Apache Ignite, имя пользователя: {}", userId, participant.getUsername());
            } else {
                logger.debug("Участник с ID '{}' не найден в Apache Ignite", userId);
                // Дополнительно проверим, есть ли вообще какие-либо участники в кэше
                try {
                    int participantCount = 0;
                    for (javax.cache.Cache.Entry<String, Participant> entry : participantsCache) {
                        participantCount++;
                        logger.debug("Найден участник в кэше: ID={}, имя={}", entry.getKey(), entry.getValue().getUsername());
                    }
                    logger.debug("Всего участников в кэше: {}", participantCount);
                } catch (Exception e) {
                    logger.debug("Ошибка при подсчете участников в кэше: {}", e.getMessage());
                }
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
            int count = 0;
            for (javax.cache.Cache.Entry<String, Participant> entry : participantsCache) {
                participants.add(entry.getValue());
                count++;
                logger.debug("Найден участник в кэше: ID={}, имя={}", entry.getKey(), entry.getValue().getUsername());
            }
            
            logger.debug("Получено {} участников из Apache Ignite", count);
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