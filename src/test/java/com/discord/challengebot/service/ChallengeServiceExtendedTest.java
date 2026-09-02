package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.repository.ProgressHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Расширенные тесты для ChallengeService: валидационные проверки, фильтрация, обновления.
 */
class ChallengeServiceExtendedTest {

	@Mock
	private ChallengeRepository challengeRepository;

	@Mock
	private ChallengeProgressRepository progressRepository;

	@Mock
	private ParticipantService participantService;

	@Mock
	private ProgressHistoryRepository progressHistoryRepository;

	@InjectMocks
	private ChallengeService challengeService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		// По умолчанию progressRepository возвращает пустую карту
		when(progressRepository.findByChallengeId(anyString())).thenReturn(new HashMap<>());
	}

	// ---- createChallenge — валидация ----

	@Test
	void createChallenge_nullName_returnsNull() {
		Challenge result = challengeService.createChallenge(null, 100L,
						LocalDateTime.now().plusDays(10), ChallengeType.GROUP, "desc", "reps");
		assertNull(result, "Must return null for null challenge name");
		verify(challengeRepository, never()).save(any());
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

		when(challengeRepository.findAll()).thenReturn(Arrays.asList(active, inactive));

		List<Challenge> result = challengeService.getActiveChallenges();

		assertEquals(1, result.size());
		assertTrue(result.getFirst().isActive());
	}

	@Test
	void getActiveChallenges_emptyList_returnsEmpty() {
		when(challengeRepository.findAll()).thenReturn(Collections.emptyList());
		List<Challenge> result = challengeService.getActiveChallenges();
		assertTrue(result.isEmpty());
	}

	// ---- getUserChallenges ----

	@Test
	void getUserChallenges_returnsOnlyChallengesWithParticipant() {
		Challenge c1 = new Challenge();
		c1.setId("c1");

		Challenge c2 = new Challenge();
		c2.setId("c2");

		when(challengeRepository.findAll()).thenReturn(Arrays.asList(c1, c2));
		// user1 состоит только в c1 — один запрос по пользователю вместо запроса на испытание
		when(progressRepository.findByUserId("user1")).thenReturn(Map.of("c1", 100L));

		List<Challenge> result = challengeService.getUserChallenges("user1");
		assertEquals(1, result.size());
		assertEquals("c1", result.getFirst().getId());
	}

	@Test
	void getUserChallenges_emptyUserId_returnsEmpty() {
		List<Challenge> result = challengeService.getUserChallenges("");
		assertTrue(result.isEmpty());
		verify(challengeRepository, never()).findAll();
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
		verify(challengeRepository).updateActive(challenge.getId(), false);
	}

	@Test
	void updateChallengeStatus_nullChallenge_returnsNull() {
		Challenge result = challengeService.updateChallengeStatus(null, false);
		assertNull(result);
		verify(challengeRepository, never()).save(any());
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
		verify(challengeRepository).updateTargetValue(challenge.getId(), 2000L);
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
		verify(challengeRepository, never()).save(any());
	}

	@Test
	void updateChallengeTarget_negativeTarget_returnsChallengeUnchanged() {
		Challenge challenge = new Challenge();
		challenge.setName("Pushups");
		challenge.setTargetValue(1000L);

		Challenge result = challengeService.updateChallengeTarget(challenge, -500L);

		assertEquals(1000L, result.getTargetValue());
		verify(challengeRepository, never()).save(any());
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
		verify(challengeRepository).updateEndDate(challenge.getId(), newDate);
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
		verify(challengeRepository, never()).save(any());
	}

	// ---- removeParticipant ----

	@Test
	void removeParticipant_removesUserAndRecalculatesTotal() {
		Challenge challenge = new Challenge();
		challenge.setId("pushups");
		challenge.setName("Pushups");
		challenge.addParticipant("user1");
		challenge.addParticipant("user2");
		challenge.getParticipantProgress().put("user1", 300L);
		challenge.getParticipantProgress().put("user2", 200L);
		challenge.setCurrentValue(500L);

		// После удаления user1, progressRepository возвращает только user2
		when(progressRepository.findByChallengeId("pushups")).thenReturn(Map.of("user2", 200L));

		Challenge result = challengeService.removeParticipant(challenge, "user1");

		assertNotNull(result);
		assertFalse(result.hasParticipant("user1"));
		assertEquals(200L, result.getCurrentValue(), "Total must be recalculated without user1");
		verify(challengeRepository).removeParticipant("pushups", "user1");
		verify(challengeRepository).refreshCurrentValue("pushups");
	}

	@Test
	void removeParticipant_nullChallenge_returnsNull() {
		assertNull(challengeService.removeParticipant(null, "user1"));
	}

	@Test
	void removeParticipant_emptyUserId_returnsChallengeUnchanged() {
		Challenge challenge = new Challenge();
		challenge.setId("pushups");
		challenge.setName("Pushups");
		challenge.addParticipant("user1");

		Challenge result = challengeService.removeParticipant(challenge, "");
		assertTrue(result.hasParticipant("user1"), "User1 must still be present");
		verify(challengeRepository, never()).save(any());
	}

	// ---- completeChallenge ----

	@Test
	void completeChallenge_setsInactive() {
		Challenge challenge = new Challenge();
		challenge.setName("Pushups");
		challenge.setActive(true);

		challengeService.completeChallenge(challenge);

		assertFalse(challenge.isActive());
		verify(challengeRepository).updateActive(challenge.getId(), false);
	}

	@Test
	void completeChallenge_nullChallenge_doesNotThrow() {
		assertDoesNotThrow(() -> challengeService.completeChallenge(null));
		verify(challengeRepository, never()).save(any());
	}

	// ---- getTopParticipants ----

	@Test
	void getTopParticipants_returnsParticipantsInDescOrder() {
		Challenge challenge = new Challenge();
		challenge.setId("pushups");
		challenge.setName("Pushups");

		when(progressRepository.findByChallengeId("pushups")).thenReturn(Map.of(
						"alice", 1000L, "bob", 3000L, "carol", 2000L));

		List<Map.Entry<String, Long>> top = challengeService.getTopParticipants(challenge, 3);

		assertEquals(3, top.size());
		assertEquals("bob", top.get(0).getKey(), "Bob has highest progress");
		assertEquals("carol", top.get(1).getKey());
		assertEquals("alice", top.get(2).getKey());
	}

	@Test
	void getTopParticipants_limitReducesResults() {
		Challenge challenge = new Challenge();
		challenge.setId("pushups");
		challenge.setName("Pushups");

		when(progressRepository.findByChallengeId("pushups")).thenReturn(Map.of(
						"alice", 100L, "bob", 200L, "carol", 300L));

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
		challenge.setId("pushups");
		challenge.setName("Pushups");
		assertTrue(challengeService.getTopParticipants(challenge, 0).isEmpty());
		assertTrue(challengeService.getTopParticipants(challenge, -1).isEmpty());
	}

	// ---- addParticipantWithUsername ----

	@Test
	void addParticipantWithUsername_addsParticipantAndSetsZeroProgress() {
		Challenge challenge = new Challenge();
		challenge.setId("pushups");
		challenge.setName("Pushups");
		when(participantService.registerForChallenge("user1", "Alice", "Pushups")).thenReturn(true);

		Challenge result = challengeService.addParticipantWithUsername(challenge, "user1", "Alice");

		assertNotNull(result);
		assertTrue(result.hasParticipant("user1"));
		assertEquals(0L, result.getParticipantProgress().get("user1"));
		verify(challengeRepository).addParticipant(challenge.getId(), "user1");
	}

	@Test
	void addParticipantWithUsername_nullChallenge_returnsNull() {
		assertNull(challengeService.addParticipantWithUsername(null, "user1", "Alice"));
	}

	@Test
	void addParticipantWithUsername_emptyUserId_returnsChallengeUnchanged() {
		Challenge challenge = new Challenge();
		challenge.setId("pushups");
		challenge.setName("Pushups");

		Challenge result = challengeService.addParticipantWithUsername(challenge, "", "Alice");
		assertEquals(0, result.getParticipants().size());
		verify(challengeRepository, never()).save(any());
	}

	@Test
	void addParticipantWithUsername_doesNotOverrideExistingProgress() {
		Challenge challenge = new Challenge();
		challenge.setId("pushups");
		challenge.setName("Pushups");
		challenge.addParticipant("user1");
		challenge.getParticipantProgress().put("user1", 500L);
		when(participantService.registerForChallenge(anyString(), anyString(), anyString())).thenReturn(true);

		Challenge result = challengeService.addParticipantWithUsername(challenge, "user1", "Alice");

		assertEquals(500L, result.getParticipantProgress().get("user1"),
						"Existing progress must not be overwritten");
	}

	// ---- getChallengeStats — граничные случаи ----

	@Test
	void getChallengeStats_nullChallenge_returnsNull() {
		assertNull(challengeService.getChallengeStats(null));
	}

	@Test
	void getChallengeStats_goalExceeded_percentageAbove100() {
		Challenge challenge = new Challenge();
		challenge.setId("pushups");
		challenge.setName("Pushups");
		challenge.setTargetValue(100L);
		challenge.setCurrentValue(120L);
		challenge.setEndDate(LocalDateTime.now().plusDays(5));

		when(progressRepository.findByChallengeId("pushups")).thenReturn(Map.of("user1", 120L));

		ChallengeStats stats = challengeService.getChallengeStats(challenge);

		assertNotNull(stats);
		assertTrue(stats.percentage() > 100.0);
		assertTrue(stats.remaining() < 0);
	}
}
