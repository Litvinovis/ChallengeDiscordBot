package com.discord.challengebot.scheduled;

import com.discord.challengebot.service.DiscordService;
import com.discord.challengebot.service.ChallengeRenewalService;
import com.discord.challengebot.service.ChallengeService;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class DailyReportSchedulerTest {

	@Mock
	private DiscordService discordService;

	@Mock
	private ChallengeService challengeService;

	@Mock
	private ChallengeRenewalService renewalService;

	@Mock
	private ChallengeRepository challengeRepository;

	@InjectMocks
	private DailyReportScheduler dailyReportScheduler;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testSendDailyProgressReports() {
		// When
		dailyReportScheduler.sendDailyProgressReports();

		// Then
		verify(discordService, times(1)).sendDailyReport();
	}

	@Test
	void testCheckChallengeCompletions_GoalReachedBeforeDeadlineWaits() {
		// Given
		Challenge challenge = new Challenge();
		challenge.setName("Тестовое испытание");
		challenge.setTargetValue(100);
		challenge.setCurrentValue(100); // Цель достигнута
		challenge.setActive(true);
		challenge.setEndDate(LocalDateTime.now().plusDays(1)); // Еще не просрочено

		when(challengeRepository.findDue(any())).thenReturn(List.of(challenge));

		// When
		dailyReportScheduler.checkChallengeCompletions();

		// Then
		verify(challengeService, never()).completeChallenge(any());
		verifyNoInteractions(renewalService, discordService);
		verify(discordService, never()).sendChallengeFailureNotification(any());
	}

	@Test
	void testCheckChallengeCompletions_DeadlineTriggersRenewal() {
		Challenge challenge = new Challenge();
		challenge.setId("test");
		challenge.setActive(true);
		challenge.setEndDate(LocalDateTime.of(2000, 7, 1, 12, 0));
		var renewal = new ChallengeRenewalService.Renewal(challenge, 700,
				LocalDateTime.of(2040, 7, 1, 12, 0), false);
		when(challengeRepository.findDue(any())).thenReturn(List.of(challenge));
		when(renewalService.renewIfDue(eq("test"), any())).thenReturn(Optional.of(renewal));

		dailyReportScheduler.checkChallengeCompletions();

		verify(discordService).sendChallengeRenewalNotification(renewal);
		verify(challengeService, never()).completeChallenge(any());
	}

	@Test
	void testCheckChallengeCompletions_ActiveNotCompleted() {
		// Given
		Challenge challenge = new Challenge();
		challenge.setName("Тестовое испытание");
		challenge.setTargetValue(100);
		challenge.setCurrentValue(50); // Цель не достигнута
		challenge.setActive(true);
		challenge.setEndDate(LocalDateTime.now().plusDays(1)); // Не просрочено

		when(challengeRepository.findDue(any())).thenReturn(List.of(challenge));

		// When
		dailyReportScheduler.checkChallengeCompletions();

		// Then
		verify(challengeService, never()).completeChallenge(any());
		verify(discordService, never()).sendChallengeCompletionNotification(any());
		verify(discordService, never()).sendChallengeFailureNotification(any());
	}
}