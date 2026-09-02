package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended tests for StatisticsService: null/edge cases, leaderboard,
 * expired challenge logic, report formatting.
 */
class StatisticsServiceExtendedTest {

	@InjectMocks
	private StatisticsService statisticsService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	// ---- calculateStats null guard ----

	@Test
	void calculateStats_nullChallenge_returnsNull() {
		assertNull(statisticsService.calculateStats(null));
	}

	@Test
	void calculateStats_zeroTarget_percentageIsZero() {
		Challenge challenge = new Challenge();
		challenge.setName("Test");
		challenge.setTargetValue(0L);
		challenge.setCurrentValue(0L);
		challenge.setEndDate(LocalDateTime.now().plusDays(5));

		ChallengeStats stats = statisticsService.calculateStats(challenge);
		assertNotNull(stats);
		assertEquals(0.0, stats.percentage(), 0.0001);
	}

	// ---- calculateRemaining null guard ----



	// ---- calculateDailyTarget null/expired guards ----



	// ---- calculatePercentage null guard ----




	// ---- generateLeaderboard ----






	// ---- formatLeaderboardForDiscord ----

	@Test
	void formatLeaderboardForDiscord_containsChallengeNameAndRanks() {
		Challenge challenge = new Challenge();
		challenge.setName("Pushups");
		challenge.setUnit("reps");

		List<Map.Entry<String, Long>> leaderboard = List.of(
						Map.entry("alice", 1000L),
						Map.entry("bob", 800L)
		);

		String result = statisticsService.formatLeaderboardForDiscord(challenge, leaderboard);

		assertTrue(result.contains("Pushups"), "Must contain challenge name");
		assertTrue(result.contains("1."), "Must contain rank 1");
		assertTrue(result.contains("2."), "Must contain rank 2");
		assertTrue(result.contains("1000"), "Must contain alice's score");
		assertTrue(result.contains("800"), "Must contain bob's score");
	}

	@Test
	void formatLeaderboardForDiscord_emptyLeaderboard_showsNoParticipantsMessage() {
		Challenge challenge = new Challenge();
		challenge.setName("Pushups");
		challenge.setUnit("reps");

		String result = statisticsService.formatLeaderboardForDiscord(challenge, List.of());

		assertTrue(result.contains("Пока нет участников"),
						"Must indicate no participants when leaderboard is empty");
	}

	@Test
	void formatLeaderboardForDiscord_nullChallenge_returnsEmpty() {
		String result = statisticsService.formatLeaderboardForDiscord(null, List.of());
		assertEquals("", result);
	}

	@Test
	void formatLeaderboardForDiscord_nullLeaderboard_returnsEmpty() {
		Challenge challenge = new Challenge();
		challenge.setName("Pushups");
		String result = statisticsService.formatLeaderboardForDiscord(challenge, null);
		assertEquals("", result);
	}

	// ---- generateProgressReport ----



	// ---- formatDailyReportForDiscord ----

	@Test
	void formatDailyReport_nullChallenge_returnsEmpty() {
		ChallengeStats stats = new ChallengeStats("X", 100L, 50L, 50L, 50.0, 5.0, 10);
		assertEquals("", statisticsService.formatDailyReportForDiscord(null, stats, List.of()));
	}

	@Test
	void formatDailyReport_nullStats_returnsEmpty() {
		Challenge ch = new Challenge();
		ch.setName("X");
		assertEquals("", statisticsService.formatDailyReportForDiscord(ch, null, List.of()));
	}

	@Test
	void formatDailyReport_containsNameAndProgressBar() {
		Challenge ch = new Challenge();
		ch.setName("Отжимания");
		ch.setUnit("раз");
		ch.setType(com.discord.challengebot.model.ChallengeType.GROUP);
		ch.setEndDate(LocalDateTime.now().plusDays(10));

		ChallengeStats stats = new ChallengeStats("Отжимания", 10000L, 5000L, 5000L, 50.0, 500.0, 10);

		String result = statisticsService.formatDailyReportForDiscord(ch, stats, List.of());

		assertTrue(result.contains("Отжимания"), "Should contain challenge name");
		assertTrue(result.contains("5000"), "Should contain current value");
		assertTrue(result.contains("10000"), "Should contain target value");
		assertTrue(result.contains("50%"), "Should contain percentage");
		assertTrue(result.contains("█"), "Should contain filled progress bar blocks");
		assertTrue(result.contains("░"), "Should contain empty progress bar blocks");
	}

	@Test
	void formatDailyReport_withTopParticipants_containsMedals() {
		Challenge ch = new Challenge();
		ch.setName("Бег");
		ch.setUnit("км");
		ch.setType(com.discord.challengebot.model.ChallengeType.INDIVIDUAL);
		ch.setEndDate(LocalDateTime.now().plusDays(5));

		ChallengeStats stats = new ChallengeStats("Бег", 100L, 60L, 40L, 60.0, 8.0, 5);

		List<Map.Entry<String, Long>> top = List.of(
						Map.entry("alice", 35L),
						Map.entry("bob", 20L),
						Map.entry("carol", 5L)
		);

		String result = statisticsService.formatDailyReportForDiscord(ch, stats, top);

		assertTrue(result.contains("🥇"), "Should contain gold medal");
		assertTrue(result.contains("🥈"), "Should contain silver medal");
		assertTrue(result.contains("🥉"), "Should contain bronze medal");
		assertTrue(result.contains("alice"));
		assertTrue(result.contains("35 км"));
	}

	@Test
	void formatDailyReport_goalReached_showsCompletedMessage() {
		Challenge ch = new Challenge();
		ch.setName("Цель");
		ch.setUnit("раз");
		ch.setType(com.discord.challengebot.model.ChallengeType.INDIVIDUAL);
		ch.setEndDate(LocalDateTime.now().plusDays(5));

		ChallengeStats stats = new ChallengeStats("Цель", 100L, 120L, -20L, 120.0, 0.0, 5);

		String result = statisticsService.formatDailyReportForDiscord(ch, stats, List.of());

		assertTrue(result.contains("✅"), "Should show completion emoji");
	}

	@Test
	void formatDailyReport_progressBar_fullAt100Percent() {
		Challenge ch = new Challenge();
		ch.setName("Финиш");
		ch.setUnit("раз");
		ch.setType(com.discord.challengebot.model.ChallengeType.GROUP);
		ch.setEndDate(LocalDateTime.now().plusDays(1));

		ChallengeStats stats = new ChallengeStats("Финиш", 100L, 100L, 0L, 100.0, 0.0, 1);

		String result = statisticsService.formatDailyReportForDiscord(ch, stats, List.of());

		assertTrue(result.contains("███████████████"), "Progress bar should be fully filled at 100%");
		assertFalse(result.contains("░"), "Progress bar should have no empty blocks at 100%");
	}
}
