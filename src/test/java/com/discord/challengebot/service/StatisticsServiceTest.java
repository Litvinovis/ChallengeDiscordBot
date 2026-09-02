package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsServiceTest {

	@InjectMocks
	private StatisticsService statisticsService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testCalculateStats() {
		Challenge challenge = new Challenge();
		challenge.setName("Отжимания");
		challenge.setTargetValue(10000);
		challenge.setCurrentValue(2500);
		challenge.setEndDate(LocalDateTime.now().plusDays(10));

		ChallengeStats stats = statisticsService.calculateStats(challenge);

		assertNotNull(stats);
		assertEquals("Отжимания", stats.challengeName());
		assertEquals(10000L, stats.targetValue());
		assertEquals(2500L, stats.currentValue());
		assertEquals(7500L, stats.remaining());
	}






	@Test
	void testFormatReportForDiscord() {
		Challenge challenge = new Challenge();
		challenge.setName("Отжимания");
		challenge.setUnit("раз");

		// Add participants
		challenge.addParticipant("user1");
		challenge.addParticipant("user2");
		challenge.addParticipant("user3");

		// Add progress for participants
		challenge.getParticipantProgress().put("user1", 1000L);
		challenge.getParticipantProgress().put("user2", 800L);
		challenge.getParticipantProgress().put("user3", 600L);

		ChallengeStats stats = new ChallengeStats("Отжимания", 10000L, 2500L, 7500L, 25.0, 750.0, 10);

		String formatted = statisticsService.formatReportForDiscord(challenge, stats);

		// Print the actual output for debugging
		System.out.println("Formatted output: " + formatted);

		assertTrue(formatted.contains("**Статистика по испытанию: Отжимания**"), "Should contain title");
		assertTrue(formatted.contains("Цель: 10000"), "Should contain target value");
		assertTrue(formatted.contains("Выполнено: 2500"), "Should contain current value");
		assertTrue(formatted.contains("Осталось: 7500"), "Should contain remaining value");
		// Use comma as decimal separator as shown in the output
		assertTrue(formatted.contains("Процент выполнения: 25,00%"), "Should contain percentage");
		assertTrue(formatted.contains("Ежедневная цель: 750,00 в день"), "Should contain daily target");
		assertTrue(formatted.contains("Дней осталось: 10"), "Should contain days remaining");
		assertTrue(formatted.contains("Зарегистрировано участников: 3"), "Should contain participant count");
		assertTrue(formatted.contains("**Топ-3 участников:**"), "Should contain top participants header");
		// Now we're using usernames instead of user IDs
		assertTrue(formatted.contains("1. user1 - 1000 раз"), "Should contain top participant 1");
		assertTrue(formatted.contains("2. user2 - 800 раз"), "Should contain top participant 2");
		assertTrue(formatted.contains("3. user3 - 600 раз"), "Should contain top participant 3");
	}

	@Test
	void testFormatReportForDiscordWithoutParticipants() {
		Challenge challenge = new Challenge();
		challenge.setName("Отжимания");
		challenge.setUnit("раз");

		ChallengeStats stats = new ChallengeStats("Отжимания", 10000L, 2500L, 7500L, 25.0, 750.0, 10);

		String formatted = statisticsService.formatReportForDiscord(challenge, stats);

		// Print the actual output for debugging
		System.out.println("Formatted output: " + formatted);

		assertTrue(formatted.contains("**Статистика по испытанию: Отжимания**"), "Should contain title");
		assertTrue(formatted.contains("Цель: 10000"), "Should contain target value");
		assertTrue(formatted.contains("Выполнено: 2500"), "Should contain current value");
		assertTrue(formatted.contains("Осталось: 7500"), "Should contain remaining value");
		// Use comma as decimal separator as shown in the output
		assertTrue(formatted.contains("Процент выполнения: 25,00%"), "Should contain percentage");
		assertTrue(formatted.contains("Ежедневная цель: 750,00 в день"), "Should contain daily target");
		assertTrue(formatted.contains("Дней осталось: 10"), "Should contain days remaining");
		assertTrue(formatted.contains("Зарегистрировано участников: 0"), "Should contain participant count");
		assertFalse(formatted.contains("**Топ-3 участников:**"), "Should not contain top participants header when no participants");
	}
}