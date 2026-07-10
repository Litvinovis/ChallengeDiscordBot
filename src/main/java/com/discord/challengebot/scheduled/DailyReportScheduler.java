package com.discord.challengebot.scheduled;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeArchiveRepository;
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

	/**
	 * Отправка ежедневных отчетов о прогрессе в 7:00 утра
	 */
	@Scheduled(cron = "${scheduled.cron.daily-report}")
	public void sendDailyProgressReports() {
		logger.info("Запуск отправки ежедневных отчетов о прогрессе");
		try {
			discordService.sendDailyReport();
			logger.info("Ежедневные отчеты успешно отправлены");
		} catch (Exception e) {
			logger.error("Ошибка при отправке ежедневных отчетов", e);
		}
	}

	/**
	 * Проверка завершения испытаний каждый час
	 */
	@Scheduled(cron = "0 0 * * * ?") // Каждый час
	public void checkChallengeCompletions() {
		logger.info("Проверка завершения испытаний");

		try {
			// Получаем все активные испытания
			List<Challenge> challenges = challengeService.getAllChallenges();
			LocalDateTime now = LocalDateTime.now(TimeZones.MOSCOW);

			logger.debug("Получено {} активных испытаний для проверки завершения", challenges.size());

			int completedChallenges = 0;
			for (Challenge challenge : challenges) {
				// Проверяем, активно ли испытание и достигнута ли цель
				if (challenge.isActive() && isChallengeCompleted(challenge)) {
					logger.info("Испытание '{}' завершено по достижению цели", challenge.getName());
					// Испытание завершено успешно по достижению цели
					challengeService.completeChallenge(challenge);
					discordService.sendChallengeCompletionNotification(challenge);
					completedChallenges++;
				} else if (challenge.isActive() && challenge.getEndDate() != null && challenge.getEndDate().isBefore(now)) {
					logger.info("Испытание '{}' завершено по истечению срока", challenge.getName());
					// Испытание завершено по истечению срока без достижения цели
					challengeService.completeChallenge(challenge);
					discordService.sendChallengeFailureNotification(challenge);
					completedChallenges++;
				}
			}

			logger.info("Проверка завершения испытаний завершена. Завершено {} испытаний", completedChallenges);
		} catch (Exception e) {
			logger.error("Ошибка при проверке завершения испытаний", e);
		}
	}

	/**
	 * Проверяет, достигнуто ли целевое значение испытания.
	 *
	 * @param challenge испытание для проверки
	 * @return {@code true}, если текущее значение >= целевого
	 */
	private boolean isChallengeCompleted(Challenge challenge) {
		try {
			logger.debug("Проверка завершения испытания по цели: {} (текущее значение: {}, целевое значение: {})",
							challenge.getName(), challenge.getCurrentValue(), challenge.getTargetValue());

			// Испытание считается завершенным, если текущее значение больше или равно целевому
			boolean isCompleted = challenge.getCurrentValue() >= challenge.getTargetValue();

			logger.debug("Статус завершения по цели для испытания '{}': {}", challenge.getName(), isCompleted);
			return isCompleted;
		} catch (Exception e) {
			logger.error("Ошибка при проверке завершения испытания по цели: {}",
							challenge != null ? challenge.getName() : "null", e);
			return false;
		}
	}

	/**
	 * Ежемесячный отчёт 1-го числа каждого месяца в 9:00
	 */
	@Scheduled(cron = "0 0 9 1 * ?")
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
	@Scheduled(cron = "0 0 2 * * ?") // Каждый день в 2:00
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
					if (challengeService.deleteChallenge(challenge.getName())) {
						deletedCount++;
						logger.info("Архивировано и удалено старое завершенное испытание: {}", challenge.getName());
					}
				}
			}

			logger.info("Очистка старых данных завершена. Удалено {} испытаний", deletedCount);
		} catch (Exception e) {
			logger.error("Ошибка при очистке старых данных", e);
		}
	}
}