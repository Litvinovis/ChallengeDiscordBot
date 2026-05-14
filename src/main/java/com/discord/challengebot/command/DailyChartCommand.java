package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ProgressHistoryRepository;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IVisualizationService;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

/**
 * Команда {@code +динамика <испытание>} — столбчатый график прогресса по дням за 30 дней.
 */
@Component
@Order(2)
public class DailyChartCommand extends BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(DailyChartCommand.class);

    @Autowired
    private IChallengeService challengeService;
    @Autowired
    private IVisualizationService visualizationService;
    @Autowired
    private ProgressHistoryRepository progressHistoryRepository;

    @Override
    public boolean canHandle(String cmd) {
        return "динамика".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            if (args.length < 2) {
                replyError(event, "Укажите название испытания. Пример: `+динамика Отжимания`");
                return;
            }
            String challengeName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            Challenge challenge = challengeService.getChallenge(challengeName);
            if (challenge == null) {
                replyError(event, "Испытание \"" + challengeName + "\" не найдено.");
                return;
            }
            Map<LocalDate, Long> dailyTotals = progressHistoryRepository.getDailyTotalsAll(challenge.getId(), 30);
            if (dailyTotals.isEmpty()) {
                reply(event, "Нет данных о прогрессе за последние 30 дней для испытания \"" + challengeName + "\".");
                return;
            }
            byte[] imageBytes = visualizationService.generateDailyProgressChart(challengeName, dailyTotals).get();
            if (imageBytes == null || imageBytes.length == 0) {
                replyError(event, "Не удалось сгенерировать график.");
                return;
            }
            event.getChannel().asTextChannel()
                    .sendFiles(FileUpload.fromData(imageBytes, "daily_chart.png"))
                    .setContent("📈 Динамика по дням: **" + challengeName + "**")
                    .queue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Прерывание при генерации графика динамики", e);
            replyError(event, "Операция была прервана. Попробуйте позже.");
        } catch (Exception e) {
            logger.error("Ошибка обработки команды +динамика", e);
            replyError(event, "Произошла ошибка при генерации графика.");
        }
    }
}
