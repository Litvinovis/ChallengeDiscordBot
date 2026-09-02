package com.discord.challengebot.repository;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ChallengeRepository {

	private static final Logger log = LoggerFactory.getLogger(ChallengeRepository.class);
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final JdbcTemplate jdbc;

	public ChallengeRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public void save(Challenge challenge) {
		if (challenge == null || challenge.getId() == null) {
			log.warn("ChallengeRepository.save: challenge или id равен null — пропуск");
			return;
		}
		jdbc.update(
						"INSERT INTO challenges (id, name, target_value, current_value, chal_type, start_date, end_date, active, description, unit, participants) " +
										"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
										"ON CONFLICT (id) DO UPDATE SET " +
										"name = EXCLUDED.name, target_value = EXCLUDED.target_value, current_value = EXCLUDED.current_value, " +
										"chal_type = EXCLUDED.chal_type, start_date = EXCLUDED.start_date, end_date = EXCLUDED.end_date, " +
										"active = EXCLUDED.active, description = EXCLUDED.description, unit = EXCLUDED.unit, participants = EXCLUDED.participants",
						challenge.getId(),
						challenge.getName(),
						challenge.getTargetValue(),
						challenge.getCurrentValue(),
						challenge.getType() != null ? challenge.getType().name() : ChallengeType.INDIVIDUAL.name(),
						challenge.getStartDate(),
						challenge.getEndDate(),
						challenge.isActive(),
						challenge.getDescription(),
						challenge.getUnit(),
						toJson(challenge.getParticipants() != null ? challenge.getParticipants() : new ArrayList<>())
		);
	}

	public Optional<Challenge> findById(String id) {
		if (id == null) return Optional.empty();
		List<Challenge> results = jdbc.query(
						"SELECT id, name, target_value, current_value, chal_type, start_date, end_date, active, description, unit, participants FROM challenges WHERE id = ?",
						this::mapRow, id);
		return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
	}

	public Optional<Challenge> findByName(String name) {
		if (name == null) return Optional.empty();
		List<Challenge> results = jdbc.query(
						"SELECT id, name, target_value, current_value, chal_type, start_date, end_date, active, description, unit, participants FROM challenges WHERE name = ?",
						this::mapRow, name);
		return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
	}

	public List<Challenge> findAll() {
		return jdbc.query(
						"SELECT id, name, target_value, current_value, chal_type, start_date, end_date, active, description, unit, participants FROM challenges ORDER BY name",
						this::mapRow);
	}

	/**
	 * Пересчитывает current_value испытания из challenge_progress одним запросом.
	 * Точечный UPDATE вместо перезаписи всей строки не даёт параллельным командам
	 * затирать чужие изменения (цель, даты, название).
	 */
	public void refreshCurrentValue(String challengeId) {
		if (challengeId == null) return;
		jdbc.update("UPDATE challenges SET current_value = " +
										"(SELECT COALESCE(SUM(progress), 0) FROM challenge_progress WHERE challenge_id = ?) " +
										"WHERE id = ?",
						challengeId, challengeId);
	}

	/** Добавляет участника в JSON-список испытания, не трогая остальные поля. */
	public void addParticipant(String challengeId, String userId) {
		if (challengeId == null || userId == null) return;
		jdbc.update("UPDATE challenges SET participants = COALESCE((" +
										"SELECT jsonb_agg(value)::text FROM (" +
										"  SELECT jsonb_array_elements_text(participants::jsonb) AS value" +
										"  UNION SELECT ?::text" +
										") s), '[]') WHERE id = ?",
						userId, challengeId);
	}

	/** Убирает участника из JSON-списка испытания, не трогая остальные поля. */
	public void removeParticipant(String challengeId, String userId) {
		if (challengeId == null || userId == null) return;
		jdbc.update("UPDATE challenges SET participants = COALESCE((" +
										"SELECT jsonb_agg(value)::text FROM (" +
										"  SELECT jsonb_array_elements_text(participants::jsonb) AS value" +
										") s WHERE value <> ?), '[]') WHERE id = ?",
						userId, challengeId);
	}

	/** Обновляет только целевое значение испытания. */
	public void updateTargetValue(String id, long targetValue) {
		if (id == null) return;
		jdbc.update("UPDATE challenges SET target_value = ? WHERE id = ?", targetValue, id);
	}

	/** Обновляет только дату окончания испытания. */
	public void updateEndDate(String id, LocalDateTime endDate) {
		if (id == null) return;
		jdbc.update("UPDATE challenges SET end_date = ? WHERE id = ?", endDate, id);
	}

	/** Обновляет только флаг активности испытания. */
	public void updateActive(String id, boolean active) {
		if (id == null) return;
		jdbc.update("UPDATE challenges SET active = ? WHERE id = ?", active, id);
	}

	public void deleteById(String id) {
		if (id == null) return;
		jdbc.update("DELETE FROM challenges WHERE id = ?", id);
	}

	public boolean existsById(String id) {
		if (id == null) return false;
		Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM challenges WHERE id = ?", Integer.class, id);
		return count != null && count > 0;
	}

	private Challenge mapRow(ResultSet rs, int rowNum) throws SQLException {
		Challenge ch = new Challenge();
		ch.setId(rs.getString("id"));
		ch.setName(rs.getString("name"));
		ch.setTargetValue(rs.getLong("target_value"));
		ch.setCurrentValue(rs.getLong("current_value"));

		String typeStr = rs.getString("chal_type");
		if (typeStr != null && !typeStr.isBlank()) {
			try {
				ch.setType(ChallengeType.valueOf(typeStr));
			} catch (IllegalArgumentException e) {
				ch.setType(ChallengeType.INDIVIDUAL);
			}
		} else {
			ch.setType(ChallengeType.INDIVIDUAL);
		}

		ch.setStartDate(rs.getObject("start_date", LocalDateTime.class));
		ch.setEndDate(rs.getObject("end_date", LocalDateTime.class));

		ch.setActive(rs.getBoolean("active"));
		ch.setDescription(rs.getString("description"));
		ch.setUnit(rs.getString("unit"));
		ch.setParticipants(fromJsonToListString(rs.getString("participants")));
		return ch;
	}

	private static String toJson(Object value) {
		try {
			return MAPPER.writeValueAsString(value);
		} catch (Exception e) {
			// Молчаливый фолбэк затирал список участников в БД пустым — след обязателен
			log.warn("Не удалось сериализовать участников испытания, записан пустой список", e);
			return "[]";
		}
	}

	private static List<String> fromJsonToListString(String json) {
		if (json == null || json.isBlank()) return new ArrayList<>();
		try {
			return MAPPER.readValue(json, new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
			log.warn("Повреждён JSON участников в БД, использован пустой список: {}", json, e);
			return new ArrayList<>();
		}
	}
}
