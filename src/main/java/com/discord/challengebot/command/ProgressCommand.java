package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IStatisticsService;
import com.discord.challengebot.service.StreakService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ProgressCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(ProgressCommand.class);

    @Autowired
    private IChallengeService challengeService;
    @Autowired
    private IStatisticsService statisticsService;
    @Autowired
    private StreakService streakService;

    @Override
    public boolean canHandle(String cmd) {
        return "прогресс".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            if (args.length < 2) {
                channel.sendMessage("Укажите название испытания. Используйте: +прогресс <испытание>").queue();
                return;
            }
            String challengeName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            Challenge challenge = challengeService.getChallenge(challengeName);
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                return;
            }

            long userProgress = challenge.getParticipantProgress().getOrDefault(authorId, 0L);
            double pct = challenge.getTargetValue() > 0 ? (double) userProgress / challenge.getTargetValue() * 100 : 0;
            int streak = streakService.getCurrentStreak(authorId);

            StringBuilder message = new StringBuilder();
            message.append(String.format("**Ваш прогресс по испытанию \"%s\":**\n%s: %d/%d (%.2f%%)",
                    challenge.getName(), challenge.getUnit(), userProgress,
                    challenge.getTargetValue(), pct));

            // Streak info
            if (streak > 0) {
                message.append(String.format("\n🔥 Серия: %d дней", streak));
            }

            // Forecast
            try {
                if (userProgress >= challenge.getTargetValue()) {
                    message.append("\n✅ Уже выполнено!");
                } else {
                    LocalDate forecast = statisticsService.forecastCompletionDate(challenge.getId(), authorId);
                    if (forecast == null && statisticsService instanceof com.discord.challengebot.service.StatisticsService) {
                        forecast = ((com.discord.challengebot.service.StatisticsService) statisticsService)
                                .forecastCompletionDate(challenge, authorId);
                    }
                    if (forecast != null) {
                        message.append(String.format("\n📅 При текущем темпе: завершишь к %s", forecast.toString()));
                    }
                }
            } catch (Exception ignored) {}

            channel.sendMessage(message.toString()).queue();
        } catch (Exception e) {
            logger.error("Ошибка обработки команды прогресс", e);
        }
    }
}
