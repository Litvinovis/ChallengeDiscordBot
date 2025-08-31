package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
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
        // Сохраняем ссылку на JDA
        this.jda = event.getJDA();
        
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
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage("У вас нет прав для выполнения этой команды.").queue();
            }
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
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(helpMessage).queue();
        }
    }

    /**
     * Обработать команду статистики
     */
    private void handleStatisticsCommand(String[] parts, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        
        if (parts.length == 1) {
            // Общая статистика по всем испытаниям
            List<Challenge> challenges = challengeService.getAllChallenges();
            if (challenges.isEmpty()) {
                channel.sendMessage("Нет доступных испытаний.").queue();
                return;
            }
            
            StringBuilder message = new StringBuilder();
            message.append("**Статистика по всем испытаниям:**\n\n");
            
            for (Challenge challenge : challenges) {
                ChallengeStats stats = challengeService.getChallengeStats(challenge);
                message.append(statisticsService.formatReportForDiscord(stats)).append("\n");
            }
            
            channel.sendMessage(message.toString()).queue();
        } else {
            // Статистика по конкретному испытанию
            String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            Challenge challenge = challengeService.getChallenge(challengeName);
            
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                return;
            }
            
            ChallengeStats stats = challengeService.getChallengeStats(challenge);
            String formattedStats = discordService.formatChallengeStats(stats);
            channel.sendMessage(formattedStats).queue();
        }
    }

    /**
     * Обработать команду создания нового испытания
     */
    private void handleNewChallengeCommand(String[] parts, String userId, String username, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        
        if (parts.length < 3) {
            channel.sendMessage("Недостаточно параметров. Используйте: +новый <название> <цель> [дата окончания] [тип]").queue();
            return;
        }
        
        String name = parts[1];
        long target;
        try {
            target = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            channel.sendMessage("Цель должна быть числом.").queue();
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
        channel.sendMessage("Испытание \"" + name + "\" успешно создано!").queue();
    }

    /**
     * Обработать команду удаления испытания
     */
    private void handleDeleteChallengeCommand(String[] parts, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        
        if (parts.length < 2) {
            channel.sendMessage("Укажите название испытания. Используйте: +удалить <название>").queue();
            return;
        }
        
        String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        boolean deleted = challengeService.deleteChallenge(challengeName);
        
        if (deleted) {
            channel.sendMessage("Испытание \"" + challengeName + "\" успешно удалено.").queue();
        } else {
            channel.sendMessage("Не удалось удалить испытание \"" + challengeName + "\".").queue();
        }
    }

    /**
     * Обработать команду остановки испытания
     */
    private void handleStopChallengeCommand(String[] parts, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        
        if (parts.length < 2) {
            channel.sendMessage("Укажите название испытания. Используйте: +остановить <название>").queue();
            return;
        }
        
        String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        Challenge challenge = challengeService.getChallenge(challengeName);
        
        if (challenge == null) {
            channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
            return;
        }
        
        challengeService.updateChallengeStatus(challenge, false);
        channel.sendMessage("Испытание \"" + challengeName + "\" остановлено.").queue();
    }

    /**
     * Обработать команду возобновления испытания
     */
    private void handleResumeChallengeCommand(String[] parts, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        
        if (parts.length < 2) {
            channel.sendMessage("Укажите название испытания. Используйте: +продолжить <название>").queue();
            return;
        }
        
        String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        Challenge challenge = challengeService.getChallenge(challengeName);
        
        if (challenge == null) {
            channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
            return;
        }
        
        challengeService.updateChallengeStatus(challenge, true);
        channel.sendMessage("Испытание \"" + challengeName + "\" возобновлено.").queue();
    }

    /**
     * Обработать команду изменения цели испытания
     */
    private void handleChangeTargetCommand(String[] parts, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        
        if (parts.length < 3) {
            channel.sendMessage("Недостаточно параметров. Используйте: +изменить <название> <новая цель>").queue();
            return;
        }
        
        String challengeName = parts[1];
        long newTarget;
        try {
            newTarget = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            channel.sendMessage("Цель должна быть числом.").queue();
            return;
        }
        
        Challenge challenge = challengeService.getChallenge(challengeName);
        
        if (challenge == null) {
            channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
            return;
        }
        
        challengeService.updateChallengeTarget(challenge, newTarget);
        channel.sendMessage("Цель испытания \"" + challengeName + "\" изменена на " + newTarget + ".").queue();
    }

    /**
     * Обработать команду установки прогресса участника
     */
    private void handleSetParticipantProgressCommand(String[] parts, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        
        if (parts.length < 4) {
            channel.sendMessage("Недостаточно параметров. Используйте: +установить_прогресс <испытание> <пользователь> <количество>").queue();
            return;
        }
        
        String challengeName = parts[1];
        String userMention = parts[2]; // Формат: <@123456789>
        long progress;
        try {
            progress = Long.parseLong(parts[3]);
        } catch (NumberFormatException e) {
            channel.sendMessage("Количество должно быть числом.").queue();
            return;
        }
        
        // Извлекаем ID пользователя из упоминания
        String userId = userMention.replaceAll("[^0-9]", "");
        
        Challenge challenge = challengeService.getChallenge(challengeName);
        
        if (challenge == null) {
            channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
            return;
        }
        
        challengeService.setParticipantProgress(challenge, userId, progress);
        channel.sendMessage("Прогресс участника <@" + userId + "> в испытании \"" + challengeName + "\" установлен на " + progress + ".").queue();
    }

    /**
     * Обработать команду добавления участника
     */
    private void handleAddParticipantCommand(String[] parts, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        
        if (parts.length < 3) {
            channel.sendMessage("Недостаточно параметров. Используйте: +добавить_участника <испытание> <пользователь>").queue();
            return;
        }
        
        String challengeName = parts[1];
        String userMention = parts[2]; // Формат: <@123456789>
        
        // Извлекаем ID пользователя из упоминания
        String userId = userMention.replaceAll("[^0-9]", "");
        
        Challenge challenge = challengeService.getChallenge(challengeName);
        
        if (challenge == null) {
            channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
            return;
        }
        
        challengeService.addParticipant(challenge, userId);
        channel.sendMessage("Участник <@" + userId + "> добавлен в испытание \"" + challengeName + "\".").queue();
    }

    /**
     * Обработать команду удаления участника
     */
    private void handleRemoveParticipantCommand(String[] parts, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        
        if (parts.length < 3) {
            channel.sendMessage("Недостаточно параметров. Используйте: +удалить_участника <испытание> <пользователь>").queue();
            return;
        }
        
        String challengeName = parts[1];
        String userMention = parts[2]; // Формат: <@123456789>
        
        // Извлекаем ID пользователя из упоминания
        String userId = userMention.replaceAll("[^0-9]", "");
        
        Challenge challenge = challengeService.getChallenge(challengeName);
        
        if (challenge == null) {
            channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
            return;
        }
        
        challengeService.removeParticipant(challenge, userId);
        channel.sendMessage("Участник <@" + userId + "> удален из испытания \"" + challengeName + "\".").queue();
    }

    /**
     * Обработать команду личных испытаний
     */
    private void handleMyChallengesCommand(String userId, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        
        List<Challenge> userChallenges = challengeService.getUserChallenges(userId);
        
        if (userChallenges.isEmpty()) {
            channel.sendMessage("У вас нет активных испытаний.").queue();
            return;
        }
        
        StringBuilder message = new StringBuilder();
        message.append("**Ваши испытания:**\n\n");
        
        for (Challenge challenge : userChallenges) {
            ChallengeStats stats = challengeService.getChallengeStats(challenge);
            message.append("- ").append(challenge.getName()).append(": ")
                   .append(stats.getCurrentValue()).append("/").append(stats.getTargetValue())
                   .append(" (").append(String.format("%.2f", stats.getPercentage())).append("%)\n");
        }
        
        channel.sendMessage(message.toString()).queue();
    }

    /**
     * Обработать команду регистрации
     */
    private void handleRegistrationCommand(String[] parts, String userId, String username, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        
        if (parts.length < 2) {
            channel.sendMessage("Укажите название испытания. Используйте: +регистрация <название>").queue();
            return;
        }
        
        String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        Challenge challenge = challengeService.getChallenge(challengeName);
        
        if (challenge == null) {
            channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
            return;
        }
        
        challengeService.addParticipant(challenge, userId);
        channel.sendMessage("Вы успешно зарегистрированы на испытание \"" + challengeName + "\".").queue();
    }

    /**
     * Обработать команду таблицы лидеров
     */
    private void handleLeaderboardCommand(String[] parts, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        
        if (parts.length < 2) {
            channel.sendMessage("Укажите название испытания. Используйте: +топ <испытание> [количество]").queue();
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
                // Используем значение по умолчанию
            }
        }
        
        Challenge challenge = challengeService.getChallenge(challengeName);
        
        if (challenge == null) {
            channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
            return;
        }
        
        List<Map.Entry<String, Long>> leaderboard = challengeService.getTopParticipants(challenge, limit);
        String leaderboardMessage = statisticsService.formatLeaderboardForDiscord(challenge, leaderboard);
        channel.sendMessage(leaderboardMessage).queue();
    }

    /**
     * Обработать команду личного прогресса
     */
    private void handleProgressCommand(String[] parts, String userId, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        
        if (parts.length < 2) {
            channel.sendMessage("Укажите название испытания. Используйте: +прогресс <испытание>").queue();
            return;
        }
        
        String challengeName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        Challenge challenge = challengeService.getChallenge(challengeName);
        
        if (challenge == null) {
            channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
            return;
        }
        
        Long userProgress = challenge.getParticipantProgress().getOrDefault(userId, 0L);
        String message = String.format("**Ваш прогресс по испытанию \"%s\":**\n%s: %d/%d (%.2f%%)", 
                                     challenge.getName(), challenge.getUnit(), userProgress, 
                                     challenge.getTargetValue(), 
                                     challenge.getTargetValue() > 0 ? (double) userProgress / challenge.getTargetValue() * 100 : 0);
        
        channel.sendMessage(message).queue();
    }

    /**
     * Обработать команду обновления прогресса
     */
    private void handleProgressUpdateCommand(String command, String userId, String username, String channelId) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        
        String[] parts = command.split("\\s+");
        if (parts.length < 2) {
            channel.sendMessage("Недостаточно параметров. Используйте: +<испытание> <количество>").queue();
            return;
        }
        
        String challengeName = parts[0];
        long amount;
        try {
            amount = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            channel.sendMessage("Количество должно быть числом.").queue();
            return;
        }
        
        Challenge challenge = challengeService.getChallenge(challengeName);
        
        if (challenge == null) {
            channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
            return;
        }
        
        if (!challenge.isActive()) {
            channel.sendMessage("Испытание \"" + challengeName + "\" не активно.").queue();
            return;
        }
        
        challengeService.addProgress(challenge, userId, username, amount);
        channel.sendMessage("Прогресс по испытанию \"" + challengeName + "\" обновлен на " + amount + ".").queue();
    }
}