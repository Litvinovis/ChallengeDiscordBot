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
public class ChangeChallengeCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(ChangeChallengeCommand.class);

    @Autowired
    private IChallengeService challengeService;

    @Override
    public boolean canHandle(String cmd) {
        return "изменить".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            if (args.length < 3) {
                channel.sendMessage("Недостаточно параметров. Используйте: +изменить <название> <новая цель>").queue();
                return;
            }
            String challengeName = args[1];
            long newTarget;
            try {
                newTarget = Long.parseLong(args[2]);
                if (newTarget < 0) {
                    channel.sendMessage("Цель не может быть отрицательным числом.").queue();
                    return;
                }
            } catch (NumberFormatException e) {
                channel.sendMessage("Цель должна быть числом.").queue();
                return;
            }
            Challenge challenge = challengeService.getChallenge(challengeName);
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                return;
            }
            Challenge updated = challengeService.updateChallengeTarget(challenge, newTarget);
            if (updated != null) {
                channel.sendMessage("Цель испытания \"" + challengeName + "\" изменена на " + newTarget + ".").queue();
            } else {
                channel.sendMessage("Ошибка при изменении цели испытания \"" + challengeName + "\".").queue();
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды изменения цели испытания", e);
        }
    }
}
