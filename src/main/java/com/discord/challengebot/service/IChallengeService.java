package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Интерфейс сервиса управления испытаниями.
 * Определяет операции создания, изменения, удаления испытаний и управления участниками.
 */
public interface IChallengeService {
	/**
	 * Создаёт новое испытание.
	 *
	 * @param name        название испытания
	 * @param targetValue целевое значение
	 * @param endDate     дата окончания
	 * @param type        тип испытания
	 * @param description описание
	 * @param unit        единица измерения
	 * @return созданное испытание или {@code null} при ошибке
	 */
	Challenge createChallenge(String name, long targetValue, LocalDateTime endDate,
	                          ChallengeType type, String description, String unit);

	/**
	 * Добавляет прогресс пользователя к испытанию.
	 *
	 * @param challenge испытание
	 * @param userId    идентификатор пользователя
	 * @param username  имя пользователя
	 * @param amount    количество добавляемого прогресса
	 * @return обновлённое испытание
	 */
	Challenge addProgress(Challenge challenge, String userId, String username, long amount);

	/**
	 * Уменьшает прогресс пользователя в испытании (не ниже нуля).
	 *
	 * @param challenge испытание
	 * @param userId    идентификатор пользователя
	 * @param username  имя пользователя
	 * @param amount    положительное количество для вычитания
	 * @return обновлённое испытание
	 */
	Challenge subtractProgress(Challenge challenge, String userId, String username, long amount);

	/**
	 * Возвращает испытание по его названию.
	 *
	 * @param name название испытания
	 * @return испытание или {@code null}, если не найдено
	 */
	Challenge getChallenge(String name);

	/**
	 * Возвращает список всех испытаний.
	 *
	 * @return список всех испытаний
	 */
	List<Challenge> getAllChallenges();

	/**
	 * Возвращает список активных испытаний.
	 *
	 * @return список активных испытаний
	 */
	List<Challenge> getActiveChallenges();

	/**
	 * Рассчитывает и возвращает статистику по испытанию.
	 *
	 * @param challenge испытание
	 * @return объект статистики или {@code null}
	 */
	ChallengeStats getChallengeStats(Challenge challenge);


	/**
	 * Возвращает список испытаний, в которых участвует пользователь.
	 *
	 * @param userId идентификатор пользователя
	 * @return список испытаний пользователя
	 */
	List<Challenge> getUserChallenges(String userId);

	/**
	 * Удаляет испытание по названию.
	 *
	 * @param challengeName название испытания
	 * @return {@code true} при успешном удалении
	 */
	boolean deleteChallenge(String challengeName);

	/**
	 * Обновляет статус активности испытания.
	 *
	 * @param challenge испытание
	 * @param active    {@code true} для активации, {@code false} для остановки
	 * @return обновлённое испытание
	 */
	Challenge updateChallengeStatus(Challenge challenge, boolean active);

	/**
	 * Обновляет целевое значение испытания.
	 *
	 * @param challenge испытание
	 * @param newTarget новое целевое значение
	 * @return обновлённое испытание
	 */
	Challenge updateChallengeTarget(Challenge challenge, long newTarget);

	/**
	 * Обновляет дату окончания испытания.
	 *
	 * @param challenge  испытание
	 * @param newEndDate новая дата окончания
	 * @return обновлённое испытание
	 */
	Challenge updateChallengeEndDate(Challenge challenge, LocalDateTime newEndDate);

	/**
	 * Устанавливает абсолютное значение прогресса участника.
	 *
	 * @param challenge испытание
	 * @param userId    идентификатор пользователя
	 * @param progress  новое значение прогресса
	 * @return обновлённое испытание
	 */
	Challenge setParticipantProgress(Challenge challenge, String userId, long progress);

	/**
	 * Удаляет участника из испытания.
	 *
	 * @param challenge испытание
	 * @param userId    идентификатор пользователя
	 * @return обновлённое испытание
	 */
	Challenge removeParticipant(Challenge challenge, String userId);

	/**
	 * Добавляет участника в испытание с регистрацией имени пользователя.
	 *
	 * @param challenge испытание
	 * @param userId    идентификатор пользователя
	 * @param username  имя пользователя
	 * @return обновлённое испытание
	 */
	Challenge addParticipantWithUsername(Challenge challenge, String userId, String username);

	/**
	 * Добавляет участника в испытание без указания имени.
	 *
	 * @param challenge испытание
	 * @param userId    идентификатор пользователя
	 * @return обновлённое испытание
	 */
	Challenge addParticipant(Challenge challenge, String userId);

	/**
	 * Возвращает список топ-участников по прогрессу в испытании.
	 *
	 * @param challenge испытание
	 * @param limit     максимальное количество участников в списке
	 * @return список пар userId -> прогресс, отсортированный по убыванию
	 */
	List<Map.Entry<String, Long>> getTopParticipants(Challenge challenge, int limit);

	/**
	 * Завершает испытание, устанавливая его статус в неактивный.
	 *
	 * @param challenge испытание
	 */
	void completeChallenge(Challenge challenge);
}
