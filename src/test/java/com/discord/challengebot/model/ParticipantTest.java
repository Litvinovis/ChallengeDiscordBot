package com.discord.challengebot.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Participant model business logic: challenge registration
 * and deduplication guards.
 */
class ParticipantTest {

	private Participant participant;

	@BeforeEach
	void setUp() {
		participant = new Participant("user1", "Alice");
	}

	// ---------- constructor ----------

	@Test
	void defaultConstructor_initializesEmptyList() {
		Participant p = new Participant();
		assertNotNull(p.getRegisteredChallenges());
		assertTrue(p.getRegisteredChallenges().isEmpty());
	}

	@Test
	void parameterizedConstructor_setsFields() {
		assertEquals("user1", participant.getUserId());
		assertEquals("Alice", participant.getUsername());
		assertNotNull(participant.getJoinDate(), "joinDate must be set on creation");
		assertNotNull(participant.getRegisteredChallenges());
	}

	// ---------- addChallenge ----------

	@Test
	void addChallenge_addsNewChallenge() {
		participant.addChallenge("Pushups");
		assertTrue(participant.isRegisteredForChallenge("Pushups"));
	}

	@Test
	void addChallenge_doesNotAddDuplicate() {
		participant.addChallenge("Pushups");
		participant.addChallenge("Pushups");
		assertEquals(1, participant.getRegisteredChallenges().size());
	}

	@Test
	void addChallenge_multipleDistinctChallenges() {
		participant.addChallenge("Pushups");
		participant.addChallenge("Squats");
		participant.addChallenge("Running");
		assertEquals(3, participant.getRegisteredChallenges().size());
	}

	// ---------- removeChallenge ----------

	@Test
	void removeChallenge_removesExistingChallenge() {
		participant.addChallenge("Pushups");
		participant.removeChallenge("Pushups");
		assertFalse(participant.isRegisteredForChallenge("Pushups"));
	}

	@Test
	void removeChallenge_nonExistingChallenge_doesNotThrow() {
		assertDoesNotThrow(() -> participant.removeChallenge("NonExistent"));
	}

	@Test
	void removeChallenge_onlyRemovesTargetChallenge() {
		participant.addChallenge("Pushups");
		participant.addChallenge("Squats");
		participant.removeChallenge("Pushups");
		assertFalse(participant.isRegisteredForChallenge("Pushups"));
		assertTrue(participant.isRegisteredForChallenge("Squats"));
	}

	// ---------- isRegisteredForChallenge ----------

	@Test
	void isRegisteredForChallenge_returnsFalse_whenNoneAdded() {
		assertFalse(participant.isRegisteredForChallenge("Pushups"));
	}

	@Test
	void isRegisteredForChallenge_returnsTrueAfterAdd() {
		participant.addChallenge("Pushups");
		assertTrue(participant.isRegisteredForChallenge("Pushups"));
	}

	@Test
	void isRegisteredForChallenge_returnsFalseAfterRemove() {
		participant.addChallenge("Pushups");
		participant.removeChallenge("Pushups");
		assertFalse(participant.isRegisteredForChallenge("Pushups"));
	}
}
