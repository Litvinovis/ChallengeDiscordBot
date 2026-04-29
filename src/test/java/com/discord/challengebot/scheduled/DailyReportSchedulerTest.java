package com.discord.challengebot.scheduled;

import com.discord.challengebot.service.DiscordService;
import com.discord.challengebot.service.ChallengeService;
import com.discord.challengebot.model.Challenge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

class DailyReportSchedulerTest {

	@Mock
	private DiscordService discordService;

	@Mock
	private ChallengeService challengeService;

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
	void testCheckChallengeCompletions_GoalReached() {
		// Given
		Challenge challenge = new Challenge();
		challenge.setName("Тестовое испытание");
		challenge.setTargetValue(100);
		challenge.setCurrentValue(100); // Цель достигнута
		challenge.setActive(true);
		challenge.setEndDate(LocalDateTime.now().plusDays(1)); // Еще не просрочено

		when(challengeService.getAllChallenges()).thenReturn(List.of(challenge));

		// When
		dailyReportScheduler.checkChallengeCompletions();

		// Then
		verify(challengeService, times(1)).completeChallenge(challenge);
		verify(discordService, times(1)).sendChallengeCompletionNotification(challenge);
		verify(discordService, never()).sendChallengeFailureNotification(any());
	}

	@Test
	void testCheckChallengeCompletions_DeadlineReachedWithoutGoal() {
		// Given
		Challenge challenge = new Challenge();
		challenge.setName("Тестовое испытание");
		challenge.setTargetValue(100);
		challenge.setCurrentValue(50); // Цель не достигнута
		challenge.setActive(true);
		challenge.setEndDate(LocalDateTime.now().minusDays(1)); // Просрочено

		when(challengeService.getAllChallenges()).thenReturn(List.of(challenge));

		// When
		dailyReportScheduler.checkChallengeCompletions();

		// Then
		verify(challengeService, times(1)).completeChallenge(challenge);
		verify(discordService, times(1)).sendChallengeFailureNotification(challenge);
		verify(discordService, never()).sendChallengeCompletionNotification(any());
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

		when(challengeService.getAllChallenges()).thenReturn(List.of(challenge));

		// When
		dailyReportScheduler.checkChallengeCompletions();

		// Then
		verify(challengeService, never()).completeChallenge(any());
		verify(discordService, never()).sendChallengeCompletionNotification(any());
		verify(discordService, never()).sendChallengeFailureNotification(any());
	}
}