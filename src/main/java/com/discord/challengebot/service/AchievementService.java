package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Achievement;
import com.discord.challengebot.model.Challenge;
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

    // Ограниченное in-memory хранилище: userId -> Set<"userId:challengeId:achievementId">
    private static final int MAX_USERS = 1000;
    private final Map<String, Set<String>> awardedAchievements = new ConcurrentHashMap<>();

    @Autowired
    private DiscordConfig discordConfig;

    @Autowired
    private IDiscordService discordService;

    @Autowired
    private IChallengeService challengeService;

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

            // Вытесняем старые записи при достижении лимита
            if (awardedAchievements.size() >= MAX_USERS && !awardedAchievements.containsKey(userId)) {
                String first = awardedAchievements.keySet().iterator().next();
                awardedAchievements.remove(first);
            }

            Set<String> userAwards = awardedAchievements.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());

            Challenge challenge = challengeService.getChallenge(challengeId);
            String challengeName = challenge != null ? challenge.getName() : challengeId;

            for (Achievement achievement : ACHIEVEMENTS) {
                String key = userId + ":" + challengeId + ":" + achievement.getId();
                if (totalProgress >= achievement.getThreshold() && !userAwards.contains(key)) {
                    userAwards.add(key);
                    sendAchievementAnnouncement(userId, achievement, challengeName);
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка при проверке достижений для пользователя {}", userId, e);
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
        Set<String> userAwards = awardedAchievements.get(userId);
        if (userAwards == null) return false;
        return userAwards.contains(userId + ":" + challengeId + ":" + achievementId);
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
