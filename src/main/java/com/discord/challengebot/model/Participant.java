package com.discord.challengebot.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Модель участника
 */
public class Participant {
    private String userId;
    private String username;
    private LocalDateTime joinDate;
    private List<String> registeredChallenges;

    public Participant() {
        this.registeredChallenges = new ArrayList<>();
    }

    public Participant(String userId, String username) {
        this.userId = userId;
        this.username = username;
        this.joinDate = LocalDateTime.now();
        this.registeredChallenges = new ArrayList<>();
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDateTime joinDate) {
        this.joinDate = joinDate;
    }

    public List<String> getRegisteredChallenges() {
        return registeredChallenges;
    }

    public void setRegisteredChallenges(List<String> registeredChallenges) {
        this.registeredChallenges = registeredChallenges;
    }
    
    // Helper methods
    public void addChallenge(String challengeName) {
        if (!registeredChallenges.contains(challengeName)) {
            registeredChallenges.add(challengeName);
        }
    }
    
    public void removeChallenge(String challengeName) {
        registeredChallenges.remove(challengeName);
    }
    
    public boolean isRegisteredForChallenge(String challengeName) {
        return registeredChallenges.contains(challengeName);
    }
}