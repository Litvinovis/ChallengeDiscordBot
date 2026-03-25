package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import net.dv8tion.jda.api.JDA;

/**
 * Интерфейс сервиса взаимодействия с Discord.
 * Определяет операции отправки сообщений, генерации справки и проверки прав пользователей.
 */
public interface IDiscordService {
    /**
     * Возвращает экземпляр JDA для прямого взаимодействия с Discord API.
     *
     * @return экземпляр {@link JDA}
     */
    JDA getJDA();

    /**
     * Отправляет текстовое сообщение в канал по его идентификатору.
     *
     * @param channelId идентификатор канала
     * @param message   текст сообщения
     */
    void sendMessage(String channelId, String message);

    /**
     * Отправляет текстовое сообщение в канал по его имени.
     *
     * @param channelName имя канала
     * @param message     текст сообщения
     */
    void sendMessageToChannel(String channelName, String message);

    /**
     * Отправляет сообщение с прикреплённым изображением (визуализацией).
     *
     * @param channelId идентификатор канала
     * @param message   текст сообщения
     * @param image     байты изображения PNG
     */
    void sendMessageWithVisualization(String channelId, String message, byte[] image);

    /**
     * Генерирует полную справку по всем командам бота.
     *
     * @return строка справочного сообщения
     */
    String generateHelpMessage();

    /**
     * Генерирует справку с учётом прав пользователя (администратор видит больше команд).
     *
     * @param userId идентификатор пользователя
     * @return строка справочного сообщения
     */
    String generateHelpMessage(String userId);

    /**
     * Формирует и отправляет ежедневный отчёт о прогрессе всех активных испытаний.
     */
    void sendDailyReport();

    /**
     * Отправляет уведомление об успешном завершении испытания.
     *
     * @param challenge завершённое испытание
     */
    void sendChallengeCompletionNotification(Challenge challenge);

    /**
     * Отправляет уведомление о завершении испытания без достижения цели.
     *
     * @param challenge завершённое испытание
     */
    void sendChallengeFailureNotification(Challenge challenge);

    /**
     * Форматирует статистику испытания в строку для вывода в Discord.
     *
     * @param challenge испытание
     * @param stats     объект статистики
     * @return отформатированная строка
     */
    String formatChallengeStats(Challenge challenge, ChallengeStats stats);

    /**
     * Проверяет, имеет ли пользователь права для выполнения указанной команды.
     *
     * @param userId  идентификатор пользователя
     * @param command строка команды
     * @return {@code true}, если пользователь авторизован
     */
    boolean isAuthorizedUser(String userId, String command);
}
