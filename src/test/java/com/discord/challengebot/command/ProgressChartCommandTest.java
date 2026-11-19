package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.repository.ProgressHistoryRepository;
import com.discord.challengebot.service.IVisualizationService;
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

import java.time.LocalDate;
import java.util.Map;
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
	private ProgressHistoryRepository progressHistoryRepository;
	@Mock
	private IVisualizationService visualizationService;

	@InjectMocks
	private ProgressChartCommand command;

	@Mock
	private MessageReceivedEvent event;
	@Mock
	private MessageChannelUnion channelUnion;
	@Mock
	private MessageCreateAction messageCreateAction;

	private static final String AUTHOR_ID = "user-123";
	private static final String USERNAME = "TestUser";

	@BeforeEach
	void setUp() {
		when(event.getChannel()).thenReturn(channelUnion);
		// replyError() вызывает event.getChannel().sendMessage() — через channelUnion
		when(channelUnion.sendMessage(anyString())).thenReturn(messageCreateAction);
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
		verify(channelUnion).sendMessage(captor.capture());
		assertTrue(captor.getValue().contains("Укажите название испытания"));
	}

	@Test
	void execute_challengeNotFound_sendsError() {
		when(challengeService.getChallenge("Отжимания")).thenReturn(null);

		command.execute(event, new String[]{"график", "Отжимания"}, AUTHOR_ID, USERNAME);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(channelUnion).sendMessage(captor.capture());
		assertTrue(captor.getValue().contains("не найдено"));
	}

	@Test
	void execute_emptyImageBytes_sendsError() {
		Challenge challenge = mock(Challenge.class);

		when(challengeService.getChallenge("Отжимания")).thenReturn(challenge);
		when(challenge.getId()).thenReturn("pushups");
		when(progressHistoryRepository.getDailyTotals("pushups", AUTHOR_ID, 365)).thenReturn(Map.of());
		when(visualizationService.generateActivityCalendar(any(), any(), eq(Map.of())))
				.thenReturn(CompletableFuture.completedFuture(new byte[0]));

		command.execute(event, new String[]{"график", "Отжимания"}, AUTHOR_ID, USERNAME);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(channelUnion).sendMessage(captor.capture());
		assertTrue(captor.getValue().contains("Не удалось сгенерировать график"));
	}

	@Test
	void execute_sendsAuthorsCalendarForMultiWordChallengeName() {
		Challenge challenge = mock(Challenge.class);
		Map<LocalDate, Long> totals = Map.of(LocalDate.of(2026, 11, 17), 12L);
		byte[] fakeImage = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x00};

		when(challengeService.getChallenge("Бег на 5 км")).thenReturn(challenge);
		when(challenge.getId()).thenReturn("running");
		when(progressHistoryRepository.getDailyTotals("running", AUTHOR_ID, 365)).thenReturn(totals);
		when(visualizationService.generateActivityCalendar(any(), any(), eq(totals)))
				.thenReturn(CompletableFuture.completedFuture(fakeImage));

		MessageCreateAction sendFilesAction = mock(MessageCreateAction.class);
		when(channelUnion.sendFiles(any(FileUpload.class))).thenReturn(sendFilesAction);
		when(sendFilesAction.setContent(anyString())).thenReturn(sendFilesAction);
		doNothing().when(sendFilesAction).queue();

		command.execute(event, new String[]{"график", "Бег", "на", "5", "км"}, AUTHOR_ID, USERNAME);

		verify(progressHistoryRepository).getDailyTotals("running", AUTHOR_ID, 365);
		ArgumentCaptor<LocalDate> start = ArgumentCaptor.forClass(LocalDate.class);
		ArgumentCaptor<LocalDate> end = ArgumentCaptor.forClass(LocalDate.class);
		verify(visualizationService).generateActivityCalendar(start.capture(), end.capture(), eq(totals));
		assertEquals(end.getValue().minusDays(364), start.getValue());
		verify(sendFilesAction).setContent("📅 Личный календарь активности · **Бег на 5 км**");
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
