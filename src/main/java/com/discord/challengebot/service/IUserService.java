package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;

import java.util.List;

/**
 * Интерфейс сервиса управления пользователями.
 * Определяет операции регистрации, получения данных и проверки прав участников.
 */
public interface IUserService {
    /**
     * Регистрирует пользователя на испытание, создавая запись участника при необходимости.
     *
     * @param userId        идентификатор пользователя
     * @param username      имя пользователя
     * @param challengeName название испытания
     * @return {@code true} при успешной регистрации
     */
    boolean registerForChallenge(String userId, String username, String challengeName);

    /**
     * Отменяет регистрацию пользователя на испытание.
     *
     * @param userId        идентификатор пользователя
     * @param challengeName название испытания
     * @return {@code true} при успешной отмене
     */
    boolean unregisterFromChallenge(String userId, String challengeName);

    /**
     * Возвращает данные участника по идентификатору пользователя.
     *
     * @param userId идентификатор пользователя
     * @return участник или {@code null}, если не найден
     */
    Participant getParticipant(String userId);

    /**
     * Возвращает список испытаний, на которые зарегистрирован пользователь.
     *
     * @param userId идентификатор пользователя
     * @return список испытаний пользователя
     */
    List<Challenge> getRegisteredChallenges(String userId);

    /**
     * Проверяет, является ли пользователь администратором бота.
     *
     * @param userId идентификатор пользователя
     * @return {@code true}, если пользователь является администратором
     */
    boolean isAdminUser(String userId);

    /**
     * Обновляет имя пользователя в системе.
     *
     * @param userId   идентификатор пользователя
     * @param username новое имя пользователя
     * @return {@code true} при успешном обновлении
     */
    boolean updateParticipantUsername(String userId, String username);
}
