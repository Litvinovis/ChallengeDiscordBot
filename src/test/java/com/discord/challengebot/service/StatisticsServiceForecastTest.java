package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.repository.ProgressHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Тесты для функции прогнозирования в StatisticsService.
 */
class StatisticsServiceForecastTest {

	@Mock private DiscordService discordService;
	@Mock private ParticipantService participantService;
	@Mock private ProgressHistoryRepository progressHistoryRepository;

	private StatisticsService statisticsService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		statisticsService = new StatisticsService(discordService, participantService, progressHistoryRepository);
	}

	@Test
	void testForecastWithChallengeNullReturnsNull() {
		LocalDate result = statisticsService.forecastCompletionDate(null, "user1");
		assertNull(result);
	}

	@Test
	void testForecastNullUserIdReturnsNull() {
		LocalDate result = statisticsService.forecastCompletionDate(buildChallenge(1000, 300), null);
		assertNull(result);
	}

	@Test
	void testForecastWithChallengeAndNoHistory() {
		when(progressHistoryRepository.getDailyTotals(anyString(), anyString(), anyInt()))
				.thenReturn(Map.of());
		Challenge challenge = buildChallenge(1000, 300);
		// Устанавливаем прогресс участника, чтобы avgPerDay > 0
		challenge.getParticipantProgress().put("user1", 300L);
		LocalDate result = statisticsService.forecastCompletionDate(challenge, "user1");
		// Нет истории, но есть прогресс участника — используется общий средний темп
		// avgPerDay = 300 / 10 = 30, remaining = 700, daysNeeded = ceil(700/30) = 24
		assertNotNull(result);
		assertTrue(result.isAfter(LocalDate.now()));
	}

	@Test
	void testForecastWithChallengeAlreadyCompleted() {
		Challenge challenge = buildChallenge(1000, 1000);
		challenge.getParticipantProgress().put("user1", 1000L);
		LocalDate result = statisticsService.forecastCompletionDate(challenge, "user1");
		assertEquals(LocalDate.now(), result);
	}

	@Test
	void testForecastWithChallengeAndHistory() {
		Challenge challenge = buildChallenge(2000, 500);
		challenge.getParticipantProgress().put("user1", 500L);

		// История: 100 единиц/день за последние 7 дней
		LocalDate today = LocalDate.now();
		Map<LocalDate, Long> daily = Map.of(
				today.minusDays(1), 100L,
				today.minusDays(2), 100L,
				today.minusDays(3), 100L,
				today.minusDays(4), 100L,
				today.minusDays(5), 100L,
				today.minusDays(6), 100L,
				today.minusDays(7), 100L);
		when(progressHistoryRepository.getDailyTotals(challenge.getId(), "user1", 7))
				.thenReturn(daily);

		LocalDate result = statisticsService.forecastCompletionDate(challenge, "user1");
		assertNotNull(result);
		// При темпе 700/7=100 в день и 1500 оставшихся — ожидаем ~15 дней вперёд
		assertEquals(today.plusDays(15), result);
	}

	@Test
	void testForecastWithNoProgressAtAllReturnsNull() {
		when(progressHistoryRepository.getDailyTotals(anyString(), anyString(), anyInt()))
				.thenReturn(Map.of());
		Challenge challenge = buildChallenge(1000, 0);
		LocalDate result = statisticsService.forecastCompletionDate(challenge, "user1");
		// Ни истории, ни накопленного прогресса — прогноз невозможен
		assertNull(result);
	}

	@Test
	void testForecastWithNullRepositoryUsesFallback() {
		// Тестовый конструктор без репозитория — используется общий средний темп
		StatisticsService withoutRepo = new StatisticsService(discordService, participantService);
		Challenge challenge = buildChallenge(1000, 300);
		challenge.getParticipantProgress().put("user1", 300L);
		LocalDate result = withoutRepo.forecastCompletionDate(challenge, "user1");
		assertNotNull(result);
		assertTrue(result.isAfter(LocalDate.now()));
	}

	// ---- Вспомогательные методы ----

	private Challenge buildChallenge(long target, long current) {
		Challenge challenge = new Challenge();
		challenge.setId("test_challenge");
		challenge.setName("Test Challenge");
		challenge.setTargetValue(target);
		challenge.setCurrentValue(current);
		challenge.setType(ChallengeType.GROUP);
		challenge.setStartDate(LocalDateTime.now().minusDays(10));
		challenge.setEndDate(LocalDateTime.now().plusDays(60));
		challenge.setActive(true);
		challenge.setUnit("раз");
		challenge.setParticipantProgress(new ConcurrentHashMap<>());
		return challenge;
	}
}
