package com.discord.challengebot.repository;

import com.discord.challengebot.model.Participant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class ParticipantRepository {

	private static final Logger log = LoggerFactory.getLogger(ParticipantRepository.class);
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final JdbcTemplate jdbc;

	public ParticipantRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public void save(Participant participant) {
		if (participant == null || participant.getUserId() == null) {
			log.warn("ParticipantRepository.save: participant или userId равен null — пропуск");
			return;
		}
		jdbc.update(
						"INSERT INTO challenge_participants (user_id, username, join_date, registered_challenges, current_streak, longest_streak, last_activity_date, awarded_achievements) " +
										"VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
										"ON CONFLICT (user_id) DO UPDATE SET " +
										"username = EXCLUDED.username, join_date = EXCLUDED.join_date, " +
										"registered_challenges = EXCLUDED.registered_challenges, " +
										"current_streak = EXCLUDED.current_streak, longest_streak = EXCLUDED.longest_streak, " +
										"last_activity_date = EXCLUDED.last_activity_date, awarded_achievements = EXCLUDED.awarded_achievements",
						participant.getUserId(),
						participant.getUsername(),
						participant.getJoinDate() != null ? participant.getJoinDate().toString() : null,
						toJson(participant.getRegisteredChallenges() != null ? participant.getRegisteredChallenges() : new ArrayList<>()),
						participant.getCurrentStreak(),
						participant.getLongestStreak(),
						participant.getLastActivityDate() != null ? participant.getLastActivityDate().toString() : null,
						toJson(participant.getAwardedAchievements() != null ? participant.getAwardedAchievements() : new HashSet<>())
		);
	}

	public Optional<Participant> findById(String userId) {
		if (userId == null) return Optional.empty();
		List<Participant> results = jdbc.query(
						"SELECT user_id, username, join_date, registered_challenges, current_streak, longest_streak, last_activity_date, awarded_achievements " +
										"FROM challenge_participants WHERE user_id = ?",
						this::mapRow, userId);
		return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
	}

	public List<Participant> findAll() {
		return jdbc.query(
						"SELECT user_id, username, join_date, registered_challenges, current_streak, longest_streak, last_activity_date, awarded_achievements " +
										"FROM challenge_participants",
						this::mapRow);
	}

	public void deleteById(String userId) {
		if (userId == null) return;
		jdbc.update("DELETE FROM challenge_participants WHERE user_id = ?", userId);
	}

	public boolean existsById(String userId) {
		if (userId == null) return false;
		Integer count = jdbc.queryForObject(
						"SELECT COUNT(*) FROM challenge_participants WHERE user_id = ?", Integer.class, userId);
		return count != null && count > 0;
	}

	private Participant mapRow(ResultSet rs, int rowNum) throws SQLException {
		Participant p = new Participant();
		p.setUserId(rs.getString("user_id"));
		p.setUsername(rs.getString("username"));

		String joinDate = rs.getString("join_date");
		if (joinDate != null && !joinDate.isBlank()) {
			try {
				p.setJoinDate(LocalDateTime.parse(joinDate));
			} catch (Exception e) {
				log.warn("Не удалось распарсить join_date для участника {}: {}", p.getUserId(), joinDate);
			}
		}

		p.setRegisteredChallenges(fromJsonToListString(rs.getString("registered_challenges")));
		p.setCurrentStreak(rs.getInt("current_streak"));
		p.setLongestStreak(rs.getInt("longest_streak"));

		String lastActivity = rs.getString("last_activity_date");
		if (lastActivity != null && !lastActivity.isBlank()) {
			try {
				p.setLastActivityDate(LocalDate.parse(lastActivity));
			} catch (Exception e) {
				log.warn("Не удалось распарсить last_activity_date для участника {}: {}", p.getUserId(), lastActivity);
			}
		}

		p.setAwardedAchievements(fromJsonToSetString(rs.getString("awarded_achievements")));
		return p;
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
			return MAPPER.readValue(json, new TypeReference<>() {
			});
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private static Set<String> fromJsonToSetString(String json) {
		if (json == null || json.isBlank()) return new HashSet<>();
		try {
			return MAPPER.readValue(json, new TypeReference<>() {
			});
		} catch (Exception e) {
			return new HashSet<>();
		}
	}
}
