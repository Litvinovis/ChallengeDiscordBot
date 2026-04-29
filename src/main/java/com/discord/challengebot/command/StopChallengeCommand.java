package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

/**
 * Команда {@code +остановить} — приостанавливает активное испытание.
 * Использование: {@code +остановить <название>}.
 * Доступна только администраторам.
 */
@Component
@Order(1)
public class StopChallengeCommand extends BaseCommand {
	private static final Logger logger = LoggerFactory.getLogger(StopChallengeCommand.class);

	@Autowired
	private IChallengeService challengeService;

	/**
	 * {@inheritDoc}
	 * Обрабатывает команду {@code остановить}.
	 *
	 * @param cmd строка команды
	 * @return {@code true}, если команда равна "остановить"
	 */
	@Override
	public boolean canHandle(String cmd) {
		return "остановить".equals(cmd);
	}

	/**
	 * {@inheritDoc}
	 * Устанавливает флаг активности испытания в {@code false}.
	 *
	 * @param event    событие получения сообщения Discord
	 * @param args     аргументы: args[1..] — название испытания
	 * @param authorId идентификатор автора команды
	 * @param username имя автора команды
	 */
	@Override
	public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
		try {
			TextChannel channel = event.getChannel().asTextChannel();
			if (args.length < 2) {
				channel.sendMessage("Укажите название испытания. Используйте: +остановить <название>").queue();
				return;
			}
			String challengeName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
			Challenge challenge = challengeService.getChallenge(challengeName);
			if (challenge == null) {
				channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
				return;
			}
			Challenge updated = challengeService.updateChallengeStatus(challenge, false);
			if (updated != null) {
				channel.sendMessage("Испытание \"" + challengeName + "\" остановлено.").queue();
			} else {
				channel.sendMessage("Ошибка при остановке испытания \"" + challengeName + "\".").queue();
			}
		} catch (Exception e) {
			logger.error("Ошибка обработки команды остановки испытания", e);
		}
	}
}
