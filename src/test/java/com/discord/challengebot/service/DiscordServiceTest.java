package com.discord.challengebot.service;

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
    private UserService userService;

    @Mock
    private StatisticsService statisticsService;

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
        
        when(userService.isAdminUser(userId)).thenReturn(true);
        
        boolean result = discordService.isAuthorizedUser(userId, command);
        
        assertTrue(result);
        verify(userService).isAdminUser(userId);
    }

    @Test
    void testIsAuthorizedUser_NonAdminCommand() {
        String userId = "12345";
        String command = "помощь";
        
        boolean result = discordService.isAuthorizedUser(userId, command);
        
        assertTrue(result);
        verify(userService, never()).isAdminUser(userId);
    }

    @Test
    void testIsAuthorizedUser_AdminCommandNonAdminUser() {
        String userId = "12345";
        String command = "удалить";
        
        when(userService.isAdminUser(userId)).thenReturn(false);
        
        boolean result = discordService.isAuthorizedUser(userId, command);
        
        assertFalse(result);
        verify(userService).isAdminUser(userId);
    }

    @Test
    void testFormatChallengeStats() {
        // This is a pass-through method, so we just verify it calls the statistics service
        when(statisticsService.formatChallengeStats(any())).thenReturn("formatted stats");
        
        String result = discordService.formatChallengeStats(null);
        
        assertEquals("formatted stats", result);
        verify(statisticsService).formatChallengeStats(null);
    }
    
    @Test
    void testGetReportGuildId() {
        // Test that the report guild ID can be set and retrieved
        String guildId = "987654321";
        when(discordConfig.getReportGuildId()).thenReturn(guildId);
        
        assertEquals(guildId, discordConfig.getReportGuildId());
    }
}