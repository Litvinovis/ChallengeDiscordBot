package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

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
	void generateProgressChart_returnsNonEmptyBytes() throws Exception {
		CompletableFuture<byte[]> future = visualizationService.generateProgressChart(stats);
		byte[] result = future.get();
		assertNotNull(result);
		assertTrue(result.length > 0, "Result must not be empty");
	}

	@Test
	void generateProgressChart_returnsPngBytes() throws Exception {
		CompletableFuture<byte[]> future = visualizationService.generateProgressChart(stats);
		byte[] result = future.get();
		assertNotNull(result);
		assertTrue(result.length >= PNG_MAGIC.length);
		for (int i = 0; i < PNG_MAGIC.length; i++) {
			assertEquals(PNG_MAGIC[i], result[i], "Byte " + i + " must match PNG magic bytes");
		}
	}

	@Test
	void generateProgressChart_withZeroValues_returnsNonEmptyBytes() throws Exception {
		ChallengeStats zeroStats = new ChallengeStats("Empty", 0L, 0L, 0L, 0.0, 0.0, 0);
		CompletableFuture<byte[]> future = visualizationService.generateProgressChart(zeroStats);
		byte[] result = future.get();
		assertNotNull(result);
		assertTrue(result.length > 0);
	}

	@Test
	void generateProgressChart_withGoalReached_returnsNonEmptyBytes() throws Exception {
		ChallengeStats fullStats = new ChallengeStats("Done", 100L, 100L, 0L, 100.0, 0.0, 0);
		CompletableFuture<byte[]> future = visualizationService.generateProgressChart(fullStats);
		byte[] result = future.get();
		assertNotNull(result);
		assertTrue(result.length > 0);
	}

	// ---------- generatePercentageChart ----------

	@Test
	void generatePercentageChart_returnsNonEmptyBytes() throws Exception {
		CompletableFuture<byte[]> future = visualizationService.generatePercentageChart(stats);
		byte[] result = future.get();
		assertNotNull(result);
		assertTrue(result.length > 0);
	}

	@Test
	void generatePercentageChart_returnsPngBytes() throws Exception {
		CompletableFuture<byte[]> future = visualizationService.generatePercentageChart(stats);
		byte[] result = future.get();
		assertNotNull(result);
		assertTrue(result.length >= PNG_MAGIC.length);
		for (int i = 0; i < PNG_MAGIC.length; i++) {
			assertEquals(PNG_MAGIC[i], result[i], "Byte " + i + " must match PNG magic bytes");
		}
	}

	@Test
	void generatePercentageChart_at100Percent_returnsNonEmptyBytes() throws Exception {
		ChallengeStats fullStats = new ChallengeStats("Done", 100L, 100L, 0L, 100.0, 0.0, 0);
		CompletableFuture<byte[]> future = visualizationService.generatePercentageChart(fullStats);
		byte[] result = future.get();
		assertNotNull(result);
		assertTrue(result.length > 0);
	}

	@Test
	void generatePercentageChart_at0Percent_returnsNonEmptyBytes() throws Exception {
		ChallengeStats emptyStats = new ChallengeStats("New", 100L, 0L, 100L, 0.0, 10.0, 10);
		CompletableFuture<byte[]> future = visualizationService.generatePercentageChart(emptyStats);
		byte[] result = future.get();
		assertNotNull(result);
		assertTrue(result.length > 0);
	}
}
