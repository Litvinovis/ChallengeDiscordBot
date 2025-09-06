package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Participant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private DiscordConfig discordConfig;

    @Mock
    private DataStorageService dataStorageService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testIsAdminUserWhenAdmin() {
        String adminUserId = "12345";
        when(discordConfig.getAdminUserIds()).thenReturn(null);
        when(discordConfig.getAdminUserId()).thenReturn(adminUserId);

        boolean isAdmin = userService.isAdminUser(adminUserId);

        assertTrue(isAdmin);
        verify(discordConfig, atLeastOnce()).getAdminUserId();
    }

    @Test
    void testIsAdminUserWhenNotAdmin() {
        String adminUserId = "12345";
        String regularUserId = "67890";
        when(discordConfig.getAdminUserIds()).thenReturn(null);
        when(discordConfig.getAdminUserId()).thenReturn(adminUserId);

        boolean isAdmin = userService.isAdminUser(regularUserId);

        assertFalse(isAdmin);
        verify(discordConfig, atLeastOnce()).getAdminUserId();
    }

    @Test
    void testRegisterForChallenge() {
        String userId = "12345";
        String username = "testuser";
        String challengeName = "Отжимания";
        
        // Мокаем поведение dataStorageService
        when(dataStorageService.getParticipant(userId)).thenReturn(null);

        boolean result = userService.registerForChallenge(userId, username, challengeName);

        assertTrue(result);
        verify(dataStorageService).getParticipant(userId);
        verify(dataStorageService).saveParticipant(any(Participant.class));
    }

    @Test
    void testUnregisterFromChallenge() {
        String userId = "12345";
        String challengeName = "Отжимания";
        
        // Мокаем поведение dataStorageService
        Participant participant = new Participant(userId, "testuser");
        participant.addChallenge(challengeName);
        when(dataStorageService.getParticipant(userId)).thenReturn(participant);

        boolean result = userService.unregisterFromChallenge(userId, challengeName);

        assertTrue(result);
        verify(dataStorageService).getParticipant(userId);
        verify(dataStorageService).saveParticipant(any(Participant.class));
    }
}