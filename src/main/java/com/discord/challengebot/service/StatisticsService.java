package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для расчета статистики
 */
@Service
public class StatisticsService {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);

    /**
     * Рассчитать статистику по испытанию
     */
    public ChallengeStats calculateStats(Challenge challenge) {
        logger.info("Расчет статистики для испытания: {}", challenge.getName());
        
        long remaining = challenge.getTargetValue() - challenge.getCurrentValue();
        double percentage = challenge.getTargetValue() > 0 ? 
                           (double) challenge.getCurrentValue() / challenge.getTargetValue() * 100 : 0;
        
        // Расчет дней до окончания
        LocalDateTime now = LocalDateTime.now();
        long daysRemaining = Duration.between(now, challenge.getEndDate()).toDays();
        double dailyTarget = daysRemaining > 0 ? (double) remaining / daysRemaining : 0;
        
        return new ChallengeStats(
            challenge.getName(),
            challenge.getTargetValue(),
            challenge.getCurrentValue(),
            remaining,
            percentage,
            dailyTarget,
            (int) daysRemaining
        );
    }

    /**
     * Рассчитать оставшееся количество
     */
    public long calculateRemaining(Challenge challenge) {
        return challenge.getTargetValue() - challenge.getCurrentValue();
    }

    /**
     * Рассчитать ежедневную цель
     */
    public double calculateDailyTarget(Challenge challenge) {
        long remaining = calculateRemaining(challenge);
        LocalDateTime now = LocalDateTime.now();
        long daysRemaining = Duration.between(now, challenge.getEndDate()).toDays();
        return daysRemaining > 0 ? (double) remaining / daysRemaining : 0;
    }

    /**
     * Рассчитать процент выполнения
     */
    public double calculatePercentage(Challenge challenge) {
        return challenge.getTargetValue() > 0 ? 
               (double) challenge.getCurrentValue() / challenge.getTargetValue() * 100 : 0;
    }

    /**
     * Сгенерировать отчет о прогрессе
     */
    public String generateProgressReport(Challenge challenge) {
        ChallengeStats stats = calculateStats(challenge);
        return formatReportForDiscord(stats);
    }

    /**
     * Сгенерировать таблицу лидеров
     */
    public List<?> generateLeaderboard(Challenge challenge, int limit) {
        // В реальной реализации здесь будет генерация таблицы лидеров
        logger.info("Генерация таблицы лидеров для испытания: {}", challenge.getName());
        return null;
    }

    /**
     * Форматировать отчет для Discord
     */
    public String formatReportForDiscord(ChallengeStats stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Статистика по испытанию: ").append(stats.getChallengeName()).append("**\n");
        sb.append("Цель: ").append(stats.getTargetValue()).append("\n");
        sb.append("Выполнено: ").append(stats.getCurrentValue()).append("\n");
        sb.append("Осталось: ").append(stats.getRemaining()).append("\n");
        sb.append("Процент выполнения: ").append(String.format("%.2f", stats.getPercentage())).append("%\n");
        sb.append("Ежедневная цель: ").append(String.format("%.2f", stats.getDailyTarget())).append(" в день\n");
        sb.append("Дней осталось: ").append(stats.getDaysRemaining()).append("\n");
        
        return sb.toString();
    }

    /**
     * Форматировать статистику испытания
     */
    public String formatChallengeStats(ChallengeStats stats) {
        return formatReportForDiscord(stats);
    }
}