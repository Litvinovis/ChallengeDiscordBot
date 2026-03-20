package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IStatisticsService;
import com.discord.challengebot.service.StatisticsService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * Команда +прогноз — показывает прогнозируемую дату завершения испытания
 * на основе среднего темпа за последние 7 дней.
 * Использование: +прогноз <название испытания>
 */
@Component
public class ForecastCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(ForecastCommand.class);

    @Autowired
    private IChallengeService challengeService;

    @Autowired
    private IStatisticsService statisticsService;

    @Override
    public boolean canHandle(String cmd) {
        return "прогноз".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();

            if (args.length < 2) {
                channel.sendMessage("Укажите название испытания. Использование: +прогноз <испытание>").queue();
                return;
            }

            String challengeName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            Challenge challenge = challengeService.getChallenge(challengeName);

            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                return;
            }

            long userProgress = challenge.getParticipantProgress().getOrDefault(authorId, 0L);
            long targetValue = challenge.getTargetValue();

            if (userProgress >= targetValue) {
                channel.sendMessage(String.format(
                        "✅ Ты уже выполнил цель по испытанию \"%s\"! (%d/%d)",
                        challenge.getName(), userProgress, targetValue
                )).queue();
                return;
            }

            // Пробуем получить прогноз через интерфейс, затем через полную реализацию
            LocalDate forecast = statisticsService.forecastCompletionDate(challenge.getId(), authorId);
            if (forecast == null && statisticsService instanceof StatisticsService) {
                forecast = ((StatisticsService) statisticsService).forecastCompletionDate(challenge, authorId);
            }

            if (forecast == null) {
                channel.sendMessage(String.format(
                        "📊 Недостаточно данных для прогноза по испытанию \"%s\".\n" +
                        "Продолжай выполнять задание, и через несколько дней появится прогноз!",
                        challenge.getName()
                )).queue();
                return;
            }

            long remaining = targetValue - userProgress;
            channel.sendMessage(String.format(
                    "📅 **Прогноз по испытанию \"%s\":**\n" +
                    "Выполнено: %d / %d\n" +
                    "Осталось: %d\n" +
                    "При текущем темпе ты закончишь цель к **%s**",
                    challenge.getName(), userProgress, targetValue, remaining, forecast.toString()
            )).queue();

        } catch (Exception e) {
            logger.error("Ошибка обработки команды прогноз для пользователя {}", username, e);
        }
    }
}
