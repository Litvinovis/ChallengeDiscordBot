package com.discord.challengebot.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.io.Serializable;

/**
 * Модель испытания.
 * Хранит всю информацию об испытании: название, целевое значение, прогресс участников,
 * даты начала и окончания, тип испытания и статус активности.
 */
public class Challenge implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(Challenge.class);

	private String id;
	private String name;
	private long targetValue;
	private long currentValue;
	private ChallengeType type;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private Map<String, Long> participantProgress;
	private boolean active;
	private String description;
	private String unit;
	private List<String> participants; // List of participant user IDs (serialization compatible)

	/**
	 * Конструктор по умолчанию. Инициализирует пустые коллекции прогресса и участников.
	 */
	public Challenge() {
		this.participantProgress = new HashMap<>();
		this.participants = new ArrayList<>();
		logger.debug("Создан новый экземпляр Challenge");
	}

	/**
	 * Конструктор с полным набором параметров.
	 *
	 * @param id          уникальный идентификатор испытания
	 * @param name        название испытания
	 * @param targetValue целевое значение (количество единиц для выполнения)
	 * @param type        тип испытания (индивидуальное или групповое)
	 * @param startDate   дата начала испытания
	 * @param endDate     дата окончания испытания
	 * @param description описание испытания
	 * @param unit        единица измерения (например, "отжимания", "км")
	 */
	public Challenge(String id, String name, long targetValue, ChallengeType type,
	                 LocalDateTime startDate, LocalDateTime endDate, String description, String unit) {
		this.id = id;
		this.name = name;
		this.targetValue = targetValue;
		this.currentValue = 0;
		this.type = type;
		this.startDate = startDate;
		this.endDate = endDate;
		this.participantProgress = new HashMap<>();
		this.active = true;
		this.description = description;
		this.unit = unit;
		this.participants = new ArrayList<>();
		logger.debug("Создан новый экземпляр Challenge с параметрами: id={}, name={}", id, name);
	}

	/**
	 * Возвращает уникальный идентификатор испытания.
	 *
	 * @return идентификатор испытания
	 */
	public String getId() {
		return id;
	}

	/**
	 * Устанавливает уникальный идентификатор испытания.
	 *
	 * @param id идентификатор испытания
	 */
	public void setId(String id) {
		logger.debug("Установка ID испытания: {}", id);
		this.id = id;
	}

	/**
	 * Возвращает название испытания.
	 *
	 * @return название испытания
	 */
	public String getName() {
		return name;
	}

	/**
	 * Устанавливает название испытания.
	 *
	 * @param name название испытания
	 */
	public void setName(String name) {
		logger.debug("Установка названия испытания: {}", name);
		this.name = name;
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
	 * @param targetValue новое целевое значение
	 */
	public void setTargetValue(long targetValue) {
		logger.debug("Установка целевого значения испытания '{}': {}", name, targetValue);
		this.targetValue = targetValue;
	}

	/**
	 * Возвращает текущее суммарное значение прогресса по всем участникам.
	 *
	 * @return текущее значение прогресса
	 */
	public long getCurrentValue() {
		return currentValue;
	}

	/**
	 * Устанавливает текущее суммарное значение прогресса.
	 *
	 * @param currentValue новое текущее значение
	 */
	public void setCurrentValue(long currentValue) {
		logger.debug("Установка текущего значения испытания '{}': {}", name, currentValue);
		this.currentValue = currentValue;
	}

	/**
	 * Возвращает тип испытания.
	 *
	 * @return тип испытания
	 */
	public ChallengeType getType() {
		return type;
	}

	/**
	 * Устанавливает тип испытания.
	 *
	 * @param type тип испытания
	 */
	public void setType(ChallengeType type) {
		logger.debug("Установка типа испытания '{}': {}", name, type);
		this.type = type;
	}

	/**
	 * Возвращает дату начала испытания.
	 *
	 * @return дата и время начала испытания
	 */
	public LocalDateTime getStartDate() {
		return startDate;
	}

	/**
	 * Устанавливает дату начала испытания.
	 *
	 * @param startDate дата и время начала испытания
	 */
	public void setStartDate(LocalDateTime startDate) {
		logger.debug("Установка даты начала испытания '{}': {}", name, startDate);
		this.startDate = startDate;
	}

	/**
	 * Возвращает дату окончания испытания.
	 *
	 * @return дата и время окончания испытания
	 */
	public LocalDateTime getEndDate() {
		return endDate;
	}

	/**
	 * Устанавливает дату окончания испытания.
	 *
	 * @param endDate дата и время окончания испытания
	 */
	public void setEndDate(LocalDateTime endDate) {
		logger.debug("Установка даты окончания испытания '{}': {}", name, endDate);
		this.endDate = endDate;
	}

	/**
	 * Возвращает карту прогресса участников: userId -> накопленное значение.
	 *
	 * @return карта прогресса участников
	 */
	public Map<String, Long> getParticipantProgress() {
		return participantProgress;
	}

	/**
	 * Устанавливает карту прогресса участников.
	 *
	 * @param participantProgress карта прогресса участников
	 */
	public void setParticipantProgress(Map<String, Long> participantProgress) {
		logger.debug("Установка прогресса участников испытания '{}'", name);
		this.participantProgress = participantProgress;
	}

	/**
	 * Возвращает признак активности испытания.
	 *
	 * @return {@code true}, если испытание активно
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Устанавливает статус активности испытания.
	 *
	 * @param active {@code true} для активации, {@code false} для остановки
	 */
	public void setActive(boolean active) {
		logger.debug("Установка статуса активности испытания '{}': {}", name, active);
		this.active = active;
	}

	/**
	 * Возвращает описание испытания.
	 *
	 * @return описание испытания
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Устанавливает описание испытания.
	 *
	 * @param description описание испытания
	 */
	public void setDescription(String description) {
		logger.debug("Установка описания испытания '{}'", name);
		this.description = description;
	}

	/**
	 * Возвращает единицу измерения прогресса (например, "отжимания", "км").
	 *
	 * @return единица измерения
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * Устанавливает единицу измерения прогресса.
	 *
	 * @param unit единица измерения
	 */
	public void setUnit(String unit) {
		logger.debug("Установка единицы измерения испытания '{}': {}", name, unit);
		this.unit = unit;
	}

	/**
	 * Возвращает список идентификаторов участников испытания.
	 *
	 * @return список userId участников
	 */
	public List<String> getParticipants() {
		return participants;
	}

	/**
	 * Устанавливает список идентификаторов участников испытания.
	 *
	 * @param participants список userId участников
	 */
	public void setParticipants(List<String> participants) {
		logger.debug("Установка списка участников испытания '{}'", name);
		this.participants = participants;
	}

	private transient Set<String> participantsSet = null;

	private Set<String> getParticipantsSet() {
		if (participantsSet == null) {
			participantsSet = new LinkedHashSet<>(participants != null ? participants : new ArrayList<>());
		}
		return participantsSet;
	}

	// Helper methods for participant management

	/**
	 * Добавляет участника в испытание. Если участник уже присутствует, повторного добавления не происходит.
	 *
	 * @param userId идентификатор пользователя в Discord
	 */
	public void addParticipant(String userId) {
		try {
			if (userId == null || userId.isEmpty()) {
				logger.warn("Попытка добавить участника с пустым ID в испытание '{}'", name);
				return;
			}

			boolean added = getParticipantsSet().add(userId);
			if (added) {
				// Keep backing list in sync
				if (participants == null) participants = new ArrayList<>();
				if (!participants.contains(userId)) participants.add(userId);
				logger.debug("Участник '{}' добавлен в испытание '{}'", userId, name);
			} else {
				logger.debug("Участник '{}' уже присутствует в испытании '{}'", userId, name);
			}
		} catch (Exception e) {
			logger.error("Ошибка при добавлении участника '{}' в испытание '{}'", userId, name, e);
		}
	}

	/**
	 * Удаляет участника из испытания.
	 *
	 * @param userId идентификатор пользователя в Discord
	 */
	public void removeParticipant(String userId) {
		try {
			if (userId == null || userId.isEmpty()) {
				logger.warn("Попытка удалить участника с пустым ID из испытания '{}'", name);
				return;
			}

			boolean removed = getParticipantsSet().remove(userId);
			if (participants != null) participants.remove(userId);
			if (removed) {
				logger.debug("Участник '{}' удален из испытания '{}'", userId, name);
			} else {
				logger.debug("Участник '{}' не найден в испытании '{}'", userId, name);
			}
		} catch (Exception e) {
			logger.error("Ошибка при удалении участника '{}' из испытания '{}'", userId, name, e);
		}
	}

	/**
	 * Проверяет, является ли пользователь участником испытания.
	 *
	 * @param userId идентификатор пользователя в Discord
	 * @return {@code true}, если пользователь является участником
	 */
	public boolean hasParticipant(String userId) {
		try {
			if (userId == null || userId.isEmpty()) {
				logger.warn("Попытка проверить наличие участника с пустым ID в испытании '{}'", name);
				return false;
			}

			boolean hasParticipant = getParticipantsSet().contains(userId);
			logger.debug("Проверка наличия участника '{}' в испытании '{}': {}", userId, name, hasParticipant);
			return hasParticipant;
		} catch (Exception e) {
			logger.error("Ошибка при проверке наличия участника '{}' в испытании '{}'", userId, name, e);
			return false;
		}
	}

	/**
	 * Called after deserialization to rebuild the transient Set from the persisted List.
	 */
	@Serial
	private Object readResolve() {
		this.participantsSet = null; // will be lazily rebuilt from participants list
		if (this.participantProgress == null) {
			this.participantProgress = new HashMap<>();
		}
		return this;
	}
}