package com.discord.challengebot.command;

import com.discord.challengebot.service.ChallengeBackupService;
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
	private ChallengeBackupService backupService;
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
	void backupAttachesCompleteSnapshot() throws Exception {
		when(backupService.exportSnapshot()).thenReturn("""
				{"version":2,"progress_history":[{"amount":42}],"challenge_archive":[]}
				""");

		command.execute(event, new String[]{"бэкап"}, "admin", "Admin");

		verify(channelUnion, never()).sendMessage(startsWith("❌"));
		ArgumentCaptor<FileUpload> upload = ArgumentCaptor.forClass(FileUpload.class);
		verify(textChannel).sendFiles(upload.capture());
		JsonNode root = new ObjectMapper().readTree(upload.getValue().getData().readAllBytes());
		assertEquals(2, root.path("version").asInt());
		assertEquals(42, root.path("progress_history").get(0).path("amount").asInt());
	}
}
