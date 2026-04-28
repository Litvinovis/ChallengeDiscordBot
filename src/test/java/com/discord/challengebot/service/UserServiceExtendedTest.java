package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Расширенные тесты для ParticipantService (переименованные из UserServiceExtendedTest).
 * UserService удалён — его функциональность объединена в ParticipantService.
 */
class UserServiceExtendedTest {

	@Mock
	private DiscordConfig discordConfig;

	@Mock
	private ParticipantRepository participantRepository;

	@Mock
	private ChallengeRepository challengeRepository;

	@InjectMocks
	private ParticipantService participantService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	// ---- registerForChallenge — валидация ----

	@Test
	void registerForChallenge_nullUserId_returnsFalse() {
		assertFalse(participantService.registerForChallenge(null, "Alice", "Pushups"));
		verify(participantRepository, never()).save(any());
	}

	@Test
	void registerForChallenge_emptyUserId_returnsFalse() {
		assertFalse(participantService.registerForChallenge("", "Alice", "Pushups"));
	}

	@Test
	void registerForChallenge_nullUsername_returnsFalse() {
		assertFalse(participantService.registerForChallenge("user1", null, "Pushups"));
	}

	@Test
	void registerForChallenge_emptyUsername_returnsFalse() {
		assertFalse(participantService.registerForChallenge("user1", "", "Pushups"));
	}

	@Test
	void registerForChallenge_nullChallengeName_returnsFalse() {
		assertFalse(participantService.registerForChallenge("user1", "Alice", null));
	}

	@Test
	void registerForChallenge_emptyChallengeName_returnsFalse() {
		assertFalse(participantService.registerForChallenge("user1", "Alice", ""));
	}

	@Test
	void registerForChallenge_existingParticipant_updatesUsernameIfChanged() {
		Participant existing = new Participant("user1", "OldName");
		existing.addChallenge("Pushups");
		when(participantRepository.findById("user1")).thenReturn(Optional.of(existing));

		boolean result = participantService.registerForChallenge("user1", "NewName", "Pushups");

		assertTrue(result);
		assertEquals("NewName", existing.getUsername());
		verify(participantRepository).save(existing);
	}

	@Test
	void registerForChallenge_newParticipant_createsAndSaves() {
		when(participantRepository.findById("user2")).thenReturn(Optional.empty());

		boolean result = participantService.registerForChallenge("user2", "Bob", "Squats");

		assertTrue(result);
		verify(participantRepository).save(argThat(p ->
						"user2".equals(p.getUserId()) && "Bob".equals(p.getUsername())
		));
	}

	// ---- unregisterFromChallenge — валидация ----

	@Test
	void unregisterFromChallenge_nullUserId_returnsFalse() {
		assertFalse(participantService.unregisterFromChallenge(null, "Pushups"));
	}

	@Test
	void unregisterFromChallenge_emptyUserId_returnsFalse() {
		assertFalse(participantService.unregisterFromChallenge("", "Pushups"));
	}

	@Test
	void unregisterFromChallenge_nullChallengeName_returnsFalse() {
		assertFalse(participantService.unregisterFromChallenge("user1", null));
	}

	@Test
	void unregisterFromChallenge_participantNotFound_returnsFalse() {
		when(participantRepository.findById("user1")).thenReturn(Optional.empty());
		assertFalse(participantService.unregisterFromChallenge("user1", "Pushups"));
	}

	@Test
	void unregisterFromChallenge_removesChallenge_andSaves() {
		Participant participant = new Participant("user1", "Alice");
		participant.addChallenge("Pushups");
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		boolean result = participantService.unregisterFromChallenge("user1", "Pushups");

		assertTrue(result);
		assertFalse(participant.isRegisteredForChallenge("Pushups"));
		verify(participantRepository).save(participant);
	}

	// ---- getParticipant ----

	@Test
	void getParticipant_nullId_returnsNull() {
		assertNull(participantService.getParticipant(null));
	}

	@Test
	void getParticipant_emptyId_returnsNull() {
		assertNull(participantService.getParticipant(""));
	}

