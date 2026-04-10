package com.discord.challengebot.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Optional;

/**
 * Базовый класс для команд Discord бота.
 * Предоставляет вспомогательные методы отправки ответов, парсинга аргументов
 * и проверки прав администратора.
 */
public abstract class BaseCommand implements Command {

    /**
     * Отправляет сообщение в канал, из которого пришла команда.
     *
     * @param event   событие получения сообщения
     * @param message текст сообщения
     */
    protected void reply(MessageReceivedEvent event, String message) {
        event.getChannel().sendMessage(message).queue();
    }

    /**
     * Отправляет сообщение об ошибке с префиксом ❌.
     *
     * @param event   событие получения сообщения
     * @param message текст ошибки
     */
    protected void replyError(MessageReceivedEvent event, String message) {
        reply(event, "❌ " + message);
    }

    /**
     * Безопасно извлекает аргумент по индексу.
     *
     * @param args  массив аргументов команды
     * @param index индекс нужного аргумента
     * @return Optional с аргументом или пустой Optional
     */
    protected Optional<String> getArg(String[] args, int index) {
        return args.length > index ? Optional.of(args[index]) : Optional.empty();
    }

    /**
     * Проверяет права администратора; при отсутствии — отправляет сообщение об ошибке и возвращает false.
     *
     * @param event    событие получения сообщения
     * @param userId   идентификатор пользователя
     * @param adminIds список идентификаторов администраторов
     * @return true если пользователь является администратором
     */
    protected boolean requireAdmin(MessageReceivedEvent event, String userId, List<String> adminIds) {
        if (adminIds == null || !adminIds.contains(userId)) {
            replyError(event, "Недостаточно прав для выполнения команды");
            return false;
        }
        return true;
    }
}
