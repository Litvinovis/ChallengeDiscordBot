package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class ChangeDateCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(ChangeDateCommand.class);

    @Autowired
    private IChallengeService challengeService;

    @Override
    public boolean canHandle(String cmd) {
        return "изменить_дату".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            if (args.length < 3) {
                channel.sendMessage("Недостаточно параметров. Используйте: +изменить_дату <название> <новая дата>").queue();
                return;
            }
            String challengeName = args[1];
            LocalDateTime newEndDate;
            try {
                newEndDate = LocalDateTime.parse(args[2] + " 00:00:00", DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
            } catch (DateTimeParseException e) {
                try {
                    newEndDate = LocalDate.parse(args[2], DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay();
                } catch (DateTimeParseException e2) {
                    try {
                        newEndDate = LocalDateTime.parse(args[2], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    } catch (DateTimeParseException e3) {
                        channel.sendMessage("Дата должна быть в формате dd.MM.yyyy (например: 31.12.2025).").queue();
                        return;
                    }
                }
            }
            Challenge challenge = challengeService.getChallenge(challengeName);
            if (challenge == null) {
                channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                return;
            }
            Challenge updated = challengeService.updateChallengeEndDate(challenge, newEndDate);
            if (updated != null) {
                channel.sendMessage("Дата окончания испытания \"" + challengeName + "\" изменена на " + args[2] + ".").queue();
            } else {
                channel.sendMessage("Ошибка при изменении даты окончания испытания \"" + challengeName + "\".").queue();
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды изменения даты испытания", e);
        }
    }
}
