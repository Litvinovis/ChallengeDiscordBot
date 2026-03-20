package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IUserService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AddParticipantCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(AddParticipantCommand.class);

    @Autowired
    private IChallengeService challengeService;
    @Autowired
    private IUserService userService;

    @Override
    public boolean canHandle(String cmd) {
        return "добавить_участника".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            if (args.length < 3) {
                channel.sendMessage("Недостаточно параметров. Используйте: +добавить_участника <испытание> <пользователь>").queue();
                return;
            }
            String challengeName = args[1];
            String userMention = args[2];
            String userId = userMention.replaceAll("[^0-9]", "");

            String resolvedUsername = "user_" + userId;
            try {
                net.dv8tion.jda.api.entities.User user = event.getJDA().getUserById(userId);
                if (user != null) {
                    resolvedUsername = user.getName();
                } else {
                    Participant cached = userService.getParticipant(userId);
                    if (cached != null && cached.getUsername() != null) {
                        resolvedUsername = cached.getUsername();
                    }
                }
            } catch (Exception ignored) {}

            Challenge challenge = challengeService.getChallenge(challengeName);
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                return;
            }
            Challenge updated = challengeService.addParticipantWithUsername(challenge, userId, resolvedUsername);
            if (updated != null) {
                channel.sendMessage("Участник <@" + userId + "> добавлен в испытание \"" + challengeName + "\".").queue();
            } else {
                channel.sendMessage("Ошибка при добавлении участника.").queue();
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды добавления участника", e);
        }
    }
}
