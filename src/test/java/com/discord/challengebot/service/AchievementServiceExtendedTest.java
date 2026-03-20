package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Расширенные тесты для AchievementService:
 * проверяет бейджи, пороги 100/500/1000/5000, и разрешение канала.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AchievementServiceExtendedTest {

    @Mock
    private IDiscordService discordService;

    @Mock
    private IChallengeService challengeService;

    @Mock
    private DiscordConfig discordConfig;

    @InjectMocks
    private AchievementService achievementService;

    private Challenge challenge;

    @BeforeEach
    void setUp() {
        challenge = new Challenge();
        challenge.setId("test_challenge");
        challenge.setName("Test Challenge");
        challenge.setTargetValue(10000);
        challenge.setCurrentValue(0);
        challenge.setType(ChallengeType.GROUP);
        challenge.setStartDate(LocalDateTime.now().minusDays(10));
        challenge.setEndDate(LocalDateTime.now().plusDays(30));
        challenge.setUnit("раз");

        when(discordConfig.getReportChannel()).thenReturn("качал-очка");
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
        // Должны выдаться бейджи 100 и 500
        verify(discordService, times(2)).sendMessageToChannel(anyString(), anyString());
    }

    @Test
    void testAchievementAt1000() {
        when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 1000);
        // Должны выдаться бейджи 100, 500 и 1000
        verify(discordService, times(3)).sendMessageToChannel(anyString(), anyString());
    }

    @Test
    void testAchievementAt5000() {
        when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 5000);
        // Должны выдаться бейджи 100, 500, 1000 и 5000
        verify(discordService, times(4)).sendMessageToChannel(anyString(), anyString());
    }

    @Test
    void testAchievementAt5000_containsMilestone() {
        when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 5000);
        // Проверяем что последнее сообщение содержит 5000
        verify(discordService, atLeastOnce()).sendMessageToChannel(anyString(), contains("5000"));
    }

    @Test
    void testAchievementNotAwardedTwice() {
        when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 100);
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 150);
        // Только одно объявление для рубежа 100
        verify(discordService, times(1)).sendMessageToChannel(anyString(), anyString());
    }

    @Test
    void testHasAchievementReturnsFalseInitially() {
        assertFalse(achievementService.hasAchievement("user1", "test_challenge", "100_reps"));
    }

    @Test
    void testHasAchievementReturnsTrueAfterAwarding() {
        when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 100);
        assertTrue(achievementService.hasAchievement("user1", "test_challenge", "100_reps"));
    }

    @Test
    void testNullUserDoesNotThrow() {
        assertDoesNotThrow(() ->
                achievementService.checkAndAwardAchievements(null, "test_challenge", 500));
    }

    @Test
    void testNullChallengeIdDoesNotThrow() {
        assertDoesNotThrow(() ->
                achievementService.checkAndAwardAchievements("user1", null, 500));
    }

    @Test
    void testGetUserAchievementsEmptyInitially() {
        assertTrue(achievementService.getUserAchievements("newUser").isEmpty());
    }

    @Test
    void testAnnouncementSentToReportChannel() {
        when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 100);
        verify(discordService).sendMessageToChannel(eq("качал-очка"), anyString());
    }

    @Test
    void testFourDistinctAchievementsDefinedInService() {
        assertEquals(4, AchievementService.ACHIEVEMENTS.size());
    }

    @Test
    void testAchievementThresholds() {
        int[] expectedThresholds = {100, 500, 1000, 5000};
        for (int i = 0; i < expectedThresholds.length; i++) {
            assertEquals(expectedThresholds[i], AchievementService.ACHIEVEMENTS.get(i).getThreshold());
        }
    }
}
