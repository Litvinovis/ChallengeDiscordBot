package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IUserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Бэкап должен сериализовать испытания с датами и выдавать JSON, который читает {@link ImportCommand}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BackupCommandTest {

	@Mock
	private IChallengeService challengeService;
	@Mock
	private ChallengeProgressRepository progressRepository;
	@Mock
	private IUserService userService;

	@InjectMocks
	private BackupCommand command;

	@Mock
	private MessageReceivedEvent event;
	@Mock
	private MessageChannelUnion channelUnion;
	@Mock
	private TextChannel textChannel;
	@Mock
	private MessageCreateAction messageCreateAction;

	@BeforeEach
	void setUp() {
		when(event.getChannel()).thenReturn(channelUnion);
		when(channelUnion.asTextChannel()).thenReturn(textChannel);
		when(channelUnion.sendMessage(anyString())).thenReturn(messageCreateAction);
		when(textChannel.sendFiles(any(FileUpload[].class))).thenReturn(messageCreateAction);
		when(messageCreateAction.setContent(anyString())).thenReturn(messageCreateAction);
		when(userService.isAdminUser("admin")).thenReturn(true);
	}

	@Test
	void backup_serializesDates_asIsoStringsReadableByImport() throws Exception {
		LocalDateTime end = LocalDateTime.of(2026, 12, 31, 0, 0);
		Challenge challenge = new Challenge("отжимания", "Отжимания", 10000, ChallengeType.GROUP,
						LocalDateTime.of(2026, 1, 1, 10, 30), end, "desc", "раз");
		when(challengeService.getAllChallenges()).thenReturn(List.of(challenge));
		when(progressRepository.findByChallengeId("отжимания")).thenReturn(Map.of("u1", 42L));

		command.execute(event, new String[]{"бэкап"}, "admin", "Admin");

		verify(channelUnion, never()).sendMessage(startsWith("❌"));
		ArgumentCaptor<FileUpload> upload = ArgumentCaptor.forClass(FileUpload.class);
		verify(textChannel).sendFiles(upload.capture());
		JsonNode root = new ObjectMapper().readTree(upload.getValue().getData().readAllBytes());

		JsonNode saved = root.path("challenges").get(0);
		// ImportCommand читает дату через LocalDateTime.parse
		assertEquals(end, LocalDateTime.parse(saved.path("endDate").asText()));
		assertEquals(42L, root.path("progress").path("отжимания").path("u1").asLong());
	}
}
