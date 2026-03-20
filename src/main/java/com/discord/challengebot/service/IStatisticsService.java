package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Interface for statistics calculation service
 */
public interface IStatisticsService {
    ChallengeStats calculateStats(Challenge challenge);
    long calculateRemaining(Challenge challenge);
    double calculateDailyTarget(Challenge challenge);
    double calculatePercentage(Challenge challenge);
    String generateProgressReport(Challenge challenge);
    List<Map.Entry<String, Long>> generateLeaderboard(Challenge challenge, int limit);
    String formatReportForDiscord(Challenge challenge, ChallengeStats stats);
    String formatLeaderboardForDiscord(Challenge challenge, List<Map.Entry<String, Long>> leaderboard);
    String formatChallengeStats(Challenge challenge, ChallengeStats stats);
    LocalDate forecastCompletionDate(String challengeId, String userId);
}
