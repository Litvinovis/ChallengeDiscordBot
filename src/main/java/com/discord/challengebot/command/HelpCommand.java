package com.discord.challengebot.command;

import com.discord.challengebot.service.IDiscordService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(HelpCommand.class);

    @Autowired
    private IDiscordService discordService;

    @Override
    public boolean canHandle(String cmd) {
        return "помощь".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            String helpMessage = discordService.generateHelpMessage(authorId);
            TextChannel channel = event.getChannel().asTextChannel();
            channel.sendMessage(helpMessage).queue();
        } catch (Exception e) {
            logger.error("Ошибка обработки команды помощи", e);
        }
    }
}
