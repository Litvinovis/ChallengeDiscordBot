package com.discord.challengebot.dto;

/**
 * DTO статистики по испытанию.
 * Содержит рассчитанные показатели: текущий прогресс, процент выполнения,
 * ежедневная цель и количество оставшихся дней.
 */
public class ChallengeStats {
    private String challengeName;
    private long targetValue;
    private long currentValue;
    private long remaining;
    private double percentage;
    private double dailyTarget;
    private int daysRemaining;

    /**
     * Конструктор по умолчанию.
     */
    public ChallengeStats() {
    }

    /**
     * Конструктор с полным набором параметров статистики.
     *
     * @param challengeName  название испытания
     * @param targetValue    целевое значение
     * @param currentValue   текущее суммарное значение прогресса
     * @param remaining      оставшееся количество до достижения цели
     * @param percentage     процент выполнения (0..100+)
     * @param dailyTarget    рекомендуемая ежедневная цель на одного участника
     * @param daysRemaining  количество оставшихся дней до окончания испытания
     */
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
    /**
     * Возвращает название испытания.
     *
     * @return название испытания
     */
    public String getChallengeName() {
        return challengeName;
    }

    /**
     * Устанавливает название испытания.
     *
     * @param challengeName название испытания
     */
    public void setChallengeName(String challengeName) {
        this.challengeName = challengeName;
    }

    /**
     * Возвращает целевое значение испытания.
     *
     * @return целевое значение
     */
    public long getTargetValue() {
        return targetValue;
    }

    /**
     * Устанавливает целевое значение испытания.
     *
     * @param targetValue целевое значение
     */
    public void setTargetValue(long targetValue) {
        this.targetValue = targetValue;
    }

    /**
     * Возвращает текущее суммарное значение прогресса.
     *
     * @return текущее значение
     */
    public long getCurrentValue() {
        return currentValue;
    }

    /**
     * Устанавливает текущее суммарное значение прогресса.
     *
     * @param currentValue текущее значение
     */
    public void setCurrentValue(long currentValue) {
        this.currentValue = currentValue;
    }

    /**
     * Возвращает оставшееся количество до достижения цели.
     *
     * @return оставшееся количество
     */
    public long getRemaining() {
        return remaining;
    }

    /**
     * Устанавливает оставшееся количество.
     *
     * @param remaining оставшееся количество
     */
    public void setRemaining(long remaining) {
        this.remaining = remaining;
    }

    /**
     * Возвращает процент выполнения испытания.
     *
     * @return процент выполнения (0..100 и выше при перевыполнении)
     */
    public double getPercentage() {
        return percentage;
    }

    /**
     * Устанавливает процент выполнения испытания.
     *
     * @param percentage процент выполнения
     */
    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    /**
     * Возвращает рекомендуемую ежедневную цель на одного участника.
     *
     * @return ежедневная цель
     */
    public double getDailyTarget() {
        return dailyTarget;
    }

    /**
     * Устанавливает рекомендуемую ежедневную цель.
     *
     * @param dailyTarget ежедневная цель
     */
    public void setDailyTarget(double dailyTarget) {
        this.dailyTarget = dailyTarget;
    }

    /**
     * Возвращает количество оставшихся дней до окончания испытания.
     *
     * @return количество оставшихся дней
     */
    public int getDaysRemaining() {
        return daysRemaining;
    }

    /**
     * Устанавливает количество оставшихся дней.
     *
     * @param daysRemaining количество оставшихся дней
     */
    public void setDaysRemaining(int daysRemaining) {
        this.daysRemaining = daysRemaining;
    }
}