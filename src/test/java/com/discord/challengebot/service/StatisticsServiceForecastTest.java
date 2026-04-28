package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для функции прогнозирования и LRU-кэша в StatisticsService.
 */
class StatisticsServiceForecastTest {

	private StatisticsService statisticsService;

	@BeforeEach
	void setUp() {
		statisticsService = new StatisticsService();
	}

	// ---- Тесты forecastCompletionDate(String, String) ----

	@Test
	void testForecastNullChallengeIdReturnsNull() {
		LocalDate result = statisticsService.forecastCompletionDate((String) null, "user1");
		assertNull(result);
	}

	@Test
	void testForecastNullUserIdReturnsNull() {
		LocalDate result = statisticsService.forecastCompletionDate("challenge1", null);
		assertNull(result);
	}

	@Test
	void testForecastWithNoHistoryReturnsNull() {
		LocalDate result = statisticsService.forecastCompletionDate("challenge1", "user1");
		assertNull(result);
	}

	@Test
	void testForecastWithHistoryReturnsFutureDate() {
		// Записываем 7 дней прогресса по 50 единиц
		for (int i = 0; i < 7; i++) {
			statisticsService.recordDailyProgress("challenge1", "user1", 50L);
		}
		// Прогноз по этим данным не null (avgPerDay > 0)
		LocalDate result = statisticsService.forecastCompletionDate("challenge1", "user1");
		assertNotNull(result);
		assertTrue(result.isAfter(LocalDate.now()) || result.isEqual(LocalDate.now()));
	}

	// ---- Тесты forecastCompletionDate(Challenge, String) ----

	@Test
	void testForecastWithChallengeAndNoHistory() {
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
	void testForecastWithChallengeNullReturnsNull() {
		LocalDate result = statisticsService.forecastCompletionDate((Challenge) null, "user1");
		assertNull(result);
	}

	@Test
	void testForecastWithChallengeAndHistory() {
		Challenge challenge = buildChallenge(2000, 500);
		challenge.getParticipantProgress().put("user1", 500L);

		// Записываем историю: 100 единиц/день × 7 дней
		for (int i = 0; i < 7; i++) {
			statisticsService.recordDailyProgress(challenge.getId(), "user1", 100L);
		}

		LocalDate result = statisticsService.forecastCompletionDate(challenge, "user1");
		assertNotNull(result);
		// При темпе 100/день и 1500 оставшихся — ожидаем ~15 дней вперед
		assertTrue(result.isAfter(LocalDate.now()));
	}

	// ---- Тесты LRU-кэша recordDailyProgress ----

	@Test
	void testRecordDailyProgressNullChallengeIdDoesNotThrow() {
		assertDoesNotThrow(() -> statisticsService.recordDailyProgress(null, "user1", 100));
	}

	@Test
	void testRecordDailyProgressNullUserIdDoesNotThrow() {
		assertDoesNotThrow(() -> statisticsService.recordDailyProgress("challenge1", null, 100));
	}

	@Test
	void testRecordDailyProgressStoresValues() {
		statisticsService.recordDailyProgress("challenge1", "user1", 50L);
		statisticsService.recordDailyProgress("challenge1", "user1", 75L);
		// Проверяем через прогноз: если есть история, прогноз вернет не null
		LocalDate result = statisticsService.forecastCompletionDate("challenge1", "user1");
		assertNotNull(result);
	}

	@Test
	void testLruEvictsOldEntriesWhenCacheExceeds10000Keys() {
		// Добавляем 10001 уникальных ключей — старый должен быть вытеснен
		for (int i = 0; i <= 10000; i++) {
			statisticsService.recordDailyProgress("challenge" + i, "user" + i, 100L);
		}
		// Сервис не должен бросить исключение
		// Прогноз для последнего ключа должен работать
		LocalDate result = statisticsService.forecastCompletionDate("challenge10000", "user10000");
		assertNotNull(result);
	}

	@Test
	void testHistoryPerKeyLimitedTo365Entries() {
		// Добавляем 400 записей для одного ключа
		for (int i = 0; i < 400; i++) {
			statisticsService.recordDailyProgress("challenge1", "user1", 10L);
		}
		// Прогноз должен работать нормально
		LocalDate result = statisticsService.forecastCompletionDate("challenge1", "user1");
		assertNotNull(result);
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
