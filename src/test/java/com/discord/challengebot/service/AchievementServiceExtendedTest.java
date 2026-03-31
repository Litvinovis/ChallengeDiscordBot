package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.model.Participant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
    private IDataStorageService dataStorageService;

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
            assertEquals(expectedThresholds[i], AchievementService.ACHIEVEMENTS.get(i).threshold());
        }
    }

    /**
     * Регрессионный тест на повторную выдачу достижений после перезапуска бота.
     *
     * Сценарий: пользователь уже получил достижение "100_reps" (данные сохранены
     * в Participant в Apache Ignite). После перезапуска бота in-memory кэш пуст,
     * но dataStorageService возвращает Participant с уже выданным достижением.
     * При повторном вызове checkAndAwardAchievements достижение НЕ должно
     * выдаваться снова.
     */
    @Test
    void testAlreadyPersistedAchievementIsNotReawardedAfterRestart() {
        // Имитируем состояние после перезапуска: in-memory кэш пуст,
        // но в Participant уже записано достижение "100_reps".
        Participant participant = new Participant("user1", "TestUser");
        Set<String> alreadyAwarded = new HashSet<>();
        alreadyAwarded.add("user1:test_challenge:100_reps");
        participant.setAwardedAchievements(alreadyAwarded);

        when(dataStorageService.getParticipant("user1")).thenReturn(participant);
        when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);

        // Пользователь добавляет упражнения — прогресс 150 (выше порога 100)
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 150);

        // Достижение "100_reps" уже было выдано — сообщение НЕ должно отправляться
        verify(discordService, never()).sendMessageToChannel(anyString(), anyString());
    }

    /**
     * Проверяет, что при повторном вызове в той же сессии (in-memory кэш заполнен)
     * достижение также не выдаётся дважды.
     */
    @Test
    void testAlreadyAwardedInSessionNotReawarded() {
        when(dataStorageService.getParticipant("user1")).thenReturn(null);
        when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);

        // Первый вызов — достижение выдаётся
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 100);
        // Второй вызов с тем же или большим прогрессом — достижение НЕ должно выдаваться снова
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 100);
        achievementService.checkAndAwardAchievements("user1", "test_challenge", 120);

        // Только одно сообщение за всё время
        verify(discordService, times(1)).sendMessageToChannel(anyString(), contains("100"));
    }
}
