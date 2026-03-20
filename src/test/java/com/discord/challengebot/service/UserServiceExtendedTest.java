package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Extended tests for UserService: validation guards, updateParticipantUsername,
 * getRegisteredChallenges, isAdminUser with multi-admin list.
 */
class UserServiceExtendedTest {

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

    // ---- registerForChallenge validation ----

    @Test
    void registerForChallenge_nullUserId_returnsFalse() {
        assertFalse(userService.registerForChallenge(null, "Alice", "Pushups"));
        verify(dataStorageService, never()).saveParticipant(any());
    }

    @Test
    void registerForChallenge_emptyUserId_returnsFalse() {
        assertFalse(userService.registerForChallenge("", "Alice", "Pushups"));
    }

    @Test
    void registerForChallenge_nullUsername_returnsFalse() {
        assertFalse(userService.registerForChallenge("user1", null, "Pushups"));
    }

    @Test
    void registerForChallenge_emptyUsername_returnsFalse() {
        assertFalse(userService.registerForChallenge("user1", "", "Pushups"));
    }

    @Test
    void registerForChallenge_nullChallengeName_returnsFalse() {
        assertFalse(userService.registerForChallenge("user1", "Alice", null));
    }

    @Test
    void registerForChallenge_emptyChallengeName_returnsFalse() {
        assertFalse(userService.registerForChallenge("user1", "Alice", ""));
    }

    @Test
    void registerForChallenge_existingParticipant_updatesUsernameIfChanged() {
        Participant existing = new Participant("user1", "OldName");
        existing.addChallenge("Pushups");
        when(dataStorageService.getParticipant("user1")).thenReturn(existing);

        boolean result = userService.registerForChallenge("user1", "NewName", "Pushups");

        assertTrue(result);
        assertEquals("NewName", existing.getUsername());
        verify(dataStorageService).saveParticipant(existing);
    }

    @Test
    void registerForChallenge_newParticipant_createsAndSaves() {
        when(dataStorageService.getParticipant("user2")).thenReturn(null);

        boolean result = userService.registerForChallenge("user2", "Bob", "Squats");

        assertTrue(result);
        verify(dataStorageService).saveParticipant(argThat(p ->
                "user2".equals(p.getUserId()) && "Bob".equals(p.getUsername())
        ));
    }

    // ---- unregisterFromChallenge validation ----

    @Test
    void unregisterFromChallenge_nullUserId_returnsFalse() {
        assertFalse(userService.unregisterFromChallenge(null, "Pushups"));
    }

    @Test
    void unregisterFromChallenge_emptyUserId_returnsFalse() {
        assertFalse(userService.unregisterFromChallenge("", "Pushups"));
    }

    @Test
    void unregisterFromChallenge_nullChallengeName_returnsFalse() {
        assertFalse(userService.unregisterFromChallenge("user1", null));
    }

    @Test
    void unregisterFromChallenge_participantNotFound_returnsFalse() {
        when(dataStorageService.getParticipant("user1")).thenReturn(null);
        assertFalse(userService.unregisterFromChallenge("user1", "Pushups"));
    }

    @Test
    void unregisterFromChallenge_removesChallenge_andSaves() {
        Participant participant = new Participant("user1", "Alice");
        participant.addChallenge("Pushups");
        when(dataStorageService.getParticipant("user1")).thenReturn(participant);

        boolean result = userService.unregisterFromChallenge("user1", "Pushups");

        assertTrue(result);
        assertFalse(participant.isRegisteredForChallenge("Pushups"));
        verify(dataStorageService).saveParticipant(participant);
    }

    // ---- getParticipant ----

    @Test
    void getParticipant_nullId_returnsNull() {
        assertNull(userService.getParticipant(null));
    }

    @Test
    void getParticipant_emptyId_returnsNull() {
        assertNull(userService.getParticipant(""));
    }

    @Test
    void getParticipant_existingId_returnsParticipant() {
        Participant participant = new Participant("user1", "Alice");
        when(dataStorageService.getParticipant("user1")).thenReturn(participant);

        Participant result = userService.getParticipant("user1");
        assertNotNull(result);
        assertEquals("Alice", result.getUsername());
    }

