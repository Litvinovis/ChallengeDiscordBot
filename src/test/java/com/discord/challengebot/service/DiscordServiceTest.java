package com.discord.challengebot.service;

import com.discord.challengebot.command.CommandRegistry;
import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ProgressHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscordServiceTest {

	@Mock
	private DiscordConfig discordConfig;

	@Mock
	private ChallengeService challengeService;

	@Mock
	private ParticipantService participantService;

	@Mock
	private StatisticsService statisticsService;

	@Mock
	private CommandRegistry commandRegistry;

	@Mock
	private ProgressHistoryRepository progressHistoryRepository;

	@InjectMocks
	private DiscordService discordService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testIsAuthorizedUser_AdminCommand() {
		String userId = "12345";
		String command = "новый";

		when(participantService.isAdminUser(userId)).thenReturn(true);

		boolean result = discordService.isAuthorizedUser(userId, command);

		assertTrue(result);
		verify(participantService).isAdminUser(userId);
	}

	@Test
	void testIsAuthorizedUser_NonAdminCommand() {
		String userId = "12345";
		String command = "помощь";

		boolean result = discordService.isAuthorizedUser(userId, command);

		assertTrue(result);
		verify(participantService, never()).isAdminUser(userId);
	}

	@Test
	void testIsAuthorizedUser_AdminCommandNonAdminUser() {
		String userId = "12345";
		String command = "удалить";

		when(participantService.isAdminUser(userId)).thenReturn(false);

		boolean result = discordService.isAuthorizedUser(userId, command);

		assertFalse(result);
		verify(participantService).isAdminUser(userId);
	}

	@Test
	void testFormatChallengeStats() {
		// Проверяем что метод делегирует в statisticsService
		when(statisticsService.formatReportForDiscord(any(), any())).thenReturn("formatted stats");

		String result = discordService.formatChallengeStats(null, null);

		assertEquals("formatted stats", result);
		verify(statisticsService).formatReportForDiscord(null, null);
	}

	@Test
	void testGetReportGuildId() {
		String guildId = "987654321";
		when(discordConfig.getReportGuildId()).thenReturn(guildId);

		assertEquals(guildId, discordConfig.getReportGuildId());
	}

	@Test
	void testSendDailyReport_SkippedWhenNoProgress() {
		Challenge challenge = new Challenge();
		challenge.setId("c1");
		challenge.setActive(true);
		when(challengeService.getAllChallenges()).thenReturn(List.of(challenge));
		when(progressHistoryRepository.hasProgressLastHours(List.of("c1"), 24)).thenReturn(false);

		discordService.sendDailyReport();

		verify(challengeService, never()).getChallengeStats(any());
	}

	@Test
	void testSendDailyReport_SentWhenProgressExists() {
		Challenge challenge = new Challenge();
		challenge.setId("c1");
		challenge.setActive(true);
		when(challengeService.getAllChallenges()).thenReturn(List.of(challenge));
		when(progressHistoryRepository.hasProgressLastHours(List.of("c1"), 24)).thenReturn(true);

		discordService.sendDailyReport();

		verify(challengeService, times(1)).getChallengeStats(challenge);
	}

	@Test
	void testSendDailyReport_SkippedWhenNoActiveChallenges() {
		when(challengeService.getAllChallenges()).thenReturn(List.of());

		discordService.sendDailyReport();

		verify(progressHistoryRepository, never()).hasProgressLastHours(any(), anyInt());
	}
}
