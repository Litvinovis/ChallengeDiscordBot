package com.discord.challengebot.event;

import com.discord.challengebot.model.Challenge;
import org.springframework.context.ApplicationEvent;

/**
 * Событие достижения 50% прогресса по испытанию.
 */
public class ChallengeHalfwayEvent extends ApplicationEvent {

    private final Challenge challenge;

    public ChallengeHalfwayEvent(Object source, Challenge challenge) {
        super(source);
        this.challenge = challenge;
    }

    public Challenge getChallenge() {
        return challenge;
    }
}
