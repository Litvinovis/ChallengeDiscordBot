package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IStatisticsService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TopCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(TopCommand.class);

    @Autowired
    private IChallengeService challengeService;
    @Autowired
    private IStatisticsService statisticsService;

    @Override
    public boolean canHandle(String cmd) {
        return "топ".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            if (args.length < 2) {
                channel.sendMessage("Укажите название испытания. Используйте: +топ <испытание> [количество]").queue();
                return;
            }
            String challengeName = args[1];
            int limit = 5;
            if (args.length > 2) {
                try {
                    limit = Math.min(Integer.parseInt(args[2]), 20);
                } catch (NumberFormatException ignored) {}
            }
            Challenge challenge = challengeService.getChallenge(challengeName);
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                return;
            }
            List<Map.Entry<String, Long>> leaderboard = challengeService.getTopParticipants(challenge, limit);
            String leaderboardMessage = statisticsService.formatLeaderboardForDiscord(challenge, leaderboard);
            channel.sendMessage(leaderboardMessage).queue();
        } catch (Exception e) {
            logger.error("Ошибка обработки команды топ", e);
        }
    }
}
