package com.discord.challengebot.event;

/**
 * Событие завершения испытания (достижение цели или истечение срока).
 * Публикуется DailyReportScheduler и обрабатывается DiscordService.
 *
 * @param challengeId   идентификатор испытания
 * @param challengeName название испытания
 * @param finalValue    итоговое значение прогресса на момент завершения
 */
public record ChallengeCompletedEvent(
        String challengeId,
        String challengeName,
        long finalValue
) {}
