package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.model.Participant;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Обработчик сообщений Discord
 */
public class DiscordMessageListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(DiscordMessageListener.class);
    
    private final DiscordService discordService;
    private final DiscordConfig discordConfig;
    private final ChallengeService challengeService;
    private final UserService userService;
    private final StatisticsService statisticsService;
    private JDA jda;

    public DiscordMessageListener(DiscordService discordService, DiscordConfig discordConfig,
                                ChallengeService challengeService, UserService userService,
                                StatisticsService statisticsService) {
        this.discordService = discordService;
        this.discordConfig = discordConfig;
        this.challengeService = challengeService;
        this.userService = userService;
        this.statisticsService = statisticsService;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try {
            // Сохраняем ссылку на JDA
            this.jda = event.getJDA();
            
            // Игнорируем сообщения от ботов
            if (event.getAuthor().isBot()) {
                logger.debug("Игнорирование сообщения от бота: {}", event.getAuthor().getName());
                return;
            }

            // Проверяем, что сообщение в правильном канале
            TextChannel channel = event.getChannel().asTextChannel();
            if (!channel.getName().equals(discordConfig.getChannel())) {
                logger.debug("Игнорирование сообщения из канала '{}', ожидается канал '{}'", 
                           channel.getName(), discordConfig.getChannel());
                return;
            }

            String messageContent = event.getMessage().getContentRaw();
            
            // Проверяем, что сообщение начинается с "+"
            if (!messageContent.startsWith("+")) {
                logger.debug("Игнорирование сообщения, не начинающегося с '+': {}", messageContent);
                return;
            }

            // Убираем "+" в начале
            String command = messageContent.substring(1).trim();
            String userId = event.getAuthor().getId();
            String username = event.getAuthor().getName();
            
            logger.info("Обработка команды: {} от пользователя: {} ({})", command, username, userId);
            
            processCommand(command, userId, username, channel.getId());
        } catch (Exception e) {
            logger.error("Ошибка обработки события сообщения от пользователя {}", 
                        event.getAuthor() != null ? event.getAuthor().getName() : "unknown", e);
            
            try {
                TextChannel channel = event.getChannel().asTextChannel();
                channel.sendMessage("Произошла ошибка при обработке команды. Пожалуйста, попробуйте позже.").queue();
            } catch (Exception sendException) {
                logger.error("Ошибка отправки сообщения об ошибке", sendException);
            }
        }
    }

    /**
     * Обработать команду
     */
    private void processCommand(String command, String userId, String username, String channelId) {
        try {
            String[] parts = command.split("\\s+");
            String commandName = parts.length > 0 ? parts[0].toLowerCase() : "";
            
            logger.debug("Обработка команды '{}' с {} параметрами", commandName, parts.length);
            
            // Проверяем авторизацию для команд, требующих прав администратора
            if (!discordService.isAuthorizedUser(userId, commandName)) {
                // Отправляем сообщение в канал
                TextChannel channel = jda.getTextChannelById(channelId);
                if (channel != null) {
                    channel.sendMessage("У вас нет прав для выполнения этой команды.").queue();
                    logger.warn("Пользователь {} попытался выполнить команду '{}' без необходимых прав", 
                               username, commandName);
                }
                return;
            }
            
            switch (commandName) {
                case "помощь":
                    handleHelpCommand(channelId, userId);
                    break;
                case "статистика":
                    handleStatisticsCommand(parts, channelId);
                    break;
                case "новый":
                    handleNewChallengeCommand(parts, userId, username, channelId);
                    break;
                case "удалить":
                    handleDeleteChallengeCommand(parts, channelId);
                    break;
                case "остановить":
                    handleStopChallengeCommand(parts, channelId);
                    break;
                case "продолжить":
                    handleResumeChallengeCommand(parts, channelId);
                    break;
                case "изменить":
                    handleChangeTargetCommand(parts, channelId);
                    break;
                case "изменить_дату":
                    handleChangeEndDateCommand(parts, channelId);
                    break;
                case "установить_прогресс":
                    handleSetParticipantProgressCommand(parts, channelId);
                    break;
                case "добавить_участника":
                    handleAddParticipantCommand(parts, channelId);
                    break;
                case "удалить_участника":
                    handleRemoveParticipantCommand(parts, channelId);
                    break;
                case "мои":
                    handleMyChallengesCommand(userId, channelId);
                    break;
                case "регистрация":
                    handleRegistrationCommand(parts, userId, username, channelId);
                    break;
                case "топ":
                    handleLeaderboardCommand(parts, channelId);
                    break;
                case "прогресс":
                    handleProgressCommand(parts, userId, channelId);
                    break;
                case "испытания":
                    handleListChallengesCommand(channelId);
                    break;
                case "обновить_имя":
                    handleUpdateUsernameCommand(parts, userId, username, channelId);
                    break;
                default:
                    // Считаем, что это команда добавления прогресса к испытанию
                    handleProgressUpdateCommand(command, userId, username, channelId);
                    break;
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды '{}' от пользователя {}", command, username, e);
            
            try {
                TextChannel channel = jda.getTextChannelById(channelId);
                if (channel != null) {
                    channel.sendMessage("Произошла ошибка при обработке команды. Пожалуйста, попробуйте позже.").queue();
                }
            } catch (Exception sendException) {
                logger.error("Ошибка отправки сообщения об ошибке", sendException);
            }
        }
    }

    /**
     * Обработать команду помощи
     */
    private void handleHelpCommand(String channelId, String userId) {
        try {
            logger.debug("Обработка команды помощи для пользователя: {}", userId);
            String helpMessage = discordService.generateHelpMessage(userId);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(helpMessage).queue();
                logger.info("Сообщение помощи отправлено в канал {} для пользователя {}", channelId, userId);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды помощи", e);
        }
    }

    /**
     * Обработать команду статистики
     */
    private void handleStatisticsCommand(String[] parts, String channelId) {
        try {
            logger.debug("Обработка команды статистики с {} параметрами", parts.length);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды статистики", channelId);
                return;
            }
            
            if (parts.length == 1) {
                // Общая статистика по всем испытаниям
                List<Challenge> challenges = challengeService.getAllChallenges();
                if (challenges.isEmpty()) {
                    channel.sendMessage("Нет доступных испытаний.").queue();
                    logger.info("Нет доступных испытаний для отображения общей статистики");
                    return;
                }
                
                StringBuilder message = new StringBuilder();
                message.append("**Статистика по всем испытаниям:**\n\n");
                
                for (Challenge challenge : challenges) {
                    ChallengeStats stats = challengeService.getChallengeStats(challenge);
                    if (stats != null) {
                        message.append(statisticsService.formatReportForDiscord(challenge, stats)).append("\n");
                    }
                }
                
                channel.sendMessage(message.toString()).queue();
                logger.info("Общая статистика по {} испытаниям отправлена", challenges.size());
            } else {
                // Статистика по конкретному испытанию
                String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
                Challenge challenge = challengeService.getChallenge(challengeName);
                
                if (challenge == null) {
                    channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                    logger.warn("Испытание '{}' не найдено при запросе статистики", challengeName);
                    return;
                }
                
                ChallengeStats stats = challengeService.getChallengeStats(challenge);
                if (stats != null) {
                    String formattedStats = discordService.formatChallengeStats(challenge, stats);
                    channel.sendMessage(formattedStats).queue();
                    logger.info("Статистика по испытанию '{}' отправлена", challengeName);
                } else {
                    channel.sendMessage("Ошибка при получении статистики по испытанию \"" + challengeName + "\".").queue();
                    logger.error("Ошибка при получении статистики по испытанию '{}'", challengeName);
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды статистики", e);
        }
    }

    /**
     * Обработать команду создания нового испытания
     */
    private void handleNewChallengeCommand(String[] parts, String userId, String username, String channelId) {
        try {
            logger.debug("Обработка команды создания нового испытания с {} параметрами", parts.length);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды создания испытания", channelId);
                return;
            }
            
            // Проверяем, является ли пользователь администратором
            boolean isAdmin = userService.isAdminUser(userId);
            
            if (parts.length < 3) {
                if (isAdmin) {
                    channel.sendMessage("Недостаточно параметров. Используйте: +новый <название> <цель> [дата окончания] [тип]").queue();
                } else {
                    channel.sendMessage("Недостаточно параметров. Используйте: +новый <название> <цель> [дата окончания]").queue();
                }
                logger.warn("Недостаточно параметров для создания испытания: {}", parts.length);
                return;
            }
            
            String name = parts[1];
            long target;
            try {
                target = Long.parseLong(parts[2]);
                // Проверка на отрицательные числа
                if (target < 0) {
                    channel.sendMessage("Цель не может быть отрицательным числом.").queue();
                    logger.warn("Попытка установить отрицательную цель '{}'", parts[2]);
                    return;
                }
            } catch (NumberFormatException e) {
                channel.sendMessage("Цель должна быть числом.").queue();
                logger.warn("Неверный формат цели '{}': {}", parts[2], e.getMessage());
                return;
            }
            
            LocalDateTime endDate = LocalDateTime.now().plusDays(365); // По умолчанию год
            ChallengeType type = ChallengeType.GROUP; // По умолчанию групповое для администраторов
            String description = "Испытание по " + name;
            String unit = "единиц";
            
            // Для обычных пользователей тип всегда индивидуальный
            if (!isAdmin) {
                type = ChallengeType.INDIVIDUAL;
            }
            
            if (parts.length > 3) {
                try {
                    // Парсим дату в формате dd.MM.yyyy и устанавливаем время на начало дня
                    endDate = LocalDateTime.parse(parts[3] + " 00:00:00", DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                } catch (DateTimeParseException e) {
                    try {
                        // Если не удалось, пробуем формат dd.MM.yyyy без времени
                        endDate = LocalDate.parse(parts[3], DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay();
                    } catch (DateTimeParseException e2) {
                        try {
                            // Если не удалось, пробуем формат yyyy-MM-dd для обратной совместимости
                            endDate = LocalDateTime.parse(parts[3], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        } catch (DateTimeParseException e3) {
                            logger.warn("Неверный формат даты '{}', используется значение по умолчанию: {}", parts[3], e.getMessage());
                            // Если не удалось распарсить дату, используем значение по умолчанию
                        }
                    }
                }
            }
            
            if (parts.length > 4 && isAdmin) {
                if ("индивидуальное".equalsIgnoreCase(parts[4])) {
                    type = ChallengeType.INDIVIDUAL;
                }
            }
            
            Challenge challenge = challengeService.createChallenge(name, target, endDate, type, description, unit);
            if (challenge != null) {
                channel.sendMessage("Испытание \"" + name + "\" успешно создано!").queue();
                logger.info("Испытание '{}' успешно создано пользователем {}", name, username);
            } else {
                channel.sendMessage("Ошибка при создании испытания \"" + name + "\".").queue();
                logger.error("Ошибка при создании испытания '{}'", name);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды создания испытания", e);
        }
    }

    /**
     * Обработать команду удаления испытания
     */
    private void handleDeleteChallengeCommand(String[] parts, String channelId) {
        try {
            logger.debug("Обработка команды удаления испытания с {} параметрами", parts.length);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды удаления испытания", channelId);
                return;
            }
            
            if (parts.length < 2) {
                channel.sendMessage("Укажите название испытания. Используйте: +удалить <название>").queue();
                logger.warn("Недостаточно параметров для удаления испытания: {}", parts.length);
                return;
            }
            
            String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            boolean deleted = challengeService.deleteChallenge(challengeName);
            
            if (deleted) {
                channel.sendMessage("Испытание \"" + challengeName + "\" успешно удалено.").queue();
                logger.info("Испытание '{}' успешно удалено", challengeName);
            } else {
                channel.sendMessage("Не удалось удалить испытание \"" + challengeName + "\".").queue();
                logger.error("Ошибка при удалении испытания '{}'", challengeName);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды удаления испытания", e);
        }
    }

    /**
     * Обработать команду остановки испытания
     */
    private void handleStopChallengeCommand(String[] parts, String channelId) {
        try {
            logger.debug("Обработка команды остановки испытания с {} параметрами", parts.length);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды остановки испытания", channelId);
                return;
            }
            
            if (parts.length < 2) {
                channel.sendMessage("Укажите название испытания. Используйте: +остановить <название>").queue();
                logger.warn("Недостаточно параметров для остановки испытания: {}", parts.length);
                return;
            }
            
            String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            Challenge challenge = challengeService.getChallenge(challengeName);
            
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                logger.warn("Испытание '{}' не найдено при попытке остановки", challengeName);
                return;
            }
            
            Challenge updatedChallenge = challengeService.updateChallengeStatus(challenge, false);
            if (updatedChallenge != null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" остановлено.").queue();
                logger.info("Испытание '{}' успешно остановлено", challengeName);
            } else {
                channel.sendMessage("Ошибка при остановке испытания \"" + challengeName + "\".").queue();
                logger.error("Ошибка при остановке испытания '{}'", challengeName);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды остановки испытания", e);
        }
    }

    /**
     * Обработать команду возобновления испытания
     */
    private void handleResumeChallengeCommand(String[] parts, String channelId) {
        try {
            logger.debug("Обработка команды возобновления испытания с {} параметрами", parts.length);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды возобновления испытания", channelId);
                return;
            }
            
            if (parts.length < 2) {
                channel.sendMessage("Укажите название испытания. Используйте: +продолжить <название>").queue();
                logger.warn("Недостаточно параметров для возобновления испытания: {}", parts.length);
                return;
            }
            
            String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            Challenge challenge = challengeService.getChallenge(challengeName);
            
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                logger.warn("Испытание '{}' не найдено при попытке возобновления", challengeName);
                return;
            }
            
            Challenge updatedChallenge = challengeService.updateChallengeStatus(challenge, true);
            if (updatedChallenge != null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" возобновлено.").queue();
                logger.info("Испытание '{}' успешно возобновлено", challengeName);
            } else {
                channel.sendMessage("Ошибка при возобновлении испытания \"" + challengeName + "\".").queue();
                logger.error("Ошибка при возобновлении испытания '{}'", challengeName);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды возобновления испытания", e);
        }
    }

    /**
     * Обработать команду изменения цели испытания
     */
    private void handleChangeTargetCommand(String[] parts, String channelId) {
        try {
            logger.debug("Обработка команды изменения цели испытания с {} параметрами", parts.length);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды изменения цели испытания", channelId);
                return;
            }
            
            if (parts.length < 3) {
                channel.sendMessage("Недостаточно параметров. Используйте: +изменить <название> <новая цель>").queue();
                logger.warn("Недостаточно параметров для изменения цели испытания: {}", parts.length);
                return;
            }
            
            String challengeName = parts[1];
            long newTarget;
            try {
                newTarget = Long.parseLong(parts[2]);
                // Проверка на отрицательные числа
                if (newTarget < 0) {
                    channel.sendMessage("Цель не может быть отрицательным числом.").queue();
                    logger.warn("Попытка установить отрицательную цель '{}'", parts[2]);
                    return;
                }
            } catch (NumberFormatException e) {
                channel.sendMessage("Цель должна быть числом.").queue();
                logger.warn("Неверный формат новой цели '{}': {}", parts[2], e.getMessage());
                return;
            }
            
            Challenge challenge = challengeService.getChallenge(challengeName);
            
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                logger.warn("Испытание '{}' не найдено при попытке изменения цели", challengeName);
                return;
            }
            
            Challenge updatedChallenge = challengeService.updateChallengeTarget(challenge, newTarget);
            if (updatedChallenge != null) {
                channel.sendMessage("Цель испытания \"" + challengeName + "\" изменена на " + newTarget + ".").queue();
                logger.info("Цель испытания '{}' успешно изменена на {}", challengeName, newTarget);
            } else {
                channel.sendMessage("Ошибка при изменении цели испытания \"" + challengeName + "\".").queue();
                logger.error("Ошибка при изменении цели испытания '{}'", challengeName);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды изменения цели испытания", e);
        }
    }

    /**
     * Обработать команду изменения даты окончания испытания
     */
    private void handleChangeEndDateCommand(String[] parts, String channelId) {
        try {
            logger.debug("Обработка команды изменения даты окончания испытания с {} параметрами", parts.length);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды изменения даты окончания испытания", channelId);
                return;
            }
            
            if (parts.length < 3) {
                channel.sendMessage("Недостаточно параметров. Используйте: +изменить_дату <название> <новая дата окончания>").queue();
                logger.warn("Недостаточно параметров для изменения даты окончания испытания: {}", parts.length);
                return;
            }
            
            String challengeName = parts[1];
            LocalDateTime newEndDate;
            try {
                // Парсим дату в формате dd.MM.yyyy и устанавливаем время на начало дня
                newEndDate = LocalDateTime.parse(parts[2] + " 00:00:00", DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
            } catch (DateTimeParseException e) {
                try {
                    // Если не удалось, пробуем формат dd.MM.yyyy без времени
                    newEndDate = LocalDate.parse(parts[2], DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay();
                } catch (DateTimeParseException e2) {
                    try {
                        // Если не удалось, пробуем формат yyyy-MM-dd для обратной совместимости
                        newEndDate = LocalDateTime.parse(parts[2], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    } catch (DateTimeParseException e3) {
                        channel.sendMessage("Дата должна быть в формате dd.MM.yyyy (например: 31.12.2025).").queue();
                        logger.warn("Неверный формат даты '{}': {}", parts[2], e.getMessage());
                        return;
                    }
                }
            }
            
            Challenge challenge = challengeService.getChallenge(challengeName);
            
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                logger.warn("Испытание '{}' не найдено при попытке изменения даты окончания", challengeName);
                return;
            }
            
            Challenge updatedChallenge = challengeService.updateChallengeEndDate(challenge, newEndDate);
            if (updatedChallenge != null) {
                channel.sendMessage("Дата окончания испытания \"" + challengeName + "\" изменена на " + parts[2] + ".").queue();
                logger.info("Дата окончания испытания '{}' успешно изменена на {}", challengeName, parts[2]);
            } else {
                channel.sendMessage("Ошибка при изменении даты окончания испытания \"" + challengeName + "\".").queue();
                logger.error("Ошибка при изменении даты окончания испытания '{}'", challengeName);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды изменения даты окончания испытания", e);
        }
    }

    /**
     * Обработать команду установки прогресса участника
     */
    private void handleSetParticipantProgressCommand(String[] parts, String channelId) {
        try {
            logger.debug("Обработка команды установки прогресса участника с {} параметрами", parts.length);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды установки прогресса участника", channelId);
                return;
            }
            
            if (parts.length < 4) {
                channel.sendMessage("Недостаточно параметров. Используйте: +установить_прогресс <испытание> <пользователь> <количество>").queue();
                logger.warn("Недостаточно параметров для установки прогресса участника: {}", parts.length);
                return;
            }
            
            String challengeName = parts[1];
            String userMention = parts[2]; // Формат: <@123456789>
            long progress;
            try {
                progress = Long.parseLong(parts[3]);
                // Проверка на отрицательные числа
                if (progress < 0) {
                    channel.sendMessage("Прогресс не может быть отрицательным числом.").queue();
                    logger.warn("Попытка установить отрицательный прогресс '{}'", parts[3]);
                    return;
                }
            } catch (NumberFormatException e) {
                channel.sendMessage("Количество должно быть числом.").queue();
                logger.warn("Неверный формат количества '{}': {}", parts[3], e.getMessage());
                return;
            }
            
            // Извлекаем ID пользователя из упоминания
            String userId = userMention.replaceAll("[^0-9]", "");
            
            // Получаем имя пользователя через Discord API
            String username = userId; // По умолчанию используем ID
            try {
                net.dv8tion.jda.api.entities.User user = jda.getUserById(userId);
                if (user != null) {
                    username = user.getName();
                }
            } catch (Exception e) {
                logger.debug("Не удалось получить имя пользователя для ID {}: {}", userId, e.getMessage());
            }
            
            Challenge challenge = challengeService.getChallenge(challengeName);
            
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                logger.warn("Испытание '{}' не найдено при попытке установки прогресса участника", challengeName);
                return;
            }
            
            // Регистрируем пользователя в системе перед установкой прогресса
            userService.registerForChallenge(userId, username, challengeName);
            
            Challenge updatedChallenge = challengeService.setParticipantProgress(challenge, userId, progress);
            if (updatedChallenge != null) {
                channel.sendMessage("Прогресс участника <@" + userId + "> в испытании \"" + challengeName + "\" установлен на " + progress + ".").queue();
                logger.info("Прогресс участника '{}' в испытании '{}' установлен на {}", userId, challengeName, progress);
            } else {
                channel.sendMessage("Ошибка при установке прогресса участника <@" + userId + "> в испытании \"" + challengeName + "\".").queue();
                logger.error("Ошибка при установке прогресса участника '{}' в испытании '{}'", userId, challengeName);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды установки прогресса участника", e);
        }
    }

    /**
     * Обработать команду добавления участника
     */
    private void handleAddParticipantCommand(String[] parts, String channelId) {
        try {
            logger.debug("Обработка команды добавления участника с {} параметрами", parts.length);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды добавления участника", channelId);
                return;
            }
            
            if (parts.length < 3) {
                channel.sendMessage("Недостаточно параметров. Используйте: +добавить_участника <испытание> <пользователь>").queue();
                logger.warn("Недостаточно параметров для добавления участника: {}", parts.length);
                return;
            }
            
            String challengeName = parts[1];
            String userMention = parts[2]; // Формат: <@123456789>
            
            // Извлекаем ID пользователя из упоминания
            String userId = userMention.replaceAll("[^0-9]", "");
            
            logger.debug("Попытка добавления участника с ID: {} в испытание: {}", userId, challengeName);
            
            // Получаем имя пользователя через Discord API
            String username = userId; // По умолчанию используем ID
            boolean usernameRetrieved = false;
            try {
                net.dv8tion.jda.api.entities.User user = jda.getUserById(userId);
                if (user != null) {
                    username = user.getName();
                    usernameRetrieved = true;
                    logger.debug("Имя пользователя получено через Discord API: {} для ID: {}", username, userId);
                } else {
                    logger.debug("Пользователь с ID {} не найден через Discord API", userId);
                }
            } catch (Exception e) {
                logger.debug("Не удалось получить имя пользователя для ID {}: {}", userId, e.getMessage());
            }
            
            // Если не удалось получить имя пользователя через Discord API, пытаемся получить его из кэша
            if (!usernameRetrieved) {
                try {
                    Participant cachedParticipant = userService.getParticipant(userId);
                    if (cachedParticipant != null && cachedParticipant.getUsername() != null && !cachedParticipant.getUsername().isEmpty()) {
                        username = cachedParticipant.getUsername();
                        usernameRetrieved = true;
                        logger.debug("Имя пользователя получено из кэша: {} для ID: {}", username, userId);
                    }
                } catch (Exception e) {
                    logger.debug("Не удалось получить имя пользователя из кэша для ID {}: {}", userId, e.getMessage());
                }
            }
            
            // Если так и не удалось получить имя пользователя, используем ID как запасной вариант
            if (!usernameRetrieved) {
                username = "user_" + userId; // Более дружелюбная замена для ID
                logger.debug("Используем сгенерированное имя для пользователя с ID: {}", userId);
            }
            
            Challenge challenge = challengeService.getChallenge(challengeName);
            
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                logger.warn("Испытание '{}' не найдено при попытке добавления участника", challengeName);
                return;
            }
            
            logger.debug("Испытание '{}' найдено, попытка добавления участника {} ({})", challengeName, username, userId);
            
            // Используем новый метод, который регистрирует пользователя с именем
            Challenge updatedChallenge = challengeService.addParticipantWithUsername(challenge, userId, username);
            if (updatedChallenge != null) {
                channel.sendMessage("Участник <@" + userId + "> добавлен в испытание \"" + challengeName + "\".").queue();
                logger.info("Участник '{}' добавлен в испытание '{}'", userId, challengeName);
                
                // Проверяем, что участник был добавлен в список участников испытания
                if (updatedChallenge.hasParticipant(userId)) {
                    logger.debug("Участник {} успешно добавлен в список участников испытания '{}'", userId, challengeName);
                } else {
                    logger.warn("Участник {} не был добавлен в список участников испытания '{}'", userId, challengeName);
                }
                
                // Проверяем, что у участника есть запись о прогрессе
                if (updatedChallenge.getParticipantProgress().containsKey(userId)) {
                    logger.debug("У участника {} есть запись о прогрессе в испытании '{}'", userId, challengeName);
                } else {
                    logger.warn("У участника {} нет записи о прогрессе в испытании '{}'", userId, challengeName);
                }
            } else {
                channel.sendMessage("Ошибка при добавлении участника <@" + userId + "> в испытание \"" + challengeName + "\".").queue();
                logger.error("Ошибка при добавлении участника '{}' в испытание '{}'", userId, challengeName);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды добавления участника", e);
        }
    }

    /**
     * Обработать команду удаления участника
     */
    private void handleRemoveParticipantCommand(String[] parts, String channelId) {
        try {
            logger.debug("Обработка команды удаления участника с {} параметрами", parts.length);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды удаления участника", channelId);
                return;
            }
            
            if (parts.length < 3) {
                channel.sendMessage("Недостаточно параметров. Используйте: +удалить_участника <испытание> <пользователь>").queue();
                logger.warn("Недостаточно параметров для удаления участника: {}", parts.length);
                return;
            }
            
            String challengeName = parts[1];
            String userMention = parts[2]; // Формат: <@123456789>
            
            // Извлекаем ID пользователя из упоминания
            String userId = userMention.replaceAll("[^0-9]", "");
            
            // Получаем имя пользователя через Discord API
            String username = userId; // По умолчанию используем ID
            try {
                net.dv8tion.jda.api.entities.User user = jda.getUserById(userId);
                if (user != null) {
                    username = user.getName();
                }
            } catch (Exception e) {
                logger.debug("Не удалось получить имя пользователя для ID {}: {}", userId, e.getMessage());
            }
            
            Challenge challenge = challengeService.getChallenge(challengeName);
            
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                logger.warn("Испытание '{}' не найдено при попытке удаления участника", challengeName);
                return;
            }
            
            Challenge updatedChallenge = challengeService.removeParticipant(challenge, userId);
            if (updatedChallenge != null) {
                channel.sendMessage("Участник <@" + userId + "> удален из испытания \"" + challengeName + "\".").queue();
                logger.info("Участник '{}' удален из испытания '{}'", userId, challengeName);
            } else {
                channel.sendMessage("Ошибка при удалении участника <@" + userId + "> из испытания \"" + challengeName + "\".").queue();
                logger.error("Ошибка при удалении участника '{}' из испытания '{}'", userId, challengeName);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды удаления участника", e);
        }
    }

    /**
     * Обработать команду личных испытаний
     */
    private void handleMyChallengesCommand(String userId, String channelId) {
        try {
            logger.debug("Обработка команды личных испытаний для пользователя {}", userId);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды личных испытаний", channelId);
                return;
            }
            
            List<Challenge> userChallenges = challengeService.getUserChallenges(userId);
            
            if (userChallenges.isEmpty()) {
                channel.sendMessage("У вас нет активных испытаний.").queue();
                logger.info("У пользователя {} нет активных испытаний", userId);
                return;
            }
            
            StringBuilder message = new StringBuilder();
            message.append("**Ваши испытания:**\n\n");
            
            for (Challenge challenge : userChallenges) {
                ChallengeStats stats = challengeService.getChallengeStats(challenge);
                if (stats != null) {
                    message.append("- ").append(challenge.getName()).append(": ")
                           .append(stats.getCurrentValue()).append("/").append(stats.getTargetValue())
                           .append(" (").append(String.format("%.2f", stats.getPercentage())).append("%)\n");
                }
            }
            
            channel.sendMessage(message.toString()).queue();
            logger.info("Список из {} личных испытаний отправлен пользователю {}", userChallenges.size(), userId);
        } catch (Exception e) {
            logger.error("Ошибка обработки команды личных испытаний для пользователя {}", userId, e);
        }
    }

    /**
     * Обработать команду регистрации
     */
    private void handleRegistrationCommand(String[] parts, String userId, String username, String channelId) {
        try {
            logger.debug("Обработка команды регистрации с {} параметрами для пользователя {}", parts.length, username);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды регистрации", channelId);
                return;
            }
            
            if (parts.length < 2) {
                channel.sendMessage("Укажите название испытания. Используйте: +регистрация <название>").queue();
                logger.warn("Недостаточно параметров для регистрации: {}", parts.length);
                return;
            }
            
            String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            Challenge challenge = challengeService.getChallenge(challengeName);
            
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                logger.warn("Испытание '{}' не найдено при попытке регистрации пользователя {}", challengeName, username);
                return;
            }
            
            // Используем новый метод, который регистрирует пользователя с именем
            Challenge updatedChallenge = challengeService.addParticipantWithUsername(challenge, userId, username);
            if (updatedChallenge != null) {
                channel.sendMessage("Вы успешно зарегистрированы на испытание \"" + challengeName + "\".").queue();
                logger.info("Пользователь '{}' успешно зарегистрирован на испытание '{}'", username, challengeName);
            } else {
                channel.sendMessage("Ошибка при регистрации на испытание \"" + challengeName + "\".").queue();
                logger.error("Ошибка при регистрации пользователя '{}' на испытание '{}'", username, challengeName);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды регистрации для пользователя {}", username, e);
        }
    }

    /**
     * Обработать команду таблицы лидеров
     */
    private void handleLeaderboardCommand(String[] parts, String channelId) {
        try {
            logger.debug("Обработка команды таблицы лидеров с {} параметрами", parts.length);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды таблицы лидеров", channelId);
                return;
            }
            
            if (parts.length < 2) {
                channel.sendMessage("Укажите название испытания. Используйте: +топ <испытание> [количество]").queue();
                logger.warn("Недостаточно параметров для таблицы лидеров: {}", parts.length);
                return;
            }
            
            String challengeName = parts[1];
            int limit = 5; // По умолчанию показываем топ-5
            
            if (parts.length > 2) {
                try {
                    limit = Integer.parseInt(parts[2]);
                    // Ограничиваем количество участников в списке
                    limit = Math.min(limit, 20);
                } catch (NumberFormatException e) {
                    logger.warn("Неверный формат лимита '{}', используется значение по умолчанию: {}", parts[2], e.getMessage());
                    // Используем значение по умолчанию
                }
            }
            
            Challenge challenge = challengeService.getChallenge(challengeName);
            
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                logger.warn("Испытание '{}' не найдено при запросе таблицы лидеров", challengeName);
                return;
            }
            
            List<Map.Entry<String, Long>> leaderboard = challengeService.getTopParticipants(challenge, limit);
            String leaderboardMessage = statisticsService.formatLeaderboardForDiscord(challenge, leaderboard);
            channel.sendMessage(leaderboardMessage).queue();
            logger.info("Таблица лидеров по испытанию '{}' отправлена ({} участников)", challengeName, leaderboard.size());
        } catch (Exception e) {
            logger.error("Ошибка обработки команды таблицы лидеров", e);
        }
    }

    /**
     * Обработать команду личного прогресса
     */
    private void handleProgressCommand(String[] parts, String userId, String channelId) {
        try {
            logger.debug("Обработка команды личного прогресса с {} параметрами для пользователя {}", parts.length, userId);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды личного прогресса", channelId);
                return;
            }
            
            if (parts.length < 2) {
                channel.sendMessage("Укажите название испытания. Используйте: +прогресс <испытание>").queue();
                logger.warn("Недостаточно параметров для личного прогресса: {}", parts.length);
                return;
            }
            
            String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            Challenge challenge = challengeService.getChallenge(challengeName);
            
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                logger.warn("Испытание '{}' не найдено при запросе личного прогресса пользователя {}", challengeName, userId);
                return;
            }
            
            Long userProgress = challenge.getParticipantProgress().getOrDefault(userId, 0L);
            String message = String.format("**Ваш прогресс по испытанию \"%s\":**\n%s: %d/%d (%.2f%%)", 
                                         challenge.getName(), challenge.getUnit(), userProgress, 
                                         challenge.getTargetValue(), 
                                         challenge.getTargetValue() > 0 ? (double) userProgress / challenge.getTargetValue() * 100 : 0);
            
            channel.sendMessage(message).queue();
            logger.info("Личный прогресс пользователя {} по испытанию '{}' отправлен", userId, challengeName);
        } catch (Exception e) {
            logger.error("Ошибка обработки команды личного прогресса для пользователя {}", userId, e);
        }
    }

    /**
     * Обработать команду списка испытаний
     */
    private void handleListChallengesCommand(String channelId) {
        try {
            logger.debug("Обработка команды списка испытаний");
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды списка испытаний", channelId);
                return;
            }
            
            List<Challenge> activeChallenges = challengeService.getActiveChallenges();
            
            if (activeChallenges.isEmpty()) {
                channel.sendMessage("Активных испытаний нет.").queue();
                logger.info("Нет активных испытаний для отображения");
                return;
            }
            
            StringBuilder message = new StringBuilder();
            message.append("**Активные испытания:**\n\n");
            
            for (Challenge challenge : activeChallenges) {
                message.append("- **").append(challenge.getName()).append("**\n");
                message.append("  Цель: ").append(challenge.getTargetValue()).append(" ").append(challenge.getUnit()).append("\n");
                message.append("  Участников: ").append(challenge.getParticipants().size()).append("\n");
                message.append("  Окончание: ").append(challenge.getEndDate().toLocalDate().toString()).append("\n\n");
            }
            
            channel.sendMessage(message.toString()).queue();
            logger.info("Список из {} активных испытаний отправлен", activeChallenges.size());
        } catch (Exception e) {
            logger.error("Ошибка обработки команды списка испытаний", e);
        }
    }

    /**
     * Обработать команду обновления имени пользователя
     */
    private void handleUpdateUsernameCommand(String[] parts, String userId, String currentUsername, String channelId) {
        try {
            logger.debug("Обработка команды обновления имени пользователя для пользователя {}", currentUsername);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды обновления имени пользователя", channelId);
                return;
            }
            
            // Получаем новое имя пользователя из команды или используем текущее имя из Discord
            String newUsername = currentUsername; // По умолчанию используем текущее имя
            
            if (parts.length > 1) {
                // Если указано новое имя в команде, используем его
                newUsername = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            }
            
            // Обновляем информацию об участнике в хранилище
            boolean updated = userService.updateParticipantUsername(userId, newUsername);
            
            if (updated) {
                channel.sendMessage("Ваше имя успешно обновлено на: " + newUsername).queue();
                logger.info("Имя пользователя {} успешно обновлено на {}", userId, newUsername);
            } else {
                channel.sendMessage("Ошибка при обновлении вашего имени. Пожалуйста, попробуйте позже.").queue();
                logger.error("Ошибка при обновлении имени пользователя {}", userId);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды обновления имени пользователя для пользователя {}", currentUsername, e);
        }
    }

    /**
     * Обработать команду обновления прогресса
     */
    private void handleProgressUpdateCommand(String command, String userId, String username, String channelId) {
        try {
            logger.debug("Обработка команды обновления прогресса '{}' для пользователя {}", command, username);
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.warn("Канал {} не найден при обработке команды обновления прогресса", channelId);
                return;
            }
            
            String[] parts = command.split("\\s+");
            if (parts.length < 2) {
                channel.sendMessage("Недостаточно параметров. Используйте: +<испытание> <количество>").queue();
                logger.warn("Недостаточно параметров для обновления прогресса: {}", parts.length);
                return;
            }
            
            String challengeName = parts[0];
            long amount;
            try {
                amount = Long.parseLong(parts[1]);
                // Проверка на отрицательные числа
                if (amount < 0) {
                    channel.sendMessage("Количество не может быть отрицательным числом.").queue();
                    logger.warn("Попытка установить отрицательное количество '{}'", parts[1]);
                    return;
                }
            } catch (NumberFormatException e) {
                channel.sendMessage("Количество должно быть числом.").queue();
                logger.warn("Неверный формат количества '{}': {}", parts[1], e.getMessage());
                return;
            }
            
            Challenge challenge = challengeService.getChallenge(challengeName);
            
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                logger.warn("Испытание '{}' не найдено при попытке обновления прогресса пользователя {}", challengeName, username);
                return;
            }
            
            if (!challenge.isActive()) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не активно.").queue();
                logger.warn("Попытка обновить прогресс по неактивному испытанию '{}'", challengeName);
                return;
            }
            
            Challenge updatedChallenge = challengeService.addProgress(challenge, userId, username, amount);
            if (updatedChallenge != null) {
                // Получаем общий прогресс пользователя по этому испытанию
                long userTotalProgress = updatedChallenge.getParticipantProgress().getOrDefault(userId, 0L);
                // Получаем общий прогресс по всем участникам
                long totalChallengeProgress = updatedChallenge.getCurrentValue();
                long targetValue = updatedChallenge.getTargetValue();
                
                String message = String.format("Прогресс по испытанию \"%s\" обновлен на %d, общее количество выполненных тобой действий - %d. Общий прогресс %d/%d.", 
                                             challengeName, amount, userTotalProgress, totalChallengeProgress, targetValue);
                channel.sendMessage(message).queue();
                logger.info("Прогресс пользователя {} по испытанию '{}' обновлен на {}", username, challengeName, amount);
            } else {
                channel.sendMessage("Ошибка при обновлении прогресса по испытанию \"" + challengeName + "\".").queue();
                logger.error("Ошибка при обновлении прогресса пользователя {} по испытанию '{}'", username, challengeName);
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды обновления прогресса для пользователя {}", username, e);
        }
    }
}