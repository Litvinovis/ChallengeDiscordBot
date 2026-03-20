package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Interface for challenge management service
 */
public interface IChallengeService {
    Challenge createChallenge(String name, long targetValue, LocalDateTime endDate,
                              ChallengeType type, String description, String unit);
    Challenge addProgress(Challenge challenge, String userId, String username, long amount);
    Challenge getChallenge(String name);
    List<Challenge> getAllChallenges();
    List<Challenge> getActiveChallenges();
    ChallengeStats getChallengeStats(Challenge challenge);
    Map<String, ChallengeStats> getAllChallengesStats();
    List<Challenge> getUserChallenges(String userId);
    boolean deleteChallenge(String challengeName);
    Challenge updateChallengeStatus(Challenge challenge, boolean active);
    Challenge updateChallengeTarget(Challenge challenge, long newTarget);
    Challenge updateChallengeEndDate(Challenge challenge, LocalDateTime newEndDate);
    Challenge setParticipantProgress(Challenge challenge, String userId, long progress);
    Challenge removeParticipant(Challenge challenge, String userId);
    Challenge addParticipantWithUsername(Challenge challenge, String userId, String username);
    Challenge addParticipant(Challenge challenge, String userId);
    List<Map.Entry<String, Long>> getTopParticipants(Challenge challenge, int limit);
    void completeChallenge(Challenge challenge);
}
