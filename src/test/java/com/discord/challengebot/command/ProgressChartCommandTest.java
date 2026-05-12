package com.discord.challengebot.command;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IStatisticsService;
import com.discord.challengebot.service.IVisualizationService;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ProgressChartCommand}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProgressChartCommandTest {

	@Mock
	private IChallengeService challengeService;
	@Mock
	private IStatisticsService statisticsService;
	@Mock
	private IVisualizationService visualizationService;

	@InjectMocks
	private ProgressChartCommand command;

	@Mock
	private MessageReceivedEvent event;
	@Mock
	private MessageChannelUnion channelUnion;
	@Mock
	private TextChannel textChannel;
	@Mock
	private MessageCreateAction messageCreateAction;

	private static final String AUTHOR_ID = "user-123";
	private static final String USERNAME = "TestUser";

	@BeforeEach
	void setUp() {
		when(event.getChannel()).thenReturn(channelUnion);
		when(channelUnion.getType()).thenReturn(ChannelType.TEXT);
		when(channelUnion.asTextChannel()).thenReturn(textChannel);
		when(textChannel.sendMessage(anyString())).thenReturn(messageCreateAction);
		doNothing().when(messageCreateAction).queue();
	}

	@Test
	void canHandle_returnsTrue_forГрафик() {
		assertTrue(command.canHandle("график"));
	}

	@Test
	void canHandle_returnsFalse_forOtherCommands() {
		assertFalse(command.canHandle("статистика"));
		assertFalse(command.canHandle("помощь"));
		assertFalse(command.canHandle("chart"));
	}

	@Test
	void execute_noArgs_sendsError() {
		command.execute(event, new String[]{"график"}, AUTHOR_ID, USERNAME);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(textChannel).sendMessage(captor.capture());
		assertTrue(captor.getValue().contains("Укажите название испытания"));
	}

	@Test
	void execute_challengeNotFound_sendsError() {
		when(challengeService.getChallenge("Отжимания")).thenReturn(null);

		command.execute(event, new String[]{"график", "Отжимания"}, AUTHOR_ID, USERNAME);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(textChannel).sendMessage(captor.capture());
		assertTrue(captor.getValue().contains("не найдено"));
	}

	@Test
	void execute_statsNull_sendsError() {
		Challenge challenge = mock(Challenge.class);
		when(challengeService.getChallenge("Отжимания")).thenReturn(challenge);
		when(statisticsService.calculateStats(challenge)).thenReturn(null);

		command.execute(event, new String[]{"график", "Отжимания"}, AUTHOR_ID, USERNAME);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(textChannel).sendMessage(captor.capture());
		assertTrue(captor.getValue().contains("Не удалось получить статистику"));
	}

	@Test
	void execute_emptyImageBytes_sendsError() throws Exception {
		Challenge challenge = mock(Challenge.class);
		ChallengeStats stats = new ChallengeStats("Отжимания", 1000L, 500L, 500L, 50.0, 10.0, 50);

		when(challengeService.getChallenge("Отжимания")).thenReturn(challenge);
		when(statisticsService.calculateStats(challenge)).thenReturn(stats);
		when(visualizationService.generateProgressChart(stats))
				.thenReturn(CompletableFuture.completedFuture(new byte[0]));

		command.execute(event, new String[]{"график", "Отжимания"}, AUTHOR_ID, USERNAME);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(textChannel).sendMessage(captor.capture());
		assertTrue(captor.getValue().contains("Не удалось сгенерировать график"));
	}

	@Test
	void execute_multiWordChallengeName_joinsCorrectly() throws Exception {
		Challenge challenge = mock(Challenge.class);
		ChallengeStats stats = new ChallengeStats("Бег на 5 км", 100L, 80L, 20L, 80.0, 2.0, 10);
		byte[] fakeImage = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x00};

		when(challengeService.getChallenge("Бег на 5 км")).thenReturn(challenge);
		when(statisticsService.calculateStats(challenge)).thenReturn(stats);
		when(visualizationService.generateProgressChart(stats))
				.thenReturn(CompletableFuture.completedFuture(fakeImage));

		MessageCreateAction sendFilesAction = mock(MessageCreateAction.class);
		when(textChannel.sendFiles(any(FileUpload.class))).thenReturn(sendFilesAction);
		when(sendFilesAction.setContent(anyString())).thenReturn(sendFilesAction);
		doNothing().when(sendFilesAction).queue();

		command.execute(event, new String[]{"график", "Бег", "на", "5", "км"}, AUTHOR_ID, USERNAME);

		verify(challengeService).getChallenge("Бег на 5 км");
		verify(sendFilesAction).queue();
	}

	@Test
	void execute_doesNotThrow_onUnexpectedException() {
		when(challengeService.getChallenge(anyString())).thenThrow(new RuntimeException("unexpected"));

		assertDoesNotThrow(() ->
				command.execute(event, new String[]{"график", "Отжимания"}, AUTHOR_ID, USERNAME)
		);
	}
}
