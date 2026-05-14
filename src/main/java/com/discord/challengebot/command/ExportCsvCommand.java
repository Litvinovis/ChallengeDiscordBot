package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ProgressRecord;
import com.discord.challengebot.repository.ProgressHistoryRepository;
import com.discord.challengebot.service.IChallengeService;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Команда {@code +экспорт <испытание>} — экспорт личного прогресса в CSV-файл.
 */
@Component
@Order(1)
public class ExportCsvCommand extends BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(ExportCsvCommand.class);

    @Autowired
    private IChallengeService challengeService;
    @Autowired
    private ProgressHistoryRepository progressHistoryRepository;

    @Override
    public boolean canHandle(String cmd) {
        return "экспорт".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            if (args.length < 2) {
                replyError(event, "Укажите название испытания. Пример: `+экспорт Отжимания`");
                return;
            }
            String challengeName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            Challenge challenge = challengeService.getChallenge(challengeName);
            if (challenge == null) {
                replyError(event, "Испытание \"" + challengeName + "\" не найдено.");
                return;
            }
            List<ProgressRecord> records = progressHistoryRepository.getAllRecords(challenge.getId(), authorId);
            if (records.isEmpty()) {
                reply(event, "Нет данных о вашем прогрессе по испытанию \"" + challengeName + "\".");
                return;
            }
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            StringBuilder csv = new StringBuilder("Дата,Количество\n");
            records.forEach(r -> csv.append(r.recordedAt().format(fmt))
                    .append(",").append(r.amount()).append("\n"));
            byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            String filename = challenge.getId() + "_export.csv";
            event.getChannel().asTextChannel()
                    .sendFiles(FileUpload.fromData(csvBytes, filename))
                    .setContent("📥 Экспорт прогресса: **" + challengeName + "**")
                    .queue();
        } catch (Exception e) {
            logger.error("Ошибка обработки команды +экспорт", e);
            replyError(event, "Произошла ошибка при экспорте данных.");
        }
    }
}
