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
     * Рассчитывает оставшееся количество для выполнения цели испытания.
     *
     * @param challenge испытание
     * @return оставшееся количество
     */
    long calculateRemaining(Challenge challenge);

    /**
     * Рассчитывает рекомендуемую ежедневную цель на одного участника.
     *
     * @param challenge испытание
     * @return ежедневная цель
     */
    double calculateDailyTarget(Challenge challenge);

    /**
     * Рассчитывает процент выполнения испытания.
     *
     * @param challenge испытание
     * @return процент выполнения (0..100)
     */
    double calculatePercentage(Challenge challenge);

    /**
     * Генерирует текстовый отчёт о прогрессе испытания.
     *
     * @param challenge испытание
     * @return строка отчёта
     */
    String generateProgressReport(Challenge challenge);

    /**
     * Генерирует таблицу лидеров для испытания.
     *
     * @param challenge испытание
     * @param limit     максимальное количество участников
     * @return список пар userId -> прогресс
     */
    List<Map.Entry<String, Long>> generateLeaderboard(Challenge challenge, int limit);

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
     * Прогнозирует дату завершения испытания для пользователя на основе истории прогресса.
     *
     * @param challengeId идентификатор испытания
     * @param userId      идентификатор пользователя
     * @return прогнозируемая дата завершения или {@code null} при недостатке данных
     */
    LocalDate forecastCompletionDate(String challengeId, String userId);
}
