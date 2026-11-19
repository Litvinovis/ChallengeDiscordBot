package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.repository.ChallengeProgressRepository.InactivityState;
import com.discord.challengebot.repository.ChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChallengeInactivityServiceTest {
    private final Instant now = Instant.parse("2026-11-16T06:00:00Z");
    private final ChallengeRepository challenges = mock(ChallengeRepository.class);
    private final ChallengeProgressRepository progress = mock(ChallengeProgressRepository.class);
    private final ChallengeService challengeService = mock(ChallengeService.class);
    private final DiscordService discord = mock(DiscordService.class);
    private final org.springframework.transaction.PlatformTransactionManager transactions = mock(org.springframework.transaction.PlatformTransactionManager.class);
    private final ChallengeInactivityService service = new ChallengeInactivityService(challenges, progress, challengeService, discord, transactions);
    private Challenge challenge;

    @BeforeEach
    void setup() {
        challenge = new Challenge();
        challenge.setId("pushups");
        challenge.setName("Отжимания");
        challenge.setActive(true);
        challenge.setTargetValue(10000);
        when(challenges.findById("pushups")).thenReturn(Optional.of(challenge));
        when(discord.sendInactivityNotification(anyString(), anyString())).thenReturn(true);
    }

    private void state(Instant last, Instant first, Instant second) {
        var states = List.of(new InactivityState("123", last, first, second));
        when(progress.findInactivityStates("pushups")).thenReturn(states);
        when(progress.findInactivityState("pushups", "123")).thenReturn(states);
    }

    @Test
    void beforeFourteenDaysDoesNothing() {
        state(now.minus(Duration.ofDays(14)).plusSeconds(1), null, null);
        service.checkChallenge("pushups", now);
        verifyNoInteractions(discord, challengeService);
    }

    @Test
    void fourteenDaysSendsMentionAndPersistsFirstWarning() {
        state(now.minus(Duration.ofDays(14)), null, null);
        service.checkChallenge("pushups", now);
        var order = inOrder(discord, progress);
        order.verify(discord).sendInactivityNotification(eq("123"), contains("<@123>"));
        order.verify(progress).markInactivityWarning("pushups", "123", false, now);
        verifyNoInteractions(challengeService);
    }

    @Test
    void failedDeliveryDoesNotAdvanceOrExclude() {
        state(now.minus(Duration.ofDays(60)), null, null);
        when(discord.sendInactivityNotification(anyString(), anyString())).thenReturn(false);
        service.checkChallenge("pushups", now);
        verify(progress, never()).markInactivityWarning(anyString(), anyString(), anyBoolean(), any());
        verifyNoInteractions(challengeService);
    }

    @Test
    void doesNotRepeatFirstWarningBeforeSevenDays() {
        state(now.minus(Duration.ofDays(20)), now.minus(Duration.ofDays(7)).plusSeconds(1), null);
        service.checkChallenge("pushups", now);
        verifyNoInteractions(discord, challengeService);
    }

    @Test
    void twentyOneDaysSendsFinalWarning() {
        state(now.minus(Duration.ofDays(21)), now.minus(Duration.ofDays(7)), null);
        service.checkChallenge("pushups", now);
        verify(discord).sendInactivityNotification(eq("123"), contains("последнее напоминание"));
        verify(progress).markInactivityWarning("pushups", "123", true, now);
        verifyNoInteractions(challengeService);
    }

    @Test
    void failedSecondWarningDoesNotStartExclusionCountdown() {
        state(now.minus(Duration.ofDays(40)), now.minus(Duration.ofDays(26)), null);
        when(discord.sendInactivityNotification(anyString(), anyString())).thenReturn(false);
        service.checkChallenge("pushups", now);
        verify(progress, never()).markInactivityWarning(anyString(), anyString(), anyBoolean(), any());
        verifyNoInteractions(challengeService);
    }

    @Test
    void beforeTwentyEightDaysDoesNotExclude() {
        state(now.minus(Duration.ofDays(28)), now.minus(Duration.ofDays(14)), now.minus(Duration.ofDays(7)).plusSeconds(1));
        service.checkChallenge("pushups", now);
        verifyNoInteractions(discord, challengeService);
    }

    @Test
    void twentyEightDaysExcludesFromThisChallenge() {
        state(now.minus(Duration.ofDays(28)), now.minus(Duration.ofDays(14)), now.minus(Duration.ofDays(7)));
        service.checkChallenge("pushups", now);
        verify(challengeService).removeParticipant(challenge, "123");
        verify(discord).sendInactivityNotification(eq("123"), contains("вы исключены"));
    }

    @Test
    void longDowntimeNeverSkipsFirstWarning() {
        state(now.minus(Duration.ofDays(100)), null, null);
        service.checkChallenge("pushups", now);
        verify(progress).markInactivityWarning("pushups", "123", false, now);
        verifyNoInteractions(challengeService);
    }

    @Test
    void overdueChecksStillGiveAWeekAfterSecondWarning() {
        state(now.minus(Duration.ofDays(100)), now.minus(Duration.ofDays(80)), now.minus(Duration.ofDays(6)));
        service.checkChallenge("pushups", now);
        verifyNoInteractions(discord, challengeService);
    }

    @Test
    void emptyChallengeIsStopped() {
        when(progress.findInactivityStates("pushups")).thenReturn(List.of());
        service.checkChallenge("pushups", now);
        verify(challenges).stopIfEmpty("pushups");
        verifyNoInteractions(discord);
    }

    @Test
    void pausedOrExpiredOrCompletedChallengesDoNotWarn() {
        challenge.setActive(false);
        service.checkChallenge("pushups", now);
        challenge.setActive(true);
        challenge.setEndDate(LocalDateTime.of(2026, 11, 1, 0, 0));
        service.checkChallenge("pushups", now);
        challenge.setEndDate(null);
        challenge.setCurrentValue(10000);
        service.checkChallenge("pushups", now);
        verifyNoInteractions(progress, discord, challengeService);
    }
}
