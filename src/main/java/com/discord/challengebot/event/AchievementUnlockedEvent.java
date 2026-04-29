package com.discord.challengebot.event;

/**
 * Событие получения достижения пользователем.
 * Публикуется AchievementService и обрабатывается DiscordService.
 *
 * @param userId          идентификатор пользователя Discord
 * @param username        имя пользователя (для отображения)
 * @param achievementName название достижения
 * @param challengeName   название испытания
 */
public record AchievementUnlockedEvent(String userId, String username, String achievementName, String challengeName) { }
