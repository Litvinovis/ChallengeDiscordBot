package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IUserService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

/**
 * Команда {@code +установить_прогресс} — устанавливает конкретное значение прогресса участника.
 * Использование: {@code +установить_прогресс <испытание> <@пользователь> <количество>}.
 * Доступна только администраторам.
 */
@Component
@Order(1)
public class SetProgressCommand extends BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(SetProgressCommand.class);

    @Autowired
    private IChallengeService challengeService;
    @Autowired
    private IUserService userService;

    /**
     * {@inheritDoc}
     * Обрабатывает команду {@code установить_прогресс}.
     *
     * @param cmd строка команды
     * @return {@code true}, если команда равна "установить_прогресс"
     */
    @Override
    public boolean canHandle(String cmd) {
        return "установить_прогресс".equals(cmd);
    }

    /**
     * {@inheritDoc}
     * Устанавливает прогресс участника в абсолютное значение и пересчитывает общий прогресс.
     *
     * @param event    событие получения сообщения Discord
     * @param args     аргументы: args[1] — название, args[2] — упоминание, args[3] — количество
     * @param authorId идентификатор автора команды
     * @param username имя автора команды
     */
    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            if (args.length < 4) {
                channel.sendMessage("Недостаточно параметров. Используйте: +установить_прогресс <испытание> <пользователь> <количество>").queue();
                return;
            }
            String challengeName = args[1];
            String userMention = args[2];
            long progress;
            try {
                progress = Long.parseLong(args[3]);
                if (progress < 0) {
                    channel.sendMessage("Прогресс не может быть отрицательным числом.").queue();
                    return;
                }
            } catch (NumberFormatException e) {
                channel.sendMessage("Количество должно быть числом.").queue();
                return;
            }

            String userId = userMention.replaceAll("[^0-9]", "");
            String resolvedUsername = userId;
            try {
                net.dv8tion.jda.api.entities.User user = event.getJDA().getUserById(userId);
                if (user != null) resolvedUsername = user.getName();
            } catch (Exception ignored) {}

            Challenge challenge = challengeService.getChallenge(challengeName);
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                return;
            }

            userService.registerForChallenge(userId, resolvedUsername, challengeName);
            Challenge updated = challengeService.setParticipantProgress(challenge, userId, progress);
            if (updated != null) {
                channel.sendMessage("Прогресс участника <@" + userId + "> в испытании \"" + challengeName + "\" установлен на " + progress + ".").queue();
            } else {
                channel.sendMessage("Ошибка при установке прогресса.").queue();
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды установки прогресса", e);
        }
    }
}
