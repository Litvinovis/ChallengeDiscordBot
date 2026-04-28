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
						challenge.getStartDate() != null ? challenge.getStartDate().toString() : null,
						challenge.getEndDate() != null ? challenge.getEndDate().toString() : null,
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

	public List<Challenge> findAll() {
		return jdbc.query(
						"SELECT id, name, target_value, current_value, chal_type, start_date, end_date, active, description, unit, participants FROM challenges ORDER BY name",
						this::mapRow);
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

		String startDate = rs.getString("start_date");
		if (startDate != null && !startDate.isBlank()) {
			try {
				ch.setStartDate(LocalDateTime.parse(startDate));
			} catch (Exception e) {
				log.warn("Не удалось распарсить start_date для испытания {}: {}", ch.getId(), startDate);
			}
		}

		String endDate = rs.getString("end_date");
		if (endDate != null && !endDate.isBlank()) {
			try {
				ch.setEndDate(LocalDateTime.parse(endDate));
			} catch (Exception e) {
				log.warn("Не удалось распарсить end_date для испытания {}: {}", ch.getId(), endDate);
			}
		}

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
			return "[]";
		}
	}

	private static List<String> fromJsonToListString(String json) {
		if (json == null || json.isBlank()) return new ArrayList<>();
		try {
			return MAPPER.readValue(json, new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}
}
