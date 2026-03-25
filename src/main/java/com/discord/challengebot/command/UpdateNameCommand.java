package com.discord.challengebot.command;

import com.discord.challengebot.service.IUserService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Команда {@code +обновить_имя} — обновляет отображаемое имя пользователя в системе.
 * Использование: {@code +обновить_имя [новое имя]}.
 * Если новое имя не указано, используется текущее имя пользователя в Discord.
 */
@Component
public class UpdateNameCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(UpdateNameCommand.class);

    @Autowired
    private IUserService userService;

    /**
     * {@inheritDoc}
     * Обрабатывает команду {@code обновить_имя}.
     *
     * @param cmd строка команды
     * @return {@code true}, если команда равна "обновить_имя"
     */
    @Override
    public boolean canHandle(String cmd) {
        return "обновить_имя".equals(cmd);
    }

    /**
     * {@inheritDoc}
     * Обновляет имя пользователя в хранилище данных.
     *
     * @param event    событие получения сообщения Discord
     * @param args     аргументы: args[1..] — новое имя (необязательно)
     * @param authorId идентификатор автора команды
     * @param username текущее имя пользователя в Discord
     */
    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            String newUsername = username;
            if (args.length > 1) {
                newUsername = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            }
            boolean updated = userService.updateParticipantUsername(authorId, newUsername);
            if (updated) {
                channel.sendMessage("Ваше имя успешно обновлено на: " + newUsername).queue();
            } else {
                channel.sendMessage("Ошибка при обновлении имени.").queue();
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды обновить_имя", e);
        }
    }
}
