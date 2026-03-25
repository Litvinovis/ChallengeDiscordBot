package com.discord.challengebot.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Интерфейс команды Discord бота.
 * Каждая реализация отвечает за обработку одной или нескольких команд.
 */
public interface Command {
    /**
     * Проверяет, может ли данная реализация обработать указанную команду.
     *
     * @param cmd строка команды (без префикса '+')
     * @return {@code true}, если команда поддерживается
     */
    boolean canHandle(String cmd);

    /**
     * Выполняет команду на основе события Discord и переданных аргументов.
     *
     * @param event    событие получения сообщения Discord
     * @param args     массив аргументов команды (args[0] — имя команды)
     * @param authorId идентификатор автора сообщения
     * @param username имя пользователя в Discord
     */
    void execute(MessageReceivedEvent event, String[] args, String authorId, String username);
}
