package com.discord.challengebot.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

/**
 * Модель участника (пользователя Discord).
 * Хранит информацию о зарегистрированных испытаниях, серии активности и дате вступления.
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

    /**
     * Конструктор по умолчанию. Инициализирует пустой список испытаний.
     */
    public Participant() {
        this.registeredChallenges = new ArrayList<>();
    }

    /**
     * Конструктор с параметрами. Дата вступления устанавливается в текущее время.
     *
     * @param userId   идентификатор пользователя в Discord
     * @param username имя пользователя в Discord
     */
    public Participant(String userId, String username) {
        this.userId = userId;
        this.username = username;
        this.joinDate = LocalDateTime.now();
        this.registeredChallenges = new ArrayList<>();
    }

    // Getters and setters
    /**
     * Возвращает идентификатор пользователя в Discord.
     *
     * @return идентификатор пользователя
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Устанавливает идентификатор пользователя.
     *
     * @param userId идентификатор пользователя в Discord
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Возвращает имя пользователя в Discord.
     *
     * @return имя пользователя
     */
    public String getUsername() {
        return username;
    }

    /**
     * Устанавливает имя пользователя.
     *
     * @param username имя пользователя в Discord
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Возвращает дату и время вступления пользователя в систему.
     *
     * @return дата и время вступления
     */
    public LocalDateTime getJoinDate() {
        return joinDate;
    }

    /**
     * Устанавливает дату и время вступления пользователя.
     *
     * @param joinDate дата и время вступления
     */
    public void setJoinDate(LocalDateTime joinDate) {
        this.joinDate = joinDate;
    }

    /**
     * Возвращает список названий испытаний, на которые зарегистрирован пользователь.
     *
     * @return список названий испытаний
     */
    public List<String> getRegisteredChallenges() {
        return registeredChallenges;
    }

    /**
     * Устанавливает список зарегистрированных испытаний.
     *
     * @param registeredChallenges список названий испытаний
     */
    public void setRegisteredChallenges(List<String> registeredChallenges) {
        this.registeredChallenges = registeredChallenges;
    }

    /**
     * Возвращает текущую серию активности (количество дней подряд).
     *
     * @return текущая серия в днях
     */
    public int getCurrentStreak() {
        return currentStreak;
    }

    /**
     * Устанавливает текущую серию активности.
     *
     * @param currentStreak количество дней подряд активности
     */
    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    /**
     * Возвращает наибольшую серию активности пользователя за всё время.
     *
     * @return наибольшая серия в днях
     */
    public int getLongestStreak() {
        return longestStreak;
    }

    /**
     * Устанавливает наибольшую серию активности.
     *
     * @param longestStreak рекордная серия в днях
     */
    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }

    /**
     * Возвращает дату последней зафиксированной активности пользователя.
     *
     * @return дата последней активности
     */
    public LocalDate getLastActivityDate() {
        return lastActivityDate;
    }

    /**
     * Устанавливает дату последней активности.
     *
     * @param lastActivityDate дата последней активности
     */
    public void setLastActivityDate(LocalDate lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    // Helper methods
    /**
     * Регистрирует пользователя на испытание, если он ещё не зарегистрирован.
     *
     * @param challengeName название испытания
     */
    public void addChallenge(String challengeName) {
        if (!registeredChallenges.contains(challengeName)) {
            registeredChallenges.add(challengeName);
        }
    }

    /**
     * Отменяет регистрацию пользователя на испытание.
     *
     * @param challengeName название испытания
     */
    public void removeChallenge(String challengeName) {
        registeredChallenges.remove(challengeName);
    }

    /**
     * Проверяет, зарегистрирован ли пользователь на указанное испытание.
     *
     * @param challengeName название испытания
     * @return {@code true}, если пользователь зарегистрирован
     */
    public boolean isRegisteredForChallenge(String challengeName) {
        return registeredChallenges.contains(challengeName);
    }
}