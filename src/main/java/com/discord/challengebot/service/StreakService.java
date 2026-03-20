package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Participant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Сервис для управления сериями (streak) активности пользователей.
 * При достижении порогов (3, 7, 30 дней) отправляет поздравительное уведомление.
 */
@Service
public class StreakService {
    private static final Logger logger = LoggerFactory.getLogger(StreakService.class);

    @Autowired
    private IUserService userService;

    @Autowired
    private IDataStorageService dataStorageService;

    @Autowired
    private IDiscordService discordService;

    @Autowired
    private DiscordConfig discordConfig;

    /**
     * Зафиксировать активность пользователя, обновить серию.
     * Серия сбрасывается, если пропуск > 1 дня с момента последней активности.
     * При достижении порогов (3, 7, 30 дней) отправляется уведомление в канал.
     */
    public void recordActivity(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                return;
            }

            Participant participant = userService.getParticipant(userId);
            if (participant == null) {
                logger.debug("Участник {} не найден для обновления серии", userId);
                return;
            }

            LocalDate today = LocalDate.now();
            LocalDate lastActivity = participant.getLastActivityDate();
            int previousStreak = participant.getCurrentStreak();

            if (lastActivity == null) {
                // Первая активность
                participant.setCurrentStreak(1);
                participant.setLongestStreak(1);
            } else {
                long dayGap = ChronoUnit.DAYS.between(lastActivity, today);
                if (dayGap == 0) {
                    // Тот же день, без изменений
                } else if (dayGap == 1) {
                    // Следующий день подряд
                    int newStreak = participant.getCurrentStreak() + 1;
                    participant.setCurrentStreak(newStreak);
                    if (newStreak > participant.getLongestStreak()) {
                        participant.setLongestStreak(newStreak);
                    }
                } else {
                    // Пропуск > 1 дня: сбрасываем серию
                    participant.setCurrentStreak(1);
                }
            }

            participant.setLastActivityDate(today);
            dataStorageService.saveParticipant(participant);

            int newStreak = participant.getCurrentStreak();
            logger.debug("Серия пользователя {} обновлена: текущая={}", userId, newStreak);

            // Отправляем уведомления о достижении порогов серии
            if (newStreak != previousStreak) {
                sendStreakNotificationIfMilestone(userId, newStreak);
            }
        } catch (Exception e) {
            logger.error("Ошибка при обновлении серии активности для пользователя {}", userId, e);
        }
    }

    /**
     * Отправляет уведомление при достижении порога серии (3, 7, 30 дней).
     */
    private void sendStreakNotificationIfMilestone(String userId, int streak) {
        try {
            String message = null;
            if (streak == 30) {
                message = String.format("🏆 <@%s> — Месяц без пропуска! Легенда!", userId);
            } else if (streak == 7) {
                message = String.format("🔥🔥 <@%s> — Неделя без пропуска!", userId);
            } else if (streak == 3) {
                message = String.format("🔥 <@%s> — 3-дневная серия!", userId);
            }

            if (message != null) {
                String channel = resolveNotificationChannel();
                discordService.sendMessageToChannel(channel, message);
                logger.info("Уведомление о серии {} дней отправлено для пользователя {}", streak, userId);
            }
        } catch (Exception e) {
            logger.error("Ошибка отправки уведомления о серии для пользователя {}", userId, e);
        }
    }

    /**
     * Определить канал для уведомлений о сериях.
     */
    private String resolveNotificationChannel() {
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
        return "general";
    }

    /**
     * Возвращает множитель на основе длины серии:
     * - серия < 7:  1.0
     * - серия >= 7: 1.1
     * - серия >= 30: 1.2
     */
    public double getStreakMultiplier(int streak) {
        if (streak >= 30) return 1.2;
        if (streak >= 7) return 1.1;
        return 1.0;
    }

    /**
     * Получить текущую серию пользователя
     */
    public int getCurrentStreak(String userId) {
        try {
            Participant participant = userService.getParticipant(userId);
            if (participant == null) return 0;
            return participant.getCurrentStreak();
        } catch (Exception e) {
            logger.error("Ошибка при получении серии для пользователя {}", userId, e);
            return 0;
        }
    }
}
