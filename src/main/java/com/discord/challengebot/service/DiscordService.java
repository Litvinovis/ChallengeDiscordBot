package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;

/**
 * Сервис для взаимодействия с Discord
 */
@Service
public class DiscordService {
    private static final Logger logger = LoggerFactory.getLogger(DiscordService.class);
    
    @Autowired
    private DiscordConfig discordConfig;
    
    @Autowired
    private ChallengeService challengeService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private StatisticsService statisticsService;
    
    private JDA jda;

    @PostConstruct
    public void init() {
        try {
            logger.info("Инициализация Discord бота");
            jda = JDABuilder.createDefault(discordConfig.getToken())
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    .addEventListeners(new DiscordMessageListener(this, discordConfig, challengeService, userService, statisticsService))
                    .build();
            jda.awaitReady();
            logger.info("Discord бот успешно инициализирован");
        } catch (Exception e) {
            logger.error("Ошибка инициализации Discord бота", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (jda != null) {
            logger.info("Выключение Discord бота");
            jda.shutdown();
        }
    }

    /**
     * Отправить сообщение в канал Discord
     */
    public void sendMessage(String channelId, String message) {
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(message).queue();
                logger.info("Сообщение отправлено в канал: {}", message);
            } else {
                logger.warn("Канал с ID {} не найден", channelId);
            }
        } catch (Exception e) {
            logger.error("Ошибка отправки сообщения в Discord", e);
        }
    }

    /**
     * Отправить сообщение в канал по имени
     */
    public void sendMessageToChannel(String channelName, String message) {
        try {
            // Ищем канал по имени
            TextChannel channel = jda.getTextChannelsByName(channelName, true).stream().findFirst().orElse(null);
            if (channel != null) {
                channel.sendMessage(message).queue();
                logger.info("Сообщение отправлено в канал {}: {}", channelName, message);
            } else {
                logger.warn("Канал с именем {} не найден", channelName);
            }
        } catch (Exception e) {
            logger.error("Ошибка отправки сообщения в Discord", e);
        }
    }

    /**
     * Отправить сообщение с визуализацией
     */
    public void sendMessageWithVisualization(String channelId, String message, byte[] image) {
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(message).queue();
                // В реальной реализации здесь будет отправка изображения
                // channel.sendFile(image, "chart.png").queue();
                logger.info("Сообщение с визуализацией отправлено в канал");
            } else {
                logger.warn("Канал с ID {} не найден", channelId);
            }
        } catch (Exception e) {
            logger.error("Ошибка отправки сообщения с визуализацией в Discord", e);
        }
    }

    /**
     * Сгенерировать сообщение справки
     */
    public String generateHelpMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("**Справка по командам бота**\n\n");
        
        sb.append("**Основные команды:**\n");
        sb.append("`+<испытание> <количество>` - Добавить прогресс к испытанию (например: `+отжимания 10`)\n");
        sb.append("`+статистика` - Показать статистику по всем испытаниям\n");
        sb.append("`+статистика <испытание>` - Показать статистику по конкретному испытанию\n");
        sb.append("`+помощь` - Показать эту справку\n\n");
        
        sb.append("**Команды управления испытаниями (только для администраторов):**\n");
        sb.append("`+новый <название> <цель> [дата окончания] [тип]` - Создать новое испытание\n");
        sb.append("`+удалить <название>` - Удалить испытание\n");
        sb.append("`+остановить <название>` - Остановить активное испытание\n");
        sb.append("`+продолжить <название>` - Продолжить остановленное испытание\n");
        sb.append("`+изменить <название> <новая цель>` - Изменить цель испытания\n");
        sb.append("`+установить_прогресс <испытание> <пользователь> <количество>` - Установить прогресс участника\n");
        sb.append("`+добавить_участника <испытание> <пользователь>` - Добавить участника в испытание\n");
        sb.append("`+удалить_участника <испытание> <пользователь>` - Удалить участника из испытания\n\n");
        
        sb.append("**Команды пользователя:**\n");
        sb.append("`+мои` - Показать личные испытания\n");
        sb.append("`+топ <испытание> [количество]` - Показать таблицу лидеров по испытанию\n");
        sb.append("`+прогресс <испытание>` - Показать личный прогресс по испытанию\n");
        sb.append("`+регистрация <название>` - Зарегистрироваться на испытание\n");
        
        return sb.toString();
    }

    /**
     * Отправить ежедневный отчет
     */
    public void sendDailyReport() {
        logger.info("Отправка ежедневного отчета");
        // Получаем все активные испытания
        List<Challenge> challenges = challengeService.getAllChallenges();
        challenges = challenges.stream().filter(Challenge::isActive).collect(java.util.stream.Collectors.toList());
        
        if (challenges.isEmpty()) {
            sendMessageToChannel(discordConfig.getReportChannel(), "Активных испытаний нет.");
            return;
        }
        
        StringBuilder report = new StringBuilder();
        report.append("**Ежедневный отчет по испытаниям**\n\n");
        
        for (Challenge challenge : challenges) {
            ChallengeStats stats = challengeService.getChallengeStats(challenge);
            report.append(statisticsService.formatReportForDiscord(stats)).append("\n");
        }
        
        sendMessageToChannel(discordConfig.getReportChannel(), report.toString());
    }

    /**
     * Отправить уведомление о завершении испытания
     */
    public void sendChallengeCompletionNotification(Challenge challenge) {
        String message = String.format("**Испытание завершено!**\nИспытание \"%s\" успешно завершено!\nПоздравляем всех участников!", 
                                     challenge.getName());
        
        // Отправляем сообщение в канал отчетов
        sendMessageToChannel(discordConfig.getReportChannel(), message);
        
        // Также отправляем топ-5 участников
        List<Map.Entry<String, Long>> leaderboard = challengeService.getTopParticipants(challenge, 5);
        if (!leaderboard.isEmpty()) {
            String leaderboardMessage = statisticsService.formatLeaderboardForDiscord(challenge, leaderboard);
            sendMessageToChannel(discordConfig.getReportChannel(), leaderboardMessage);
        }
    }

    /**
     * Форматировать статистику испытания для Discord
     */
    public String formatChallengeStats(ChallengeStats stats) {
        return statisticsService.formatChallengeStats(stats);
    }

    /**
     * Проверить, авторизован ли пользователь для команды
     */
    public boolean isAuthorizedUser(String userId, String command) {
        // Некоторые команды доступны только администраторам
        if (command.startsWith("новый") || command.startsWith("удалить") || 
            command.startsWith("остановить") || command.startsWith("продолжить") || 
            command.startsWith("изменить") || command.startsWith("установить_прогресс") ||
            command.startsWith("добавить_участника") || command.startsWith("удалить_участника")) {
            return userService.isAdminUser(userId);
        }
        return true;
    }
}