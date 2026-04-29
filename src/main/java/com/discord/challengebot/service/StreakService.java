package com.discord.challengebot.service;

import com.discord.challengebot.event.StreakMilestoneEvent;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Сервис управления сериями (streak) активности пользователей.
 * При достижении порогов (3, 7, 30 дней) публикует {@link StreakMilestoneEvent}
 * для отправки поздравления через DiscordService.
 * Работает напрямую с ParticipantRepository, без промежуточного DataStorageService.
 */
@Service
public class StreakService {
	private static final Logger logger = LoggerFactory.getLogger(StreakService.class);
	// Фиксированный часовой пояс для корректного расчёта границ суток независимо от timezone сервера
	private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");

	private final ParticipantRepository participantRepository;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * Создаёт сервис серий активности.
	 *
	 * @param participantRepository репозиторий участников
	 * @param eventPublisher        публикатор событий Spring
	 */
	public StreakService(ParticipantRepository participantRepository,
	                     ApplicationEventPublisher eventPublisher) {
		this.participantRepository = participantRepository;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Фиксирует активность пользователя и обновляет серию.
	 * Серия сбрасывается, если пропуск более 1 дня с момента последней активности.
	 * При достижении порогов (3, 7, 30 дней) публикуется {@link StreakMilestoneEvent}.
	 *
	 * @param userId идентификатор пользователя
	 */
	public void recordActivity(String userId) {
		try {
			if (userId == null || userId.isBlank()) return;

			Participant participant = participantRepository.findById(userId).orElse(null);
			if (participant == null) {
				logger.debug("Участник {} не найден для обновления серии", userId);
				return;
			}

			LocalDate today = LocalDate.now(ZONE);
			LocalDate lastActivity = participant.getLastActivityDate();
			int previousStreak = participant.getCurrentStreak();

			if (lastActivity == null) {
				participant.setCurrentStreak(1);
				participant.setLongestStreak(1);
			} else {
				long dayGap = ChronoUnit.DAYS.between(lastActivity, today);
				if (dayGap == 0) {
					// Тот же день — без изменений
				} else if (dayGap == 1) {
					int newStreak = participant.getCurrentStreak() + 1;
					participant.setCurrentStreak(newStreak);
					if (newStreak > participant.getLongestStreak()) {
						participant.setLongestStreak(newStreak);
					}
				} else {
					// Пропуск > 1 дня: сбрасываем серию
					participant.setCurrentStreak(1);
				}
			}

			participant.setLastActivityDate(today);
			participantRepository.save(participant);

			int newStreak = participant.getCurrentStreak();
			logger.debug("Серия пользователя {} обновлена: текущая={}", userId, newStreak);

			// Публикуем событие при достижении порогового значения серии
			if (newStreak != previousStreak) {
				sendStreakEventIfMilestone(userId, participant.getUsername(), newStreak);
			}
		} catch (Exception e) {
			logger.error("Ошибка при обновлении серии активности для пользователя {}", userId, e);
		}
	}

	/**
	 * Публикует StreakMilestoneEvent при достижении порогового значения (3, 7, 30 дней).
	 */
	private void sendStreakEventIfMilestone(String userId, String username, int streak) {
		if (streak == 3 || streak == 7 || streak == 30) {
			try {
				eventPublisher.publishEvent(new StreakMilestoneEvent(userId,
								username != null ? username : userId, streak));
				logger.info("Событие серии {} дней опубликовано для пользователя {}", streak, userId);
			} catch (Exception e) {
				logger.error("Ошибка публикации события серии для пользователя {}", userId, e);
			}
		}
	}

	/**
	 * Возвращает множитель активности в зависимости от длины серии.
	 * Серия менее 7 дней — 1.0, от 7 дней — 1.1, от 30 дней — 1.2.
	 *
	 * @param streak длина серии в днях
	 * @return множитель (1.0, 1.1 или 1.2)
	 */
	public double getStreakMultiplier(int streak) {
		if (streak >= 30) return 1.2;
		if (streak >= 7) return 1.1;
		return 1.0;
	}

	/**
	 * Возвращает текущую длину серии активности пользователя.
	 *
	 * @param userId идентификатор пользователя
	 * @return длина серии в днях, 0 если участник не найден
	 */
	public int getCurrentStreak(String userId) {
		try {
			return participantRepository.findById(userId)
							.map(Participant::getCurrentStreak)
							.orElse(0);
		} catch (Exception e) {
			logger.error("Ошибка при получении серии для пользователя {}", userId, e);
			return 0;
		}
	}
}
