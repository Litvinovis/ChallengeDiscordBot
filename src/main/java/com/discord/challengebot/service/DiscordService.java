package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
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
            
            // Устанавливаем ссылку на DiscordService в StatisticsService ДО создания DiscordMessageListener
            statisticsService.setDiscordService(this);
            statisticsService.setUserService(userService);
            
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
        try {
            if (jda != null) {
                logger.info("Выключение Discord бота");
                jda.shutdown();
                logger.info("Discord бот успешно выключен");
            }
        } catch (Exception e) {
            logger.error("Ошибка при выключении Discord бота", e);
        }
    }

    /**
     * Получить экземпляр JDA
     */
    public JDA getJDA() {
        return jda;
    }

    /**
     * Отправить сообщение в канал Discord
     */
    public void sendMessage(String channelId, String message) {
        try {
            if (channelId == null || channelId.isEmpty()) {
                logger.warn("Попытка отправить сообщение в канал с пустым ID");
                return;
            }
            
            if (message == null || message.isEmpty()) {
                logger.warn("Попытка отправить пустое сообщение");
                return;
            }
            
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(message).queue();
                logger.info("Сообщение отправлено в канал: {}", message);
            } else {
                logger.warn("Канал с ID {} не найден", channelId);
            }
        } catch (Exception e) {
            logger.error("Ошибка отправки сообщения в Discord в канал {}", channelId, e);
        }
    }

    /**
     * Отправить сообщение в канал по имени
     */
    public void sendMessageToChannel(String channelName, String message) {
        try {
            if (channelName == null || channelName.isEmpty()) {
                logger.warn("Попытка отправить сообщение в канал с пустым именем");
                return;
            }
            
            if (message == null || message.isEmpty()) {
                logger.warn("Попытка отправить пустое сообщение");
                return;
            }
            
            TextChannel channel = null;
            
            // Если указан конкретный сервер для отчетов, ищем канал на этом сервере
            if (discordConfig.getReportGuildId() != null && !discordConfig.getReportGuildId().isEmpty()) {
                Guild guild = jda.getGuildById(discordConfig.getReportGuildId());
                if (guild != null) {
                    channel = guild.getTextChannelsByName(channelName, true).stream().findFirst().orElse(null);
                    if (channel != null) {
                        logger.debug("Канал '{}' найден на сервере с ID {}", channelName, discordConfig.getReportGuildId());
                    }
                } else {
                    logger.warn("Сервер с ID {} не найден", discordConfig.getReportGuildId());
                }
            }
            
            // Если не нашли канал на указанном сервере или сервер не указан, ищем на всех серверах
            if (channel == null) {
                channel = jda.getTextChannelsByName(channelName, true).stream().findFirst().orElse(null);
                if (channel != null) {
                    logger.debug("Канал '{}' найден на одном из серверов", channelName);
                }
            }
            
            if (channel != null) {
                channel.sendMessage(message).queue();
                logger.info("Сообщение отправлено в канал {}: {}", channelName, message);
            } else {
                logger.warn("Канал с именем {} не найден", channelName);
            }
        } catch (Exception e) {
            logger.error("Ошибка отправки сообщения в Discord в канал {}", channelName, e);
        }
    }

    /**
     * Отправить сообщение с визуализацией
     */
    public void sendMessageWithVisualization(String channelId, String message, byte[] image) {
        try {
            if (channelId == null || channelId.isEmpty()) {
                logger.warn("Попытка отправить сообщение с визуализацией в канал с пустым ID");
                return;
            }
            
            if (message == null || message.isEmpty()) {
                logger.warn("Попытка отправить сообщение с визуализацией с пустым текстом");
                return;
            }
            
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                if (image != null && image.length > 0) {
                    // Отправка сообщения с изображением
                    channel.sendMessage(message).addFiles(FileUpload.fromData(image, "chart.png")).queue();
                } else {
                    // Отправка только текстового сообщения
                    channel.sendMessage(message).queue();
                }
                logger.info("Сообщение с визуализацией отправлено в канал");
            } else {
                logger.warn("Канал с ID {} не найден", channelId);
            }
        } catch (Exception e) {
            logger.error("Ошибка отправки сообщения с визуализацией в Discord в канал {}", channelId, e);
        }
    }

    /**
     * Сгенерировать сообщение справки
     */
    public String generateHelpMessage() {
        try {
            logger.debug("Генерация сообщения справки");
            StringBuilder sb = new StringBuilder();
            sb.append("**Справка по командам бота**\n\n");
            
            sb.append("**Основные команды:**\n");
            sb.append("`+<испытание> <количество>` - Добавить прогресс к испытанию (например: `+отжимания 10`)\n");
            sb.append("`+статистика` - Показать статистику по всем испытаниям\n");
            sb.append("`+статистика <испытание>` - Показать статистику по конкретному испытанию\n");
            sb.append("`+испытания` - Показать список всех активных испытаний\n");
            sb.append("`+помощь` - Показать эту справку\n\n");
            
            sb.append("**Команды управления испытаниями (только для администраторов):**\n");
            sb.append("`+новый <название> <цель> [дата окончания] [тип]` - Создать новое испытание\n");
            sb.append("`+удалить <название>` - Удалить испытание\n");
            sb.append("`+остановить <название>` - Остановить активное испытание\n");
            sb.append("`+продолжить <название>` - Продолжить остановленное испытание\n");
            sb.append("`+изменить <название> <новая цель>` - Изменить цель испытания\n");
            sb.append("`+изменить_дату <название> <новая дата>` - Изменить дату окончания испытания\n");
            sb.append("`+установить_прогресс <испытание> <пользователь> <количество>` - Установить прогресс участника\n");
            sb.append("`+добавить_участника <испытание> <пользователь>` - Добавить участника в испытание\n");
            sb.append("`+удалить_участника <испытание> <пользователь>` - Удалить участника из испытания\n\n");
            
            sb.append("**Команды пользователя:**\n");
            sb.append("`+мои` - Показать личные испытания\n");
            sb.append("`+топ <испытание> [количество]` - Показать таблицу лидеров по испытанию\n");
            sb.append("`+прогресс <испытание>` - Показать личный прогресс по испытанию\n");
            sb.append("`+регистрация <название>` - Зарегистрироваться на испытание\n");
            
            logger.debug("Сообщение справки успешно сгенерировано");
            return sb.toString();
        } catch (Exception e) {
            logger.error("Ошибка при генерации сообщения справки", e);
            return "**Ошибка при генерации справки. Пожалуйста, попробуйте позже.**";
        }
    }
    
    /**
     * Сгенерировать сообщение справки для конкретного пользователя
     */
    public String generateHelpMessage(String userId) {
        try {
            logger.debug("Генерация сообщения справки для пользователя: {}", userId);
            StringBuilder sb = new StringBuilder();
            sb.append("**Справка по командам бота**\n\n");
            
            sb.append("**Основные команды:**\n");
            sb.append("`+<испытание> <количество>` - Добавить прогресс к испытанию (например: `+отжимания 10`)\n");
            sb.append("`+статистика` - Показать статистику по всем испытаниям\n");
            sb.append("`+статистика <испытание>` - Показать статистику по конкретному испытанию\n");
            sb.append("`+испытания` - Показать список всех активных испытаний\n");
            sb.append("`+помощь` - Показать эту справку\n\n");
            
            // Проверяем, является ли пользователь администратором
            boolean isAdmin = (userId != null) && userService.isAdminUser(userId);
            
            if (isAdmin) {
                sb.append("**Команды управления испытаниями (только для администраторов):**\n");
                sb.append("`+новый <название> <цель> [дата окончания] [тип]` - Создать новое испытание\n");
                sb.append("`+удалить <название>` - Удалить испытание\n");
                sb.append("`+остановить <название>` - Остановить активное испытание\n");
                sb.append("`+продолжить <название>` - Продолжить остановленное испытание\n");
                sb.append("`+изменить <название> <новая цель>` - Изменить цель испытания\n");
                sb.append("`+изменить_дату <название> <новая дата>` - Изменить дату окончания испытания\n");
                sb.append("`+установить_прогресс <испытание> <пользователь> <количество>` - Установить прогресс участника\n");
                sb.append("`+добавить_участника <испытание> <пользователь>` - Добавить участника в испытание\n");
                sb.append("`+удалить_участника <испытание> <пользователь>` - Удалить участника из испытания\n\n");
            }
            
            sb.append("**Команды пользователя:**\n");
            sb.append("`+мои` - Показать личные испытания\n");
            sb.append("`+топ <испытание> [количество]` - Показать таблицу лидеров по испытанию\n");
            sb.append("`+прогресс <испытание>` - Показать личный прогресс по испытанию\n");
            sb.append("`+регистрация <название>` - Зарегистрироваться на испытание\n");
            
            logger.debug("Сообщение справки успешно сгенерировано");
            return sb.toString();
        } catch (Exception e) {
            logger.error("Ошибка при генерации сообщения справки", e);
            return "**Ошибка при генерации справки. Пожалуйста, попробуйте позже.**";
        }
    }

    /**
     * Отправить ежедневный отчет
     */
    public void sendDailyReport() {
        try {
            logger.info("Отправка ежедневного отчета");
            // Получаем все активные испытания
            List<Challenge> challenges = challengeService.getAllChallenges();
            challenges = challenges.stream().filter(Challenge::isActive).collect(java.util.stream.Collectors.toList());
            
            if (challenges.isEmpty()) {
                sendMessageToChannel(discordConfig.getReportChannel(), "Активных испытаний нет.");
                logger.info("Нет активных испытаний для отправки отчета");
                return;
            }
            
            StringBuilder report = new StringBuilder();
            report.append("**Ежедневный отчет по испытаниям**\n\n");
            
            for (Challenge challenge : challenges) {
                ChallengeStats stats = challengeService.getChallengeStats(challenge);
                if (stats != null) {
                    report.append(statisticsService.formatReportForDiscord(challenge, stats)).append("\n");
                }
            }
            
            sendMessageToChannel(discordConfig.getReportChannel(), report.toString());
            logger.info("Ежедневный отчет успешно отправлен, обработано {} испытаний", challenges.size());
        } catch (Exception e) {
            logger.error("Ошибка при отправке ежедневного отчета", e);
        }
    }

    /**
     * Отправить уведомление о завершении испытания
     */
    public void sendChallengeCompletionNotification(Challenge challenge) {
        try {
            logger.info("Отправка уведомления о завершении испытания: {}", challenge != null ? challenge.getName() : "null");
            
            if (challenge == null) {
                logger.warn("Попытка отправить уведомление о завершении null испытания");
                return;
            }
            
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
            
            logger.info("Уведомление о завершении испытания '{}' успешно отправлено", challenge.getName());
        } catch (Exception e) {
            logger.error("Ошибка при отправке уведомления о завершении испытания: {}", 
                        challenge != null ? challenge.getName() : "null", e);
        }
    }

    /**
     * Форматировать статистику испытания для Discord
     */
    public String formatChallengeStats(Challenge challenge, ChallengeStats stats) {
        try {
            logger.debug("Форматирование статистики испытания для Discord");
            return statisticsService.formatReportForDiscord(challenge, stats);
        } catch (Exception e) {
            logger.error("Ошибка при форматировании статистики испытания для Discord", e);
            return "**Ошибка при форматировании статистики. Пожалуйста, попробуйте позже.**";
        }
    }

    /**
     * Проверить, авторизован ли пользователь для команды
     */
    public boolean isAuthorizedUser(String userId, String command) {
        try {
            logger.debug("Проверка авторизации пользователя {} для команды {}", userId, command);
            
            if (userId == null || userId.isEmpty()) {
                logger.warn("Попытка проверить авторизацию для пользователя с пустым ID");
                return false;
            }
            
            if (command == null || command.isEmpty()) {
                logger.warn("Попытка проверить авторизацию для пустой команды");
                return true;
            }
            
            // Некоторые команды доступны только администраторам
            if (command.startsWith("новый") || command.startsWith("удалить") || 
                command.startsWith("остановить") || command.startsWith("продолжить") || 
                command.startsWith("изменить") || command.startsWith("изменить_дату") ||
                command.startsWith("установить_прогресс") ||
                command.startsWith("добавить_участника") || command.startsWith("удалить_участника")) {
                boolean isAdmin = userService.isAdminUser(userId);
                logger.debug("Пользователь {} {} администратором", userId, isAdmin ? "является" : "не является");
                return isAdmin;
            }
            
            logger.debug("Команда '{}' доступна всем пользователям", command);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при проверке авторизации пользователя {} для команды {}", userId, command, e);
            return false;
        }
    }
}