package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
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

    @Test
    void calculateRemaining_nullChallenge_returnsZero() {
        assertEquals(0L, statisticsService.calculateRemaining(null));
    }

    @Test
    void calculateRemaining_goalExceeded_returnsNegative() {
        Challenge challenge = new Challenge();
        challenge.setTargetValue(100L);
        challenge.setCurrentValue(150L);

        long remaining = statisticsService.calculateRemaining(challenge);
        assertEquals(-50L, remaining);
    }

    // ---- calculateDailyTarget null/expired guards ----

    @Test
    void calculateDailyTarget_nullChallenge_returnsZero() {
        assertEquals(0.0, statisticsService.calculateDailyTarget(null), 0.0001);
    }

    @Test
    void calculateDailyTarget_expiredChallenge_returnsZero() {
        Challenge challenge = new Challenge();
        challenge.setTargetValue(10000L);
        challenge.setCurrentValue(2500L);
        challenge.setEndDate(LocalDateTime.now().minusDays(1)); // past

        double dailyTarget = statisticsService.calculateDailyTarget(challenge);
        assertEquals(0.0, dailyTarget, 0.0001);
    }

    // ---- calculatePercentage null guard ----

    @Test
    void calculatePercentage_nullChallenge_returnsZero() {
        assertEquals(0.0, statisticsService.calculatePercentage(null), 0.0001);
    }

    @Test
    void calculatePercentage_goalReached_returns100() {
        Challenge challenge = new Challenge();
        challenge.setTargetValue(1000L);
        challenge.setCurrentValue(1000L);

        assertEquals(100.0, statisticsService.calculatePercentage(challenge), 0.0001);
    }

    @Test
    void calculatePercentage_goalExceeded_returnsMoreThan100() {
        Challenge challenge = new Challenge();
        challenge.setTargetValue(100L);
        challenge.setCurrentValue(110L);

        assertTrue(statisticsService.calculatePercentage(challenge) > 100.0);
    }

    // ---- generateLeaderboard ----

    @Test
    void generateLeaderboard_sortsByValueDescending() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        challenge.getParticipantProgress().put("alice", 500L);
        challenge.getParticipantProgress().put("bob", 1500L);
        challenge.getParticipantProgress().put("carol", 1000L);

        List<Map.Entry<String, Long>> leaderboard = statisticsService.generateLeaderboard(challenge, 3);

        assertEquals(3, leaderboard.size());
        assertEquals("bob", leaderboard.get(0).getKey());
        assertEquals("carol", leaderboard.get(1).getKey());
        assertEquals("alice", leaderboard.get(2).getKey());
    }

    @Test
    void generateLeaderboard_respectsLimit() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        challenge.getParticipantProgress().put("alice", 100L);
        challenge.getParticipantProgress().put("bob", 200L);
        challenge.getParticipantProgress().put("carol", 300L);

        List<Map.Entry<String, Long>> leaderboard = statisticsService.generateLeaderboard(challenge, 2);
        assertEquals(2, leaderboard.size());
    }

    @Test
    void generateLeaderboard_nullChallenge_returnsEmpty() {
        assertTrue(statisticsService.generateLeaderboard(null, 5).isEmpty());
    }

    @Test
    void generateLeaderboard_invalidLimit_returnsEmpty() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        assertTrue(statisticsService.generateLeaderboard(challenge, 0).isEmpty());
        assertTrue(statisticsService.generateLeaderboard(challenge, -1).isEmpty());
    }

    @Test
    void generateLeaderboard_emptyProgress_returnsEmpty() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");

        List<Map.Entry<String, Long>> leaderboard = statisticsService.generateLeaderboard(challenge, 5);
        assertTrue(leaderboard.isEmpty());
    }

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

    @Test
    void generateProgressReport_nullChallenge_returnsEmpty() {
        assertEquals("", statisticsService.generateProgressReport(null));
    }

    @Test
    void generateProgressReport_validChallenge_returnsNonEmpty() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        challenge.setTargetValue(1000L);
        challenge.setCurrentValue(250L);
        challenge.setUnit("reps");
        challenge.setEndDate(LocalDateTime.now().plusDays(10));

        String report = statisticsService.generateProgressReport(challenge);
        assertFalse(report.isEmpty());
        assertTrue(report.contains("Pushups"));
    }

    // ---- formatReportForDiscord (deprecated single-arg) ----

    @Test
    void formatReportForDiscord_deprecated_nullStats_returnsEmpty() {
        assertEquals("", statisticsService.formatReportForDiscord((ChallengeStats) null));
    }

    @Test
    void formatReportForDiscord_deprecated_validStats_containsKeyFields() {
        ChallengeStats stats = new ChallengeStats("Squats", 500L, 100L, 400L, 20.0, 40.0, 10);

        String result = statisticsService.formatReportForDiscord(stats);

        assertTrue(result.contains("Squats"));
        assertTrue(result.contains("500"));
        assertTrue(result.contains("100"));
        assertTrue(result.contains("400"));
    }

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
