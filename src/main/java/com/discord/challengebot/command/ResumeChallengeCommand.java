package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResumeChallengeCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(ResumeChallengeCommand.class);

    @Autowired
    private IChallengeService challengeService;

    @Override
    public boolean canHandle(String cmd) {
        return "продолжить".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            if (args.length < 2) {
                channel.sendMessage("Укажите название испытания. Используйте: +продолжить <название>").queue();
                return;
            }
            String challengeName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            Challenge challenge = challengeService.getChallenge(challengeName);
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                return;
            }
            Challenge updated = challengeService.updateChallengeStatus(challenge, true);
            if (updated != null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" возобновлено.").queue();
            } else {
                channel.sendMessage("Ошибка при возобновлении испытания \"" + challengeName + "\".").queue();
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды возобновления испытания", e);
        }
    }
}
