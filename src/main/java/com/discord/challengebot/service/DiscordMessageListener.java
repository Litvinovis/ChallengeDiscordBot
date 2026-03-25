package com.discord.challengebot.service;

import com.discord.challengebot.command.Command;
import com.discord.challengebot.command.CommandRegistry;
import com.discord.challengebot.config.DiscordConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Обработчик входящих сообщений Discord.
 * Фильтрует сообщения по каналам, разбирает команды с префиксом '+' и делегирует
 * их выполнение в {@link CommandRegistry}. Игнорирует сообщения от ботов (кроме доверенного).
 */
public class DiscordMessageListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(DiscordMessageListener.class);
    private static final String OPENCLAW_ROVER_BOT_ID = "1481319611533365483";

    private final IDiscordService discordService;
    private final DiscordConfig discordConfig;
    private final IChallengeService challengeService;
    private final IUserService userService;
    private final IStatisticsService statisticsService;
    private final CommandRegistry commandRegistry;
    private JDA jda;

    /**
     * Конструктор с внедрением всех зависимостей.
     *
     * @param discordService   сервис взаимодействия с Discord
     * @param discordConfig    конфигурация Discord бота
     * @param challengeService сервис управления испытаниями
     * @param userService      сервис управления пользователями
     * @param statisticsService сервис статистики
     * @param commandRegistry  реестр команд
     */
    public DiscordMessageListener(IDiscordService discordService, DiscordConfig discordConfig,
                                   IChallengeService challengeService, IUserService userService,
                                   IStatisticsService statisticsService,
                                   CommandRegistry commandRegistry) {
        this.discordService = discordService;
        this.discordConfig = discordConfig;
        this.challengeService = challengeService;
        this.userService = userService;
        this.statisticsService = statisticsService;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try {
            this.jda = event.getJDA();

            // Ignore bot messages except trusted OpenClawRover
            if (event.getAuthor().isBot() && !OPENCLAW_ROVER_BOT_ID.equals(event.getAuthor().getId())) {
                logger.debug("Игнорирование сообщения от бота: {} ({})", event.getAuthor().getName(), event.getAuthor().getId());
                return;
            }

            // Only handle TEXT channels
            if (!event.getChannel().getType().equals(ChannelType.TEXT)) {
                return;
            }

            TextChannel channel = event.getChannel().asTextChannel();
            List<String> configuredChannelIds = discordConfig.getChannelIds();
            String configuredChannelId = discordConfig.getChannelId();
            if (configuredChannelIds != null && !configuredChannelIds.isEmpty()) {
                if (!configuredChannelIds.contains(channel.getId())) {
                    return;
                }
            } else if (configuredChannelId != null && !configuredChannelId.isBlank()) {
                if (!channel.getId().equals(configuredChannelId)) {
                    return;
                }
            } else if (!channel.getName().equals(discordConfig.getChannel())) {
                return;
            }

            String messageContent = event.getMessage().getContentRaw();
            if (!messageContent.startsWith("+")) {
                return;
            }

            String command = messageContent.substring(1).trim();
            String userId = event.getAuthor().getId();
            String username = event.getAuthor().getName();

            logger.info("Обработка команды: {} от пользователя: {} ({})", command, username, userId);

            processCommand(event, command, userId, username, channel.getId());
        } catch (Exception e) {
            logger.error("Ошибка обработки события сообщения", e);
            try {
                if (event.getChannel().getType().equals(ChannelType.TEXT)) {
                    event.getChannel().asTextChannel()
                            .sendMessage("Произошла ошибка при обработке команды. Пожалуйста, попробуйте позже.").queue();
                }
            } catch (Exception sendException) {
                logger.error("Ошибка отправки сообщения об ошибке", sendException);
            }
        }
    }

    private void processCommand(MessageReceivedEvent event, String command, String userId, String username, String channelId) {
        try {
            String[] parts = command.split("\\s+");
            String commandName = parts.length > 0 ? parts[0].toLowerCase() : "";

            // Check authorization
            if (!discordService.isAuthorizedUser(userId, commandName)) {
                TextChannel channel = jda.getTextChannelById(channelId);
                if (channel != null) {
                    channel.sendMessage("У вас нет прав для выполнения этой команды.").queue();
                }
                return;
            }

            // Find and execute the matching command via CommandRegistry
            Optional<Command> matchedCommand = commandRegistry.findCommand(commandName);
            if (matchedCommand.isPresent()) {
                matchedCommand.get().execute(event, parts, userId, username);
            } else {
                // Fallback: let DefaultProgressCommand handle it
                commandRegistry.findCommand("__default__").ifPresent(
                        cmd -> cmd.execute(event, parts, userId, username)
                );
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды '{}' от пользователя {}", command, username, e);
            try {
                TextChannel channel = jda.getTextChannelById(channelId);
                if (channel != null) {
                    channel.sendMessage("Произошла ошибка при обработке команды. Пожалуйста, попробуйте позже.").queue();
                }
            } catch (Exception sendException) {
                logger.error("Ошибка отправки сообщения об ошибке", sendException);
            }
        }
    }
}
