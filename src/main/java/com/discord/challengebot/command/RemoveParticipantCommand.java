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
public class RemoveParticipantCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(RemoveParticipantCommand.class);

    @Autowired
    private IChallengeService challengeService;

    @Override
    public boolean canHandle(String cmd) {
        return "удалить_участника".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            if (args.length < 3) {
                channel.sendMessage("Недостаточно параметров. Используйте: +удалить_участника <испытание> <пользователь>").queue();
                return;
            }
            String challengeName = args[1];
            String userMention = args[2];
            String userId = userMention.replaceAll("[^0-9]", "");

            Challenge challenge = challengeService.getChallenge(challengeName);
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                return;
            }
            Challenge updated = challengeService.removeParticipant(challenge, userId);
            if (updated != null) {
                channel.sendMessage("Участник <@" + userId + "> удален из испытания \"" + challengeName + "\".").queue();
            } else {
                channel.sendMessage("Ошибка при удалении участника.").queue();
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды удаления участника", e);
        }
    }
}
