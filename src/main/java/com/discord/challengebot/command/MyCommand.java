package com.discord.challengebot.command;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Команда {@code +мои} — выводит список испытаний, в которых участвует пользователь,
 * с текущим прогрессом по каждому из них.
 */
@Component
public class MyCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(MyCommand.class);

    @Autowired
    private IChallengeService challengeService;

    /**
     * {@inheritDoc}
     * Обрабатывает команду {@code мои}.
     *
     * @param cmd строка команды
     * @return {@code true}, если команда равна "мои"
     */
    @Override
    public boolean canHandle(String cmd) {
        return "мои".equals(cmd);
    }

    /**
     * {@inheritDoc}
     * Отправляет в канал список личных испытаний с прогрессом.
     *
     * @param event    событие получения сообщения Discord
     * @param args     аргументы команды (не используются)
     * @param authorId идентификатор автора команды
     * @param username имя автора команды
     */
    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            List<Challenge> userChallenges = challengeService.getUserChallenges(authorId);

            if (userChallenges.isEmpty()) {
                channel.sendMessage("У вас нет активных испытаний.").queue();
                return;
            }

            StringBuilder message = new StringBuilder("**Ваши испытания:**\n\n");
            for (Challenge challenge : userChallenges) {
                ChallengeStats stats = challengeService.getChallengeStats(challenge);
                if (stats != null) {
                    message.append("- ").append(challenge.getName()).append(": ")
                            .append(stats.getCurrentValue()).append("/").append(stats.getTargetValue())
                            .append(" (").append(String.format("%.2f", stats.getPercentage())).append("%)\n");
                }
            }
            channel.sendMessage(message.toString()).queue();
        } catch (Exception e) {
            logger.error("Ошибка обработки команды мои", e);
        }
    }
}
