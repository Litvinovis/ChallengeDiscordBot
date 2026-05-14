package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.repository.ChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubtractProgressTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ChallengeProgressRepository progressRepository;

    @Mock
    private ParticipantService participantService;

    @InjectMocks
    private ChallengeService challengeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Challenge makeChallenge(long current) {
        Challenge c = new Challenge();
        c.setId("test");
        c.setName("Тест");
        c.setTargetValue(1000L);
        c.setCurrentValue(current);
        c.setActive(true);
        return c;
    }

    @Test
    void subtractProgress_reducesUserProgress() {
        Challenge challenge = makeChallenge(50L);
        Map<String, Long> progress = new HashMap<>();
        progress.put("user1", 50L);
        when(progressRepository.findByChallengeId("test")).thenReturn(progress);

        Challenge result = challengeService.subtractProgress(challenge, "user1", "alice", 20L);

        assertNotNull(result);
        verify(progressRepository).upsert("test", "user1", 30L);
        assertEquals(30L, result.getParticipantProgress().get("user1"));
        assertEquals(30L, result.getCurrentValue());
    }

    @Test
    void subtractProgress_floorAtZero() {
        Challenge challenge = makeChallenge(10L);
        Map<String, Long> progress = new HashMap<>();
        progress.put("user1", 10L);
        when(progressRepository.findByChallengeId("test")).thenReturn(progress);

        Challenge result = challengeService.subtractProgress(challenge, "user1", "alice", 50L);

        assertNotNull(result);
        verify(progressRepository).upsert("test", "user1", 0L);
        assertEquals(0L, result.getParticipantProgress().get("user1"));
        assertEquals(0L, result.getCurrentValue());
    }

    @Test
    void subtractProgress_nullChallenge_returnsNull() {
        Challenge result = challengeService.subtractProgress(null, "user1", "alice", 10L);
        assertNull(result);
        verifyNoInteractions(progressRepository);
    }

    @Test
    void subtractProgress_zeroAmount_doesNothing() {
        Challenge challenge = makeChallenge(50L);

        Challenge result = challengeService.subtractProgress(challenge, "user1", "alice", 0L);

        assertNotNull(result);
        verifyNoInteractions(progressRepository);
    }
}
