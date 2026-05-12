package com.discord.challengebot.command;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IStatisticsService;
import com.discord.challengebot.service.IVisualizationService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Команда {@code +график [название испытания]} — генерирует и отправляет PNG-график прогресса.
 * Использование: {@code +график <название>}.
 */
@Component
@Order(2)
public class ProgressChartCommand extends BaseCommand {
	private static final Logger logger = LoggerFactory.getLogger(ProgressChartCommand.class);

	@Autowired
	private IChallengeService challengeService;
	@Autowired
	private IStatisticsService statisticsService;
	@Autowired
	private IVisualizationService visualizationService;

	/**
	 * {@inheritDoc}
	 * Обрабатывает команду {@code график}.
	 *
	 * @param cmd строка команды
	 * @return {@code true}, если команда равна "график"
	 */
	@Override
	public boolean canHandle(String cmd) {
		return "график".equals(cmd);
	}

	/**
	 * {@inheritDoc}
	 * Ищет испытание по названию, генерирует PNG-график прогресса и отправляет в канал.
	 *
	 * @param event    событие получения сообщения Discord
	 * @param args     args[0] — "график", args[1..] — название испытания
	 * @param authorId идентификатор автора команды
	 * @param username имя автора команды
	 */
	@Override
	public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
		TextChannel channel = event.getChannel().asTextChannel();
		try {
			if (args.length < 2) {
				replyError(event, "Укажите название испытания. Пример: `+график Отжимания`");
				return;
			}

			String challengeName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
			Challenge challenge = challengeService.getChallenge(challengeName);
			if (challenge == null) {
				replyError(event, "Испытание \"" + challengeName + "\" не найдено.");
				return;
			}

			ChallengeStats stats = statisticsService.calculateStats(challenge);
			if (stats == null) {
				replyError(event, "Не удалось получить статистику для испытания \"" + challengeName + "\".");
				return;
			}

			byte[] imageBytes = visualizationService.generateProgressChart(stats).get();
			if (imageBytes == null || imageBytes.length == 0) {
				replyError(event, "Не удалось сгенерировать график для испытания \"" + challengeName + "\".");
				return;
			}

			channel.sendFiles(FileUpload.fromData(imageBytes, "progress.png"))
					.setContent("📊 График прогресса: **" + challengeName + "**")
					.queue();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error("Прерывание при генерации графика", e);
			replyError(event, "Операция была прервана. Попробуйте позже.");
		} catch (Exception e) {
			logger.error("Ошибка обработки команды +график", e);
			replyError(event, "Произошла ошибка при генерации графика.");
		}
	}
}
