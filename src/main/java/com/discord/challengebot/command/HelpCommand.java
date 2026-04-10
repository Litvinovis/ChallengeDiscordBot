package com.discord.challengebot.command;

import com.discord.challengebot.service.IDiscordService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Команда {@code +помощь} — выводит справочное сообщение со списком доступных команд.
 * Для администраторов показываются дополнительные административные команды.
 */
@Component
@Order(1)
public class HelpCommand extends BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(HelpCommand.class);

    @Autowired
    private IDiscordService discordService;

    /**
     * {@inheritDoc}
     * Обрабатывает команду {@code помощь}.
     *
     * @param cmd строка команды
     * @return {@code true}, если команда равна "помощь"
     */
    @Override
    public boolean canHandle(String cmd) {
        return "помощь".equals(cmd);
    }

    /**
     * {@inheritDoc}
     * Отправляет в канал справочное сообщение с учётом прав пользователя.
     *
     * @param event    событие получения сообщения Discord
     * @param args     аргументы команды (не используются)
     * @param authorId идентификатор автора команды
     * @param username имя автора команды
     */
    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            String helpMessage = discordService.generateHelpMessage(authorId);
            TextChannel channel = event.getChannel().asTextChannel();
            channel.sendMessage(helpMessage).queue();
        } catch (Exception e) {
            logger.error("Ошибка обработки команды помощи", e);
        }
    }
}
