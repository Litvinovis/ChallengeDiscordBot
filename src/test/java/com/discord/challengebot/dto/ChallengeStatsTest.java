package com.discord.challengebot.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ChallengeStats DTO: constructor correctness and field semantics.
 */
class ChallengeStatsTest {

    @Test
    void defaultConstructor_createsObjectWithDefaultValues() {
        ChallengeStats stats = new ChallengeStats();
        assertNotNull(stats);
        assertNull(stats.getChallengeName());
        assertEquals(0L, stats.getTargetValue());
        assertEquals(0L, stats.getCurrentValue());
        assertEquals(0L, stats.getRemaining());
        assertEquals(0.0, stats.getPercentage(), 0.0001);
        assertEquals(0.0, stats.getDailyTarget(), 0.0001);
        assertEquals(0, stats.getDaysRemaining());
    }

    @Test
    void parameterizedConstructor_setsAllFields() {
        ChallengeStats stats = new ChallengeStats("Pushups", 10000L, 2500L, 7500L, 25.0, 750.0, 10);

        assertEquals("Pushups", stats.getChallengeName());
        assertEquals(10000L, stats.getTargetValue());
        assertEquals(2500L, stats.getCurrentValue());
        assertEquals(7500L, stats.getRemaining());
        assertEquals(25.0, stats.getPercentage(), 0.0001);
        assertEquals(750.0, stats.getDailyTarget(), 0.0001);
        assertEquals(10, stats.getDaysRemaining());
    }

    @Test
    void percentage_isConsistentWithCurrentAndTarget() {
        ChallengeStats stats = new ChallengeStats("Test", 1000L, 500L, 500L, 50.0, 100.0, 5);
        assertEquals(50.0, stats.getPercentage(), 0.0001);
    }

    @Test
    void remaining_isTargetMinusCurrent() {
        ChallengeStats stats = new ChallengeStats("Test", 1000L, 300L, 700L, 30.0, 140.0, 5);
        assertEquals(700L, stats.getRemaining());
    }

    @Test
    void stats_withZeroTarget_percentageIsZero() {
        ChallengeStats stats = new ChallengeStats("Empty", 0L, 0L, 0L, 0.0, 0.0, 0);
        assertEquals(0.0, stats.getPercentage(), 0.0001);
    }

    @Test
    void stats_withGoalExceeded_remainingIsNegative() {
        // Edge case: currentValue > targetValue (overshoot)
        ChallengeStats stats = new ChallengeStats("Overdone", 100L, 120L, -20L, 120.0, 0.0, 0);
        assertEquals(-20L, stats.getRemaining());
        assertEquals(120.0, stats.getPercentage(), 0.0001);
    }

    @Test
    void stats_withNoDaysRemaining_dailyTargetIsZero() {
        ChallengeStats stats = new ChallengeStats("Done", 1000L, 800L, 200L, 80.0, 0.0, 0);
        assertEquals(0.0, stats.getDailyTarget(), 0.0001);
        assertEquals(0, stats.getDaysRemaining());
    }
}