    @Test
    void getParticipant_notFound_returnsNull() {
        when(dataStorageService.getParticipant("ghost")).thenReturn(null);
        assertNull(userService.getParticipant("ghost"));
    }

    // ---- updateParticipantUsername ----

    @Test
    void updateParticipantUsername_nullUserId_returnsFalse() {
        assertFalse(userService.updateParticipantUsername(null, "Alice"));
    }

    @Test
    void updateParticipantUsername_emptyUserId_returnsFalse() {
        assertFalse(userService.updateParticipantUsername("", "Alice"));
    }

    @Test
    void updateParticipantUsername_nullUsername_returnsFalse() {
        assertFalse(userService.updateParticipantUsername("user1", null));
    }

    @Test
    void updateParticipantUsername_emptyUsername_returnsFalse() {
        assertFalse(userService.updateParticipantUsername("user1", ""));
    }

    @Test
    void updateParticipantUsername_existingParticipant_updatesAndSaves() {
        Participant existing = new Participant("user1", "OldName");
        when(dataStorageService.getParticipant("user1")).thenReturn(existing);

        boolean result = userService.updateParticipantUsername("user1", "NewName");

        assertTrue(result);
        assertEquals("NewName", existing.getUsername());
        verify(dataStorageService).saveParticipant(existing);
    }

    @Test
    void updateParticipantUsername_newParticipant_createsWithNewName() {
        when(dataStorageService.getParticipant("user99")).thenReturn(null);

        boolean result = userService.updateParticipantUsername("user99", "Charlie");

        assertTrue(result);
        verify(dataStorageService).saveParticipant(argThat(p ->
                "user99".equals(p.getUserId()) && "Charlie".equals(p.getUsername())
        ));
    }

    // ---- getRegisteredChallenges ----

    @Test
    void getRegisteredChallenges_nullUserId_returnsEmpty() {
        assertTrue(userService.getRegisteredChallenges(null).isEmpty());
    }

    @Test
    void getRegisteredChallenges_emptyUserId_returnsEmpty() {
        assertTrue(userService.getRegisteredChallenges("").isEmpty());
    }

    @Test
    void getRegisteredChallenges_participantNotFound_returnsEmpty() {
        when(dataStorageService.getParticipant("user1")).thenReturn(null);
        assertTrue(userService.getRegisteredChallenges("user1").isEmpty());
    }

    @Test
    void getRegisteredChallenges_returnsOnlyRegisteredChallenges() {
        Participant participant = new Participant("user1", "Alice");
        participant.addChallenge("Pushups");

        Challenge pushups = new Challenge();
        pushups.setName("Pushups");
        Challenge squats = new Challenge();
        squats.setName("Squats");

        when(dataStorageService.getParticipant("user1")).thenReturn(participant);
        when(dataStorageService.getAllChallenges()).thenReturn(Arrays.asList(pushups, squats));

        List<Challenge> result = userService.getRegisteredChallenges("user1");

        assertEquals(1, result.size());
        assertEquals("Pushups", result.get(0).getName());
    }

    // ---- isAdminUser with list ----

    @Test
    void isAdminUser_withAdminList_matchesUserInList() {
        when(discordConfig.getAdminUserIds()).thenReturn(Arrays.asList("admin1", "admin2"));

        assertTrue(userService.isAdminUser("admin1"));
        assertTrue(userService.isAdminUser("admin2"));
        assertFalse(userService.isAdminUser("regular"));
    }

    @Test
    void isAdminUser_emptyId_returnsFalse() {
        assertFalse(userService.isAdminUser(""));
    }

    @Test
    void isAdminUser_nullId_returnsFalse() {
        assertFalse(userService.isAdminUser(null));
    }

    @Test
    void isAdminUser_nullAdminUserId_returnsFalse() {
        when(discordConfig.getAdminUserIds()).thenReturn(null);
        when(discordConfig.getAdminUserId()).thenReturn(null);

        assertFalse(userService.isAdminUser("anyUser"));
    }
}
