package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.AchievementService;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.StatisticsService;
import com.discord.challengebot.service.StreakService;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для DefaultProgressCommand.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultProgressCommandTest {

    @Mock
    private IChallengeService challengeService;

    @Mock
    private AchievementService achievementService;

    @Mock
    private StreakService streakService;

    @Mock
    private StatisticsService statisticsService;

    @InjectMocks
    private DefaultProgressCommand command;

    @Mock
    private MessageReceivedEvent event;

    @Mock
    private MessageChannelUnion channelUnion;

    @Mock
    private TextChannel textChannel;

    @Mock
    private MessageCreateAction messageCreateAction;

    private Challenge challenge;

    @BeforeEach
    void setUp() {
        challenge = new Challenge();
        challenge.setId("отжимания");
        challenge.setName("отжимания");
        challenge.setTargetValue(1000L);
        challenge.setCurrentValue(50L);
        challenge.setActive(true);
        challenge.setParticipantProgress(new ConcurrentHashMap<>());
        challenge.getParticipantProgress().put("user1", 50L);

        when(event.getChannel()).thenReturn(channelUnion);
        when(channelUnion.getType()).thenReturn(ChannelType.TEXT);
        when(channelUnion.asTextChannel()).thenReturn(textChannel);
        when(textChannel.sendMessage(anyString())).thenReturn(messageCreateAction);
        doNothing().when(messageCreateAction).queue();
    }

    @Test
    void execute_negativeAmount_callsSubtractProgress() {
        when(challengeService.getChallenge("отжимания")).thenReturn(challenge);
        when(challengeService.subtractProgress(eq(challenge), eq("user1"), eq("alice"), eq(5L)))
            .thenReturn(challenge);

        command.execute(event, new String[]{"отжимания", "-5"}, "user1", "alice");

        verify(challengeService).subtractProgress(challenge, "user1", "alice", 5L);
        verify(challengeService, never()).addProgress(any(), anyString(), anyString(), anyLong());
    }

    @Test
    void execute_positiveAmount_callsAddProgress() {
        when(challengeService.getChallenge("отжимания")).thenReturn(challenge);
        when(challengeService.addProgress(eq(challenge), eq("user1"), eq("alice"), eq(10L)))
            .thenReturn(challenge);

        command.execute(event, new String[]{"отжимания", "10"}, "user1", "alice");

        verify(challengeService).addProgress(challenge, "user1", "alice", 10L);
        verify(challengeService, never()).subtractProgress(any(), anyString(), anyString(), anyLong());
    }

    @Test
    void execute_zeroAmount_sendsErrorMessage() {
        command.execute(event, new String[]{"отжимания", "0"}, "user1", "alice");

        verify(textChannel).sendMessage(contains("нулю"));
        verify(challengeService, never()).addProgress(any(), anyString(), anyString(), anyLong());
        verify(challengeService, never()).subtractProgress(any(), anyString(), anyString(), anyLong());
    }

    @Test
    void execute_negativeAmount_doesNotCallStreakOrAchievements() {
        when(challengeService.getChallenge("отжимания")).thenReturn(challenge);
        when(challengeService.subtractProgress(any(), anyString(), anyString(), anyLong()))
            .thenReturn(challenge);

        command.execute(event, new String[]{"отжимания", "-5"}, "user1", "alice");

        verify(streakService, never()).recordActivity(anyString());
        verify(achievementService, never()).checkAndAwardAchievements(anyString(), anyString(), anyLong());
    }
}
