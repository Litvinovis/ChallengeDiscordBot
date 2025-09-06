package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        try {
            logger.info("Расчет статистики для испытания: {}", challenge != null ? challenge.getName() : "null");
            
            if (challenge == null) {
                logger.warn("Попытка расчета статистики для null испытания");
                return null;
            }
            
            long remaining = challenge.getTargetValue() - challenge.getCurrentValue();
            double percentage = challenge.getTargetValue() > 0 ? 
                               (double) challenge.getCurrentValue() / challenge.getTargetValue() * 100 : 0;
            
            // Расчет дней до окончания
            LocalDateTime now = LocalDateTime.now();
            long daysRemaining = Duration.between(now, challenge.getEndDate()).toDays();
            double dailyTarget = daysRemaining > 0 ? (double) remaining / daysRemaining : 0;
            
            ChallengeStats stats = new ChallengeStats(
                challenge.getName(),
                challenge.getTargetValue(),
                challenge.getCurrentValue(),
                remaining,
                percentage,
                dailyTarget,
                (int) daysRemaining
            );
            
            logger.debug("Статистика для испытания '{}' успешно рассчитана", challenge.getName());
            return stats;
        } catch (Exception e) {
            logger.error("Ошибка при расчете статистики для испытания: {}", 
                        challenge != null ? challenge.getName() : "null", e);
            return null;
        }
    }

    /**
     * Рассчитать оставшееся количество
     */
    public long calculateRemaining(Challenge challenge) {
        try {
            logger.debug("Расчет оставшегося количества для испытания: {}", 
                        challenge != null ? challenge.getName() : "null");
            
            if (challenge == null) {
                logger.warn("Попытка расчета оставшегося количества для null испытания");
                return 0;
            }
            
            long remaining = challenge.getTargetValue() - challenge.getCurrentValue();
            logger.debug("Оставшееся количество для испытания '{}': {}", challenge.getName(), remaining);
            return remaining;
        } catch (Exception e) {
            logger.error("Ошибка при расчете оставшегося количества для испытания: {}", 
                        challenge != null ? challenge.getName() : "null", e);
            return 0;
        }
    }

    /**
     * Рассчитать ежедневную цель
     */
    public double calculateDailyTarget(Challenge challenge) {
        try {
            logger.debug("Расчет ежедневной цели для испытания: {}", 
                        challenge != null ? challenge.getName() : "null");
            
            if (challenge == null) {
                logger.warn("Попытка расчета ежедневной цели для null испытания");
                return 0;
            }
            
            long remaining = calculateRemaining(challenge);
            LocalDateTime now = LocalDateTime.now();
            long daysRemaining = Duration.between(now, challenge.getEndDate()).toDays();
            double dailyTarget = daysRemaining > 0 ? (double) remaining / daysRemaining : 0;
            
            logger.debug("Ежедневная цель для испытания '{}': {}", challenge.getName(), dailyTarget);
            return dailyTarget;
        } catch (Exception e) {
            logger.error("Ошибка при расчете ежедневной цели для испытания: {}", 
                        challenge != null ? challenge.getName() : "null", e);
            return 0;
        }
    }

    /**
     * Рассчитать процент выполнения
     */
    public double calculatePercentage(Challenge challenge) {
        try {
            logger.debug("Расчет процента выполнения для испытания: {}", 
                        challenge != null ? challenge.getName() : "null");
            
            if (challenge == null) {
                logger.warn("Попытка расчета процента выполнения для null испытания");
                return 0;
            }
            
            double percentage = challenge.getTargetValue() > 0 ? 
                   (double) challenge.getCurrentValue() / challenge.getTargetValue() * 100 : 0;
                   
            logger.debug("Процент выполнения для испытания '{}': {}", challenge.getName(), percentage);
            return percentage;
        } catch (Exception e) {
            logger.error("Ошибка при расчете процента выполнения для испытания: {}", 
                        challenge != null ? challenge.getName() : "null", e);
            return 0;
        }
    }

    /**
     * Сгенерировать отчет о прогрессе
     */
    public String generateProgressReport(Challenge challenge) {
        try {
            logger.debug("Генерация отчета о прогрессе для испытания: {}", 
                        challenge != null ? challenge.getName() : "null");
            
            if (challenge == null) {
                logger.warn("Попытка генерации отчета о прогрессе для null испытания");
                return "";
            }
            
            ChallengeStats stats = calculateStats(challenge);
            String report = formatReportForDiscord(stats);
            
            logger.debug("Отчет о прогрессе для испытания '{}' успешно сгенерирован", challenge.getName());
            return report;
        } catch (Exception e) {
            logger.error("Ошибка при генерации отчета о прогрессе для испытания: {}", 
                        challenge != null ? challenge.getName() : "null", e);
            return "";
        }
    }

    /**
     * Сгенерировать таблицу лидеров
     */
    public List<Map.Entry<String, Long>> generateLeaderboard(Challenge challenge, int limit) {
        try {
            logger.info("Генерация таблицы лидеров для испытания: {}", 
                       challenge != null ? challenge.getName() : "null");
            
            if (challenge == null) {
                logger.warn("Попытка генерации таблицы лидеров для null испытания");
                return new java.util.ArrayList<>();
            }
            
            if (limit <= 0) {
                logger.warn("Попытка генерации таблицы лидеров с недопустимым лимитом: {}", limit);
                return new java.util.ArrayList<>();
            }
            
            List<Map.Entry<String, Long>> leaderboard = challenge.getParticipantProgress().entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
            
            logger.debug("Таблица лидеров для испытания '{}' успешно сгенерирована ({} участников)", 
                        challenge.getName(), leaderboard.size());
            return leaderboard;
        } catch (Exception e) {
            logger.error("Ошибка при генерации таблицы лидеров для испытания: {}", 
                        challenge != null ? challenge.getName() : "null", e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Форматировать отчет для Discord
     */
    public String formatReportForDiscord(ChallengeStats stats) {
        try {
            logger.debug("Форматирование отчета для Discord");
            
            if (stats == null) {
                logger.warn("Попытка форматирования отчета для Discord с null статистикой");
                return "";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("**Статистика по испытанию: ").append(stats.getChallengeName()).append("**\n");
            sb.append("Цель: ").append(stats.getTargetValue()).append("\n");
            sb.append("Выполнено: ").append(stats.getCurrentValue()).append("\n");
            sb.append("Осталось: ").append(stats.getRemaining()).append("\n");
            // Используем запятую как десятичный разделитель для русской локали
            sb.append("Процент выполнения: ").append(String.format("%.2f", stats.getPercentage()).replace('.', ',')).append("%\n");
            sb.append("Ежедневная цель: ").append(String.format("%.2f", stats.getDailyTarget()).replace('.', ',')).append(" в день\n");
            sb.append("Дней осталось: ").append(stats.getDaysRemaining()).append("\n");
            
            logger.debug("Отчет для Discord успешно отформатирован");
            return sb.toString();
        } catch (Exception e) {
            logger.error("Ошибка при форматировании отчета для Discord", e);
            return "";
        }
    }

    /**
     * Форматировать таблицу лидеров для Discord
     */
    public String formatLeaderboardForDiscord(Challenge challenge, List<Map.Entry<String, Long>> leaderboard) {
        try {
            logger.debug("Форматирование таблицы лидеров для Discord по испытанию: {}", 
                        challenge != null ? challenge.getName() : "null");
            
            if (challenge == null) {
                logger.warn("Попытка форматирования таблицы лидеров для Discord с null испытанием");
                return "";
            }
            
            if (leaderboard == null) {
                logger.warn("Попытка форматирования таблицы лидеров для Discord с null leaderboard");
                return "";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("**Топ участников по испытанию: ").append(challenge.getName()).append("**\n");
            
            if (leaderboard.isEmpty()) {
                sb.append("Пока нет участников.\n");
                logger.debug("Таблица лидеров пуста для испытания '{}'", challenge.getName());
            } else {
                for (int i = 0; i < leaderboard.size(); i++) {
                    Map.Entry<String, Long> entry = leaderboard.get(i);
                    sb.append((i + 1)).append(". <@").append(entry.getKey()).append("> - ").append(entry.getValue()).append(" ").append(challenge.getUnit()).append("\n");
                }
                logger.debug("Таблица лидеров для испытания '{}' содержит {} участников", 
                            challenge.getName(), leaderboard.size());
            }
            
            logger.debug("Таблица лидеров для Discord успешно отформатирована");
            return sb.toString();
        } catch (Exception e) {
            logger.error("Ошибка при форматировании таблицы лидеров для Discord по испытанию: {}", 
                        challenge != null ? challenge.getName() : "null", e);
            return "";
        }
    }

    /**
     * Форматировать статистику испытания
     */
    public String formatChallengeStats(ChallengeStats stats) {
        try {
            logger.debug("Форматирование статистики испытания");
            return formatReportForDiscord(stats);
        } catch (Exception e) {
            logger.error("Ошибка при форматировании статистики испытания", e);
            return "";
        }
    }
}