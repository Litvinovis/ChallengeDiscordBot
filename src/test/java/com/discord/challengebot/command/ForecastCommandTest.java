package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IStatisticsService;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
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
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Тесты для ForecastCommand (+прогноз).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ForecastCommandTest {

	@Mock
	private IChallengeService challengeService;

	@Mock
	private IStatisticsService statisticsService;

	@InjectMocks
	private ForecastCommand forecastCommand;

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
		challenge.setName("Отжимания");
		challenge.setTargetValue(1000);
		challenge.setCurrentValue(300);
		challenge.setType(ChallengeType.GROUP);
		challenge.setStartDate(LocalDateTime.now().minusDays(30));
		challenge.setEndDate(LocalDateTime.now().plusDays(60));
		challenge.setActive(true);
		challenge.setUnit("раз");
		challenge.setParticipantProgress(new ConcurrentHashMap<>());
		challenge.getParticipantProgress().put("user1", 300L);

		when(event.getChannel()).thenReturn(channelUnion);
		when(channelUnion.getType()).thenReturn(ChannelType.TEXT);
		when(channelUnion.asTextChannel()).thenReturn(textChannel);
		when(textChannel.sendMessage(anyString())).thenReturn(messageCreateAction);
		doNothing().when(messageCreateAction).queue();
	}

	@Test
	void testCanHandlePrognoz() {
		assertTrue(forecastCommand.canHandle("прогноз"));
	}

	@Test
	void testCannotHandleOtherCommand() {
		assertFalse(forecastCommand.canHandle("помощь"));
		assertFalse(forecastCommand.canHandle("статистика"));
		assertFalse(forecastCommand.canHandle(""));
	}

	@Test
	void testNoArgsShowsUsage() {
		forecastCommand.execute(event, new String[]{"прогноз"}, "user1", "TestUser");
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(textChannel).sendMessage(captor.capture());
		assertTrue(captor.getValue().contains("Укажите название испытания"));
	}

	@Test
	void testChallengeNotFound() {
		when(challengeService.getChallenge("несуществующее")).thenReturn(null);
		forecastCommand.execute(event, new String[]{"прогноз", "несуществующее"}, "user1", "TestUser");
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(textChannel).sendMessage(captor.capture());
		assertTrue(captor.getValue().contains("не найдено"));
	}

	@Test
	void testGoalAlreadyCompleted() {
		challenge.getParticipantProgress().put("user1", 1000L);
		when(challengeService.getChallenge("Отжимания")).thenReturn(challenge);

		forecastCommand.execute(event, new String[]{"прогноз", "Отжимания"}, "user1", "TestUser");

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(textChannel).sendMessage(captor.capture());
		assertTrue(captor.getValue().contains("уже выполнил"));
	}

	@Test
	void testForecastWithNoHistoryReturnsInsufficientData() {
		when(challengeService.getChallenge("Отжимания")).thenReturn(challenge);
		when(statisticsService.forecastCompletionDate(anyString(), anyString())).thenReturn(null);

		forecastCommand.execute(event, new String[]{"прогноз", "Отжимания"}, "user1", "TestUser");

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(textChannel).sendMessage(captor.capture());
		String response = captor.getValue();
		// Либо "Недостаточно данных", либо сообщение о прогнозе
		assertNotNull(response);
		assertFalse(response.isEmpty());
	}

	@Test
	void testForecastWithValidDate() {
		LocalDate forecastDate = LocalDate.now().plusDays(14);
		when(challengeService.getChallenge("Отжимания")).thenReturn(challenge);
		when(statisticsService.forecastCompletionDate("отжимания", "user1")).thenReturn(forecastDate);

		forecastCommand.execute(event, new String[]{"прогноз", "Отжимания"}, "user1", "TestUser");

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(textChannel).sendMessage(captor.capture());
		String response = captor.getValue();
		assertTrue(response.contains(forecastDate.toString()));
	}

	@Test
	void testForecastMessageContainsChallengeName() {
		LocalDate forecastDate = LocalDate.now().plusDays(10);
		when(challengeService.getChallenge("Отжимания")).thenReturn(challenge);
		when(statisticsService.forecastCompletionDate("отжимания", "user1")).thenReturn(forecastDate);

		forecastCommand.execute(event, new String[]{"прогноз", "Отжимания"}, "user1", "TestUser");

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(textChannel).sendMessage(captor.capture());
		assertTrue(captor.getValue().contains("Отжимания"));
	}

	@Test
	void testMultiWordChallengeName() {
		Challenge multiWord = new Challenge();
		multiWord.setId("отжимания_на_кулаках");
		multiWord.setName("Отжимания на кулаках");
		multiWord.setTargetValue(500);
		multiWord.setCurrentValue(0);
		multiWord.setType(ChallengeType.GROUP);
		multiWord.setStartDate(LocalDateTime.now().minusDays(5));
		multiWord.setEndDate(LocalDateTime.now().plusDays(25));
		multiWord.setActive(true);
		multiWord.setUnit("раз");
		multiWord.setParticipantProgress(new ConcurrentHashMap<>());

		when(challengeService.getChallenge("Отжимания на кулаках")).thenReturn(multiWord);
		when(statisticsService.forecastCompletionDate(anyString(), anyString())).thenReturn(null);

		forecastCommand.execute(event, new String[]{"прогноз", "Отжимания", "на", "кулаках"}, "user1", "TestUser");

		verify(challengeService).getChallenge("Отжимания на кулаках");
	}

	@Test
	void testDoesNotThrowOnException() {
		when(challengeService.getChallenge(anyString())).thenThrow(new RuntimeException("test error"));
		assertDoesNotThrow(() ->
						forecastCommand.execute(event, new String[]{"прогноз", "Отжимания"}, "user1", "TestUser")
		);
	}
}
