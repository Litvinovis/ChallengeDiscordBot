package com.discord.challengebot.service;

import com.discord.challengebot.event.AchievementUnlockedEvent;
import com.discord.challengebot.model.Achievement;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Сервис управления достижениями пользователей.
 * При достижении порогов (100, 500, 1000, 5000 повторений) выдаётся бейдж
 * и публикуется событие {@link AchievementUnlockedEvent} для отправки уведомления в Discord.
 * <p>
 * Кэш "achievements" (Caffeine) хранит ключи выданных достижений пользователя в памяти,
 * снижая нагрузку на Ignite при частых проверках.
 */
@Service
public class AchievementService {
    private static final Logger logger = LoggerFactory.getLogger(AchievementService.class);

    // Предопределённые пороги достижений
    static final List<Achievement> ACHIEVEMENTS = Arrays.asList(
            new Achievement("100_reps",  "Первые 100",         "Выполнено 100 повторений в испытании",   100),
            new Achievement("500_reps",  "500 повторений",     "Выполнено 500 повторений в испытании",   500),
            new Achievement("1000_reps", "1000 повторений",    "Выполнено 1000 повторений в испытании",  1000),
            new Achievement("5000_reps", "Легенда: 5000",      "Выполнено 5000 повторений в испытании",  5000)
    );

    private final ParticipantRepository participantRepository;
    private final IChallengeService challengeService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Создаёт сервис достижений.
     *
     * @param participantRepository репозиторий участников
     * @param challengeService      сервис испытаний
     * @param eventPublisher        публикатор событий Spring
     */
    public AchievementService(ParticipantRepository participantRepository,
                              IChallengeService challengeService,
                              ApplicationEventPublisher eventPublisher) {
        this.participantRepository = participantRepository;
        this.challengeService = challengeService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Проверяет и выдаёт достижения для пользователя на основе общего прогресса.
     * При получении нового бейджа публикует {@link AchievementUnlockedEvent}.
     *
     * @param userId        идентификатор пользователя
     * @param challengeId   идентификатор испытания
     * @param totalProgress общий накопленный прогресс пользователя в испытании
     */
    @CacheEvict(value = "achievements", key = "#userId")
    public void checkAndAwardAchievements(String userId, String challengeId, long totalProgress) {
        try {
            if (userId == null || challengeId == null) return;

            // Загружаем персистентный список достижений из Ignite
            Set<String> userAwards = loadAwardedAchievements(userId);

            var challenge = challengeService.getChallenge(challengeId);
            String challengeName = challenge != null ? challenge.getName() : challengeId;

            // Получаем имя пользователя для уведомления
            String username = participantRepository.findById(userId)
                    .map(Participant::getUsername)
                    .orElse(userId);

            for (Achievement achievement : ACHIEVEMENTS) {
                String key = userId + ":" + challengeId + ":" + achievement.id();
                if (totalProgress >= achievement.threshold() && !userAwards.contains(key)) {
                    userAwards.add(key);
                    persistAwardedAchievement(userId, key);
                    // Публикуем событие вместо прямого вызова DiscordService
                    eventPublisher.publishEvent(new AchievementUnlockedEvent(
                            userId, username, achievement.name(), challengeName));
                    logger.info("Достижение '{}' выдано пользователю {}", achievement.name(), userId);
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка при проверке достижений для пользователя {}", userId, e);
        }
    }

    /**
     * Возвращает множество ключей выданных достижений для пользователя.
     * Результат кэшируется Spring Cache ("achievements").
     *
     * @param userId идентификатор пользователя
     * @return множество ключей вида "userId:challengeId:achievementId"
     */
    @Cacheable(value = "achievements", key = "#userId")
    public Set<String> getUserAchievements(String userId) {
        return loadAwardedAchievements(userId);
    }

    /**
     * Проверяет, получено ли пользователем конкретное достижение в испытании.
     *
     * @param userId        идентификатор пользователя
     * @param challengeId   идентификатор испытания
     * @param achievementId идентификатор достижения
     * @return {@code true}, если достижение уже выдано
     */
    public boolean hasAchievement(String userId, String challengeId, String achievementId) {
        String key = userId + ":" + challengeId + ":" + achievementId;
        try {
            Participant participant = participantRepository.findById(userId).orElse(null);
            if (participant != null && participant.getAwardedAchievements() != null) {
                return participant.getAwardedAchievements().contains(key);
            }
        } catch (Exception e) {
            logger.warn("Не удалось проверить достижение '{}' для пользователя {}", key, userId, e);
        }
        return false;
    }

    // ---- вспомогательные методы ----

    /**
     * Загружает персистентный список достижений пользователя из Ignite.
     */
    private Set<String> loadAwardedAchievements(String userId) {
        try {
            Participant participant = participantRepository.findById(userId).orElse(null);
            if (participant != null && participant.getAwardedAchievements() != null) {
                return participant.getAwardedAchievements();
            }
        } catch (Exception e) {
            logger.warn("Не удалось загрузить достижения из хранилища для пользователя {}", userId, e);
        }
        return java.util.concurrent.ConcurrentHashMap.newKeySet();
    }

    /**
     * Сохраняет ключ выданного достижения в записи участника в Apache Ignite.
     */
    private void persistAwardedAchievement(String userId, String key) {
        try {
            Participant participant = participantRepository.findById(userId)
                    .orElse(new Participant(userId, userId));
            participant.getAwardedAchievements().add(key);
            participantRepository.save(participant);
        } catch (Exception e) {
            logger.warn("Не удалось сохранить достижение '{}' для пользователя {}", key, userId, e);
        }
    }
}
