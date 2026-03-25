package com.discord.challengebot.command;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IDiscordService;
import com.discord.challengebot.service.IStatisticsService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Команда {@code +статистика} — выводит статистику по всем испытаниям или по конкретному.
 * Использование: {@code +статистика} или {@code +статистика <название>}.
 */
@Component
public class StatsCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(StatsCommand.class);

    @Autowired
    private IChallengeService challengeService;
    @Autowired
    private IDiscordService discordService;
    @Autowired
    private IStatisticsService statisticsService;

    /**
     * {@inheritDoc}
     * Обрабатывает команду {@code статистика}.
     *
     * @param cmd строка команды
     * @return {@code true}, если команда равна "статистика"
     */
    @Override
    public boolean canHandle(String cmd) {
        return "статистика".equals(cmd);
    }

    /**
     * {@inheritDoc}
     * Без аргументов выводит статистику по всем испытаниям, с аргументом — по конкретному.
     *
     * @param event    событие получения сообщения Discord
     * @param args     аргументы: при наличии args[1..] — название испытания
     * @param authorId идентификатор автора команды
     * @param username имя автора команды
     */
    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            TextChannel channel = event.getChannel().asTextChannel();
            if (args.length == 1) {
                List<Challenge> challenges = challengeService.getAllChallenges();
                if (challenges.isEmpty()) {
                    channel.sendMessage("Нет доступных испытаний.").queue();
                    return;
                }
                StringBuilder message = new StringBuilder("**Статистика по всем испытаниям:**\n\n");
                for (Challenge challenge : challenges) {
                    ChallengeStats stats = challengeService.getChallengeStats(challenge);
                    if (stats != null) {
                        message.append(statisticsService.formatReportForDiscord(challenge, stats)).append("\n");
                    }
                }
                channel.sendMessage(message.toString()).queue();
            } else {
                String challengeName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                Challenge challenge = challengeService.getChallenge(challengeName);
                if (challenge == null) {
                    channel.sendMessage("Испытание \"" + challengeName + "\" не найдено.").queue();
                    return;
                }
                ChallengeStats stats = challengeService.getChallengeStats(challenge);
                if (stats != null) {
                    channel.sendMessage(discordService.formatChallengeStats(challenge, stats)).queue();
                } else {
                    channel.sendMessage("Ошибка при получении статистики.").queue();
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка обработки команды статистики", e);
        }
    }
}
