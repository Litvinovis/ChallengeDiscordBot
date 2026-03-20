package com.discord.challengebot.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Interface for bot commands
 */
public interface Command {
    /**
     * Returns true if this command can handle the given command string
     */
    boolean canHandle(String cmd);

    /**
     * Execute the command
     */
    void execute(MessageReceivedEvent event, String[] args, String authorId, String username);
}
