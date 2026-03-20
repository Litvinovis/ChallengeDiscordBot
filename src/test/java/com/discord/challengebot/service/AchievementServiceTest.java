package com.discord.challengebot.service;

import com.discord.challengebot.model.Achievement;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    @Mock
    private IDiscordService discordService;

    @Mock
    private IChallengeService challengeService;

    @InjectMocks
    private AchievementService achievementService;

    private Challenge challenge;

    @BeforeEach
    void setUp() {
        challenge = new Challenge();
        challenge.setId("test_challenge");
        challenge.setName("Test Challenge");
        challenge.setTargetValue(2000);
        challenge.setCurrentValue(0);
        challenge.setType(ChallengeType.GROUP);
        challenge.setStartDate(LocalDateTime.now().minusDays(10));
        challenge.setEndDate(LocalDateTime.now().plusDays(30));
        challenge.setUnit("раз");
    }

    @Test
    void testNoAchievementBelowThreshold() {
        when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 50);
        verify(discordService, never()).sendMessageToChannel(anyString(), anyString());
    }

    @Test
    void testAchievementAt100() {
        when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 100);
        verify(discordService, times(1)).sendMessageToChannel(anyString(), contains("100"));
    }

    @Test
    void testAchievementAt500() {
        when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 500);
        // Should get both 100 and 500
        verify(discordService, times(2)).sendMessageToChannel(anyString(), anyString());
    }

    @Test
    void testAchievementAt1000() {
        when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 1000);
        // Should get 100, 500, and 1000
        verify(discordService, times(3)).sendMessageToChannel(anyString(), anyString());
    }

    @Test
    void testAchievementNotAwardedTwice() {
        when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 100);
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 150);
        // Only one announcement for the 100 milestone
        verify(discordService, times(1)).sendMessageToChannel(anyString(), anyString());
    }

    @Test
    void testHasAchievementReturnsFalseInitially() {
        boolean has = achievementService.hasAchievement("user1", "test_challenge", "100_reps");
        org.junit.jupiter.api.Assertions.assertFalse(has);
    }

    @Test
    void testNullUserDoesNotThrow() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                achievementService.checkAndAwardAchievements(null, "test_challenge", 500));
    }

    @Test
    void testNullChallengeIdDoesNotThrow() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                achievementService.checkAndAwardAchievements("user1", null, 500));
    }
}
