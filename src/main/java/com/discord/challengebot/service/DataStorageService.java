package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.repository.ParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для работы с хранилищем данных (Apache Ignite 3).
 * Делегирует все операции в ChallengeRepository и ParticipantRepository.
 */
@Service
public class DataStorageService implements IDataStorageService {
    private static final Logger logger = LoggerFactory.getLogger(DataStorageService.class);

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    /**
     * Сохранить испытание
     */
    public void saveChallenge(Challenge challenge) {
        try {
            if (challenge == null) {
                logger.warn("Попытка сохранить null испытание");
                return;
            }
            logger.debug("Сохранение испытания в Apache Ignite 3: {}", challenge.getName());
            challengeRepository.save(challenge);
            logger.info("Испытание '{}' успешно сохранено в Apache Ignite 3", challenge.getName());
        } catch (Exception e) {
            logger.error("Ошибка при сохранении испытания в Apache Ignite 3: {}",
                    challenge != null ? challenge.getName() : "null", e);
        }
    }

    /**
     * Получить испытание по имени
     */
    public Challenge getChallenge(String name) {
        try {
            if (name == null || name.isEmpty()) {
                logger.warn("Попытка получить испытание с пустым именем");
                return null;
            }
            logger.debug("Получение испытания из Apache Ignite 3: {}", name);

            // O(1) direct key lookup: id = name.toLowerCase().replace(" ","_")
            String id = name.toLowerCase().replace(" ", "_");
            var opt = challengeRepository.findById(id);
            if (opt.isPresent()) {
                logger.debug("Испытание '{}' успешно получено из Apache Ignite 3 по ключу '{}'", name, id);
                return opt.get();
            }

            // Fallback: полный перебор для legacy id
            for (Challenge c : challengeRepository.findAll()) {
                if (name.equals(c.getName())) {
                    logger.debug("Испытание '{}' найдено через полный перебор (legacy id)", name);
                    return c;
                }
            }

            logger.debug("Испытание '{}' не найдено в Apache Ignite 3", name);
            return null;
        } catch (Exception e) {
            logger.error("Ошибка при получении испытания из Apache Ignite 3: {}", name, e);
            return null;
        }
    }

    /**
     * Получить все испытания
     */
    public List<Challenge> getAllChallenges() {
        try {
            logger.debug("Получение всех испытаний из Apache Ignite 3");
            List<Challenge> challenges = challengeRepository.findAll();
            logger.debug("Получено {} испытаний из Apache Ignite 3", challenges.size());
            return challenges;
        } catch (Exception e) {
            logger.error("Ошибка при получении всех испытаний из Apache Ignite 3", e);
            return new ArrayList<>();
        }
    }

    /**
     * Удалить испытание
     */
    public boolean deleteChallenge(String challengeName) {
        try {
            if (challengeName == null || challengeName.isEmpty()) {
                logger.warn("Попытка удалить испытание с пустым именем");
                return false;
            }
            logger.debug("Удаление испытания из Apache Ignite 3: {}", challengeName);

            // O(1) direct key removal first
            String id = challengeName.toLowerCase().replace(" ", "_");
            if (challengeRepository.existsById(id)) {
                challengeRepository.deleteById(id);
                logger.info("Испытание '{}' успешно удалено из Apache Ignite 3 по ключу '{}'", challengeName, id);
                return true;
            }

            // Fallback: полный перебор для legacy id
            for (Challenge c : challengeRepository.findAll()) {
                if (challengeName.equals(c.getName())) {
                    challengeRepository.deleteById(c.getId());
                    logger.info("Испытание '{}' успешно удалено из Apache Ignite 3 (legacy id)", challengeName);
                    return true;
                }
            }

            logger.warn("Испытание '{}' не найдено для удаления в Apache Ignite 3", challengeName);
            return false;
        } catch (Exception e) {
            logger.error("Ошибка при удалении испытания из Apache Ignite 3: {}", challengeName, e);
            return false;
        }
    }

    /**
     * Сохранить участника
     */
    public void saveParticipant(Participant participant) {
        try {
            if (participant == null) {
                logger.warn("Попытка сохранить null участника");
                return;
            }
            logger.debug("Сохранение участника в Apache Ignite 3: {}", participant.getUsername());
            participantRepository.save(participant);
            logger.info("Участник '{}' успешно сохранен в Apache Ignite 3", participant.getUsername());
        } catch (Exception e) {
            logger.error("Ошибка при сохранении участника в Apache Ignite 3: {}",
                    participant != null ? participant.getUsername() : "null", e);
        }
    }

    /**
     * Получить участника по ID
     */
    public Participant getParticipant(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка получить участника с пустым ID");
                return null;
            }
            logger.debug("Получение участника из Apache Ignite 3 по ID: {}", userId);
            Participant participant = participantRepository.findById(userId).orElse(null);
            if (participant != null) {
                logger.debug("Участник '{}' успешно получен из Apache Ignite 3", userId);
            } else {
                logger.debug("Участник с ID '{}' не найден в Apache Ignite 3", userId);
            }
            return participant;
        } catch (Exception e) {
            logger.error("Ошибка при получении участника из Apache Ignite 3 по ID: {}", userId, e);
            return null;
        }
    }

    /**
     * Получить всех участников
     */
    public List<Participant> getAllParticipants() {
        try {
            logger.debug("Получение всех участников из Apache Ignite 3");
            List<Participant> participants = participantRepository.findAll();
            logger.debug("Получено {} участников из Apache Ignite 3", participants.size());
            return participants;
        } catch (Exception e) {
            logger.error("Ошибка при получении всех участников из Apache Ignite 3", e);
            return new ArrayList<>();
        }
    }

    /**
     * Удалить участника
     */
    public boolean deleteParticipant(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка удалить участника с пустым ID");
                return false;
            }
            logger.debug("Удаление участника из Apache Ignite 3 по ID: {}", userId);
            if (participantRepository.existsById(userId)) {
                participantRepository.deleteById(userId);
                logger.info("Участник с ID '{}' успешно удален из Apache Ignite 3", userId);
                return true;
            }
            logger.warn("Участник с ID '{}' не найден для удаления в Apache Ignite 3", userId);
            return false;
        } catch (Exception e) {
            logger.error("Ошибка при удалении участника из Apache Ignite 3 по ID: {}", userId, e);
            return false;
        }
    }
}
