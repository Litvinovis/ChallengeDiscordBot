package com.discord.challengebot.service;

import com.discord.challengebot.command.CommandRegistry;
import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ProgressHistoryRepository;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InactivityDiscordTest {
    private final DiscordConfig config = new DiscordConfig();
    private final ChallengeService challenges = mock(ChallengeService.class);
    private final StatisticsService statistics = mock(StatisticsService.class);
    private final ProgressHistoryRepository history = mock(ProgressHistoryRepository.class);
    private final JDA jda = mock(JDA.class);
    private final Guild guild = mock(Guild.class);
    private final TextChannel channel = mock(TextChannel.class);
    private final MessageCreateAction action = mock(MessageCreateAction.class, RETURNS_SELF);
    private final DiscordService discord = new DiscordService(config, challenges, mock(ParticipantService.class), statistics,
            mock(CommandRegistry.class), history);

    @BeforeEach
    void setup() {
        config.setReportGuildId("456");
        config.setReportChannel("sport");
        ReflectionTestUtils.setField(discord, "jda", jda);
        when(jda.getGuildById("456")).thenReturn(guild);
        when(guild.getTextChannelsByName("sport", true)).thenReturn(List.of(channel));
        when(channel.sendMessage(anyString())).thenReturn(action);
    }

    @Test
    void warningPingsOnlyTargetAndWaitsForAcknowledgement() {
        assertTrue(discord.sendInactivityNotification("123", "<@123> напоминание"));
        verify(action).setAllowedMentions(java.util.Collections.emptySet());
        verify(action).mentionUsers("123");
        verify(action).timeout(15, TimeUnit.SECONDS);
        verify(action).complete();
    }

    @Test
    void failedOrMissingChannelIsNotAcknowledged() {
        when(action.complete()).thenThrow(new IllegalStateException("network unavailable"));
        assertFalse(discord.sendInactivityNotification("123", "message"));
        when(guild.getTextChannelsByName("sport", true)).thenReturn(List.of());
        assertFalse(discord.sendInactivityNotification("123", "message"));
        verify(jda, never()).getTextChannelsByName(anyString(), anyBoolean());
    }

    @Test
    void allPeriodicReportsExcludeEmptyChallenges() {
        Challenge empty = new Challenge();
        empty.setId("empty");
        empty.setActive(true);
        when(challenges.getAllChallenges()).thenReturn(List.of(empty));
        when(challenges.getActiveChallenges()).thenReturn(List.of(empty));
        discord.sendDailyReport();
        discord.sendWeeklyReport();
        discord.sendMonthlyReport();
        verifyNoInteractions(statistics, history);
        verify(challenges, never()).getChallengeStats(any());
        verify(challenges, never()).getTopParticipants(any(), anyInt());
    }

    @Test
    void emptyChallengeIsNotIncludedInDailyActivityLookup() {
        Challenge empty = new Challenge();
        empty.setId("empty"); empty.setActive(true);
        Challenge occupied = new Challenge();
        occupied.setId("occupied"); occupied.setActive(true);
        occupied.setParticipantProgress(Map.of("123", 1L));
        when(challenges.getAllChallenges()).thenReturn(List.of(empty, occupied));
        discord.sendDailyReport();
        verify(history).hasProgressLastHours(List.of("occupied"), 24);
    }
}
