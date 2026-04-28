package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для ParticipantService (переименованные из UserServiceTest после рефакторинга).
 * UserService удалён — его функциональность объединена в ParticipantService.
 */
class UserServiceTest {

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

	@Test
	void testIsAdminUserWhenAdmin() {
		String adminUserId = "12345";
		when(discordConfig.getAdminUserIds()).thenReturn(null);
		when(discordConfig.getAdminUserId()).thenReturn(adminUserId);

		boolean isAdmin = participantService.isAdminUser(adminUserId);

		assertTrue(isAdmin);
		verify(discordConfig, atLeastOnce()).getAdminUserId();
	}

	@Test
	void testIsAdminUserWhenNotAdmin() {
		String adminUserId = "12345";
		String regularUserId = "67890";
		when(discordConfig.getAdminUserIds()).thenReturn(null);
		when(discordConfig.getAdminUserId()).thenReturn(adminUserId);

		boolean isAdmin = participantService.isAdminUser(regularUserId);

		assertFalse(isAdmin);
		verify(discordConfig, atLeastOnce()).getAdminUserId();
	}

	@Test
	void testRegisterForChallenge() {
		String userId = "12345";
		String username = "testuser";
		String challengeName = "Отжимания";

		when(participantRepository.findById(userId)).thenReturn(Optional.empty());

		boolean result = participantService.registerForChallenge(userId, username, challengeName);

		assertTrue(result);
		verify(participantRepository).findById(userId);
		verify(participantRepository).save(any(Participant.class));
	}

	@Test
	void testUnregisterFromChallenge() {
		String userId = "12345";
		String challengeName = "Отжимания";

		Participant participant = new Participant(userId, "testuser");
		participant.addChallenge(challengeName);
		when(participantRepository.findById(userId)).thenReturn(Optional.of(participant));

		boolean result = participantService.unregisterFromChallenge(userId, challengeName);

		assertTrue(result);
		verify(participantRepository).findById(userId);
		verify(participantRepository).save(any(Participant.class));
	}
}
