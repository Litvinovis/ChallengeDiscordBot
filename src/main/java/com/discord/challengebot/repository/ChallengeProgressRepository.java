package com.discord.challengebot.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ChallengeProgressRepository {

	private static final Logger log = LoggerFactory.getLogger(ChallengeProgressRepository.class);

	private final JdbcTemplate jdbc;

	public ChallengeProgressRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public void upsert(String challengeId, String userId, long progress) {
		jdbc.update(
						"INSERT INTO challenge_progress (challenge_id, user_id, progress) VALUES (?, ?, ?) " +
										"ON CONFLICT (challenge_id, user_id) DO UPDATE SET progress = EXCLUDED.progress",
						challengeId, userId, progress);
	}

	/** Атомарно прибавляет amount к прогрессу участника (защита от гонок при одновременных командах). */
	public void addAmount(String challengeId, String userId, long amount) {
		jdbc.update(
						"INSERT INTO challenge_progress (challenge_id, user_id, progress) VALUES (?, ?, ?) " +
										"ON CONFLICT (challenge_id, user_id) DO UPDATE SET progress = challenge_progress.progress + EXCLUDED.progress",
						challengeId, userId, amount);
	}

	/**
	 * Атомарно вычитает amount из прогресса участника, не опускаясь ниже нуля.
	 * Строка не создаётся, если участник ещё ничего не вносил.
	 *
	 * @return фактически вычтенное значение (меньше amount, если прогресса не хватило)
	 */
	public long subtractAmount(String challengeId, String userId, long amount) {
		List<Long> removed = jdbc.query(
						"UPDATE challenge_progress cp SET progress = GREATEST(0, cp.progress - ?) " +
										"FROM (SELECT progress FROM challenge_progress WHERE challenge_id = ? AND user_id = ? FOR UPDATE) old " +
										"WHERE cp.challenge_id = ? AND cp.user_id = ? " +
										"RETURNING old.progress - cp.progress",
						(rs, rowNum) -> rs.getLong(1),
						amount, challengeId, userId, challengeId, userId);
		return removed.isEmpty() ? 0L : removed.getFirst();
	}

	/** Прогресс всех участников по всем испытаниям одним запросом: challengeId -> (userId -> progress). */
	public Map<String, Map<String, Long>> findAllGroupedByChallenge() {
		Map<String, Map<String, Long>> result = new HashMap<>();
		jdbc.query(
						"SELECT challenge_id, user_id, progress FROM challenge_progress",
						(RowCallbackHandler) rs -> result
										.computeIfAbsent(rs.getString("challenge_id"), k -> new HashMap<>())
										.put(rs.getString("user_id"), rs.getLong("progress")));
		return result;
	}

	public Map<String, Long> findByChallengeId(String challengeId) {
		Map<String, Long> result = new HashMap<>();
		jdbc.query(
						"SELECT user_id, progress FROM challenge_progress WHERE challenge_id = ?",
						(RowCallbackHandler) rs -> result.put(rs.getString("user_id"), rs.getLong("progress")),
						challengeId);
		return result;
	}

	public Map<String, Long> findByUserId(String userId) {
		Map<String, Long> result = new HashMap<>();
		jdbc.query(
						"SELECT challenge_id, progress FROM challenge_progress WHERE user_id = ?",
						(RowCallbackHandler) rs -> result.put(rs.getString("challenge_id"), rs.getLong("progress")),
						userId);
		return result;
	}

	public void delete(String challengeId, String userId) {
		jdbc.update("DELETE FROM challenge_progress WHERE challenge_id = ? AND user_id = ?", challengeId, userId);
	}

	public void deleteByChallengeId(String challengeId) {
		jdbc.update("DELETE FROM challenge_progress WHERE challenge_id = ?", challengeId);
	}

	public boolean existsByChallengeId(String challengeId) {
		Integer count = jdbc.queryForObject(
						"SELECT COUNT(*) FROM challenge_progress WHERE challenge_id = ?", Integer.class, challengeId);
		return count != null && count > 0;
	}
}
