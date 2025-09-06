package com.discord.challengebot.scheduled;

import com.discord.challengebot.service.DiscordService;
import com.discord.challengebot.service.ChallengeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
}