package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;

import java.util.List;

/**
 * Интерфейс сервиса хранения данных.
 * Определяет операции CRUD для испытаний и участников.
 */
public interface IDataStorageService {
    /**
     * Сохраняет или обновляет испытание в хранилище.
     *
     * @param challenge испытание для сохранения
     */
    void saveChallenge(Challenge challenge);

    /**
     * Возвращает испытание по названию.
     *
     * @param name название испытания
     * @return испытание или {@code null}, если не найдено
     */
    Challenge getChallenge(String name);

    /**
     * Возвращает все испытания из хранилища.
     *
     * @return список всех испытаний
     */
    List<Challenge> getAllChallenges();

    /**
     * Удаляет испытание по названию.
     *
     * @param challengeName название испытания
     * @return {@code true} при успешном удалении
     */
    boolean deleteChallenge(String challengeName);

    /**
     * Сохраняет или обновляет участника в хранилище.
     *
     * @param participant участник для сохранения
     */
    void saveParticipant(Participant participant);

    /**
     * Возвращает участника по идентификатору пользователя.
     *
     * @param userId идентификатор пользователя
     * @return участник или {@code null}, если не найден
     */
    Participant getParticipant(String userId);

    /**
     * Возвращает всех участников из хранилища.
     *
     * @return список всех участников
     */
    List<Participant> getAllParticipants();

    /**
     * Удаляет участника по идентификатору пользователя.
     *
     * @param userId идентификатор пользователя
     * @return {@code true} при успешном удалении
     */
    boolean deleteParticipant(String userId);
}
