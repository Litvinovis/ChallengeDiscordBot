package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChallengeServiceTest {

    @Mock
    private DataStorageService dataStorageService;

    @Mock
    private UserService userService;

    @Mock
    private Challenge mockChallenge;

    @InjectMocks
    private ChallengeService challengeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateChallenge() {
        String name = "Отжимания";
        long targetValue = 10000;
        LocalDateTime endDate = LocalDateTime.now().plusDays(365);
        ChallengeType type = ChallengeType.GROUP;
        String description = "Испытание по отжиманиям";
        String unit = "раз";

        Challenge challenge = challengeService.createChallenge(name, targetValue, endDate, type, description, unit);

        assertNotNull(challenge);
        assertEquals(name, challenge.getName());
        assertEquals(targetValue, challenge.getTargetValue());
        assertEquals(type, challenge.getType());
        assertEquals(description, challenge.getDescription());
        assertEquals(unit, challenge.getUnit());
        assertTrue(challenge.isActive());
        assertNotNull(challenge.getStartDate());
        assertEquals(endDate, challenge.getEndDate());

        // Verify that saveChallenge was called
        verify(dataStorageService).saveChallenge(any(Challenge.class));
    }

    @Test
    void testAddProgress() {
        String userId = "12345";
        String username = "testuser";
        long amount = 10;

        when(mockChallenge.getCurrentValue()).thenReturn(100L);
        java.util.Map<String, Long> participantProgress = new ConcurrentHashMap<>();
        when(mockChallenge.getParticipantProgress()).thenReturn(participantProgress);
        when(mockChallenge.getName()).thenReturn("Отжимания");

        Challenge updatedChallenge = challengeService.addProgress(mockChallenge, userId, username, amount);

        assertNotNull(updatedChallenge);
        verify(userService).registerForChallenge(userId, username, "Отжимания");
        verify(mockChallenge).setCurrentValue(110L);
        assertEquals(10L, participantProgress.get(userId));
        verify(dataStorageService).saveChallenge(mockChallenge);
    }

    /**
     * Bug fix #2: addProgress must not throw NPE when challenge is null.
     */
    @Test
    void testAddProgressWithNullChallenge() {
        Challenge result = challengeService.addProgress(null, "user1", "username", 10);
        assertNull(result, "addProgress should return null for null challenge without throwing NPE");
    }

    /**
     * Bug fix #2: setParticipantProgress must not throw NPE when challenge is null.
     */
    @Test
    void testSetParticipantProgressWithNullChallenge() {
        Challenge result = challengeService.setParticipantProgress(null, "user1", 10);
        assertNull(result, "setParticipantProgress should return null for null challenge without throwing NPE");
    }

    /**
     * Bug fix #3: Thread safety for concurrent progress updates is ensured via ReentrantLock
     * in the service layer. The model uses HashMap (not ConcurrentHashMap) to avoid
     * Ignite serialization issues under Java 21.
     */
    @Test
    void testChallengeParticipantProgressIsInitialized() {
        Challenge challenge = challengeService.createChallenge(
                "ConcurrencyTest", 1000L, LocalDateTime.now().plusDays(30),
                ChallengeType.GROUP, "desc", "rep");

        assertNotNull(challenge);
        assertNotNull(challenge.getParticipantProgress(),
                "participantProgress must be initialized (thread safety via ReentrantLock in service layer)");
    }

    /**
     * Bug fix #3: concurrent addProgress calls from multiple threads should not lose updates.
     */
    @Test
    void testConcurrentProgressUpdates() throws InterruptedException {
        Challenge challenge = new Challenge();
        challenge.setId("concurrent_test");
        challenge.setName("concurrent_test");
        challenge.setTargetValue(10000L);
        challenge.setCurrentValue(0L);
        challenge.setActive(true);
        challenge.setEndDate(LocalDateTime.now().plusDays(30));
        challenge.setUnit("rep");

        when(userService.registerForChallenge(anyString(), anyString(), anyString())).thenReturn(true);

        int threads = 10;
        int incrementsPerThread = 50;
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final String userId = "user" + i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        challengeService.addProgress(challenge, userId, userId, 1L);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Each user should have exactly incrementsPerThread progress
        for (int i = 0; i < threads; i++) {
            Long progress = challenge.getParticipantProgress().get("user" + i);
            assertNotNull(progress);
            assertEquals(incrementsPerThread, progress,
                    "Each user must have exactly " + incrementsPerThread + " progress (no race condition data loss)");
        }
    }

    /**
     * Bug fix #5: hasParticipant should be O(1) via Set (result correctness check).
     */
    @Test
    void testHasParticipantUsesSetSemantics() {
        Challenge challenge = new Challenge();
        challenge.setId("set_test");
        challenge.setName("set_test");

        assertFalse(challenge.hasParticipant("user1"), "User1 should not be present initially");
        challenge.addParticipant("user1");
        assertTrue(challenge.hasParticipant("user1"), "User1 should be present after addParticipant");

        // Adding twice should not create duplicates
        challenge.addParticipant("user1");
        assertEquals(1, challenge.getParticipants().size(), "Participants list should not contain duplicates");

        challenge.removeParticipant("user1");
        assertFalse(challenge.hasParticipant("user1"), "User1 should be absent after removeParticipant");
    }

    @Test
    void testGetChallengeStats() {
        when(mockChallenge.getName()).thenReturn("Отжимания");
        when(mockChallenge.getTargetValue()).thenReturn(10000L);
        when(mockChallenge.getCurrentValue()).thenReturn(2500L);
        when(mockChallenge.getEndDate()).thenReturn(LocalDateTime.now().plusDays(10));

        ChallengeStats stats = challengeService.getChallengeStats(mockChallenge);

        assertNotNull(stats);
        assertEquals("Отжимания", stats.getChallengeName());
        assertEquals(10000L, stats.getTargetValue());
        assertEquals(2500L, stats.getCurrentValue());
        assertEquals(7500L, stats.getRemaining());
        assertEquals(25.0, stats.getPercentage(), 0.01);
    }
}
