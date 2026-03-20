package com.discord.challengebot.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

/**
 * Модель участника
 */
public class Participant implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String username;
    private LocalDateTime joinDate;
    private List<String> registeredChallenges;

    // Streak fields
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastActivityDate;

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
    
    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }

    public LocalDate getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(LocalDate lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
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