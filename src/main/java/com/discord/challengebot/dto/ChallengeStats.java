package com.discord.challengebot.dto;

/**
 * DTO статистики по испытанию.
 * Содержит рассчитанные показатели: текущий прогресс, процент выполнения,
 * ежедневная цель и количество оставшихся дней.
 */
public record ChallengeStats(
				String challengeName,
				long targetValue,
				long currentValue,
				long remaining,
				double percentage,
				double dailyTarget,
				int daysRemaining
) {
}
