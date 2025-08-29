package com.discord.challengebot.dto;

/**
 * Статистика по испытанию
 */
public class ChallengeStats {
    private String challengeName;
    private long targetValue;
    private long currentValue;
    private long remaining;
    private double percentage;
    private double dailyTarget;
    private int daysRemaining;

    public ChallengeStats() {
    }

    public ChallengeStats(String challengeName, long targetValue, long currentValue, 
                         long remaining, double percentage, double dailyTarget, int daysRemaining) {
        this.challengeName = challengeName;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.remaining = remaining;
        this.percentage = percentage;
        this.dailyTarget = dailyTarget;
        this.daysRemaining = daysRemaining;
    }

    // Getters and setters
    public String getChallengeName() {
        return challengeName;
    }

    public void setChallengeName(String challengeName) {
        this.challengeName = challengeName;
    }

    public long getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(long targetValue) {
        this.targetValue = targetValue;
    }

    public long getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(long currentValue) {
        this.currentValue = currentValue;
    }

    public long getRemaining() {
        return remaining;
    }

    public void setRemaining(long remaining) {
        this.remaining = remaining;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public double getDailyTarget() {
        return dailyTarget;
    }

    public void setDailyTarget(double dailyTarget) {
        this.dailyTarget = dailyTarget;
    }

    public int getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(int daysRemaining) {
        this.daysRemaining = daysRemaining;
    }
}