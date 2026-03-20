package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Participant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Расширенные тесты для StreakService:
 * проверяет streak-уведомления при достижении 3, 7, 30 дней.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StreakServiceExtendedTest {

    @Mock
    private IUserService userService;

    @Mock
    private IDataStorageService dataStorageService;

    @Mock
    private IDiscordService discordService;

    @Mock
    private DiscordConfig discordConfig;

    @InjectMocks
    private StreakService streakService;

    @BeforeEach
    void setUp() {
        when(discordConfig.getReportChannel()).thenReturn("качал-очка");
    }

    // ---- Базовое поведение серии ----

    @Test
    void testFirstActivitySetsStreakTo1() {
        Participant participant = new Participant("user1", "TestUser");
        when(userService.getParticipant("user1")).thenReturn(participant);

        streakService.recordActivity("user1");

        assertEquals(1, participant.getCurrentStreak());
    }

    @Test
    void testConsecutiveDayIncrementsStreak() {
        Participant participant = new Participant("user1", "TestUser");
        participant.setCurrentStreak(2);
        participant.setLastActivityDate(LocalDate.now().minusDays(1));
        when(userService.getParticipant("user1")).thenReturn(participant);

        streakService.recordActivity("user1");

        assertEquals(3, participant.getCurrentStreak());
    }

    @Test
    void testSameDayDoesNotChangeStreak() {
        Participant participant = new Participant("user1", "TestUser");
        participant.setCurrentStreak(5);
        participant.setLastActivityDate(LocalDate.now());
        when(userService.getParticipant("user1")).thenReturn(participant);

        streakService.recordActivity("user1");

        assertEquals(5, participant.getCurrentStreak());
    }

    @Test
    void testMissedDayResetsStreakTo1() {
        Participant participant = new Participant("user1", "TestUser");
        participant.setCurrentStreak(10);
        participant.setLastActivityDate(LocalDate.now().minusDays(3));
        when(userService.getParticipant("user1")).thenReturn(participant);

        streakService.recordActivity("user1");

        assertEquals(1, participant.getCurrentStreak());
    }

    // ---- Уведомления при достижении порогов ----

    @Test
    void testNotificationAt3Days() {
        Participant participant = new Participant("user1", "TestUser");
        participant.setCurrentStreak(2);
        participant.setLastActivityDate(LocalDate.now().minusDays(1));
        when(userService.getParticipant("user1")).thenReturn(participant);

        streakService.recordActivity("user1");

        assertEquals(3, participant.getCurrentStreak());
        verify(discordService).sendMessageToChannel(anyString(), contains("3-дневная серия"));
    }

    @Test
    void testNotificationAt7Days() {
        Participant participant = new Participant("user1", "TestUser");
        participant.setCurrentStreak(6);
        participant.setLastActivityDate(LocalDate.now().minusDays(1));
        when(userService.getParticipant("user1")).thenReturn(participant);

        streakService.recordActivity("user1");

        assertEquals(7, participant.getCurrentStreak());
        verify(discordService).sendMessageToChannel(anyString(), contains("Неделя без пропуска"));
    }

    @Test
    void testNotificationAt30Days() {
        Participant participant = new Participant("user1", "TestUser");
        participant.setCurrentStreak(29);
        participant.setLastActivityDate(LocalDate.now().minusDays(1));
        when(userService.getParticipant("user1")).thenReturn(participant);

        streakService.recordActivity("user1");

        assertEquals(30, participant.getCurrentStreak());
        verify(discordService).sendMessageToChannel(anyString(), contains("Легенда"));
    }

    @Test
    void testNoNotificationAt5Days() {
        Participant participant = new Participant("user1", "TestUser");
        participant.setCurrentStreak(4);
        participant.setLastActivityDate(LocalDate.now().minusDays(1));
        when(userService.getParticipant("user1")).thenReturn(participant);

        streakService.recordActivity("user1");

        assertEquals(5, participant.getCurrentStreak());
        verify(discordService, never()).sendMessageToChannel(anyString(), anyString());
    }

    @Test
    void testNoNotificationOnFirstActivity() {
        Participant participant = new Participant("user1", "TestUser");
        when(userService.getParticipant("user1")).thenReturn(participant);

        streakService.recordActivity("user1");

        // Серия 1 — нет уведомления
        verify(discordService, never()).sendMessageToChannel(anyString(), anyString());
    }

    @Test
    void testNotificationUsesReportChannel() {
        Participant participant = new Participant("user1", "TestUser");
        participant.setCurrentStreak(2);
        participant.setLastActivityDate(LocalDate.now().minusDays(1));
        when(userService.getParticipant("user1")).thenReturn(participant);

        streakService.recordActivity("user1");

        verify(discordService).sendMessageToChannel(eq("качал-очка"), anyString());
    }

    // ---- Мультипликаторы ----

    @Test
    void testMultiplierBelow7() {
        assertEquals(1.0, streakService.getStreakMultiplier(3), 0.001);
    }

    @Test
    void testMultiplierAt7() {
        assertEquals(1.1, streakService.getStreakMultiplier(7), 0.001);
    }

    @Test
    void testMultiplierAt30() {
        assertEquals(1.2, streakService.getStreakMultiplier(30), 0.001);
    }

    // ---- Прочее ----

    @Test
    void testNullUserDoesNotThrow() {
        assertDoesNotThrow(() -> streakService.recordActivity(null));
    }

    @Test
    void testEmptyUserDoesNotThrow() {
        assertDoesNotThrow(() -> streakService.recordActivity(""));
    }

    @Test
    void testGetCurrentStreakForUnknownUser() {
        when(userService.getParticipant("unknown")).thenReturn(null);
        assertEquals(0, streakService.getCurrentStreak("unknown"));
    }
}
