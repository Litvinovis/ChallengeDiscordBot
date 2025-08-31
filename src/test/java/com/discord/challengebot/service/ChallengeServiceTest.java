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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChallengeServiceTest {

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
    }

    @Test
    void testAddProgress() {
        String userId = "12345";
        String username = "testuser";
        long amount = 10;
        
        when(mockChallenge.getCurrentValue()).thenReturn(100L);
        java.util.Map<String, Long> participantProgress = new java.util.HashMap<>();
        when(mockChallenge.getParticipantProgress()).thenReturn(participantProgress);

        Challenge updatedChallenge = challengeService.addProgress(mockChallenge, userId, username, amount);

        assertNotNull(updatedChallenge);
        verify(mockChallenge).setCurrentValue(110L);
        assertEquals(10L, participantProgress.get(userId));
    }

    @Test
    void testGetChallengeStats() {
        when(mockChallenge.getName()).thenReturn("Отжимания");
        when(mockChallenge.getTargetValue()).thenReturn(10000L);
        when(mockChallenge.getCurrentValue()).thenReturn(2500L);

        ChallengeStats stats = challengeService.getChallengeStats(mockChallenge);

        assertNotNull(stats);
        assertEquals("Отжимания", stats.getChallengeName());
        assertEquals(10000L, stats.getTargetValue());
        assertEquals(2500L, stats.getCurrentValue());
        assertEquals(7500L, stats.getRemaining());
        assertEquals(25.0, stats.getPercentage(), 0.01);
    }
}