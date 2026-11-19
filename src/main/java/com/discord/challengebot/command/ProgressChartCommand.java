package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.repository.ProgressHistoryRepository;
import com.discord.challengebot.service.IVisualizationService;
import com.discord.challengebot.util.TimeZones;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

/**
 * Команда {@code +график <название испытания>} — личный календарь активности.
 */
@Component
@Order(2)
public class ProgressChartCommand extends BaseCommand {
	private static final int CALENDAR_DAYS = 365;
	private static final Logger logger = LoggerFactory.getLogger(ProgressChartCommand.class);

	@Autowired
	private IChallengeService challengeService;
	@Autowired
	private ProgressHistoryRepository progressHistoryRepository;
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
	 * Отправляет календарь прогресса автора команды по выбранному испытанию.
	 *
	 * @param event    событие получения сообщения Discord
	 * @param args     args[0] — "график", args[1..] — название испытания
	 * @param authorId идентификатор автора команды
	 * @param username имя автора команды
	 */
	@Override
	public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
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

			LocalDate today = LocalDate.now(TimeZones.MOSCOW);
			LocalDate start = today.minusDays(CALENDAR_DAYS - 1);
			Map<LocalDate, Long> dailyTotals = progressHistoryRepository.getDailyTotals(
					challenge.getId(), authorId, CALENDAR_DAYS);
			byte[] imageBytes = visualizationService.generateActivityCalendar(start, today, dailyTotals).get();
			if (imageBytes == null || imageBytes.length == 0) {
				replyError(event, "Не удалось сгенерировать график для испытания \"" + challengeName + "\".");
				return;
			}

			event.getChannel().sendFiles(FileUpload.fromData(imageBytes, "activity-calendar.png"))
					.setContent("📅 Личный календарь активности · **" + challengeName + "**")
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
