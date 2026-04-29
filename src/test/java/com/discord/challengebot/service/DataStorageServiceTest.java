package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Тесты для ParticipantService (заменяют ранее существовавший DataStorageServiceTest).
 * DataStorageService удалён в ходе рефакторинга — вся логика хранения перенесена
 * в ParticipantService + ChallengeService, работающие напрямую с репозиториями.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataStorageServiceTest {

	@Mock
	private ParticipantRepository participantRepository;

	@Mock
	private ChallengeRepository challengeRepository;

	@Mock
	private DiscordConfig discordConfig;

	@InjectMocks
	private ParticipantService participantService;

	@BeforeEach
	void setUp() {
		// Репозитории готовы — дополнительная инициализация не нужна
	}

	private Challenge buildChallenge(String name) {
		Challenge challenge = new Challenge();
		challenge.setId(name.toLowerCase().replace(" ", "_"));
		challenge.setName(name);
		challenge.setTargetValue(10000);
		challenge.setCurrentValue(0);
		challenge.setType(ChallengeType.GROUP);
		challenge.setStartDate(LocalDateTime.now());
		challenge.setEndDate(LocalDateTime.now().plusDays(365));
		challenge.setActive(true);
		challenge.setDescription("Test description");
		challenge.setUnit("раз");
		return challenge;
	}

	@Test
	void testSaveParticipant() {
		Participant participant = new Participant("user1", "TestUser");
		participantService.saveParticipant(participant);
		verify(participantRepository).save(participant);
	}

	@Test
	void testGetParticipant_found() {
		Participant participant = new Participant("user1", "TestUser");
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		Participant result = participantService.getParticipant("user1");
		assertNotNull(result);
		assertEquals("TestUser", result.getUsername());
	}

	@Test
	void testGetParticipant_notFound() {
		when(participantRepository.findById(anyString())).thenReturn(Optional.empty());

		Participant result = participantService.getParticipant("unknownUser");
		assertNull(result);
	}

	@Test
	void testGetAllParticipants() {
		when(participantRepository.findAll()).thenReturn(Collections.emptyList());
		List<Participant> participants = participantService.getAllParticipants();
		assertNotNull(participants);
		assertTrue(participants.isEmpty());
	}

	@Test
	void testRegisterForChallenge_newParticipant() {
		when(participantRepository.findById("user1")).thenReturn(Optional.empty());

		boolean result = participantService.registerForChallenge("user1", "TestUser", "Отжимания");

		assertTrue(result);
		verify(participantRepository).save(any(Participant.class));
	}

	@Test
	void testRegisterForChallenge_existingParticipant() {
		Participant existing = new Participant("user1", "TestUser");
		when(participantRepository.findById("user1")).thenReturn(Optional.of(existing));

		boolean result = participantService.registerForChallenge("user1", "TestUser", "Отжимания");

		assertTrue(result);
		verify(participantRepository).save(existing);
	}

	@Test
	void testSaveNull() {
		// Не должен бросать исключения
		assertDoesNotThrow(() -> participantService.saveParticipant(null));
	}

	@Test
	void testIsAdminUser_legacySingleAdmin() {
		when(discordConfig.getAdminUserIds()).thenReturn(null);
		when(discordConfig.getAdminUserId()).thenReturn("admin1");

		assertTrue(participantService.isAdminUser("admin1"));
		assertFalse(participantService.isAdminUser("regular"));
	}

	@Test
	void testIsAdminUser_multipleAdmins() {
		when(discordConfig.getAdminUserIds()).thenReturn(Arrays.asList("admin1", "admin2"));

		assertTrue(participantService.isAdminUser("admin1"));
		assertTrue(participantService.isAdminUser("admin2"));
		assertFalse(participantService.isAdminUser("regular"));
	}

	@Test
	void testIsAdminUser_nullId_returnsFalse() {
		assertFalse(participantService.isAdminUser(null));
	}

	@Test
	void testIsAdminUser_emptyId_returnsFalse() {
		assertFalse(participantService.isAdminUser(""));
	}
}
