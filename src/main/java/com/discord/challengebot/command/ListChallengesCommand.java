package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListChallengesCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(ListChallengesCommand.class);

    @Autowired
    private IChallengeService challengeService;

    @Override
    public boolean canHandle(String cmd) {
        return "испытания".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            List<Challenge> activeChallenges = challengeService.getActiveChallenges();

            if (activeChallenges.isEmpty()) {
                channel.sendMessage("Активных испытаний нет.").queue();
                return;
            }

            StringBuilder message = new StringBuilder("**Активные испытания:**\n\n");
            for (Challenge challenge : activeChallenges) {
                message.append("- **").append(challenge.getName()).append("**\n");
                message.append("  Цель: ").append(challenge.getTargetValue()).append(" ").append(challenge.getUnit()).append("\n");
                message.append("  Участников: ").append(challenge.getParticipants().size()).append("\n");
                message.append("  Окончание: ").append(challenge.getEndDate().toLocalDate().toString()).append("\n\n");
            }
            channel.sendMessage(message.toString()).queue();
        } catch (Exception e) {
            logger.error("Ошибка обработки команды испытания", e);
        }
    }
}
