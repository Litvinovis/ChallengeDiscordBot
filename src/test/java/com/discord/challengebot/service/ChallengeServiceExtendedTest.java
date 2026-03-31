package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Extended tests for ChallengeService covering validation edge cases,
 * filtering methods, and update operations not covered in ChallengeServiceTest.
 */
class ChallengeServiceExtendedTest {

    @Mock
    private DataStorageService dataStorageService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ChallengeService challengeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---- createChallenge validation ----

    @Test
    void createChallenge_nullName_returnsNull() {
        Challenge result = challengeService.createChallenge(null, 100L,
                LocalDateTime.now().plusDays(10), ChallengeType.GROUP, "desc", "reps");
        assertNull(result, "Must return null for null challenge name");
        verify(dataStorageService, never()).saveChallenge(any());
    }

    @Test
    void createChallenge_emptyName_returnsNull() {
        Challenge result = challengeService.createChallenge("", 100L,
                LocalDateTime.now().plusDays(10), ChallengeType.GROUP, "desc", "reps");
        assertNull(result);
    }

    @Test
    void createChallenge_zeroTarget_returnsNull() {
        Challenge result = challengeService.createChallenge("Pushups", 0L,
                LocalDateTime.now().plusDays(10), ChallengeType.GROUP, "desc", "reps");
        assertNull(result);
    }

    @Test
    void createChallenge_negativeTarget_returnsNull() {
        Challenge result = challengeService.createChallenge("Pushups", -1L,
                LocalDateTime.now().plusDays(10), ChallengeType.GROUP, "desc", "reps");
        assertNull(result);
    }

    @Test
    void createChallenge_nullEndDate_returnsNull() {
        Challenge result = challengeService.createChallenge("Pushups", 100L,
                null, ChallengeType.GROUP, "desc", "reps");
        assertNull(result);
    }

    @Test
    void createChallenge_nullType_returnsNull() {
        Challenge result = challengeService.createChallenge("Pushups", 100L,
                LocalDateTime.now().plusDays(10), null, "desc", "reps");
        assertNull(result);
    }

    @Test
    void createChallenge_nameWithSpaces_idUsesUnderscores() {
        Challenge result = challengeService.createChallenge("Push Ups", 100L,
                LocalDateTime.now().plusDays(10), ChallengeType.INDIVIDUAL, "desc", "reps");
        assertNotNull(result);
        assertEquals("push_ups", result.getId());
    }

    // ---- getActiveChallenges ----

    @Test
    void getActiveChallenges_filtersInactiveChallenges() {
        Challenge active = new Challenge();
        active.setActive(true);

        Challenge inactive = new Challenge();
        inactive.setActive(false);

        when(dataStorageService.getAllChallenges()).thenReturn(Arrays.asList(active, inactive));

        List<Challenge> result = challengeService.getActiveChallenges();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    @Test
    void getActiveChallenges_emptyList_returnsEmpty() {
        when(dataStorageService.getAllChallenges()).thenReturn(Collections.emptyList());
        List<Challenge> result = challengeService.getActiveChallenges();
        assertTrue(result.isEmpty());
    }

    // ---- getUserChallenges ----

    @Test
    void getUserChallenges_returnsOnlyChallengesWithParticipant() {
        Challenge c1 = new Challenge();
        c1.addParticipant("user1");

        Challenge c2 = new Challenge();
        c2.addParticipant("user2");

        when(dataStorageService.getAllChallenges()).thenReturn(Arrays.asList(c1, c2));

        List<Challenge> result = challengeService.getUserChallenges("user1");
        assertEquals(1, result.size());
        assertTrue(result.get(0).hasParticipant("user1"));
    }

    @Test
    void getUserChallenges_emptyUserId_returnsEmpty() {
        List<Challenge> result = challengeService.getUserChallenges("");
        assertTrue(result.isEmpty());
        verify(dataStorageService, never()).getAllChallenges();
    }

    @Test
    void getUserChallenges_nullUserId_returnsEmpty() {
        List<Challenge> result = challengeService.getUserChallenges(null);
        assertTrue(result.isEmpty());
    }

    // ---- updateChallengeStatus ----

    @Test
    void updateChallengeStatus_setsActiveFlag() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        challenge.setActive(true);

        Challenge result = challengeService.updateChallengeStatus(challenge, false);

        assertNotNull(result);
        assertFalse(result.isActive());
        verify(dataStorageService).saveChallenge(challenge);
    }

