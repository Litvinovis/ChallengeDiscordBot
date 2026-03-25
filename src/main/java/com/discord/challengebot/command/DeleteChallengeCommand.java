package com.discord.challengebot.command;

import com.discord.challengebot.service.IChallengeService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Команда {@code +удалить} — полностью удаляет испытание из системы.
 * Использование: {@code +удалить <название>}.
 * Доступна только администраторам.
 */
@Component
public class DeleteChallengeCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(DeleteChallengeCommand.class);

    @Autowired
    private IChallengeService challengeService;

    /**
     * {@inheritDoc}
     * Обрабатывает команду {@code удалить}.
     *
     * @param cmd строка команды
     * @return {@code true}, если команда равна "удалить"
     */
    @Override
    public boolean canHandle(String cmd) {
        return "удалить".equals(cmd);
    }

    /**
     * {@inheritDoc}
     * Удаляет испытание с указанным названием.
     *
     * @param event    событие получения сообщения Discord
     * @param args     аргументы: args[1..] — название испытания
     * @param authorId идентификатор автора команды
     * @param username имя автора команды
     */
    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            if (args.length < 2) {
                channel.sendMessage("Укажите название испытания. Используйте: +удалить <название>").queue();
                return;
            }
            String challengeName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            boolean deleted = challengeService.deleteChallenge(challengeName);
            if (deleted) {
                channel.sendMessage("Испытание \"" + challengeName + "\" успешно удалено.").queue();
            } else {
                channel.sendMessage("Не удалось удалить испытание \"" + challengeName + "\".").queue();
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды удаления испытания", e);
        }
    }
}
