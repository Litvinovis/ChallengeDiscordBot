package com.discord.challengebot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandArgumentsTest {

	@Test
	void splitsBySpacesWhenNoQuotes() {
		assertArrayEquals(new String[]{"новый", "бег", "1000"},
						CommandArguments.split("новый бег 1000"));
	}

	@Test
	void keepsQuotedNameTogether() {
		assertArrayEquals(new String[]{"новый", "бег утром", "1000"},
						CommandArguments.split("новый \"бег утром\" 1000"));
	}

	@Test
	void supportsRussianQuotes() {
		assertArrayEquals(new String[]{"топ", "бег утром"},
						CommandArguments.split("топ «бег утром»"));
	}

	@Test
	void supportsSmartQuotes() {
		assertArrayEquals(new String[]{"топ", "бег утром"},
						CommandArguments.split("топ “бег утром”"));
	}

	@Test
	void unclosedQuoteRunsToEndOfLine() {
		assertArrayEquals(new String[]{"топ", "бег утром"},
						CommandArguments.split("топ \"бег утром"));
	}

	@Test
	void collapsesRepeatedSpaces() {
		assertArrayEquals(new String[]{"топ", "бег"},
						CommandArguments.split("топ    бег"));
	}

	@Test
	void returnsEmptyArrayForBlankInput() {
		assertEquals(0, CommandArguments.split("   ").length);
		assertEquals(0, CommandArguments.split("").length);
		assertEquals(0, CommandArguments.split(null).length);
	}
}
