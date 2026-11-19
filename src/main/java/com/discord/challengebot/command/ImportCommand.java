package com.discord.challengebot.command;

import com.discord.challengebot.service.ChallengeBackupService;
import com.discord.challengebot.service.IUserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Команда {@code +импорт} — импорт испытаний и прогресса из прикреплённого JSON-файла (только для администраторов).
 */
@Component
@Order(1)
public class ImportCommand extends BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(ImportCommand.class);

    @Autowired
    private ChallengeBackupService backupService;
    @Autowired
    private IUserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public boolean canHandle(String cmd) {
        return "импорт".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            if (!userService.isAdminUser(authorId)) {
                replyError(event, "Недостаточно прав для выполнения команды");
                return;
            }
            List<Message.Attachment> attachments = event.getMessage().getAttachments();
            if (attachments.isEmpty()) {
                replyError(event, "Прикрепите JSON-файл бэкапа к сообщению.");
                return;
            }
            JsonNode root;
            try (var input = attachments.getFirst().getProxy().download().get(30, TimeUnit.SECONDS)) {
                root = objectMapper.readTree(input);
            }
            int imported = backupService.restore(root);
            reply(event, "✅ Импорт завершён. Загружено испытаний: " + imported);
        } catch (IllegalArgumentException e) {
            replyError(event, e.getMessage());
        } catch (Exception e) {
            logger.error("Ошибка обработки команды +импорт", e);
            replyError(event, "Произошла ошибка при импорте данных.");
        }
    }
}
