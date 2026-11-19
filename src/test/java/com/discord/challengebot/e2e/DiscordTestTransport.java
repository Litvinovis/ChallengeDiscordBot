package com.discord.challengebot.e2e;

import com.discord.challengebot.service.DiscordMessageListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.NamedAttachmentProxy;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Подмена границы JDA. Событие, слушатель, маршрутизация и команды остаются настоящими. */
final class DiscordTestTransport {
    static final String CHANNEL_ID = "123456789000000010";
    static final String GUILD_ID = "123456789000000020";
    static final String CHANNEL_NAME = "local-e2e";
    static final String ADMIN = "123456789000000001";
    static final String ALICE = "123456789000000002";
    static final String BOB = "123456789000000003";

    record Attachment(String name, byte[] bytes) {}
    record Response(String text, List<Attachment> files) {}

    private final JDA gateway = mock(JDA.class);
    private final List<Response> responses = new ArrayList<>();
    private final MessageChannelUnion incoming = mock(MessageChannelUnion.class);
    private DiscordMessageListener listener;
    private long nextMessageId;

    DiscordTestTransport() {
        TextChannel channel = mock(TextChannel.class);
        Guild guild = mock(Guild.class);
        when(channel.getId()).thenReturn(CHANNEL_ID);
        when(channel.getName()).thenReturn(CHANNEL_NAME);
        when(incoming.getType()).thenReturn(ChannelType.TEXT);
        when(incoming.asTextChannel()).thenReturn(channel);
        when(gateway.getTextChannelById(CHANNEL_ID)).thenReturn(channel);
        when(gateway.getGuildById(GUILD_ID)).thenReturn(guild);
        when(guild.getTextChannelsByName(CHANNEL_NAME, true)).thenReturn(List.of(channel));
        when(channel.sendMessage(any(CharSequence.class))).thenAnswer(call -> outgoing(call.getArgument(0), List.of()));
        when(incoming.sendMessage(any(CharSequence.class))).thenAnswer(call -> outgoing(call.getArgument(0), List.of()));
        when(channel.sendFiles(any(FileUpload[].class))).thenAnswer(call -> outgoing("", readUploads(call.getRawArguments()[0])));
        when(incoming.sendFiles(any(FileUpload[].class))).thenAnswer(call -> outgoing("", readUploads(call.getRawArguments()[0])));
        User alice = user(ALICE, "Алиса", false);
        User bob = user(BOB, "Боб", false);
        when(gateway.getUserById(ALICE)).thenReturn(alice);
        when(gateway.getUserById(BOB)).thenReturn(bob);
    }

    MockedStatic<JDABuilder> interceptConnection() {
        JDABuilder builder = mock(JDABuilder.class, RETURNS_SELF);
        when(builder.addEventListeners(any(Object[].class))).thenAnswer(call -> {
            var registered = (Object[]) call.getRawArguments()[0];
            listener = (DiscordMessageListener) registered[0];
            return builder;
        });
        when(builder.build()).thenReturn(gateway);
        MockedStatic<JDABuilder> connection = mockStatic(JDABuilder.class);
        connection.when(() -> JDABuilder.createDefault(anyString())).thenReturn(builder);
        return connection;
    }

    List<Response> command(String text, String sender, byte[] attachment) {
        return deliver(text, sender, false, incoming, attachment);
    }

    List<Response> botMessage(String text) {
        return deliver(text, BOB, true, incoming, null);
    }

    List<Response> wrongChannel(String text) {
        var another = mock(MessageChannelUnion.class);
        var channel = mock(TextChannel.class);
        when(another.getType()).thenReturn(ChannelType.TEXT);
        when(another.asTextChannel()).thenReturn(channel);
        when(channel.getId()).thenReturn("123456789000000011");
        return deliver(text, ADMIN, false, another, null);
    }

    List<Response> scheduled(Runnable job) {
        responses.clear();
        job.run();
        return List.copyOf(responses);
    }

    private List<Response> deliver(String text, String sender, boolean bot, MessageChannelUnion channel, byte[] attachment) {
        responses.clear();
        Message message = mock(Message.class);
        when(message.getIdLong()).thenReturn(++nextMessageId);
        when(message.getChannel()).thenReturn(channel);
        User author = user(sender, switch (sender) {
            case ADMIN -> "Админ";
            case ALICE -> "Алиса";
            default -> "Боб";
        }, bot);
        List<Message.Attachment> attachments = attachment == null ? List.of() : List.of(upload(attachment));
        when(message.getAuthor()).thenReturn(author);
        when(message.getContentRaw()).thenReturn(text);
        when(message.getAttachments()).thenReturn(attachments);
        listener.onMessageReceived(new MessageReceivedEvent(gateway, nextMessageId, message));
        return List.copyOf(responses);
    }

    private static User user(String id, String name, boolean bot) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn(name);
        when(user.isBot()).thenReturn(bot);
        return user;
    }

    private static Message.Attachment upload(byte[] bytes) {
        var proxy = mock(NamedAttachmentProxy.class);
        when(proxy.download()).thenAnswer(call -> CompletableFuture.completedFuture(new ByteArrayInputStream(bytes)));
        var attachment = mock(Message.Attachment.class);
        when(attachment.getProxy()).thenReturn(proxy);
        return attachment;
    }

    private static List<Attachment> readUploads(Object argument) {
        return Arrays.stream((FileUpload[]) argument).map(file -> {
            try (file) {
                return new Attachment(file.getName(), file.getData().readAllBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).toList();
    }

    private MessageCreateAction outgoing(CharSequence content, List<Attachment> files) {
        var action = mock(MessageCreateAction.class, RETURNS_SELF);
        var text = new AtomicReference<>(content.toString());
        when(action.setContent(anyString())).thenAnswer(call -> {
            text.set(call.getArgument(0));
            return action;
        });
        doAnswer(call -> {
            responses.add(new Response(text.get(), files));
            return null;
        }).when(action).queue();
        when(action.complete()).thenAnswer(call -> {
            responses.add(new Response(text.get(), files));
            return mock(Message.class);
        });
        when(action.timeout(anyLong(), any(TimeUnit.class))).thenReturn(action);
        return action;
    }
}
