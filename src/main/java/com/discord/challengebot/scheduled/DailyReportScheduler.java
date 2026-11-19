package com.discord.challengebot.scheduled;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeRepository;
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
	private ChallengeRepository challengeRepository;

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
		for (Challenge challenge : challengeRepository.findDue(now)) {
			try {
				if (challenge.getEndDate() != null) {
					if (!challenge.getEndDate().isAfter(now)) {
						challengeRenewalService.renewIfDue(challenge.getId(), now)
								.ifPresent(discordService::sendChallengeRenewalNotification);
					}
				} else if (challengeService.completeChallenge(challenge)) {
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
		LocalDateTime cutoff = LocalDateTime.now(TimeZones.MOSCOW).minusDays(30);
		for (String id : challengeRepository.findCompletedBefore(cutoff)) {
			try {
				if (challengeRenewalService.archiveCompleted(id, cutoff)) {
					logger.info("Испытание '{}' перенесено в архив", id);
				}
			} catch (Exception e) {
				logger.error("Не удалось архивировать испытание '{}'", id, e);
			}
		}
	}
}
