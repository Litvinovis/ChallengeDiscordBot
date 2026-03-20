package com.discord.challengebot.command;

import com.discord.challengebot.service.IUserService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateNameCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(UpdateNameCommand.class);

    @Autowired
    private IUserService userService;

    @Override
    public boolean canHandle(String cmd) {
        return "обновить_имя".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            String newUsername = username;
            if (args.length > 1) {
                newUsername = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            }
            boolean updated = userService.updateParticipantUsername(authorId, newUsername);
            if (updated) {
                channel.sendMessage("Ваше имя успешно обновлено на: " + newUsername).queue();
            } else {
                channel.sendMessage("Ошибка при обновлении имени.").queue();
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды обновить_имя", e);
        }
    }
}
