package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис управления испытаниями.
 * Работает напрямую с ChallengeRepository и ChallengeProgressRepository
 * без промежуточного слоя DataStorageService.
 */
@Service
public class ChallengeService implements IChallengeService {
	private static final Logger logger = LoggerFactory.getLogger(ChallengeService.class);

	private final ChallengeRepository challengeRepository;
	private final ChallengeProgressRepository progressRepository;
	private final ParticipantService participantService;

	/**
	 * Создаёт сервис управления испытаниями.
	 *
	 * @param challengeRepository репозиторий испытаний
	 * @param progressRepository  репозиторий прогресса участников
	 * @param participantService  сервис управления участниками
	 */
	public ChallengeService(ChallengeRepository challengeRepository,
	                        ChallengeProgressRepository progressRepository,
	                        ParticipantService participantService) {
		this.challengeRepository = challengeRepository;
		this.progressRepository = progressRepository;
		this.participantService = participantService;
	}

	// ---- вспомогательные методы работы с хранилищем ----

	private void saveChallenge(Challenge challenge) {
		if (challenge == null) return;
		challengeRepository.save(challenge);
	}

	private Challenge findChallenge(String name) {
		if (name == null || name.isBlank()) return null;
		String id = name.toLowerCase().replace(" ", "_");
		var opt = challengeRepository.findById(id);
		return opt.orElseGet(() -> challengeRepository.findAll().stream()
						.filter(c -> name.equals(c.getName()))
						.findFirst().orElse(null));
		// Fallback: полный перебор для legacy id
	}

	private Map<String, Long> loadProgress(Challenge challenge) {
		return progressRepository.findByChallengeId(challenge.getId());
	}

	private List<String> loadParticipantIds(Challenge challenge) {
		return new ArrayList<>(progressRepository.findByChallengeId(challenge.getId()).keySet());
	}

