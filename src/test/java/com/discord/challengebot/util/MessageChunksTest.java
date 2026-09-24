package com.discord.challengebot.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageChunksTest {

	@Test
	void shortText_singleChunk() {
		assertEquals(List.of("привет"), MessageChunks.split("привет", 2000));
	}

	@Test
	void emptyOrNull_noChunks() {
		assertTrue(MessageChunks.split("", 10).isEmpty());
		assertTrue(MessageChunks.split(null, 10).isEmpty());
	}

	@Test
	void splitsOnNewlines_withinLimit() {
		List<String> chunks = MessageChunks.split("aaaa\nbbbb\ncccc", 10);

		assertEquals(List.of("aaaa\nbbbb", "cccc"), chunks);
	}

	@Test
	void longLineWithoutNewlines_hardCut() {
		List<String> chunks = MessageChunks.split("x".repeat(25), 10);

		assertEquals(3, chunks.size());
		assertTrue(chunks.stream().allMatch(c -> c.length() <= 10));
		assertEquals("x".repeat(25), String.join("", chunks));
	}

	@Test
	void realisticReport_allChunksFitDiscordLimit() {
		String report = ("**Статистика по испытанию: Отжимания**\nЦель: 10000\n").repeat(100);

		List<String> chunks = MessageChunks.split(report, MessageChunks.DISCORD_LIMIT);

		assertTrue(chunks.size() > 1);
		assertTrue(chunks.stream().allMatch(c -> c.length() <= MessageChunks.DISCORD_LIMIT));
	}
}
