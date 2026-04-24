package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.model.Participant;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Сервис расчёта статистики по испытаниям.
 * Вычисляет показатели прогресса, формирует отчёты для Discord,
 * ведёт in-memory историю прогресса и прогнозирует дату завершения испытания.
 */
@Service
public class StatisticsService implements IStatisticsService {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);

    // Ограниченный кэш истории прогресса: не более 10000 записей (ключей userId:challengeId)
    private static final int MAX_CACHE_SIZE = 10000;
    // Ограничение на количество значений прогресса на один ключ (LRU-подобное поведение)
    private static final int MAX_HISTORY_PER_KEY = 365;
    private final Map<String, List<Long>> progressHistoryCache = new ConcurrentHashMap<>();

    // Зависимости для получения имён пользователей при форматировании лидербордов
    private DiscordService discordService;
    private ParticipantService participantService;

    /**
     * Устанавливает DiscordService (вызывается из DiscordService.init для обхода циклической зависимости).
     *
     * @param discordService сервис Discord
     */
    public void setDiscordService(DiscordService discordService) {
        this.discordService = discordService;
    }

    /**
     * Устанавливает ParticipantService (вызывается из DiscordService.init для обхода циклической зависимости).
     *
     * @param participantService сервис участников
     */
    public void setParticipantService(ParticipantService participantService) {
        this.participantService = participantService;
    }
    
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
            
            // Расчет дней до окончания (по календарным дням, без потери из-за времени суток)
            LocalDate today = LocalDate.now();
            LocalDate endDate = challenge.getEndDate().toLocalDate();
            long daysRemaining = ChronoUnit.DAYS.between(today, endDate);
            
            // Расчет ежедневной цели с распределением между участниками
            double dailyTarget = 0;
            if (daysRemaining > 0) {
                // Получаем количество участников
                int participantCount = challenge.getParticipants().size();
                
                // Если нет участников, распределяем на одного участника
                if (participantCount <= 0) {
                    participantCount = 1;
                }
                
                // Распределяем оставшуюся цель среди участников и делим на количество дней
                dailyTarget = (double) remaining / participantCount / daysRemaining;
            }
            
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
            LocalDate today = LocalDate.now();
            LocalDate endDate = challenge.getEndDate().toLocalDate();
            long daysRemaining = ChronoUnit.DAYS.between(today, endDate);
            
            // Если дней не осталось, возвращаем 0
            if (daysRemaining <= 0) {
                return 0;
            }
            
            // Получаем количество участников
            int participantCount = challenge.getParticipants().size();
            
            // Если нет участников, распределяем на одного участника
            if (participantCount <= 0) {
                participantCount = 1;
            }
            
            // Распределяем оставшуюся цель среди участников и делим на количество дней
            double dailyTarget = (double) remaining / participantCount / daysRemaining;
            
            logger.debug("Ежедневная цель для испытания '{}': {} для {} участников", 
                        challenge.getName(), dailyTarget, participantCount);
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
            String report = formatReportForDiscord(challenge, stats);
            
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
    public String formatReportForDiscord(Challenge challenge, ChallengeStats stats) {
        try {
            logger.debug("Форматирование отчета для Discord");
            
            if (stats == null) {
                logger.warn("Попытка форматирования отчета для Discord с null статистикой");
                return "";
            }
            
            if (challenge == null) {
                logger.warn("Попытка форматирования отчета для Discord с null испытанием");
                return "";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("**Статистика по испытанию: ").append(stats.challengeName()).append("**\n");
            sb.append("Цель: ").append(stats.targetValue()).append("\n");
            sb.append("Выполнено: ").append(stats.currentValue()).append("\n");
            sb.append("Осталось: ").append(stats.remaining()).append("\n");
            // Используем запятую как десятичный разделитель для русской локали
            sb.append("Процент выполнения: ").append(String.format("%.2f", stats.percentage()).replace('.', ',')).append("%\n");
            sb.append("Ежедневная цель: ").append(String.format("%.2f", stats.dailyTarget()).replace('.', ',')).append(" в день\n");
            sb.append("Дней осталось: ").append(stats.daysRemaining()).append("\n");
            
            // Добавляем количество зарегистрированных участников
            int participantCount = challenge.getParticipants().size();
            sb.append("Зарегистрировано участников: ").append(participantCount).append("\n");
            
            // Добавляем топ-3 участников
            List<Map.Entry<String, Long>> topParticipants = challenge.getParticipantProgress().entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .collect(Collectors.toList());
            
            if (!topParticipants.isEmpty()) {
                sb.append("\n**Топ-3 участников:**\n");
                for (int i = 0; i < topParticipants.size(); i++) {
                    Map.Entry<String, Long> entry = topParticipants.get(i);
                    String userId = entry.getKey();
                    String username = userId; // По умолчанию используем ID
                    
                    logger.debug("Обработка пользователя с ID: {}", userId);
                    
                    // Пытаемся получить имя пользователя из кэша
                    if (participantService != null) {
                        try {
                            Participant participant = participantService.getParticipant(userId);
                            if (participant != null && participant.getUsername() != null
                                    && !participant.getUsername().isEmpty()) {
                                username = participant.getUsername();
                            } else if (discordService != null) {
                                User user = discordService.getJDA().getUserById(userId);
                                if (user != null) username = user.getName();
                            }
                        } catch (Exception e) {
                            logger.debug("Не удалось получить имя пользователя для ID {}: {}", userId, e.getMessage());
                        }
                    } else if (discordService != null) {
                        try {
                            User user = discordService.getJDA().getUserById(userId);
                            if (user != null) username = user.getName();
                        } catch (Exception e) {
                            logger.debug("Не удалось получить имя пользователя для ID {}: {}", userId, e.getMessage());
                        }
                    }
                    
                    sb.append((i + 1)).append(". ").append(username).append(" - ").append(entry.getValue()).append(" ").append(challenge.getUnit()).append("\n");
                }
            }
            
            logger.debug("Отчет для Discord успешно отформатирован");
            return sb.toString();
        } catch (Exception e) {
            logger.error("Ошибка при форматировании отчета для Discord", e);
            return "";
        }
    }
    
    /**
     * Форматирует отчёт по статистике без данных об участниках (устаревший метод).
     *
     * @param stats объект статистики
     * @return отформатированная строка
     * @deprecated Используйте {@link #formatReportForDiscord(Challenge, ChallengeStats)}
     */
    @Deprecated
    public String formatReportForDiscord(ChallengeStats stats) {
        try {
            logger.debug("Форматирование отчета для Discord (устаревшая версия)");
            
            if (stats == null) {
                logger.warn("Попытка форматирования отчета для Discord с null статистикой");
                return "";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("**Статистика по испытанию: ").append(stats.challengeName()).append("**\n");
            sb.append("Цель: ").append(stats.targetValue()).append("\n");
            sb.append("Выполнено: ").append(stats.currentValue()).append("\n");
            sb.append("Осталось: ").append(stats.remaining()).append("\n");
            // Используем запятую как десятичный разделитель для русской локали
            sb.append("Процент выполнения: ").append(String.format("%.2f", stats.percentage()).replace('.', ',')).append("%\n");
            sb.append("Ежедневная цель: ").append(String.format("%.2f", stats.dailyTarget()).replace('.', ',')).append(" в день\n");
            sb.append("Дней осталось: ").append(stats.daysRemaining()).append("\n");
            
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
                    String userId = entry.getKey();
                    String username = userId; // По умолчанию используем ID
                    
                    logger.debug("Обработка пользователя с ID: {}", userId);
                    
                    // Пытаемся получить имя пользователя из кэша
                    if (participantService != null) {
                        try {
                            Participant participant = participantService.getParticipant(userId);
                            if (participant != null && participant.getUsername() != null
                                    && !participant.getUsername().isEmpty()) {
                                username = participant.getUsername();
                            } else if (discordService != null) {
                                User user = discordService.getJDA().getUserById(userId);
                                if (user != null) username = user.getName();
                            }
                        } catch (Exception e) {
                            logger.debug("Не удалось получить имя пользователя для ID {}: {}", userId, e.getMessage());
                        }
                    } else if (discordService != null) {
                        try {
                            User user = discordService.getJDA().getUserById(userId);
                            if (user != null) username = user.getName();
                        } catch (Exception e) {
                            logger.debug("Не удалось получить имя пользователя для ID {}: {}", userId, e.getMessage());
                        }
                    }
                    
                    sb.append((i + 1)).append(". ").append(username).append(" - ").append(entry.getValue()).append(" ").append(challenge.getUnit()).append("\n");
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
    public String formatChallengeStats(Challenge challenge, ChallengeStats stats) {
        try {
            logger.debug("Форматирование статистики испытания");
            return formatReportForDiscord(challenge, stats);
        } catch (Exception e) {
            logger.error("Ошибка при форматировании статистики испытания", e);
            return "";
        }
    }

    /**
     * Прогнозировать дату завершения испытания на основе среднего темпа за последние 7 дней.
     * Если данных за 7 дней нет, используется общий средний темп.
     */
    @Override
    public LocalDate forecastCompletionDate(String challengeId, String userId) {
        try {
            if (challengeId == null || userId == null) {
                return null;
            }
            String key = challengeId + ":" + userId;
            List<Long> history = progressHistoryCache.get(key);
            if (history == null || history.isEmpty()) {
                return null;
            }
            // Use last 7 entries as "days"
            int windowSize = Math.min(7, history.size());
            List<Long> window = history.subList(history.size() - windowSize, history.size());
            double avgPerDay = window.stream().mapToLong(Long::longValue).average().orElse(0);
            if (avgPerDay <= 0) {
                return null;
            }
            // Get remaining from cache or just return null if not calculable
            return LocalDate.now().plusDays((long) Math.ceil(1 / avgPerDay));
        } catch (Exception e) {
            logger.error("Ошибка при прогнозировании даты завершения", e);
            return null;
        }
    }

    /**
     * Прогнозирует дату завершения испытания для пользователя на основе текущего прогресса и истории.
     * Использует историю за последние 7 дней, при отсутствии — общий средний темп.
     *
     * @param challenge испытание
     * @param userId    идентификатор пользователя
     * @return прогнозируемая дата или {@code null} при недостатке данных
     */
    public LocalDate forecastCompletionDate(Challenge challenge, String userId) {
        try {
            if (challenge == null || userId == null) {
                return null;
            }
            long userProgress = challenge.getParticipantProgress().getOrDefault(userId, 0L);
            long remaining = challenge.getTargetValue() - userProgress;
            if (remaining <= 0) {
                return LocalDate.now(); // already done
            }

            String key = challenge.getId() + ":" + userId;
            List<Long> history = progressHistoryCache.get(key);
            double avgPerDay;
            if (history != null && !history.isEmpty()) {
                int windowSize = Math.min(7, history.size());
                List<Long> window = history.subList(history.size() - windowSize, history.size());
                avgPerDay = window.stream().mapToLong(Long::longValue).average().orElse(0);
            } else {
                // Use overall start-to-now rate
                LocalDate start = challenge.getStartDate() != null ? challenge.getStartDate().toLocalDate() : LocalDate.now();
                long daysSinceStart = ChronoUnit.DAYS.between(start, LocalDate.now());
                if (daysSinceStart <= 0) {
                    return null;
                }
                avgPerDay = (double) userProgress / daysSinceStart;
            }
            if (avgPerDay <= 0) {
                return null;
            }
            long daysNeeded = (long) Math.ceil((double) remaining / avgPerDay);
            return LocalDate.now().plusDays(daysNeeded);
        } catch (Exception e) {
            logger.error("Ошибка при прогнозировании даты завершения", e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String formatDailyReportForDiscord(Challenge challenge, ChallengeStats stats,
                                               List<Map.Entry<String, Long>> topParticipants) {
        try {
            if (challenge == null || stats == null) return "";
            String unit = challenge.getUnit() != null ? challenge.getUnit() : "";
            String typeLabel = challenge.getType() == ChallengeType.GROUP ? "👥 Групповое" : "👤 Личное";
            int participantCount = challenge.getParticipants().size();

            int pct = (int) Math.min(100, Math.max(0, stats.percentage()));
            int filled = pct * 15 / 100;
            String bar = "█".repeat(filled) + "░".repeat(15 - filled);

            String endDate = challenge.getEndDate() != null
                    ? challenge.getEndDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "—";

            StringBuilder sb = new StringBuilder();
            sb.append("**").append(challenge.getName()).append("**\n");
            sb.append(typeLabel).append("  ·  ").append(participantCount).append(" уч.")
              .append("  ·  до **").append(endDate).append("**  ·  **")
              .append(stats.daysRemaining()).append(" дн.**\n\n");

            sb.append("📊 **").append(stats.currentValue()).append("** / ")
              .append(stats.targetValue()).append(" ").append(unit)
              .append("  —  **").append(String.format("%.0f%%", stats.percentage())).append("**\n");
            sb.append("`").append(bar).append("`\n");

            if (stats.remaining() <= 0) {
                sb.append("✅ **Цель достигнута!**\n");
            } else if (stats.daysRemaining() > 0) {
                sb.append("⏳ Осталось: **").append(stats.remaining()).append(" ").append(unit).append("**");
                if (stats.dailyTarget() > 0) {
                    sb.append("  ·  норма **~").append(Math.round(stats.dailyTarget()))
                      .append(" ").append(unit).append("/чел/день**");
                }
                sb.append("\n");
            } else {
                sb.append("⌛ Срок истёк\n");
            }

            if (topParticipants != null && !topParticipants.isEmpty()) {
                sb.append("\n🏆 **Топ-3:**\n");
                String[] medals = {"🥇", "🥈", "🥉"};
                for (int i = 0; i < topParticipants.size(); i++) {
                    var entry = topParticipants.get(i);
                    String medal = i < medals.length ? medals[i] : (i + 1) + ".";
                    sb.append(medal).append(" ").append(resolveUsername(entry.getKey()))
                      .append(" — ").append(entry.getValue()).append(" ").append(unit).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("Ошибка при форматировании ежедневного отчёта для испытания: {}",
                    challenge != null ? challenge.getName() : "null", e);
            return "";
        }
    }

    private String resolveUsername(String userId) {
        if (participantService != null) {
            try {
                Participant participant = participantService.getParticipant(userId);
                if (participant != null && participant.getUsername() != null
                        && !participant.getUsername().isEmpty()) {
                    return participant.getUsername();
                }
            } catch (Exception e) {
                logger.debug("Не удалось получить имя участника для ID {}: {}", userId, e.getMessage());
            }
        }
        if (discordService != null) {
            try {
                User user = discordService.getJDA().getUserById(userId);
                if (user != null) return user.getName();
            } catch (Exception e) {
                logger.debug("Не удалось получить имя Discord для ID {}: {}", userId, e.getMessage());
            }
        }
        return userId;
    }

    /**
     * Записывает ежедневный прогресс в in-memory кэш для последующего прогнозирования.
     * Кэш ограничен {@code MAX_CACHE_SIZE} ключами, для каждого — {@code MAX_HISTORY_PER_KEY} значений.
     *
     * @param challengeId    идентификатор испытания
     * @param userId         идентификатор пользователя
     * @param progressAmount добавленное количество прогресса за день
     */
    public void recordDailyProgress(String challengeId, String userId, long progressAmount) {
        try {
            if (challengeId == null || userId == null) {
                return;
            }
            // Evict if at capacity
            if (progressHistoryCache.size() >= MAX_CACHE_SIZE) {
                String firstKey = progressHistoryCache.keySet().iterator().next();
                progressHistoryCache.remove(firstKey);
            }
            String key = challengeId + ":" + userId;
            List<Long> history = progressHistoryCache.computeIfAbsent(key, k -> new java.util.ArrayList<>());
            history.add(progressAmount);
            // Удаляем старые записи если превышен лимит на ключ (LRU-подобное поведение)
            if (history.size() > MAX_HISTORY_PER_KEY) {
                history.removeFirst();
            }
        } catch (Exception e) {
            logger.error("Ошибка при записи ежедневного прогресса", e);
        }
    }
}