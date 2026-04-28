package com.discord.challengebot.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Реестр команд бота.
 * Хранит все зарегистрированные бины {@link Command} и ищет подходящую команду по входной строке.
 */
@Component
public class CommandRegistry {
	private static final Logger logger = LoggerFactory.getLogger(CommandRegistry.class);

	private final List<Command> commands;

	/**
	 * Конструктор с внедрением списка всех доступных команд.
	 *
	 * @param commands список всех зарегистрированных команд в контексте Spring
	 */
	@Autowired
	public CommandRegistry(List<Command> commands) {
		this.commands = commands;
	}

	/**
	 * Находит первую команду, способную обработать указанную строку.
	 *
	 * @param cmd строка команды (без префикса '+')
	 * @return Optional с найденной командой, либо пустой Optional
	 */
	public Optional<Command> findCommand(String cmd) {
		if (cmd == null) return Optional.empty();
		return commands.stream()
						.filter(c -> c.canHandle(cmd))
						.findFirst();
	}
}
