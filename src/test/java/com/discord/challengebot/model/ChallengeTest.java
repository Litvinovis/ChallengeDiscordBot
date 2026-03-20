package com.discord.challengebot.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Challenge model business logic: participant management,
 * thread-safe progress map, and edge cases.
 */
class ChallengeTest {

    private Challenge challenge;

    @BeforeEach
    void setUp() {
        challenge = new Challenge("pushups", "Pushups", 10000L,
                ChallengeType.GROUP,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                "Do 10k pushups", "reps");
    }

    // ---------- constructor ----------

    @Test
    void defaultConstructor_initializesCollections() {
        Challenge c = new Challenge();
        assertNotNull(c.getParticipants(), "participants list must not be null");
        assertNotNull(c.getParticipantProgress(), "participantProgress map must not be null");
        assertTrue(c.getParticipantProgress() instanceof ConcurrentHashMap,
                "participantProgress must be ConcurrentHashMap for thread safety");
    }

    @Test
    void parameterizedConstructor_setsFields() {
        assertEquals("pushups", challenge.getId());
        assertEquals("Pushups", challenge.getName());
        assertEquals(10000L, challenge.getTargetValue());
        assertEquals(0L, challenge.getCurrentValue());
        assertEquals(ChallengeType.GROUP, challenge.getType());
        assertTrue(challenge.isActive());
        assertEquals("Do 10k pushups", challenge.getDescription());
        assertEquals("reps", challenge.getUnit());
    }

    // ---------- addParticipant ----------

    @Test
    void addParticipant_addsUserSuccessfully() {
        challenge.addParticipant("user1");
        assertTrue(challenge.hasParticipant("user1"));
        assertEquals(1, challenge.getParticipants().size());
    }

    @Test
    void addParticipant_noDuplicates_whenAddedTwice() {
        challenge.addParticipant("user1");
        challenge.addParticipant("user1");
        assertEquals(1, challenge.getParticipants().size());
    }

    @Test
    void addParticipant_nullId_doesNothing() {
        challenge.addParticipant(null);
        assertEquals(0, challenge.getParticipants().size());
    }

    @Test
    void addParticipant_emptyId_doesNothing() {
        challenge.addParticipant("");
        assertEquals(0, challenge.getParticipants().size());
    }

    @Test
    void addParticipant_multipleUsers_allPresent() {
        challenge.addParticipant("alice");
        challenge.addParticipant("bob");
        challenge.addParticipant("carol");
        assertEquals(3, challenge.getParticipants().size());
        assertTrue(challenge.hasParticipant("alice"));
        assertTrue(challenge.hasParticipant("bob"));
        assertTrue(challenge.hasParticipant("carol"));
    }

    // ---------- removeParticipant ----------

    @Test
    void removeParticipant_existingUser_removesSuccessfully() {
        challenge.addParticipant("user1");
        challenge.removeParticipant("user1");
        assertFalse(challenge.hasParticipant("user1"));
        assertEquals(0, challenge.getParticipants().size());
    }

    @Test
    void removeParticipant_nonExistingUser_doesNotThrow() {
        assertDoesNotThrow(() -> challenge.removeParticipant("ghost"));
    }

    @Test
    void removeParticipant_nullId_doesNothing() {
        challenge.addParticipant("user1");
        challenge.removeParticipant(null);
        assertTrue(challenge.hasParticipant("user1"), "Existing user must still be present");
    }

    @Test
    void removeParticipant_emptyId_doesNothing() {
        challenge.addParticipant("user1");
        challenge.removeParticipant("");
        assertTrue(challenge.hasParticipant("user1"), "Existing user must still be present");
    }

    // ---------- hasParticipant ----------

    @Test
    void hasParticipant_absentUser_returnsFalse() {
        assertFalse(challenge.hasParticipant("nobody"));
    }

    @Test
    void hasParticipant_nullId_returnsFalse() {
        assertFalse(challenge.hasParticipant(null));
    }

    @Test
    void hasParticipant_emptyId_returnsFalse() {
        assertFalse(challenge.hasParticipant(""));
    }

    // ---------- participantProgress map ----------

    @Test
    void participantProgressMap_isConcurrentHashMap() {
        assertTrue(challenge.getParticipantProgress() instanceof ConcurrentHashMap,
                "Must be ConcurrentHashMap to prevent race conditions (Bug fix #3)");
    }

    @Test
    void participantProgressMap_accumulatesValues() {
        challenge.getParticipantProgress().put("user1", 100L);
        challenge.getParticipantProgress().merge("user1", 50L, Long::sum);
        assertEquals(150L, challenge.getParticipantProgress().get("user1"));
    }

    // ---------- readResolve (deserialization) ----------

    @Test
    void readResolve_rebuildsConcurrentHashMapFromHashMap() throws Exception {
        // Simulate a plain HashMap being set (as from old serialized data)
        java.util.Map<String, Long> plain = new java.util.HashMap<>();
        plain.put("user1", 42L);
        challenge.setParticipantProgress(plain);

        // Invoke readResolve via reflection
        java.lang.reflect.Method readResolve = Challenge.class.getDeclaredMethod("readResolve");
        readResolve.setAccessible(true);
        readResolve.invoke(challenge);

        assertTrue(challenge.getParticipantProgress() instanceof ConcurrentHashMap,
                "readResolve must upgrade HashMap to ConcurrentHashMap");
        assertEquals(42L, challenge.getParticipantProgress().get("user1"));
    }
}
