package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IStatisticsService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StatsCommandInactivityTest {
    @Test
    void summaryNeverFormatsEmptyChallenges() {
        IChallengeService service = mock(IChallengeService.class);
        IStatisticsService statistics = mock(IStatisticsService.class);
        var event = mock(MessageReceivedEvent.class, RETURNS_DEEP_STUBS);
        TextChannel channel = mock(TextChannel.class);
        when(event.getChannel().asTextChannel()).thenReturn(channel);
        when(channel.sendMessage(anyString())).thenReturn(mock(MessageCreateAction.class));
        Challenge empty = new Challenge();
        empty.setName("Пустое испытание");
        when(service.getAllChallenges()).thenReturn(List.of(empty));
        StatsCommand command = new StatsCommand();
        ReflectionTestUtils.setField(command, "challengeService", service);
        ReflectionTestUtils.setField(command, "statisticsService", statistics);

        command.execute(event, new String[]{"статистика"}, "123", "Участник");

        verify(service, never()).getChallengeStats(any());
        verifyNoInteractions(statistics);
        verify(channel).sendMessage("Нет доступных испытаний.");
    }
}
