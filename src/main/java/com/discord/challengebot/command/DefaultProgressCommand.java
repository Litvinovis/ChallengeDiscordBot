package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.AchievementService;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.StatisticsService;
import com.discord.challengebot.service.StreakService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handles default progress update commands like "+отжимания 10"
 */
@Component
public class DefaultProgressCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(DefaultProgressCommand.class);

    @Autowired
    private IChallengeService challengeService;
    @Autowired
    private AchievementService achievementService;
    @Autowired
    private StreakService streakService;
    @Autowired
    private StatisticsService statisticsService;

    @Override
    public boolean canHandle(String cmd) {
        // This is the catch-all command - matches any command
        return true;
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            if (args.length < 2) {
                channel.sendMessage("Недостаточно параметров. Используйте: +<испытание> <количество>").queue();
                return;
            }

            String challengeName = args[0];
            long amount;
            try {
                amount = Long.parseLong(args[1]);
                if (amount < 0) {
                    channel.sendMessage("Количество не может быть отрицательным числом.").queue();
                    return;
                }
            } catch (NumberFormatException e) {
                channel.sendMessage("Количество должно быть числом.").queue();
                return;
            }

            Challenge challenge = challengeService.getChallenge(challengeName);
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                return;
            }

            if (!challenge.isActive()) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не активно.").queue();
                return;
            }

            Challenge updatedChallenge = challengeService.addProgress(challenge, authorId, username, amount);
            if (updatedChallenge != null) {
                long userTotalProgress = updatedChallenge.getParticipantProgress().getOrDefault(authorId, 0L);
                long totalChallengeProgress = updatedChallenge.getCurrentValue();
                long targetValue = updatedChallenge.getTargetValue();

                String message = String.format(
                        "Прогресс по испытанию \"%s\" обновлен на %d, общее количество выполненных тобой действий - %d. Общий прогресс %d/%d.",
                        challengeName, amount, userTotalProgress, totalChallengeProgress, targetValue);
                channel.sendMessage(message).queue();

                // Record streak activity
                try {
                    streakService.recordActivity(authorId);
                } catch (Exception e) {
                    logger.debug("Ошибка обновления серии для пользователя {}", authorId);
                }

                // Record daily progress for forecast
                try {
                    statisticsService.recordDailyProgress(updatedChallenge.getId(), authorId, amount);
                } catch (Exception e) {
                    logger.debug("Ошибка записи ежедневного прогресса для пользователя {}", authorId);
                }

                // Check achievements
                try {
                    achievementService.checkAndAwardAchievements(authorId, updatedChallenge.getId(), userTotalProgress);
                } catch (Exception e) {
                    logger.debug("Ошибка проверки достижений для пользователя {}", authorId);
                }
            } else {
                channel.sendMessage("Ошибка при обновлении прогресса по испытанию \"" + challengeName + "\".").queue();
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды обновления прогресса для пользователя {}", username, e);
        }
    }
}
