package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Achievement;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления достижениями (бейджами) пользователей.
 * При достижении порогов (100, 500, 1000, 5000 повторений) выдаётся бейдж
 * и отправляется поздравление в общий канал.
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

    // In-memory cache: userId -> Set<"userId:challengeId:achievementId">
    // Acts as a read-through cache over the persisted Participant records.
    // On cache miss the Participant record is loaded from IDataStorageService
    // so that achievements survive bot restarts.
    private static final int MAX_USERS = 1000;
    private final Map<String, Set<String>> awardedAchievements = new ConcurrentHashMap<>();

    @Autowired
    private DiscordConfig discordConfig;

    @Autowired
    private IDiscordService discordService;

    @Autowired
    private IChallengeService challengeService;

    @Autowired
    private IDataStorageService dataStorageService;

    /**
     * Проверяет и выдаёт достижения для пользователя на основе общего прогресса.
     * При получении нового бейджа отправляет поздравление в общий канал.
     *
     * @param userId        идентификатор пользователя
     * @param challengeId   идентификатор испытания
     * @param totalProgress общий накопленный прогресс пользователя в испытании
     */
    public void checkAndAwardAchievements(String userId, String challengeId, long totalProgress) {
        try {
            if (userId == null || challengeId == null) {
                return;
            }

            // Вытесняем старые записи при достижении лимита кэша
            if (awardedAchievements.size() >= MAX_USERS && !awardedAchievements.containsKey(userId)) {
                String first = awardedAchievements.keySet().iterator().next();
                awardedAchievements.remove(first);
            }

            // Инициализируем кэш для пользователя из персистентного хранилища при первом обращении.
            // Это гарантирует, что после перезапуска бота уже выданные достижения не будут
            // выданы повторно, т.к. они хранятся в записи Participant в Apache Ignite.
            Set<String> userAwards = awardedAchievements.computeIfAbsent(userId, k -> {
                Set<String> persisted = ConcurrentHashMap.newKeySet();
                try {
                    Participant participant = dataStorageService.getParticipant(k);
                    if (participant != null && participant.getAwardedAchievements() != null) {
                        persisted.addAll(participant.getAwardedAchievements());
                    }
                } catch (Exception ex) {
                    logger.warn("Не удалось загрузить выданные достижения из хранилища для пользователя {}", k, ex);
                }
                return persisted;
            });

            Challenge challenge = challengeService.getChallenge(challengeId);
            String challengeName = challenge != null ? challenge.getName() : challengeId;

            for (Achievement achievement : ACHIEVEMENTS) {
                String key = userId + ":" + challengeId + ":" + achievement.getId();
                if (totalProgress >= achievement.getThreshold() && !userAwards.contains(key)) {
                    userAwards.add(key);
                    // Персистентно сохраняем выданное достижение в записи Participant,
                    // чтобы оно пережило перезапуск бота.
                    persistAwardedAchievement(userId, key);
                    sendAchievementAnnouncement(userId, achievement, challengeName);
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка при проверке достижений для пользователя {}", userId, e);
        }
    }

    /**
     * Сохраняет ключ выданного достижения в записи участника в Apache Ignite.
     * Если участник не найден, создаёт минимальную запись.
     *
     * @param userId идентификатор пользователя
     * @param key    ключ достижения вида "userId:challengeId:achievementId"
     */
    private void persistAwardedAchievement(String userId, String key) {
        try {
            Participant participant = dataStorageService.getParticipant(userId);
            if (participant == null) {
                participant = new Participant(userId, userId);
            }
            participant.getAwardedAchievements().add(key);
            dataStorageService.saveParticipant(participant);
        } catch (Exception e) {
            logger.warn("Не удалось персистентно сохранить достижение '{}' для пользователя {}", key, userId, e);
        }
    }

    /**
     * Отправляет поздравительное сообщение о получении достижения в общий канал.
     *
     * @param userId        идентификатор пользователя
     * @param achievement   полученное достижение
     * @param challengeName название испытания
     */
    private void sendAchievementAnnouncement(String userId, Achievement achievement, String challengeName) {
        try {
            String channel = resolveAnnouncementChannel();
            String message = String.format(
                    "🏆 <@%s> получил достижение **%s**! Выполнено %d повторений в испытании %s",
                    userId, achievement.getName(), achievement.getThreshold(), challengeName
            );
            discordService.sendMessageToChannel(channel, message);
            logger.info("Достижение '{}' выдано пользователю {}", achievement.getName(), userId);
        } catch (Exception e) {
            logger.error("Ошибка отправки сообщения о достижении для пользователя {}", userId, e);
        }
    }

    /**
     * Определяет канал для публикации объявлений о достижениях.
     * Приоритет: report-channel из конфига, затем основной канал, иначе "announcements".
     *
     * @return имя канала для объявлений
     */
    private String resolveAnnouncementChannel() {
        if (discordConfig != null) {
            String reportChannel = discordConfig.getReportChannel();
            if (reportChannel != null && !reportChannel.isBlank()) {
                return reportChannel;
            }
            String channel = discordConfig.getChannel();
            if (channel != null && !channel.isBlank()) {
                return channel;
            }
        }
        return "announcements";
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
        // Проверяем кэш
        Set<String> userAwards = awardedAchievements.get(userId);
        if (userAwards != null) {
            return userAwards.contains(key);
        }
        // При отсутствии в кэше проверяем персистентное хранилище напрямую
        try {
            Participant participant = dataStorageService.getParticipant(userId);
            if (participant != null && participant.getAwardedAchievements() != null) {
                return participant.getAwardedAchievements().contains(key);
            }
        } catch (Exception e) {
            logger.warn("Не удалось проверить достижение '{}' в хранилище для пользователя {}", key, userId, e);
        }
        return false;
    }

    /**
     * Возвращает все ключи выданных достижений для пользователя.
     * Используется для тестирования и отладки.
     *
     * @param userId идентификатор пользователя
     * @return множество строк вида "userId:challengeId:achievementId"
     */
    public Set<String> getUserAchievements(String userId) {
        return awardedAchievements.getOrDefault(userId, ConcurrentHashMap.newKeySet());
    }
}
