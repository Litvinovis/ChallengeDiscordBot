package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;

import java.util.List;

/**
 * Interface for data storage service
 */
public interface IDataStorageService {
    void saveChallenge(Challenge challenge);
    Challenge getChallenge(String name);
    List<Challenge> getAllChallenges();
    boolean deleteChallenge(String challengeName);
    void saveParticipant(Participant participant);
    Participant getParticipant(String userId);
    List<Participant> getAllParticipants();
    boolean deleteParticipant(String userId);
}
