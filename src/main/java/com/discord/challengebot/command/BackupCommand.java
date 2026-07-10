package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import com.discord.challengebot.util.TimeZones;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Команда {@code +бэкап} — экспорт всех испытаний и прогресса в JSON-файл (только для администраторов).
 */
@Component
@Order(1)
public class BackupCommand extends BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(BackupCommand.class);

    @Autowired
    private IChallengeService challengeService;
    @Autowired
    private ChallengeProgressRepository progressRepository;
    @Autowired
    private IUserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .findAndRegisterModules();

    @Override
    public boolean canHandle(String cmd) {
        return "бэкап".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            if (!userService.isAdminUser(authorId)) {
                replyError(event, "Недостаточно прав для выполнения команды");
                return;
            }
            List<Challenge> challenges = challengeService.getAllChallenges();
            Map<String, Map<String, Long>> progress = new HashMap<>();
            for (Challenge c : challenges) {
                progress.put(c.getId(), progressRepository.findByChallengeId(c.getId()));
            }
            Map<String, Object> backup = new LinkedHashMap<>();
            backup.put("exportedAt", LocalDateTime.now(TimeZones.MOSCOW).toString());
            backup.put("challenges", challenges);
            backup.put("progress", progress);
            byte[] jsonBytes = objectMapper.writeValueAsBytes(backup);
            event.getChannel().asTextChannel()
                    .sendFiles(FileUpload.fromData(jsonBytes, "backup.json"))
                    .setContent("💾 Бэкап данных создан (" + challenges.size() + " испытаний)")
                    .queue();
        } catch (Exception e) {
            logger.error("Ошибка обработки команды +бэкап", e);
            replyError(event, "Произошла ошибка при создании бэкапа.");
        }
    }
}
