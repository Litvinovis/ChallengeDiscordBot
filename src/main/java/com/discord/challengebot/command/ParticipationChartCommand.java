package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IVisualizationService;
import com.discord.challengebot.service.ParticipantService;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Команда {@code +участие <испытание>} — круговая диаграмма вклада участников.
 */
@Component
@Order(2)
public class ParticipationChartCommand extends BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(ParticipationChartCommand.class);

    @Autowired
    private IChallengeService challengeService;
    @Autowired
    private IVisualizationService visualizationService;
    @Autowired
    private ParticipantService participantService;

    @Override
    public boolean canHandle(String cmd) {
        return "участие".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            if (args.length < 2) {
                replyError(event, "Укажите название испытания. Пример: `+участие Отжимания`");
                return;
            }
            String challengeName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            Challenge challenge = challengeService.getChallenge(challengeName);
            if (challenge == null) {
                replyError(event, "Испытание \"" + challengeName + "\" не найдено.");
                return;
            }
            Map<String, Long> progress = challenge.getParticipantProgress();
            if (progress.isEmpty()) {
                reply(event, "Нет данных об участниках для испытания \"" + challengeName + "\".");
                return;
            }
            // Resolve usernames for display
            Map<String, Long> named = new LinkedHashMap<>();
            progress.forEach((uid, amount) -> {
                String name = uid;
                try {
                    Participant p = participantService.getParticipant(uid);
                    if (p != null && p.getUsername() != null && !p.getUsername().isBlank()) {
                        name = p.getUsername();
                    }
                } catch (Exception ignored) {}
                named.put(name, amount);
            });
            byte[] imageBytes = visualizationService.generateParticipationPieChart(challengeName, named).get();
            if (imageBytes == null || imageBytes.length == 0) {
                replyError(event, "Не удалось сгенерировать диаграмму.");
                return;
            }
            event.getChannel().asTextChannel()
                    .sendFiles(FileUpload.fromData(imageBytes, "participation.png"))
                    .setContent("🥧 Вклад участников: **" + challengeName + "**")
                    .queue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Прерывание при генерации диаграммы участия", e);
            replyError(event, "Операция была прервана. Попробуйте позже.");
        } catch (Exception e) {
            logger.error("Ошибка обработки команды +участие", e);
            replyError(event, "Произошла ошибка при генерации диаграммы.");
        }
    }
}
