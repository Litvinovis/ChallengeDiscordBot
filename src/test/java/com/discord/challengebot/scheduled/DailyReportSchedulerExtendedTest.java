package com.discord.challengebot.scheduled;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.ChallengeService;
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

    @InjectMocks
    private DailyReportScheduler dailyReportScheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---- cleanupOldData ----

    @Test
    void cleanupOldData_deletesInactiveChallengesOlderThan30Days() {
        Challenge old = new Challenge();
        old.setName("OldChallenge");
        old.setActive(false);
        old.setEndDate(LocalDateTime.now().minusDays(31));

        when(challengeService.getAllChallenges()).thenReturn(List.of(old));
        when(challengeService.deleteChallenge("OldChallenge")).thenReturn(true);

        dailyReportScheduler.cleanupOldData();

        verify(challengeService).deleteChallenge("OldChallenge");
    }

    @Test
    void cleanupOldData_doesNotDeleteActiveChallenges() {
        Challenge active = new Challenge();
        active.setName("ActiveChallenge");
        active.setActive(true);
        active.setEndDate(LocalDateTime.now().minusDays(31));

        when(challengeService.getAllChallenges()).thenReturn(List.of(active));

        dailyReportScheduler.cleanupOldData();

        verify(challengeService, never()).deleteChallenge(anyString());
    }

    @Test
    void cleanupOldData_doesNotDeleteRecentInactiveChallenges() {
        Challenge recent = new Challenge();
        recent.setName("RecentCompleted");
        recent.setActive(false);
        recent.setEndDate(LocalDateTime.now().minusDays(5)); // within 30 days

        when(challengeService.getAllChallenges()).thenReturn(List.of(recent));

        dailyReportScheduler.cleanupOldData();

        verify(challengeService, never()).deleteChallenge(anyString());
    }

    @Test
    void cleanupOldData_emptyList_doesNothing() {
        when(challengeService.getAllChallenges()).thenReturn(Collections.emptyList());

        dailyReportScheduler.cleanupOldData();

        verify(challengeService, never()).deleteChallenge(anyString());
    }

    @Test
    void cleanupOldData_mixedChallenges_deletesOnlyEligible() {
        Challenge oldCompleted = new Challenge();
        oldCompleted.setName("OldDone");
        oldCompleted.setActive(false);
        oldCompleted.setEndDate(LocalDateTime.now().minusDays(40));

        Challenge recentCompleted = new Challenge();
        recentCompleted.setName("RecentDone");
        recentCompleted.setActive(false);
        recentCompleted.setEndDate(LocalDateTime.now().minusDays(10));

        Challenge stillActive = new Challenge();
        stillActive.setName("Active");
        stillActive.setActive(true);
        stillActive.setEndDate(LocalDateTime.now().plusDays(10));

        when(challengeService.getAllChallenges())
                .thenReturn(Arrays.asList(oldCompleted, recentCompleted, stillActive));
        when(challengeService.deleteChallenge("OldDone")).thenReturn(true);

        dailyReportScheduler.cleanupOldData();

        verify(challengeService, times(1)).deleteChallenge("OldDone");
        verify(challengeService, never()).deleteChallenge("RecentDone");
        verify(challengeService, never()).deleteChallenge("Active");
    }

    // ---- checkChallengeCompletions — multiple challenges ----

    @Test
    void checkChallengeCompletions_multipleCompleted_allProcessed() {
        Challenge c1 = new Challenge();
        c1.setName("C1");
        c1.setTargetValue(100L);
        c1.setCurrentValue(100L);
        c1.setActive(true);
        c1.setEndDate(LocalDateTime.now().plusDays(1));

        Challenge c2 = new Challenge();
        c2.setName("C2");
        c2.setTargetValue(100L);
        c2.setCurrentValue(100L);
        c2.setActive(true);
        c2.setEndDate(LocalDateTime.now().plusDays(1));

        when(challengeService.getAllChallenges()).thenReturn(Arrays.asList(c1, c2));

        dailyReportScheduler.checkChallengeCompletions();

        verify(challengeService).completeChallenge(c1);
        verify(challengeService).completeChallenge(c2);
        verify(discordService).sendChallengeCompletionNotification(c1);
        verify(discordService).sendChallengeCompletionNotification(c2);
    }

    @Test
    void checkChallengeCompletions_inactiveChallenge_isSkipped() {
        Challenge inactive = new Challenge();
        inactive.setName("InactiveChallenge");
        inactive.setTargetValue(100L);
        inactive.setCurrentValue(100L);
        inactive.setActive(false); // already inactive
        inactive.setEndDate(LocalDateTime.now().plusDays(1));

        when(challengeService.getAllChallenges()).thenReturn(List.of(inactive));

        dailyReportScheduler.checkChallengeCompletions();

        verify(challengeService, never()).completeChallenge(any());
        verify(discordService, never()).sendChallengeCompletionNotification(any());
        verify(discordService, never()).sendChallengeFailureNotification(any());
    }

    @Test
    void checkChallengeCompletions_emptyList_doesNothing() {
        when(challengeService.getAllChallenges()).thenReturn(Collections.emptyList());

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
        overachieved.setEndDate(LocalDateTime.now().plusDays(5));

        when(challengeService.getAllChallenges()).thenReturn(List.of(overachieved));

        dailyReportScheduler.checkChallengeCompletions();

        verify(challengeService).completeChallenge(overachieved);
        verify(discordService).sendChallengeCompletionNotification(overachieved);
    }
}
