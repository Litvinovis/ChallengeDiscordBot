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
    void testGenerateHelpMessage() {
        String helpMessage = discordService.generateHelpMessage();

        assertNotNull(helpMessage);
        assertTrue(helpMessage.contains("Справка по командам бота"));
        assertTrue(helpMessage.contains("Основные команды:"));
        assertTrue(helpMessage.contains("Команды управления испытаниями"));
        assertTrue(helpMessage.contains("Команды пользователя:"));
    }

    @Test
    void testIsAuthorizedUserForAdminCommandWhenAdmin() {
        String adminUserId = "12345";
        String command = "новый";
        when(discordConfig.getAdminUserId()).thenReturn(adminUserId);
        when(userService.isAdminUser(adminUserId)).thenReturn(true);

        boolean isAuthorized = discordService.isAuthorizedUser(adminUserId, command);

        assertTrue(isAuthorized);
    }

    @Test
    void testIsAuthorizedUserForAdminCommandWhenNotAdmin() {
        String regularUserId = "67890";
        String command = "новый";
        when(discordConfig.getAdminUserId()).thenReturn("12345");
        when(userService.isAdminUser(regularUserId)).thenReturn(false);

        boolean isAuthorized = discordService.isAuthorizedUser(regularUserId, command);

        assertFalse(isAuthorized);
    }

    @Test
    void testIsAuthorizedUserForRegularCommand() {
        String userId = "12345";
        String command = "статистика";

        boolean isAuthorized = discordService.isAuthorizedUser(userId, command);

        assertTrue(isAuthorized);
    }
}