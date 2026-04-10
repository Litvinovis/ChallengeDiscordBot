package com.discord.challengebot.event;

/**
 * Событие достижения порогового значения серии активности.
 * Публикуется StreakService при достижении порогов (3, 7, 30 дней).
 *
 * @param userId   идентификатор пользователя Discord
 * @param username имя пользователя (для отображения)
 * @param streak   текущая длина серии в днях
 */
public record StreakMilestoneEvent(
        String userId,
        String username,
        int streak
) {}
