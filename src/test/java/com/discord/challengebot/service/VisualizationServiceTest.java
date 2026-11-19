package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Map;
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

	@Test
	void generateActivityCalendar_placesDaysInWeeklyColumnsAndColorsByTotal() throws Exception {
		LocalDate monday = LocalDate.of(2025, 12, 29);
		LocalDate start = monday.plusDays(1);
		LocalDate end = monday.plusDays(7);
		Map<LocalDate, Long> totals = Map.of(
				monday, Long.MAX_VALUE,
				start, 4L,
				monday.plusDays(2), -3L,
				end, 20L,
				end.plusDays(1), Long.MAX_VALUE);

		byte[] png = visualizationService.generateActivityCalendar(start, end, totals).get();
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
		assertNotNull(image);
		assertEquals(0x131C28, image.getRGB(59, 89) & 0xFFFFFF);
		assertEquals(0x135348, image.getRGB(59, 107) & 0xFFFFFF);
		assertEquals(0x263242, image.getRGB(59, 125) & 0xFFFFFF);
		assertEquals(0x263242, image.getRGB(59, 143) & 0xFFFFFF);
		assertEquals(0x5CDBA3, image.getRGB(77, 89) & 0xFFFFFF);
		assertEquals(0x131C28, image.getRGB(77, 107) & 0xFFFFFF);
	}

	@Test
	void generateActivityCalendar_withoutHistoryRendersEmptyDays() throws Exception {
		LocalDate end = LocalDate.of(2026, 11, 19);
		byte[] png = visualizationService.generateActivityCalendar(end.minusDays(364), end, Map.of()).get();
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));

		assertNotNull(image);
		assertEquals(0x263242, image.getRGB(77, 89) & 0xFFFFFF);
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
