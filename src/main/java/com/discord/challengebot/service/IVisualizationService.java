package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for visualization service
 */
public interface IVisualizationService {
    CompletableFuture<byte[]> generateProgressChart(ChallengeStats stats);
    CompletableFuture<byte[]> generatePercentageChart(ChallengeStats stats);
}
