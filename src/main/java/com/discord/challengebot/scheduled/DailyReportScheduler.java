package com.discord.challengebot.scheduled;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeArchiveRepository;
import com.discord.challengebot.service.ChallengeRenewalService;
import com.discord.challengebot.service.ChallengeService;
import com.discord.challengebot.service.DiscordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import com.discord.challengebot.util.TimeZones;
import java.util.List;

/**
 * Планировщик периодических задач бота.
 * Отправляет ежедневные отчёты о прогрессе, проверяет завершение испытаний
 * и очищает устаревшие данные по расписанию из конфигурации.
 */
@Component
public class DailyReportScheduler {
	private static final Logger logger = LoggerFactory.getLogger(DailyReportScheduler.class);

	@Autowired
	private DiscordService discordService;

	@Autowired
	private ChallengeService challengeService;

	@Autowired
	private ChallengeArchiveRepository challengeArchiveRepository;

	@Autowired
	private ChallengeRenewalService challengeRenewalService;

	/**
	 * Отправка ежедневных отчетов о прогрессе в 7:00 утра
	 */
	@Scheduled(cron = "${scheduled.cron.daily-report}", zone = "Europe/Moscow")
	public void sendDailyProgressReports() {
		logger.info("Запуск отправки ежедневных отчетов о прогрессе");
		try {
			discordService.sendDailyReport();
			logger.info("Ежедневные отчеты успешно отправлены");
		} catch (Exception e) {
			logger.error("Ошибка при отправке ежедневных отчетов", e);
		}
	}

	/** Проверка сроков испытаний по московскому времени. */
	@Scheduled(cron = "0 * * * * *", zone = "Europe/Moscow")
	public void checkChallengeCompletions() {
		LocalDateTime now = LocalDateTime.now(TimeZones.MOSCOW);
		for (Challenge challenge : challengeService.getAllChallenges()) {
			if (!challenge.isActive()) continue;
			try {
				if (challenge.getEndDate() != null) {
					if (!challenge.getEndDate().isAfter(now)) {
						challengeRenewalService.renewIfDue(challenge.getId(), now)
								.ifPresent(discordService::sendChallengeRenewalNotification);
					}
				} else if (challenge.getCurrentValue() >= challenge.getTargetValue()) {
					challengeService.completeChallenge(challenge);
					discordService.sendChallengeCompletionNotification(challenge);
				}
			} catch (Exception e) {
				logger.error("Ошибка при завершении испытания '{}'", challenge.getName(), e);
			}
		}
	}

	/**
	 * Ежемесячный отчёт 1-го числа каждого месяца в 9:00
	 */
	@Scheduled(cron = "0 0 9 1 * ?", zone = "Europe/Moscow")
	public void sendMonthlyReport() {
		logger.info("Отправка ежемесячного отчёта");
		try {
			discordService.sendMonthlyReport();
		} catch (Exception e) {
			logger.error("Ошибка при отправке ежемесячного отчёта", e);
		}
	}

	/**
	 * Очистка старых данных каждый день в 2:00 ночи
	 */
	@Scheduled(cron = "0 0 2 * * ?", zone = "Europe/Moscow") // Каждый день в 2:00
	public void cleanupOldData() {
		logger.info("Очистка старых данных");
		try {
			// Получаем все испытания
			List<Challenge> allChallenges = challengeService.getAllChallenges();
			LocalDateTime thirtyDaysAgo = LocalDateTime.now(TimeZones.MOSCOW).minusDays(30);
			int deletedCount = 0;

			// Архивируем и удаляем завершенные испытания старше 30 дней
			for (Challenge challenge : allChallenges) {
				if (!challenge.isActive() && challenge.getEndDate() != null && challenge.getEndDate().isBefore(thirtyDaysAgo)) {
					try {
						challengeArchiveRepository.archive(challenge);
					} catch (Exception e) {
						logger.warn("Не удалось архивировать испытание {}: {}", challenge.getName(), e.getMessage());
					}
					try {
						if (challengeService.deleteChallenge(challenge.getName())) {
							deletedCount++;
							logger.info("Архивировано и удалено старое завершенное испытание: {}", challenge.getName());
						}
					} catch (Exception e) {
						logger.error("Не удалось удалить архивированное испытание {}", challenge.getName(), e);
					}
				}
			}

			logger.info("Очистка старых данных завершена. Удалено {} испытаний", deletedCount);
		} catch (Exception e) {
			logger.error("Ошибка при очистке старых данных", e);
		}
	}
}