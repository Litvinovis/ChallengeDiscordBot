package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.repository.ProgressHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Регрессии: прогресс участников при загрузке испытания, история вычитаний, полное удаление.
 */
class ChallengeServiceAuditTest {

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
	}

	private Challenge challenge(String id, String name) {
		Challenge c = new Challenge();
		c.setId(id);
		c.setName(name);
		c.setTargetValue(1000L);
		c.setActive(true);
		return c;
	}

	@Test
	void getChallenge_loadsParticipantProgress() {
		when(challengeRepository.findById("отжимания")).thenReturn(Optional.of(challenge("отжимания", "Отжимания")));
		when(progressRepository.findByChallengeId("отжимания")).thenReturn(Map.of("u1", 120L, "u2", 30L));

		Challenge result = challengeService.getChallenge("Отжимания");

		assertEquals(120L, result.getParticipantProgress().get("u1"));
		assertEquals(30L, result.getParticipantProgress().get("u2"));
	}

	@Test
	void getChallenge_legacyNameFallback_alsoLoadsProgress() {
		when(challengeRepository.findById("бег")).thenReturn(Optional.empty());
		when(challengeRepository.findByName("Бег")).thenReturn(Optional.of(challenge("legacy-id", "Бег")));
		when(progressRepository.findByChallengeId("legacy-id")).thenReturn(Map.of("u1", 5L));

		Challenge result = challengeService.getChallenge("Бег");

		assertEquals(5L, result.getParticipantProgress().get("u1"));
	}

	@Test
	void getAllChallenges_loadsProgressWithSingleQuery() {
		when(challengeRepository.findAll()).thenReturn(List.of(challenge("a", "A"), challenge("b", "B")));
		when(progressRepository.findAllGroupedByChallenge()).thenReturn(Map.of("a", Map.of("u1", 7L)));

		List<Challenge> result = challengeService.getAllChallenges();

		assertEquals(7L, result.get(0).getParticipantProgress().get("u1"));
		assertTrue(result.get(1).getParticipantProgress().isEmpty());
		verify(progressRepository, never()).findByChallengeId(any());
	}

	@Test
	void subtractProgress_recordsActuallyRemovedAmountInHistory() {
		Challenge c = challenge("test", "Тест");
		// Просили вычесть 50, но у участника было только 30
		when(progressRepository.subtractAmount("test", "user1", 50L)).thenReturn(30L);
		when(progressRepository.findByChallengeId("test")).thenReturn(Map.of("user1", 0L));

		challengeService.subtractProgress(c, "user1", "alice", 50L);

		verify(progressHistoryRepository).insert("test", "user1", "alice", -30L);
	}

	@Test
	void subtractProgress_nothingRemoved_noHistoryRecord() {
		Challenge c = challenge("test", "Тест");
		when(progressRepository.subtractAmount("test", "user1", 10L)).thenReturn(0L);
		when(progressRepository.findByChallengeId("test")).thenReturn(Map.of());

		challengeService.subtractProgress(c, "user1", "alice", 10L);

		verify(progressHistoryRepository, never()).insert(any(), any(), any(), anyLong());
	}

	@Test
	void deleteChallenge_removesProgressAndHistory() {
		when(challengeRepository.existsById("отжимания")).thenReturn(true);

		assertTrue(challengeService.deleteChallenge("Отжимания"));

		verify(progressRepository).deleteByChallengeId("отжимания");
		verify(progressHistoryRepository).deleteByChallengeId("отжимания");
		verify(challengeRepository).deleteById("отжимания");
	}

	@Test
	void deleteChallenge_legacyIdFoundByName() {
		when(challengeRepository.existsById("бег")).thenReturn(false);
		when(challengeRepository.findByName("Бег")).thenReturn(Optional.of(challenge("legacy-id", "Бег")));

		assertTrue(challengeService.deleteChallenge("Бег"));

		verify(progressHistoryRepository).deleteByChallengeId("legacy-id");
		verify(challengeRepository).deleteById("legacy-id");
	}

	@Test
	void deleteChallenge_unknown_returnsFalse() {
		when(challengeRepository.existsById("нет")).thenReturn(false);
		when(challengeRepository.findByName("нет")).thenReturn(Optional.empty());

		assertFalse(challengeService.deleteChallenge("нет"));

		verify(challengeRepository, never()).deleteById(any());
	}

	private static <T> T any() {
		return org.mockito.ArgumentMatchers.any();
	}
}
