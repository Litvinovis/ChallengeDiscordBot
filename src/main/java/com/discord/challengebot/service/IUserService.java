package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;

import java.util.List;

/**
 * Interface for user management service
 */
public interface IUserService {
    boolean registerForChallenge(String userId, String username, String challengeName);
    boolean unregisterFromChallenge(String userId, String challengeName);
    Participant getParticipant(String userId);
    List<Challenge> getRegisteredChallenges(String userId);
    boolean isAdminUser(String userId);
    boolean updateParticipantUsername(String userId, String username);
}
