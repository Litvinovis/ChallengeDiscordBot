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
 * Команда {@code +продолжить} — возобновляет остановленное испытание.
 * Использование: {@code +продолжить <название>}.
 * Доступна только администраторам.
 */
@Component
@Order(1)
public class ResumeChallengeCommand extends BaseCommand {
	private static final Logger logger = LoggerFactory.getLogger(ResumeChallengeCommand.class);

	@Autowired
	private IChallengeService challengeService;

	/**
	 * {@inheritDoc}
	 * Обрабатывает команду {@code продолжить}.
	 *
	 * @param cmd строка команды
	 * @return {@code true}, если команда равна "продолжить"
	 */
	@Override
	public boolean canHandle(String cmd) {
		return "продолжить".equals(cmd);
	}

	/**
	 * {@inheritDoc}
	 * Устанавливает флаг активности испытания в {@code true}.
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
				channel.sendMessage("Укажите название испытания. Используйте: +продолжить <название>").queue();
				return;
			}
			String challengeName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
			Challenge challenge = challengeService.getChallenge(challengeName);
			if (challenge == null) {
				channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
				return;
			}
			Challenge updated = challengeService.updateChallengeStatus(challenge, true);
			if (updated != null) {
				channel.sendMessage("Испытание \"" + challengeName + "\" возобновлено.").queue();
			} else {
				channel.sendMessage("Ошибка при возобновлении испытания \"" + challengeName + "\".").queue();
			}
		} catch (Exception e) {
			logger.error("Ошибка обработки команды возобновления испытания", e);
		}
	}
}
