package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для расчета статистики
 */
@Service
public class StatisticsService {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);
    
    // Добавляем зависимости
    private DiscordService discordService;
    private UserService userService;
    
    // Setter для внедрения зависимости
    public void setDiscordService(DiscordService discordService) {
        this.discordService = discordService;
    }
    
    // Setter для внедрения зависимости UserService
    public void setUserService(UserService userService) {
        this.userService = userService;
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
            sb.append("**Статистика по испытанию: ").append(stats.getChallengeName()).append("**\n");
            sb.append("Цель: ").append(stats.getTargetValue()).append("\n");
            sb.append("Выполнено: ").append(stats.getCurrentValue()).append("\n");
            sb.append("Осталось: ").append(stats.getRemaining()).append("\n");
            // Используем запятую как десятичный разделитель для русской локали
            sb.append("Процент выполнения: ").append(String.format("%.2f", stats.getPercentage()).replace('.', ',')).append("%\n");
            sb.append("Ежедневная цель: ").append(String.format("%.2f", stats.getDailyTarget()).replace('.', ',')).append(" в день\n");
            sb.append("Дней осталось: ").append(stats.getDaysRemaining()).append("\n");
            
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
                    if (userService != null) {
                        logger.debug("UserService доступен, попытка получить информацию об участнике: {}", userId);
                        try {
                            Participant participant = userService.getParticipant(userId);
                            if (participant != null) {
                                logger.debug("Участник найден в кэше: {}, имя пользователя: {}", userId, participant.getUsername());
                                if (participant.getUsername() != null && !participant.getUsername().isEmpty()) {
                                    username = participant.getUsername();
                                    logger.debug("Используем имя пользователя из кэша: {}", username);
                                } else {
                                    logger.debug("Имя пользователя в кэше пустое для участника: {}", userId);
                                }
                            } else {
                                logger.debug("Участник не найден в кэше: {}", userId);
                                if (discordService != null) {
                                    // Если в кэше нет имени, пытаемся получить его через Discord API
                                    logger.debug("Попытка получить имя пользователя через Discord API: {}", userId);
                                    User user = discordService.getJDA().getUserById(userId);
                                    if (user != null) {
                                        username = user.getName();
                                        logger.debug("Имя пользователя получено через Discord API: {} для ID: {}", username, userId);
                                        // Обновляем кэш UserService с новым именем пользователя
                                        try {
                                            logger.debug("Обновление кэша UserService для пользователя: {}", userId);
                                            // Пытаемся получить текущие испытания пользователя для сохранения их при обновлении
                                            List<com.discord.challengebot.model.Challenge> userChallenges = userService.getRegisteredChallenges(userId);
                                            logger.debug("Получено {} зарегистрированных испытаний для пользователя: {}", userChallenges.size(), userId);
                                            if (!userChallenges.isEmpty()) {
                                                // Если у пользователя есть зарегистрированные испытания, обновляем имя в кэше
                                                for (com.discord.challengebot.model.Challenge userChallenge : userChallenges) {
                                                    logger.debug("Регистрация пользователя {} с именем {} на испытание {}", userId, username, userChallenge.getName());
                                                    userService.registerForChallenge(userId, username, userChallenge.getName());
                                                }
                                            } else {
                                                // Если у пользователя нет зарегистрированных испытаний, 
                                                // но он есть в таблице лидеров, регистрируем его на текущее испытание
                                                logger.debug("У пользователя {} нет зарегистрированных испытаний, регистрируем на текущее испытание {}", userId, challenge.getName());
                                                userService.registerForChallenge(userId, username, challenge.getName());
                                                
                                                // Проверяем, что участник был успешно зарегистрирован
                                                Participant registeredParticipant = userService.getParticipant(userId);
                                                if (registeredParticipant != null) {
                                                    logger.debug("Участник {} успешно зарегистрирован на испытание {}, имя в кэше: {}", 
                                                                userId, challenge.getName(), registeredParticipant.getUsername());
                                                } else {
                                                    logger.warn("Не удалось зарегистрировать участника {} на испытание {}", userId, challenge.getName());
                                                }
                                            }
                                        } catch (Exception cacheUpdateException) {
                                            logger.debug("Не удалось обновить имя пользователя {} в кэше: {}", userId, cacheUpdateException.getMessage());
                                        }
                                    } else {
                                        logger.debug("Не удалось получить пользователя через Discord API для ID: {}", userId);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.debug("Не удалось получить имя пользователя для ID {}: {}", userId, e.getMessage());
                            // Используем ID как запасной вариант
                        }
                    } else {
                        logger.debug("UserService недоступен");
                        if (discordService != null) {
                            // Если нет UserService, пытаемся получить имя через Discord API
                            logger.debug("Попытка получить имя пользователя через Discord API (UserService недоступен): {}", userId);
                            try {
                                User user = discordService.getJDA().getUserById(userId);
                                if (user != null) {
                                    username = user.getName();
                                    logger.debug("Имя пользователя получено через Discord API: {} для ID: {}", username, userId);
                                } else {
                                    logger.debug("Не удалось получить пользователя через Discord API для ID: {}", userId);
                                }
                            } catch (Exception e) {
                                logger.debug("Не удалось получить имя пользователя для ID {}: {}", userId, e.getMessage());
                            }
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
     * Форматировать отчет для Discord (устаревшая версия для обратной совместимости)
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
                    String userId = entry.getKey();
                    String username = userId; // По умолчанию используем ID
                    
                    logger.debug("Обработка пользователя с ID: {}", userId);
                    
                    // Пытаемся получить имя пользователя из кэша
                    if (userService != null) {
                        logger.debug("UserService доступен, попытка получить информацию об участнике: {}", userId);
                        try {
                            Participant participant = userService.getParticipant(userId);
                            if (participant != null) {
                                logger.debug("Участник найден в кэше: {}, имя пользователя: {}", userId, participant.getUsername());
                                if (participant.getUsername() != null && !participant.getUsername().isEmpty()) {
                                    username = participant.getUsername();
                                    logger.debug("Используем имя пользователя из кэша: {}", username);
                                } else {
                                    logger.debug("Имя пользователя в кэше пустое для участника: {}", userId);
                                }
                            } else {
                                logger.debug("Участник не найден в кэше: {}", userId);
                                if (discordService != null) {
                                    // Если в кэше нет имени, пытаемся получить его через Discord API
                                    logger.debug("Попытка получить имя пользователя через Discord API: {}", userId);
                                    User user = discordService.getJDA().getUserById(userId);
                                    if (user != null) {
                                        username = user.getName();
                                        logger.debug("Имя пользователя получено через Discord API: {} для ID: {}", username, userId);
                                        // Обновляем кэш UserService с новым именем пользователя
                                        try {
                                            logger.debug("Обновление кэша UserService для пользователя: {}", userId);
                                            // Пытаемся получить текущие испытания пользователя для сохранения их при обновлении
                                            List<com.discord.challengebot.model.Challenge> userChallenges = userService.getRegisteredChallenges(userId);
                                            logger.debug("Получено {} зарегистрированных испытаний для пользователя: {}", userChallenges.size(), userId);
                                            if (!userChallenges.isEmpty()) {
                                                // Если у пользователя есть зарегистрированные испытания, обновляем имя в кэше
                                                for (com.discord.challengebot.model.Challenge userChallenge : userChallenges) {
                                                    logger.debug("Регистрация пользователя {} с именем {} на испытание {}", userId, username, userChallenge.getName());
                                                    userService.registerForChallenge(userId, username, userChallenge.getName());
                                                }
                                            } else {
                                                // Если у пользователя нет зарегистрированных испытаний, 
                                                // но он есть в таблице лидеров, регистрируем его на текущее испытание
                                                logger.debug("У пользователя {} нет зарегистрированных испытаний, регистрируем на текущее испытание {}", userId, challenge.getName());
                                                userService.registerForChallenge(userId, username, challenge.getName());
                                                
                                                // Проверяем, что участник был успешно зарегистрирован
                                                Participant registeredParticipant = userService.getParticipant(userId);
                                                if (registeredParticipant != null) {
                                                    logger.debug("Участник {} успешно зарегистрирован на испытание {}, имя в кэше: {}", 
                                                                userId, challenge.getName(), registeredParticipant.getUsername());
                                                } else {
                                                    logger.warn("Не удалось зарегистрировать участника {} на испытание {}", userId, challenge.getName());
                                                }
                                            }
                                        } catch (Exception cacheUpdateException) {
                                            logger.debug("Не удалось обновить имя пользователя {} в кэше: {}", userId, cacheUpdateException.getMessage());
                                        }
                                    } else {
                                        logger.debug("Не удалось получить пользователя через Discord API для ID: {}", userId);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.debug("Не удалось получить имя пользователя для ID {}: {}", userId, e.getMessage());
                            // Используем ID как запасной вариант
                        }
                    } else {
                        logger.debug("UserService недоступен");
                        if (discordService != null) {
                            // Если нет UserService, пытаемся получить имя через Discord API
                            logger.debug("Попытка получить имя пользователя через Discord API (UserService недоступен): {}", userId);
                            try {
                                User user = discordService.getJDA().getUserById(userId);
                                if (user != null) {
                                    username = user.getName();
                                    logger.debug("Имя пользователя получено через Discord API: {} для ID: {}", username, userId);
                                } else {
                                    logger.debug("Не удалось получить пользователя через Discord API для ID: {}", userId);
                                }
                            } catch (Exception e) {
                                logger.debug("Не удалось получить имя пользователя для ID {}: {}", userId, e.getMessage());
                            }
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
}