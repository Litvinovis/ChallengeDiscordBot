package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ProgressHistoryRepository;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class StatisticsService implements IStatisticsService {
	private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);

	private static final int MAX_CACHE_SIZE = 10000;
	private static final int MAX_HISTORY_PER_KEY = 365;
	private final Map<String, List<Long>> progressHistoryCache = new ConcurrentHashMap<>();

	private final DiscordService discordService;
	private final ParticipantService participantService;
	private final ProgressHistoryRepository progressHistoryRepository;

	public StatisticsService(@Lazy DiscordService discordService,
	                         @Lazy ParticipantService participantService,
	                         @Lazy ProgressHistoryRepository progressHistoryRepository) {
		this.discordService = discordService;
		this.participantService = participantService;
		this.progressHistoryRepository = progressHistoryRepository;
	}

	/** For tests only — progressHistoryRepository will be null (guarded). */
	public StatisticsService(@Lazy DiscordService discordService,
	                         @Lazy ParticipantService participantService) {
		this(discordService, participantService, null);
	}

	@Override
	public ChallengeStats calculateStats(Challenge challenge) {
		if (challenge == null) return null;
		try {
			long remaining = challenge.getTargetValue() - challenge.getCurrentValue();
			double percentage = challenge.getTargetValue() > 0
					? (double) challenge.getCurrentValue() / challenge.getTargetValue() * 100 : 0;
			LocalDate today = LocalDate.now();
			LocalDate endDate = challenge.getEndDate().toLocalDate();
			long daysRemaining = ChronoUnit.DAYS.between(today, endDate);
			int participantCount = Math.max(challenge.getParticipants().size(), 1);
			double dailyTarget = daysRemaining > 0 ? (double) remaining / participantCount / daysRemaining : 0;
			return new ChallengeStats(challenge.getName(), challenge.getTargetValue(),
					challenge.getCurrentValue(), remaining, percentage, dailyTarget, (int) daysRemaining);
		} catch (Exception e) {
			logger.error("Ошибка при расчете статистики для испытания: {}", challenge.getName(), e);
			return null;
		}
	}

	@Override
	public long calculateRemaining(Challenge challenge) {
		if (challenge == null) return 0;
		return challenge.getTargetValue() - challenge.getCurrentValue();
	}

	@Override
	public double calculateDailyTarget(Challenge challenge) {
		if (challenge == null) return 0;
		try {
			long remaining = calculateRemaining(challenge);
			long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), challenge.getEndDate().toLocalDate());
			if (daysRemaining <= 0) return 0;
			int participantCount = Math.max(challenge.getParticipants().size(), 1);
			return (double) remaining / participantCount / daysRemaining;
		} catch (Exception e) {
			logger.error("Ошибка при расчете ежедневной цели для испытания: {}", challenge.getName(), e);
			return 0;
		}
	}

	@Override
	public double calculatePercentage(Challenge challenge) {
		if (challenge == null) return 0;
		return challenge.getTargetValue() > 0
				? (double) challenge.getCurrentValue() / challenge.getTargetValue() * 100 : 0;
	}

	@Override
	public String generateProgressReport(Challenge challenge) {
		if (challenge == null) return "";
		try {
			ChallengeStats stats = calculateStats(challenge);
			return formatReportForDiscord(challenge, stats);
		} catch (Exception e) {
			logger.error("Ошибка при генерации отчета о прогрессе для испытания: {}", challenge.getName(), e);
			return "";
		}
	}

	@Override
	public List<Map.Entry<String, Long>> generateLeaderboard(Challenge challenge, int limit) {
		if (challenge == null || limit <= 0) return new java.util.ArrayList<>();
		try {
			return challenge.getParticipantProgress().entrySet().stream()
					.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
					.limit(limit)
					.collect(Collectors.toList());
		} catch (Exception e) {
			logger.error("Ошибка при генерации таблицы лидеров для испытания: {}", challenge.getName(), e);
			return new java.util.ArrayList<>();
		}
	}

	@Override
	public String formatReportForDiscord(Challenge challenge, ChallengeStats stats) {
		if (challenge == null || stats == null) return "";
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("**Статистика по испытанию: ").append(stats.challengeName()).append("**\n");
			sb.append("Цель: ").append(stats.targetValue()).append("\n");
			sb.append("Выполнено: ").append(stats.currentValue()).append("\n");
			sb.append("Осталось: ").append(stats.remaining()).append("\n");
			sb.append("Процент выполнения: ").append(String.format(Locale.forLanguageTag("ru"), "%.2f", stats.percentage())).append("%\n");
			sb.append("Ежедневная цель: ").append(String.format(Locale.forLanguageTag("ru"), "%.2f", stats.dailyTarget())).append(" в день\n");
			sb.append("Дней осталось: ").append(stats.daysRemaining()).append("\n");
			sb.append("Зарегистрировано участников: ").append(challenge.getParticipants().size()).append("\n");

			List<Map.Entry<String, Long>> topParticipants = challenge.getParticipantProgress().entrySet().stream()
					.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
					.limit(3)
					.toList();

			if (!topParticipants.isEmpty()) {
				sb.append("\n**Топ-3 участников:**\n");
				for (int i = 0; i < topParticipants.size(); i++) {
					Map.Entry<String, Long> entry = topParticipants.get(i);
					int streak = getParticipantStreak(entry.getKey());
					String streakSuffix = streak > 1 ? " 🔥 " + streak + " дн." : "";
					sb.append((i + 1)).append(". ").append(resolveUsername(entry.getKey()))
							.append(" - ").append(entry.getValue()).append(" ").append(challenge.getUnit())
							.append(streakSuffix).append("\n");
				}
			}

			// Streak leader among all participants
			appendStreakLeader(sb, challenge);

			// Weekly comparison
			appendWeeklyComparison(sb, challenge);

			// Best day
			appendBestDay(sb, challenge);

			return sb.toString();
		} catch (Exception e) {
			logger.error("Ошибка при форматировании отчета для Discord", e);
			return "";
		}
	}

	@Override
	public String formatLeaderboardForDiscord(Challenge challenge, List<Map.Entry<String, Long>> leaderboard) {
		if (challenge == null || leaderboard == null) return "";
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("**Топ участников по испытанию: ").append(challenge.getName()).append("**\n");
			if (leaderboard.isEmpty()) {
				sb.append("Пока нет участников.\n");
			} else {
				for (int i = 0; i < leaderboard.size(); i++) {
					Map.Entry<String, Long> entry = leaderboard.get(i);
					sb.append((i + 1)).append(". ").append(resolveUsername(entry.getKey()))
							.append(" - ").append(entry.getValue()).append(" ").append(challenge.getUnit()).append("\n");
				}
			}
			return sb.toString();
		} catch (Exception e) {
			logger.error("Ошибка при форматировании таблицы лидеров для Discord по испытанию: {}", challenge.getName(), e);
			return "";
		}
	}

	@Override
	public String formatChallengeStats(Challenge challenge, ChallengeStats stats) {
		return formatReportForDiscord(challenge, stats);
	}

	@Override
	public String formatDailyReportForDiscord(Challenge challenge, ChallengeStats stats,
	                                          List<Map.Entry<String, Long>> topParticipants) {
		if (challenge == null || stats == null) return "";
		try {
			String unit = challenge.getUnit() != null ? challenge.getUnit() : "";
			String typeLabel = challenge.getType() == ChallengeType.GROUP ? "👥 Групповое" : "👤 Личное";
			int participantCount = challenge.getParticipants().size();

			int pct = (int) Math.clamp(stats.percentage(), 0, 100);
			int filled = pct * 15 / 100;
			String bar = "█".repeat(filled) + "░".repeat(15 - filled);

			String endDate = challenge.getEndDate() != null
					? challenge.getEndDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "—";

			StringBuilder sb = new StringBuilder();
			sb.append("──────────────────────────────\n");
			sb.append("**").append(challenge.getName()).append("**\n");
			sb.append(typeLabel).append("  ·  ").append(participantCount).append(" уч.")
					.append("  ·  до **").append(endDate).append("**  ·  **")
					.append(stats.daysRemaining()).append(" дн.**\n\n");

			sb.append("📊 **").append(stats.currentValue()).append("** / ")
					.append(stats.targetValue()).append(" ").append(unit)
					.append("  —  **").append(String.format("%.0f%%", stats.percentage())).append("**\n");
			sb.append("`").append(bar).append("`\n");

			if (stats.remaining() <= 0) {
				sb.append("✅ **Цель достигнута!**\n");
			} else if (stats.daysRemaining() > 0) {
				sb.append("⏳ Осталось: **").append(stats.remaining()).append(" ").append(unit).append("**");
				if (stats.dailyTarget() > 0) {
					sb.append("  ·  норма **~").append(Math.round(stats.dailyTarget()))
							.append(" ").append(unit).append("/чел/день**");
				}
				sb.append("\n");
			} else {
				sb.append("⌛ Срок истёк\n");
			}

			if (topParticipants != null && !topParticipants.isEmpty()) {
				sb.append("\n🏆 **Топ-3:**\n");
				String[] medals = {"🥇", "🥈", "🥉"};
				for (int i = 0; i < topParticipants.size(); i++) {
					var entry = topParticipants.get(i);
					String medal = i < medals.length ? medals[i] : (i + 1) + ".";
					sb.append(medal).append(" ").append(resolveUsername(entry.getKey()))
							.append(" — ").append(entry.getValue()).append(" ").append(unit).append("\n");
				}
			}
			return sb.toString();
		} catch (Exception e) {
			logger.error("Ошибка при форматировании ежедневного отчёта для испытания: {}", challenge.getName(), e);
			return "";
		}
	}

	@Override
	public LocalDate forecastCompletionDate(String challengeId, String userId) {
		try {
			if (challengeId == null || userId == null) return null;
			String key = challengeId + ":" + userId;
			List<Long> history = progressHistoryCache.get(key);
			if (history == null || history.isEmpty()) return null;
			int windowSize = Math.min(7, history.size());
			List<Long> window = history.subList(history.size() - windowSize, history.size());
			double avgPerDay = window.stream().mapToLong(Long::longValue).average().orElse(0);
			if (avgPerDay <= 0) return null;
			return LocalDate.now().plusDays((long) Math.ceil(1 / avgPerDay));
		} catch (Exception e) {
			logger.error("Ошибка при прогнозировании даты завершения", e);
			return null;
		}
	}

	public LocalDate forecastCompletionDate(Challenge challenge, String userId) {
		try {
			if (challenge == null || userId == null) return null;
			long userProgress = challenge.getParticipantProgress().getOrDefault(userId, 0L);
			long remaining = challenge.getTargetValue() - userProgress;
			if (remaining <= 0) return LocalDate.now();

			String key = challenge.getId() + ":" + userId;
			List<Long> history = progressHistoryCache.get(key);
			double avgPerDay;
			if (history != null && !history.isEmpty()) {
				int windowSize = Math.min(7, history.size());
				List<Long> window = history.subList(history.size() - windowSize, history.size());
				avgPerDay = window.stream().mapToLong(Long::longValue).average().orElse(0);
			} else {
				LocalDate start = challenge.getStartDate() != null
						? challenge.getStartDate().toLocalDate() : LocalDate.now();
				long daysSinceStart = ChronoUnit.DAYS.between(start, LocalDate.now());
				if (daysSinceStart <= 0) return null;
				avgPerDay = (double) userProgress / daysSinceStart;
			}
			if (avgPerDay <= 0) return null;
			long daysNeeded = (long) Math.ceil((double) remaining / avgPerDay);
			return LocalDate.now().plusDays(daysNeeded);
		} catch (Exception e) {
			logger.error("Ошибка при прогнозировании даты завершения", e);
			return null;
		}
	}

	public void recordDailyProgress(String challengeId, String userId, long progressAmount) {
		recordDailyProgress(challengeId, userId, null, progressAmount);
	}

	public void recordDailyProgress(String challengeId, String userId, String username, long progressAmount) {
		try {
			if (challengeId == null || userId == null) return;
			// Persist to DB
			if (progressHistoryRepository != null) {
				try {
					progressHistoryRepository.insert(challengeId, userId, username, progressAmount);
				} catch (Exception ex) {
					logger.warn("Не удалось записать историю прогресса в БД: {}", ex.getMessage());
				}
			}
			// Keep in-memory cache for forecast
			if (progressHistoryCache.size() >= MAX_CACHE_SIZE) {
				String firstKey = progressHistoryCache.keySet().iterator().next();
				progressHistoryCache.remove(firstKey);
			}
			String key = challengeId + ":" + userId;
			List<Long> history = progressHistoryCache.computeIfAbsent(key, k -> new java.util.ArrayList<>());
			history.add(progressAmount);
			if (history.size() > MAX_HISTORY_PER_KEY) {
				history.removeFirst();
			}
		} catch (Exception e) {
			logger.error("Ошибка при записи ежедневного прогресса", e);
		}
	}

	private int getParticipantStreak(String userId) {
		try {
			Participant participant = participantService.getParticipant(userId);
			return participant != null ? participant.getCurrentStreak() : 0;
		} catch (Exception e) {
			logger.debug("Не удалось получить серию для участника {}: {}", userId, e.getMessage());
			return 0;
		}
	}

	private void appendStreakLeader(StringBuilder sb, Challenge challenge) {
		try {
			String leaderId = null;
			int maxStreak = 1;
			for (String userId : challenge.getParticipantProgress().keySet()) {
				int streak = getParticipantStreak(userId);
				if (streak > maxStreak) {
					maxStreak = streak;
					leaderId = userId;
				}
			}
			if (leaderId != null) {
				sb.append("🔥 Стрик-лидер: ").append(resolveUsername(leaderId))
						.append(" — ").append(maxStreak).append(" дней подряд\n");
			}
		} catch (Exception e) {
			logger.debug("Ошибка при определении лидера серии: {}", e.getMessage());
		}
	}

	private void appendWeeklyComparison(StringBuilder sb, Challenge challenge) {
		try {
			if (progressHistoryRepository == null) return;
			LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
			Map<String, Long> thisWeek = progressHistoryRepository.getUserTotalsInRange(
					challenge.getId(), now.minusDays(7), now);
			Map<String, Long> lastWeek = progressHistoryRepository.getUserTotalsInRange(
					challenge.getId(), now.minusDays(14), now.minusDays(7));
			long thisWeekTotal = thisWeek.values().stream().mapToLong(Long::longValue).sum();
			long lastWeekTotal = lastWeek.values().stream().mapToLong(Long::longValue).sum();
			long delta = thisWeekTotal - lastWeekTotal;
			String sign = delta >= 0 ? "+" : "";
			sb.append(String.format("📈 Эта неделя: %d | Прошлая неделя: %d | Δ %s%d\n",
					thisWeekTotal, lastWeekTotal, sign, delta));
		} catch (Exception e) {
			logger.debug("Ошибка при добавлении сравнения по неделям: {}", e.getMessage());
		}
	}

	private void appendBestDay(StringBuilder sb, Challenge challenge) {
		try {
			if (progressHistoryRepository == null) return;
			Map.Entry<LocalDate, Long> best = progressHistoryRepository.getBestDayAll(challenge.getId());
			if (best != null) {
				String dateStr = best.getKey().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
				sb.append("🏆 Лучший день: ").append(dateStr)
						.append(" (").append(best.getValue()).append(" ")
						.append(challenge.getUnit() != null ? challenge.getUnit() : "").append(")\n");
			}
		} catch (Exception e) {
			logger.debug("Ошибка при добавлении лучшего дня: {}", e.getMessage());
		}
	}

	private String resolveUsername(String userId) {
		try {
			Participant participant = participantService.getParticipant(userId);
			if (participant != null && participant.getUsername() != null
					&& !participant.getUsername().isEmpty()) {
				return participant.getUsername();
			}
		} catch (Exception e) {
			logger.debug("Не удалось получить имя участника для ID {}: {}", userId, e.getMessage());
		}
		try {
			if (discordService.getJDA() != null) {
				User user = discordService.getJDA().getUserById(userId);
				if (user == null) {
					user = discordService.getJDA().retrieveUserById(userId).complete();
				}
				if (user != null) return user.getName();
			}
		} catch (Exception e) {
			logger.debug("Не удалось получить имя Discord для ID {}: {}", userId, e.getMessage());
		}
		return userId;
	}
}
