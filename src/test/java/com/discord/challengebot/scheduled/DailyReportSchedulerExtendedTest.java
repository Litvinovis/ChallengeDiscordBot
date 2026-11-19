package com.discord.challengebot.scheduled;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.service.ChallengeService;
import com.discord.challengebot.service.ChallengeRenewalService;
import com.discord.challengebot.service.DiscordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Extended tests for DailyReportScheduler: cleanupOldData, multiple challenges,
 * edge cases (empty lists, inactive challenges skipped).
 */
class DailyReportSchedulerExtendedTest {

	@Mock
	private DiscordService discordService;

	@Mock
	private ChallengeService challengeService;

	@Mock
	private ChallengeRepository challengeRepository;

	@Mock
	private ChallengeRenewalService renewalService;

	@InjectMocks
	private DailyReportScheduler dailyReportScheduler;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void cleanupArchivesOnlyConfirmedCompletions() {
		when(challengeRepository.findCompletedBefore(any())).thenReturn(List.of("old"));

		dailyReportScheduler.cleanupOldData();

		verify(renewalService).archiveCompleted(eq("old"), any());
		verifyNoInteractions(challengeService);
	}

	@Test
	void cleanupContinuesWhenOneArchiveFails() {
		when(challengeRepository.findCompletedBefore(any())).thenReturn(List.of("failed", "other"));
		when(renewalService.archiveCompleted(eq("failed"), any())).thenThrow(new IllegalStateException("archive"));

		dailyReportScheduler.cleanupOldData();

		verify(renewalService).archiveCompleted(eq("other"), any());
		verifyNoInteractions(challengeService);
	}

	// ---- checkChallengeCompletions — multiple challenges ----

	@Test
	void checkChallengeCompletions_multipleCompleted_allProcessed() {
		Challenge c1 = new Challenge();
		c1.setName("C1");
		c1.setTargetValue(100L);
		c1.setCurrentValue(100L);
		c1.setActive(true);

		Challenge c2 = new Challenge();
		c2.setName("C2");
		c2.setTargetValue(100L);
		c2.setCurrentValue(100L);
		c2.setActive(true);

		when(challengeRepository.findDue(any())).thenReturn(Arrays.asList(c1, c2));
		when(challengeService.completeChallenge(any())).thenReturn(true);

		dailyReportScheduler.checkChallengeCompletions();

		verify(challengeService).completeChallenge(c1);
		verify(challengeService).completeChallenge(c2);
		verify(discordService).sendChallengeCompletionNotification(c1);
		verify(discordService).sendChallengeCompletionNotification(c2);
	}


	@Test
	void checkChallengeCompletions_emptyList_doesNothing() {
		when(challengeRepository.findDue(any())).thenReturn(Collections.emptyList());

		dailyReportScheduler.checkChallengeCompletions();

		verify(challengeService, never()).completeChallenge(any());
		verify(discordService, never()).sendChallengeCompletionNotification(any());
		verify(discordService, never()).sendChallengeFailureNotification(any());
	}

	@Test
	void checkChallengeCompletions_goalExceeded_treatedAsCompleted() {
		Challenge overachieved = new Challenge();
		overachieved.setName("Overachiever");
		overachieved.setTargetValue(100L);
		overachieved.setCurrentValue(150L); // exceeded target
		overachieved.setActive(true);

		when(challengeRepository.findDue(any())).thenReturn(List.of(overachieved));
		when(challengeService.completeChallenge(any())).thenReturn(true);

		dailyReportScheduler.checkChallengeCompletions();

		verify(challengeService).completeChallenge(overachieved);
		verify(discordService).sendChallengeCompletionNotification(overachieved);
	}
}
