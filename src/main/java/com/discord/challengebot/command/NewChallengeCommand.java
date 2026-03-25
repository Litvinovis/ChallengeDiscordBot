package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IUserService;
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

/**
 * Команда {@code +новый} — создаёт новое испытание.
 * Использование: {@code +новый <название> <цель> [дата окончания] [тип]}.
 * Не-администраторы могут создавать только индивидуальные испытания.
 */
@Component
public class NewChallengeCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(NewChallengeCommand.class);

    @Autowired
    private IChallengeService challengeService;
    @Autowired
    private IUserService userService;

    /**
     * {@inheritDoc}
     * Обрабатывает команду {@code новый}.
     *
     * @param cmd строка команды
     * @return {@code true}, если команда равна "новый"
     */
    @Override
    public boolean canHandle(String cmd) {
        return "новый".equals(cmd);
    }

    /**
     * {@inheritDoc}
     * Создаёт новое испытание с указанными параметрами.
     *
     * @param event    событие получения сообщения Discord
     * @param args     аргументы: args[1] — название, args[2] — цель, args[3] — дата, args[4] — тип
     * @param authorId идентификатор автора команды
     * @param username имя автора команды
     */
    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            boolean isAdmin = userService.isAdminUser(authorId);

            if (args.length < 3) {
                channel.sendMessage("Недостаточно параметров. Используйте: +новый <название> <цель> [дата окончания] [тип]").queue();
                return;
            }

            String name = args[1];
            long target;
            try {
                target = Long.parseLong(args[2]);
                if (target < 0) {
                    channel.sendMessage("Цель не может быть отрицательным числом.").queue();
                    return;
                }
            } catch (NumberFormatException e) {
                channel.sendMessage("Цель должна быть числом.").queue();
                return;
            }

            LocalDateTime endDate = LocalDateTime.now().plusDays(365);
            ChallengeType type = isAdmin ? ChallengeType.GROUP : ChallengeType.INDIVIDUAL;
            String description = "Испытание по " + name;
            String unit = "единиц";

            if (args.length > 3) {
                try {
                    endDate = LocalDateTime.parse(args[3] + " 00:00:00", DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                } catch (DateTimeParseException e) {
                    try {
                        endDate = LocalDate.parse(args[3], DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay();
                    } catch (DateTimeParseException e2) {
                        try {
                            endDate = LocalDateTime.parse(args[3], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        } catch (DateTimeParseException e3) {
                            logger.warn("Неверный формат даты '{}', используется значение по умолчанию", args[3]);
                        }
                    }
                }
            }

            if (args.length > 4 && isAdmin && "индивидуальное".equalsIgnoreCase(args[4])) {
                type = ChallengeType.INDIVIDUAL;
            }

            Challenge challenge = challengeService.createChallenge(name, target, endDate, type, description, unit);
            if (challenge != null) {
                channel.sendMessage("Испытание \"" + name + "\" успешно создано!").queue();
            } else {
                channel.sendMessage("Ошибка при создании испытания \"" + name + "\".").queue();
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды создания испытания", e);
        }
    }
}
