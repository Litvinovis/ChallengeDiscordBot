package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VisualizationService: verifies that chart bytes are generated
 * correctly and that the output is a valid PNG.
 */
class VisualizationServiceTest {

    // PNG magic bytes: 0x89 0x50 0x4E 0x47 (decimal 137, 80, 78, 71)
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};

    private VisualizationService visualizationService;
    private ChallengeStats stats;

    @BeforeEach
    void setUp() {
        visualizationService = new VisualizationService();
        stats = new ChallengeStats("Pushups", 10000L, 2500L, 7500L, 25.0, 750.0, 10);
    }

    // ---------- generateProgressChart ----------

    @Test
    void generateProgressChart_returnsNonEmptyBytes() {
        byte[] result = visualizationService.generateProgressChart(stats);
        assertNotNull(result);
        assertTrue(result.length > 0, "Result must not be empty");
    }

    @Test
    void generateProgressChart_returnsPngBytes() {
        byte[] result = visualizationService.generateProgressChart(stats);
        assertNotNull(result);
        assertTrue(result.length >= PNG_MAGIC.length);
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            assertEquals(PNG_MAGIC[i], result[i], "Byte " + i + " must match PNG magic bytes");
        }
    }

    @Test
    void generateProgressChart_withZeroValues_returnsNonEmptyBytes() {
        ChallengeStats zeroStats = new ChallengeStats("Empty", 0L, 0L, 0L, 0.0, 0.0, 0);
        byte[] result = visualizationService.generateProgressChart(zeroStats);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateProgressChart_withGoalReached_returnsNonEmptyBytes() {
        ChallengeStats fullStats = new ChallengeStats("Done", 100L, 100L, 0L, 100.0, 0.0, 0);
        byte[] result = visualizationService.generateProgressChart(fullStats);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    // ---------- generatePercentageChart ----------

    @Test
    void generatePercentageChart_returnsNonEmptyBytes() {
        byte[] result = visualizationService.generatePercentageChart(stats);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generatePercentageChart_returnsPngBytes() {
        byte[] result = visualizationService.generatePercentageChart(stats);
        assertNotNull(result);
        assertTrue(result.length >= PNG_MAGIC.length);
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            assertEquals(PNG_MAGIC[i], result[i], "Byte " + i + " must match PNG magic bytes");
        }
    }

    @Test
    void generatePercentageChart_at100Percent_returnsNonEmptyBytes() {
        ChallengeStats fullStats = new ChallengeStats("Done", 100L, 100L, 0L, 100.0, 0.0, 0);
        byte[] result = visualizationService.generatePercentageChart(fullStats);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generatePercentageChart_at0Percent_returnsNonEmptyBytes() {
        ChallengeStats emptyStats = new ChallengeStats("New", 100L, 0L, 100L, 0.0, 10.0, 10);
        byte[] result = visualizationService.generatePercentageChart(emptyStats);
        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