	// ---- IChallengeService implementation ----

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Challenge createChallenge(String name, long targetValue, LocalDateTime endDate,
	                                 ChallengeType type, String description, String unit) {
		try {
			logger.info("Создание нового испытания: {}", name);
			if (name == null || name.isBlank() || targetValue <= 0 || endDate == null || type == null) {
				logger.warn("Некорректные параметры создания испытания");
				return null;
			}
			var challenge = new Challenge();
			challenge.setId(name.toLowerCase().replace(" ", "_"));
			challenge.setName(name);
			challenge.setTargetValue(targetValue);
			challenge.setCurrentValue(0);
			challenge.setType(type);
			challenge.setStartDate(LocalDateTime.now());
			challenge.setEndDate(endDate);
			challenge.setActive(true);
			challenge.setDescription(description);
			challenge.setUnit(unit);
			saveChallenge(challenge);
			logger.info("Испытание '{}' успешно создано", name);
			return challenge;
		} catch (Exception e) {
			logger.error("Ошибка при создании испытания: {}", name, e);
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Challenge addProgress(Challenge challenge, String userId, String username, long amount) {
		try {
			if (challenge == null || userId == null || userId.isBlank() || amount < 0) {
				return challenge;
			}

			// Регистрируем участника в системе
			participantService.registerForChallenge(userId, username != null ? username : userId, challenge.getName());

			// Обновляем прогресс через нормализованную таблицу
			var progress = loadProgress(challenge);
			long currentUserProgress = progress.getOrDefault(userId, 0L);
			long newUserProgress = currentUserProgress + amount;
			progressRepository.upsert(challenge.getId(), userId, newUserProgress);

			// Добавляем участника в список (для обратной совместимости с Challenge.participants)
			challenge.addParticipant(userId);

			// Обновляем общий прогресс (сумма по всем участникам)
			progress.put(userId, newUserProgress);
			long totalProgress = progress.values().stream().mapToLong(Long::longValue).sum();
			challenge.setCurrentValue(totalProgress);

			// Синхронизируем в-памяти карту для текущей сессии
			challenge.getParticipantProgress().put(userId, newUserProgress);

			saveChallenge(challenge);
			logger.info("Прогресс добавлен: пользователь={}, испытание={}, добавлено={}, итого={}",
							userId, challenge.getName(), amount, newUserProgress);
			return challenge;
		} catch (Exception e) {
			logger.error("Ошибка при добавлении прогресса к испытанию: {}",
							challenge != null ? challenge.getName() : "null", e);
			return challenge;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Challenge subtractProgress(Challenge challenge, String userId, String username, long amount) {
		try {
			if (challenge == null || userId == null || amount <= 0) return challenge;
			var progress = loadProgress(challenge);
			long currentUserProgress = progress.getOrDefault(userId, 0L);
			long newUserProgress = Math.max(0, currentUserProgress - amount);
			progressRepository.upsert(challenge.getId(), userId, newUserProgress);
			progress.put(userId, newUserProgress);
			long totalProgress = progress.values().stream().mapToLong(Long::longValue).sum();
			challenge.setCurrentValue(totalProgress);
			challenge.getParticipantProgress().put(userId, newUserProgress);
			saveChallenge(challenge);
			logger.info("Прогресс уменьшен: пользователь={}, испытание={}, убрано={}, итого={}", userId, challenge.getName(), amount, newUserProgress);
			return challenge;
		} catch (Exception e) {
			logger.error("Ошибка при уменьшении прогресса: {}", challenge != null ? challenge.getName() : "null", e);
			return challenge;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Challenge getChallenge(String name) {
		try {
			return findChallenge(name);
		} catch (Exception e) {
			logger.error("Ошибка при получении испытания: {}", name, e);
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Challenge> getAllChallenges() {
		try {
			return challengeRepository.findAll();
		} catch (Exception e) {
			logger.error("Ошибка при получении всех испытаний", e);
			return new ArrayList<>();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Challenge> getActiveChallenges() {
		try {
			return getAllChallenges().stream()
							.filter(Challenge::isActive)
							.collect(Collectors.toList());
		} catch (Exception e) {
			logger.error("Ошибка при получении активных испытаний", e);
			return new ArrayList<>();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ChallengeStats getChallengeStats(Challenge challenge) {
		try {
			if (challenge == null) return null;
			long remaining = challenge.getTargetValue() - challenge.getCurrentValue();
			double percentage = challenge.getTargetValue() > 0
							? (double) challenge.getCurrentValue() / challenge.getTargetValue() * 100 : 0;
			long daysRemaining = challenge.getEndDate() != null
							? ChronoUnit.DAYS.between(LocalDate.now(), challenge.getEndDate().toLocalDate()) : 0;
			int participantCount = Math.max(loadParticipantIds(challenge).size(), 1);
			double dailyTarget = daysRemaining > 0
							? (double) remaining / participantCount / daysRemaining : 0;
			return new ChallengeStats(challenge.getName(), challenge.getTargetValue(),
							challenge.getCurrentValue(), remaining, percentage, dailyTarget, (int) daysRemaining);
		} catch (Exception e) {
			logger.error("Ошибка при расчёте статистики для испытания: {}",
							challenge != null ? challenge.getName() : "null", e);
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, ChallengeStats> getAllChallengesStats() {
		var result = new java.util.HashMap<String, ChallengeStats>();
		for (var challenge : getAllChallenges()) {
			var stats = getChallengeStats(challenge);
			if (stats != null) result.put(challenge.getName(), stats);
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Challenge> getUserChallenges(String userId) {
		try {
			if (userId == null || userId.isBlank()) return new ArrayList<>();
			return getAllChallenges().stream()
							.filter(ch -> progressRepository.findByChallengeId(ch.getId()).containsKey(userId))
							.collect(Collectors.toList());
		} catch (Exception e) {
			logger.error("Ошибка при получении испытаний пользователя: {}", userId, e);
			return new ArrayList<>();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean deleteChallenge(String challengeName) {
		try {
			logger.info("Удаление испытания: {}", challengeName);
			if (challengeName == null || challengeName.isBlank()) return false;
			String id = challengeName.toLowerCase().replace(" ", "_");
			if (challengeRepository.existsById(id)) {
				progressRepository.deleteByChallengeId(id);
				challengeRepository.deleteById(id);
				return true;
			}
			for (var c : challengeRepository.findAll()) {
				if (challengeName.equals(c.getName())) {
					progressRepository.deleteByChallengeId(c.getId());
					challengeRepository.deleteById(c.getId());
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			logger.error("Ошибка при удалении испытания: {}", challengeName, e);
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Challenge updateChallengeStatus(Challenge challenge, boolean active) {
		if (challenge == null) return null;
		challenge.setActive(active);
		saveChallenge(challenge);
		return challenge;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Challenge updateChallengeTarget(Challenge challenge, long newTarget) {
		if (challenge == null || newTarget <= 0) return challenge;
		challenge.setTargetValue(newTarget);
		saveChallenge(challenge);
		return challenge;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Challenge updateChallengeEndDate(Challenge challenge, LocalDateTime newEndDate) {
		if (challenge == null || newEndDate == null) return challenge;
		challenge.setEndDate(newEndDate);
		saveChallenge(challenge);
		return challenge;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Challenge setParticipantProgress(Challenge challenge, String userId, long progress) {
		try {
			if (challenge == null || userId == null || userId.isBlank() || progress < 0) return challenge;
			progressRepository.upsert(challenge.getId(), userId, progress);
			challenge.addParticipant(userId);
			// Синхронизируем в-памяти для текущей сессии
			challenge.getParticipantProgress().put(userId, progress);
			// Пересчитываем общий прогресс
			var allProgress = loadProgress(challenge);
			long totalProgress = allProgress.values().stream().mapToLong(Long::longValue).sum();
			challenge.setCurrentValue(totalProgress);
			saveChallenge(challenge);
			return challenge;
		} catch (Exception e) {
			logger.error("Ошибка при установке прогресса: userId={}, challenge={}", userId,
							challenge != null ? challenge.getName() : "null", e);
			return challenge;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Challenge removeParticipant(Challenge challenge, String userId) {
		try {
			if (challenge == null || userId == null || userId.isBlank()) return challenge;
			progressRepository.delete(challenge.getId(), userId);
			challenge.removeParticipant(userId);
			challenge.getParticipantProgress().remove(userId);
			long totalProgress = loadProgress(challenge).values().stream().mapToLong(Long::longValue).sum();
			challenge.setCurrentValue(totalProgress);
			saveChallenge(challenge);
			return challenge;
		} catch (Exception e) {
			logger.error("Ошибка при удалении участника: userId={}, challenge={}", userId,
							challenge != null ? challenge.getName() : "null", e);
			return challenge;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Challenge addParticipantWithUsername(Challenge challenge, String userId, String username) {
		try {
			if (challenge == null || userId == null || userId.isBlank()) return challenge;
			participantService.registerForChallenge(userId,
							username != null ? username : userId, challenge.getName());
			challenge.addParticipant(userId);
			if (!challenge.getParticipantProgress().containsKey(userId)) {
				progressRepository.upsert(challenge.getId(), userId, 0L);
				challenge.getParticipantProgress().put(userId, 0L);
			}
			saveChallenge(challenge);
			return challenge;
		} catch (Exception e) {
			logger.error("Ошибка при добавлении участника: userId={}, challenge={}", userId,
							challenge != null ? challenge.getName() : "null", e);
			return challenge;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Challenge addParticipant(Challenge challenge, String userId) {
		return addParticipantWithUsername(challenge, userId, userId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Map.Entry<String, Long>> getTopParticipants(Challenge challenge, int limit) {
		try {
			if (challenge == null || limit <= 0) return new ArrayList<>();
			var progress = loadProgress(challenge);
			return progress.entrySet().stream()
							.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
							.limit(limit)
							.collect(Collectors.toList());
		} catch (Exception e) {
			logger.error("Ошибка при получении топ участников: {}", challenge != null ? challenge.getName() : "null", e);
			return new ArrayList<>();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void completeChallenge(Challenge challenge) {
		if (challenge == null) return;
		challenge.setActive(false);
		saveChallenge(challenge);
		logger.info("Испытание '{}' завершено", challenge.getName());
	}
}
