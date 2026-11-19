package com.discord.challengebot.command;

import com.discord.challengebot.service.ChallengeBackupService;
import com.discord.challengebot.service.IUserService;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Команда {@code +бэкап} — экспорт всех испытаний и прогресса в JSON-файл (только для администраторов).
 */
@Component
@Order(1)
public class BackupCommand extends BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(BackupCommand.class);

    @Autowired
    private ChallengeBackupService backupService;
    @Autowired
    private IUserService userService;

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
            byte[] jsonBytes = backupService.exportSnapshot().getBytes(StandardCharsets.UTF_8);
            event.getChannel().asTextChannel()
                    .sendFiles(FileUpload.fromData(jsonBytes, "backup.json"))
                    .setContent("💾 Полный бэкап данных создан.")
                    .queue();
        } catch (Exception e) {
            logger.error("Ошибка обработки команды +бэкап", e);
            replyError(event, "Произошла ошибка при создании бэкапа.");
        }
    }
}
