package com.discord.challengebot.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Registry holding all Command beans, finds matching command for a given input
 */
@Component
public class CommandRegistry {
    private static final Logger logger = LoggerFactory.getLogger(CommandRegistry.class);

    private final List<Command> commands;

    @Autowired
    public CommandRegistry(List<Command> commands) {
        this.commands = commands;
    }

    /**
     * Find the first command that can handle the given command string
     */
    public Optional<Command> findCommand(String cmd) {
        if (cmd == null) return Optional.empty();
        return commands.stream()
                .filter(c -> c.canHandle(cmd))
                .findFirst();
    }
}
