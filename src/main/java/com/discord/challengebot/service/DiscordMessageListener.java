package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
        // Игнорируем сообщения от ботов
        if (event.getAuthor().isBot()) {
            return;
        }

        // Проверяем, что сообщение в правильном канале
        TextChannel channel = event.getChannel().asTextChannel();
        if (!channel.getName().equals(discordConfig.getChannel())) {
            return;
        }

        String messageContent = event.getMessage().getContentRaw();
        
        // Проверяем, что сообщение начинается с "+"
        if (!messageContent.startsWith("+")) {
            return;
        }

        // Убираем "+" в начале
        String command = messageContent.substring(1).trim();
        String userId = event.getAuthor().getId();
        String username = event.getAuthor().getName();
        
        logger.info("Обработка команды: {} от пользователя: {}", command, username);
        
        try {
            processCommand(command, userId, username, channel.getId());
        } catch (Exception e) {
            logger.error("Ошибка обработки команды: " + command, e);
            channel.sendMessage("Произошла ошибка при обработке команды. Пожалуйста, попробуйте позже.").queue();
        }
    }

    /**
     * Обработать команду
     */
    private void processCommand(String command, String userId, String username, String channelId) {
        String[] parts = command.split("\\s+");
        String commandName = parts[0].toLowerCase();
        
        // Проверяем авторизацию для команд, требующих прав администратора
        if (!discordService.isAuthorizedUser(userId, commandName)) {
            // Отправляем сообщение в канал
            // Получаем канал по ID
            // channel.sendMessage("У вас нет прав для выполнения этой команды.").queue();
            return;
        }
        
        switch (commandName) {
            case "помощь":
                handleHelpCommand(channelId);
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
            default:
                // Считаем, что это команда добавления прогресса к испытанию
                handleProgressUpdateCommand(command, userId, username, channelId);
                break;
        }
    }

    /**
     * Обработать команду помощи
     */
    private void handleHelpCommand(String channelId) {
        String helpMessage = discordService.generateHelpMessage();
        // В реальной реализации здесь будет отправка сообщения
    }

    /**
     * Обработать команду статистики
     */
    private void handleStatisticsCommand(String[] parts, String channelId) {
        if (parts.length == 1) {
            // Общая статистика по всем испытаниям
            // В реальной реализации здесь будет получение всех испытаний и их статистики
        } else {
            // Статистика по конкретному испытанию
            String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            // В реальной реализации здесь будет получение испытания и его статистики
        }
    }

    /**
     * Обработать команду создания нового испытания
     */
    private void handleNewChallengeCommand(String[] parts, String userId, String username, String channelId) {
        if (parts.length < 3) {
            // channel.sendMessage("Недостаточно параметров. Используйте: +новый <название> <цель> [дата окончания] [тип]").queue();
            return;
        }
        
        String name = parts[1];
        long target;
        try {
            target = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            // channel.sendMessage("Цель должна быть числом.").queue();
            return;
        }
        
        LocalDateTime endDate = LocalDateTime.now().plusDays(365); // По умолчанию год
        ChallengeType type = ChallengeType.GROUP; // По умолчанию групповое
        String description = "Испытание по " + name;
        String unit = "единиц";
        
        if (parts.length > 3) {
            try {
                endDate = LocalDateTime.parse(parts[3], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (DateTimeParseException e) {
                // Если не удалось распарсить дату, используем значение по умолчанию
            }
        }
        
        if (parts.length > 4) {
            if ("индивидуальное".equalsIgnoreCase(parts[4])) {
                type = ChallengeType.INDIVIDUAL;
            }
        }
        
        Challenge challenge = challengeService.createChallenge(name, target, endDate, type, description, unit);
        // В реальной реализации здесь будет сохранение испытания в Ignite
        
        // channel.sendMessage("Испытание \"" + name + "\" успешно создано!").queue();
    }

    /**
     * Обработать команду удаления испытания
     */
    private void handleDeleteChallengeCommand(String[] parts, String channelId) {
        if (parts.length < 2) {
            // channel.sendMessage("Укажите название испытания. Используйте: +удалить <название>").queue();
            return;
        }
        
        String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        boolean deleted = challengeService.deleteChallenge(challengeName);
        
        if (deleted) {
            // channel.sendMessage("Испытание \"" + challengeName + "\" успешно удалено.").queue();
        } else {
            // channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
        }
    }

    /**
     * Обработать команду остановки испытания
     */
    private void handleStopChallengeCommand(String[] parts, String channelId) {
        if (parts.length < 2) {
            // channel.sendMessage("Укажите название испытания. Используйте: +остановить <название>").queue();
            return;
        }
        
        String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        // В реальной реализации здесь будет получение и обновление испытания
        // channel.sendMessage("Испытание \"" + challengeName + "\" остановлено.").queue();
    }

    /**
     * Обработать команду возобновления испытания
     */
    private void handleResumeChallengeCommand(String[] parts, String channelId) {
        if (parts.length < 2) {
            // channel.sendMessage("Укажите название испытания. Используйте: +продолжить <название>").queue();
            return;
        }
        
        String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        // В реальной реализации здесь будет получение и обновление испытания
        // channel.sendMessage("Испытание \"" + challengeName + "\" возобновлено.").queue();
    }

    /**
     * Обработать команду изменения цели испытания
     */
    private void handleChangeTargetCommand(String[] parts, String channelId) {
        if (parts.length < 3) {
            // channel.sendMessage("Укажите название испытания и новую цель. Используйте: +изменить <название> <новая цель>").queue();
            return;
        }
        
        String challengeName = parts[1];
        long newTarget;
        try {
            newTarget = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            // channel.sendMessage("Новая цель должна быть числом.").queue();
            return;
        }
        
        // В реальной реализации здесь будет получение и обновление испытания
        // channel.sendMessage("Цель испытания \"" + challengeName + "\" изменена на " + newTarget + ".").queue();
    }

    /**
     * Обработать команду просмотра личных испытаний
     */
    private void handleMyChallengesCommand(String userId, String channelId) {
        // В реальной реализации здесь будет получение испытаний пользователя
        // channel.sendMessage("Ваши испытания: ...").queue();
    }

    /**
     * Обработать команду регистрации на испытание
     */
    private void handleRegistrationCommand(String[] parts, String userId, String username, String channelId) {
        if (parts.length < 2) {
            // channel.sendMessage("Укажите название испытания. Используйте: +регистрация <название>").queue();
            return;
        }
        
        String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        boolean registered = userService.registerForChallenge(userId, username, challengeName);
        
        if (registered) {
            // channel.sendMessage("Вы успешно зарегистрированы на испытание \"" + challengeName + "\"!").queue();
        } else {
            // channel.sendMessage("Не удалось зарегистрироваться на испытание \"" + challengeName + "\".").queue();
        }
    }

    /**
     * Обработать команду таблицы лидеров
     */
    private void handleLeaderboardCommand(String[] parts, String channelId) {
        if (parts.length < 2) {
            // channel.sendMessage("Укажите название испытания. Используйте: +топ <название> [количество]").queue();
            return;
        }
        
        String challengeName = parts[1];
        int limit = 10; // По умолчанию
        
        if (parts.length > 2) {
            try {
                limit = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                // Используем значение по умолчанию
            }
        }
        
        // В реальной реализации здесь будет генерация таблицы лидеров
        // channel.sendMessage("Таблица лидеров по испытанию \"" + challengeName + "\": ...").queue();
    }

    /**
     * Обработать команду просмотра личного прогресса
     */
    private void handleProgressCommand(String[] parts, String userId, String channelId) {
        if (parts.length < 2) {
            // channel.sendMessage("Укажите название испытания. Используйте: +прогресс <название>").queue();
            return;
        }
        
        String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        // В реальной реализации здесь будет получение прогресса пользователя
        // channel.sendMessage("Ваш прогресс по испытанию \"" + challengeName + "\": ...").queue();
    }

    /**
     * Обработать команду обновления прогресса
     */
    private void handleProgressUpdateCommand(String command, String userId, String username, String channelId) {
        String[] parts = command.split("\\s+");
        if (parts.length < 2) {
            // channel.sendMessage("Укажите количество. Используйте: +<испытание> <количество>").queue();
            return;
        }
        
        String challengeName = parts[0];
        long amount;
        try {
            amount = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            // channel.sendMessage("Количество должно быть числом.").queue();
            return;
        }
        
        // В реальной реализации здесь будет:
        // 1. Получение испытания из Ignite
        // 2. Проверка, что испытание существует и активно
        // 3. Проверка регистрации пользователя (для индивидуальных испытаний)
        // 4. Обновление прогресса
        // 5. Расчет статистики
        // 6. Отправка ответа в канал
        
        // channel.sendMessage("Прогресс по испытанию \"" + challengeName + "\" обновлен. " + statistics).queue();
    }
}