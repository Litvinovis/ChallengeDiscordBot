package com.discord.challengebot.service;

import com.discord.challengebot.command.CommandRegistry;
import com.discord.challengebot.config.DiscordConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscordServiceTest {

    @Mock
    private DiscordConfig discordConfig;

    @Mock
    private ChallengeService challengeService;

    @Mock
    private ParticipantService participantService;

    @Mock
    private StatisticsService statisticsService;

    @Mock
    private CommandRegistry commandRegistry;

    @InjectMocks
    private DiscordService discordService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testIsAuthorizedUser_AdminCommand() {
        String userId = "12345";
        String command = "новый";

        when(participantService.isAdminUser(userId)).thenReturn(true);

        boolean result = discordService.isAuthorizedUser(userId, command);

        assertTrue(result);
        verify(participantService).isAdminUser(userId);
    }

    @Test
    void testIsAuthorizedUser_NonAdminCommand() {
        String userId = "12345";
        String command = "помощь";

        boolean result = discordService.isAuthorizedUser(userId, command);

        assertTrue(result);
        verify(participantService, never()).isAdminUser(userId);
    }

    @Test
    void testIsAuthorizedUser_AdminCommandNonAdminUser() {
        String userId = "12345";
        String command = "удалить";

        when(participantService.isAdminUser(userId)).thenReturn(false);

        boolean result = discordService.isAuthorizedUser(userId, command);

        assertFalse(result);
        verify(participantService).isAdminUser(userId);
    }

    @Test
    void testFormatChallengeStats() {
        // Проверяем что метод делегирует в statisticsService
        when(statisticsService.formatReportForDiscord(any(), any())).thenReturn("formatted stats");

        String result = discordService.formatChallengeStats(null, null);

        assertEquals("formatted stats", result);
        verify(statisticsService).formatReportForDiscord(null, null);
    }

    @Test
    void testGetReportGuildId() {
        String guildId = "987654321";
        when(discordConfig.getReportGuildId()).thenReturn(guildId);

        assertEquals(guildId, discordConfig.getReportGuildId());
    }
}