    @Test
    void updateChallengeStatus_nullChallenge_returnsNull() {
        Challenge result = challengeService.updateChallengeStatus(null, false);
        assertNull(result);
        verify(dataStorageService, never()).saveChallenge(any());
    }

    // ---- updateChallengeTarget ----

    @Test
    void updateChallengeTarget_setsNewTarget() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        challenge.setTargetValue(1000L);

        Challenge result = challengeService.updateChallengeTarget(challenge, 2000L);

        assertNotNull(result);
        assertEquals(2000L, result.getTargetValue());
        verify(dataStorageService).saveChallenge(challenge);
    }

    @Test
    void updateChallengeTarget_nullChallenge_returnsNull() {
        assertNull(challengeService.updateChallengeTarget(null, 100L));
    }

    @Test
    void updateChallengeTarget_zeroTarget_returnsChallengeUnchanged() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        challenge.setTargetValue(1000L);

        Challenge result = challengeService.updateChallengeTarget(challenge, 0L);

        assertEquals(1000L, result.getTargetValue(), "Target must not change for invalid value");
        verify(dataStorageService, never()).saveChallenge(any());
    }

    @Test
    void updateChallengeTarget_negativeTarget_returnsChallengeUnchanged() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        challenge.setTargetValue(1000L);

        Challenge result = challengeService.updateChallengeTarget(challenge, -500L);

        assertEquals(1000L, result.getTargetValue());
        verify(dataStorageService, never()).saveChallenge(any());
    }

    // ---- updateChallengeEndDate ----

    @Test
    void updateChallengeEndDate_setsNewEndDate() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        LocalDateTime newDate = LocalDateTime.now().plusDays(60);

        Challenge result = challengeService.updateChallengeEndDate(challenge, newDate);

        assertNotNull(result);
        assertEquals(newDate, result.getEndDate());
        verify(dataStorageService).saveChallenge(challenge);
    }

    @Test
    void updateChallengeEndDate_nullChallenge_returnsNull() {
        assertNull(challengeService.updateChallengeEndDate(null, LocalDateTime.now().plusDays(10)));
    }

    @Test
    void updateChallengeEndDate_nullDate_returnsChallengeUnchanged() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        LocalDateTime original = LocalDateTime.now().plusDays(30);
        challenge.setEndDate(original);

        Challenge result = challengeService.updateChallengeEndDate(challenge, null);

        assertEquals(original, result.getEndDate());
        verify(dataStorageService, never()).saveChallenge(any());
    }

    // ---- removeParticipant ----

    @Test
    void removeParticipant_removesUserAndRecalculatesTotal() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        challenge.addParticipant("user1");
        challenge.addParticipant("user2");
        challenge.getParticipantProgress().put("user1", 300L);
        challenge.getParticipantProgress().put("user2", 200L);
        challenge.setCurrentValue(500L);

        Challenge result = challengeService.removeParticipant(challenge, "user1");

        assertNotNull(result);
        assertFalse(result.hasParticipant("user1"));
        assertEquals(200L, result.getCurrentValue(), "Total must be recalculated without user1");
        verify(dataStorageService).saveChallenge(challenge);
    }

    @Test
    void removeParticipant_nullChallenge_returnsNull() {
        assertNull(challengeService.removeParticipant(null, "user1"));
    }

    @Test
    void removeParticipant_emptyUserId_returnsChallengeUnchanged() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        challenge.addParticipant("user1");

        Challenge result = challengeService.removeParticipant(challenge, "");
        assertTrue(result.hasParticipant("user1"), "User1 must still be present");
        verify(dataStorageService, never()).saveChallenge(any());
    }

    // ---- completeChallenge ----

    @Test
    void completeChallenge_setsInactive() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        challenge.setActive(true);

        challengeService.completeChallenge(challenge);

        assertFalse(challenge.isActive());
        verify(dataStorageService).saveChallenge(challenge);
    }

    @Test
    void completeChallenge_nullChallenge_doesNotThrow() {
        assertDoesNotThrow(() -> challengeService.completeChallenge(null));
        verify(dataStorageService, never()).saveChallenge(any());
    }

    // ---- getTopParticipants ----

    @Test
    void getTopParticipants_returnsParticipantsInDescOrder() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        challenge.getParticipantProgress().put("alice", 1000L);
        challenge.getParticipantProgress().put("bob", 3000L);
        challenge.getParticipantProgress().put("carol", 2000L);

        List<Map.Entry<String, Long>> top = challengeService.getTopParticipants(challenge, 3);

        assertEquals(3, top.size());
        assertEquals("bob", top.get(0).getKey(), "Bob has highest progress");
        assertEquals("carol", top.get(1).getKey());
        assertEquals("alice", top.get(2).getKey());
    }

    @Test
    void getTopParticipants_limitReducesResults() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        challenge.getParticipantProgress().put("alice", 100L);
        challenge.getParticipantProgress().put("bob", 200L);
        challenge.getParticipantProgress().put("carol", 300L);

        List<Map.Entry<String, Long>> top = challengeService.getTopParticipants(challenge, 2);
        assertEquals(2, top.size());
    }

    @Test
    void getTopParticipants_nullChallenge_returnsEmpty() {
        assertTrue(challengeService.getTopParticipants(null, 5).isEmpty());
    }

    @Test
    void getTopParticipants_invalidLimit_returnsEmpty() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        assertTrue(challengeService.getTopParticipants(challenge, 0).isEmpty());
        assertTrue(challengeService.getTopParticipants(challenge, -1).isEmpty());
    }

    // ---- addParticipantWithUsername ----

    @Test
    void addParticipantWithUsername_addsParticipantAndSetsZeroProgress() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        when(userService.registerForChallenge("user1", "Alice", "Pushups")).thenReturn(true);

        Challenge result = challengeService.addParticipantWithUsername(challenge, "user1", "Alice");

        assertNotNull(result);
        assertTrue(result.hasParticipant("user1"));
        assertEquals(0L, result.getParticipantProgress().get("user1"));
        verify(dataStorageService).saveChallenge(challenge);
    }

    @Test
    void addParticipantWithUsername_nullChallenge_returnsNull() {
        assertNull(challengeService.addParticipantWithUsername(null, "user1", "Alice"));
    }

    @Test
    void addParticipantWithUsername_emptyUserId_returnsChallengeUnchanged() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");

        Challenge result = challengeService.addParticipantWithUsername(challenge, "", "Alice");
        assertEquals(0, result.getParticipants().size());
        verify(dataStorageService, never()).saveChallenge(any());
    }

    @Test
    void addParticipantWithUsername_emptyUsername_returnsChallengeUnchanged() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");

        Challenge result = challengeService.addParticipantWithUsername(challenge, "user1", "");
        assertEquals(0, result.getParticipants().size());
        verify(dataStorageService, never()).saveChallenge(any());
    }

    @Test
    void addParticipantWithUsername_doesNotOverrideExistingProgress() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        challenge.addParticipant("user1");
        challenge.getParticipantProgress().put("user1", 500L);
        when(userService.registerForChallenge(anyString(), anyString(), anyString())).thenReturn(true);

        Challenge result = challengeService.addParticipantWithUsername(challenge, "user1", "Alice");

        assertEquals(500L, result.getParticipantProgress().get("user1"),
                "Existing progress must not be overwritten");
    }

    // ---- getChallengeStats edge cases ----

    @Test
    void getChallengeStats_nullChallenge_returnsNull() {
        assertNull(challengeService.getChallengeStats(null));
    }

    @Test
    void getChallengeStats_goalExceeded_percentageAbove100() {
        Challenge challenge = new Challenge();
        challenge.setName("Pushups");
        challenge.setTargetValue(100L);
        challenge.setCurrentValue(120L);
        challenge.setEndDate(LocalDateTime.now().plusDays(5));

        ChallengeStats stats = challengeService.getChallengeStats(challenge);

        assertNotNull(stats);
        assertTrue(stats.percentage() > 100.0);
        assertTrue(stats.remaining() < 0);
    }
}
