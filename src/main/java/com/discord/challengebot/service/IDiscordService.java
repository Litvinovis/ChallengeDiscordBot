package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import net.dv8tion.jda.api.JDA;

/**
 * Interface for Discord interaction service
 */
public interface IDiscordService {
    JDA getJDA();
    void sendMessage(String channelId, String message);
    void sendMessageToChannel(String channelName, String message);
    void sendMessageWithVisualization(String channelId, String message, byte[] image);
    String generateHelpMessage();
    String generateHelpMessage(String userId);
    void sendDailyReport();
    void sendChallengeCompletionNotification(Challenge challenge);
    void sendChallengeFailureNotification(Challenge challenge);
    String formatChallengeStats(Challenge challenge, ChallengeStats stats);
    boolean isAuthorizedUser(String userId, String command);
}
