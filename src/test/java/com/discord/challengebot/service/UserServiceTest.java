package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private DiscordConfig discordConfig;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testIsAdminUserWhenAdmin() {
        String adminUserId = "12345";
        when(discordConfig.getAdminUserId()).thenReturn(adminUserId);

        boolean isAdmin = userService.isAdminUser(adminUserId);

        assertTrue(isAdmin);
        verify(discordConfig).getAdminUserId();
    }

    @Test
    void testIsAdminUserWhenNotAdmin() {
        String adminUserId = "12345";
        String regularUserId = "67890";
        when(discordConfig.getAdminUserId()).thenReturn(adminUserId);

        boolean isAdmin = userService.isAdminUser(regularUserId);

        assertFalse(isAdmin);
        verify(discordConfig).getAdminUserId();
    }

    @Test
    void testRegisterForChallenge() {
        String userId = "12345";
        String username = "testuser";
        String challengeName = "Отжимания";

        boolean result = userService.registerForChallenge(userId, username, challengeName);

        assertTrue(result);
    }

    @Test
    void testUnregisterFromChallenge() {
        String userId = "12345";
        String challengeName = "Отжимания";

        boolean result = userService.unregisterFromChallenge(userId, challengeName);

        assertTrue(result);
    }
}