package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Интерфейс сервиса расчёта статистики по испытаниям.
 * Определяет методы расчёта показателей, форматирования отчётов и прогнозирования дат завершения.
 */
public interface IStatisticsService {
	/**
	 * Рассчитывает полную статистику по испытанию.
	 *
	 * @param challenge испытание
	 * @return объект статистики или {@code null}
	 */
	ChallengeStats calculateStats(Challenge challenge);






	/**
	 * Форматирует статистику испытания для вывода в Discord.
	 *
	 * @param challenge испытание
	 * @param stats     объект статистики
	 * @return отформатированная строка
	 */
	String formatReportForDiscord(Challenge challenge, ChallengeStats stats);

	/**
	 * Форматирует таблицу лидеров для вывода в Discord.
	 *
	 * @param challenge   испытание
	 * @param leaderboard список пар userId -> прогресс
	 * @return отформатированная строка
	 */
	String formatLeaderboardForDiscord(Challenge challenge, List<Map.Entry<String, Long>> leaderboard);

	/**
	 * Форматирует полную статистику испытания для вывода в Discord.
	 *
	 * @param challenge испытание
	 * @param stats     объект статистики
	 * @return отформатированная строка
	 */
	String formatChallengeStats(Challenge challenge, ChallengeStats stats);

	/**
	 * Форматирует красивый отчёт для ежедневной рассылки в Discord.
	 * Отличается от {@link #formatReportForDiscord} наличием прогресс-бара, медалей и
	 * принимает заранее загруженный список топ-участников (из нормализованной таблицы).
	 *
	 * @param challenge       испытание
	 * @param stats           объект статистики
	 * @param topParticipants топ участников (userId → прогресс), уже отсортированный по убыванию
	 * @return отформатированная строка
	 */
	String formatDailyReportForDiscord(Challenge challenge, ChallengeStats stats,
	                                   List<Map.Entry<String, Long>> topParticipants);

	/**
	 * Прогнозирует дату завершения испытания для пользователя на основе истории прогресса:
	 * остаток до цели делится на средний дневной темп за последние 7 дней.
	 *
	 * @param challenge испытание
	 * @param userId    идентификатор пользователя
	 * @return прогнозируемая дата завершения или {@code null} при недостатке данных
	 */
	LocalDate forecastCompletionDate(Challenge challenge, String userId);
}