	@Test
	void getParticipant_existingId_returnsParticipant() {
		Participant participant = new Participant("user1", "Alice");
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		Participant result = participantService.getParticipant("user1");
		assertNotNull(result);
		assertEquals("Alice", result.getUsername());
	}

	@Test
	void getParticipant_notFound_returnsNull() {
		when(participantRepository.findById("ghost")).thenReturn(Optional.empty());
		assertNull(participantService.getParticipant("ghost"));
	}

	// ---- updateParticipantUsername ----

	@Test
	void updateParticipantUsername_nullUserId_returnsFalse() {
		assertFalse(participantService.updateParticipantUsername(null, "Alice"));
	}

	@Test
	void updateParticipantUsername_emptyUserId_returnsFalse() {
		assertFalse(participantService.updateParticipantUsername("", "Alice"));
	}

	@Test
	void updateParticipantUsername_nullUsername_returnsFalse() {
		assertFalse(participantService.updateParticipantUsername("user1", null));
	}

	@Test
	void updateParticipantUsername_emptyUsername_returnsFalse() {
		assertFalse(participantService.updateParticipantUsername("user1", ""));
	}

	@Test
	void updateParticipantUsername_existingParticipant_updatesAndSaves() {
		Participant existing = new Participant("user1", "OldName");
		when(participantRepository.findById("user1")).thenReturn(Optional.of(existing));

		boolean result = participantService.updateParticipantUsername("user1", "NewName");

		assertTrue(result);
		assertEquals("NewName", existing.getUsername());
		verify(participantRepository).save(existing);
	}

	@Test
	void updateParticipantUsername_newParticipant_createsWithNewName() {
		when(participantRepository.findById("user99")).thenReturn(Optional.empty());

		boolean result = participantService.updateParticipantUsername("user99", "Charlie");

		assertTrue(result);
		verify(participantRepository).save(argThat(p ->
						"user99".equals(p.getUserId()) && "Charlie".equals(p.getUsername())
		));
	}

	// ---- getRegisteredChallenges ----

	@Test
	void getRegisteredChallenges_nullUserId_returnsEmpty() {
		assertTrue(participantService.getRegisteredChallenges(null).isEmpty());
	}

	@Test
	void getRegisteredChallenges_emptyUserId_returnsEmpty() {
		assertTrue(participantService.getRegisteredChallenges("").isEmpty());
	}

	@Test
	void getRegisteredChallenges_participantNotFound_returnsEmpty() {
		when(participantRepository.findById("user1")).thenReturn(Optional.empty());
		assertTrue(participantService.getRegisteredChallenges("user1").isEmpty());
	}

	@Test
	void getRegisteredChallenges_returnsOnlyRegisteredChallenges() {
		Participant participant = new Participant("user1", "Alice");
		participant.addChallenge("Pushups");

		Challenge pushups = new Challenge();
		pushups.setName("Pushups");
		Challenge squats = new Challenge();
		squats.setName("Squats");

		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));
		when(challengeRepository.findAll()).thenReturn(Arrays.asList(pushups, squats));

		List<Challenge> result = participantService.getRegisteredChallenges("user1");

		assertEquals(1, result.size());
		assertEquals("Pushups", result.getFirst().getName());
	}

	// ---- isAdminUser с несколькими администраторами ----

	@Test
	void isAdminUser_withAdminList_matchesUserInList() {
		when(discordConfig.getAdminUserIds()).thenReturn(Arrays.asList("admin1", "admin2"));

		assertTrue(participantService.isAdminUser("admin1"));
		assertTrue(participantService.isAdminUser("admin2"));
		assertFalse(participantService.isAdminUser("regular"));
	}

	@Test
	void isAdminUser_emptyId_returnsFalse() {
		assertFalse(participantService.isAdminUser(""));
	}

	@Test
	void isAdminUser_nullId_returnsFalse() {
		assertFalse(participantService.isAdminUser(null));
	}

	@Test
	void isAdminUser_nullAdminUserId_returnsFalse() {
		when(discordConfig.getAdminUserIds()).thenReturn(null);
		when(discordConfig.getAdminUserId()).thenReturn(null);

		assertFalse(participantService.isAdminUser("anyUser"));
	}
}
