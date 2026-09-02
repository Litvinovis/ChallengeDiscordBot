package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ProgressHistoryRepository;
import com.discord.challengebot.util.TimeZones;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsService implements IStatisticsService {
	private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);

	private final DiscordService discordService;
	private final ParticipantService participantService;
	private final ProgressHistoryRepository progressHistoryRepository;

	@Autowired
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
			LocalDate today = LocalDate.now(TimeZones.MOSCOW);
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
	public LocalDate forecastCompletionDate(Challenge challenge, String userId) {
		try {
			if (challenge == null || userId == null) return null;
			long userProgress = challenge.getParticipantProgress().getOrDefault(userId, 0L);
			long remaining = challenge.getTargetValue() - userProgress;
			LocalDate today = LocalDate.now(TimeZones.MOSCOW);
			if (remaining <= 0) return today;

			// Средний дневной темп за последние 7 дней из истории прогресса (переживает рестарты)
			double avgPerDay = 0;
			if (progressHistoryRepository != null) {
				Map<LocalDate, Long> daily = progressHistoryRepository.getDailyTotals(
						challenge.getId(), userId, 7);
				long weekTotal = daily.values().stream().mapToLong(Long::longValue).sum();
				avgPerDay = weekTotal / 7.0;
			}
			if (avgPerDay <= 0) {
				// Fallback: общий средний темп с начала испытания
				LocalDate start = challenge.getStartDate() != null
						? challenge.getStartDate().toLocalDate() : today;
				long daysSinceStart = ChronoUnit.DAYS.between(start, today);
				if (daysSinceStart <= 0) return null;
				avgPerDay = (double) userProgress / daysSinceStart;
			}
			if (avgPerDay <= 0) return null;
			long daysNeeded = (long) Math.ceil((double) remaining / avgPerDay);
			return today.plusDays(daysNeeded);
		} catch (Exception e) {
			logger.error("Ошибка при прогнозировании даты завершения", e);
			return null;
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
			LocalDateTime now = LocalDateTime.now(TimeZones.MOSCOW);
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
					// Блокирующий REST-запрос ограничиваем таймаутом, чтобы не подвесить поток отчёта
					user = discordService.getJDA().retrieveUserById(userId)
							.timeout(5, java.util.concurrent.TimeUnit.SECONDS).complete();
				}
				if (user != null) return user.getName();
			}
		} catch (Exception e) {
			logger.debug("Не удалось получить имя Discord для ID {}: {}", userId, e.getMessage());
		}
		return userId;
	}
}
