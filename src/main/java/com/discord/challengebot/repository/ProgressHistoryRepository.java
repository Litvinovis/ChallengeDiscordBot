package com.discord.challengebot.repository;

import com.discord.challengebot.model.ProgressRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.discord.challengebot.util.TimeZones;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ProgressHistoryRepository {

    private final JdbcTemplate jdbc;

    public ProgressHistoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(String challengeId, String userId, String username, long amount) {
        jdbc.update(
                "INSERT INTO progress_history (challenge_id, user_id, username, amount) VALUES (?, ?, ?, ?)",
                challengeId, userId, username, amount);
    }

    /** Daily totals for a specific user in a challenge over last N days. */
    public Map<LocalDate, Long> getDailyTotals(String challengeId, String userId, int days) {
        String sql = "SELECT DATE(recorded_at AT TIME ZONE 'Europe/Moscow') AS day, SUM(amount) AS total " +
                "FROM progress_history WHERE challenge_id = ? AND user_id = ? " +
                "AND recorded_at >= NOW() - INTERVAL '" + days + " days' " +
                "GROUP BY day ORDER BY day";
        Map<LocalDate, Long> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            result.put(rs.getObject("day", LocalDate.class), rs.getLong("total"));
        }, challengeId, userId);
        return result;
    }

    /** Daily totals for all users in a challenge combined over last N days. */
    public Map<LocalDate, Long> getDailyTotalsAll(String challengeId, int days) {
        String sql = "SELECT DATE(recorded_at AT TIME ZONE 'Europe/Moscow') AS day, SUM(amount) AS total " +
                "FROM progress_history WHERE challenge_id = ? " +
                "AND recorded_at >= NOW() - INTERVAL '" + days + " days' " +
                "GROUP BY day ORDER BY day";
        Map<LocalDate, Long> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            result.put(rs.getObject("day", LocalDate.class), rs.getLong("total"));
        }, challengeId);
        return result;
    }

    /** Total amount for a specific user in a challenge over last N hours. */
    public long getTotalLastHours(String challengeId, String userId, int hours) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM progress_history " +
                "WHERE challenge_id = ? AND user_id = ? " +
                "AND recorded_at >= NOW() - INTERVAL '" + hours + " hours'";
        Long result = jdbc.queryForObject(sql, Long.class, challengeId, userId);
        return result != null ? result : 0L;
    }

    /** Total per user for a challenge in a date range. Returns Map<userId, total>. */
    public Map<String, Long> getUserTotalsInRange(String challengeId, LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT user_id, SUM(amount) AS total FROM progress_history " +
                "WHERE challenge_id = ? AND recorded_at >= ? AND recorded_at < ? " +
                "GROUP BY user_id";
        Map<String, Long> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            result.put(rs.getString("user_id"), rs.getLong("total"));
        }, challengeId,
                Timestamp.from(from.atZone(TimeZones.MOSCOW).toInstant()),
                Timestamp.from(to.atZone(TimeZones.MOSCOW).toInstant()));
        return result;
    }

    /** Best single day (date with highest total) for a specific user in a challenge. */
    public LocalDate getBestDay(String challengeId, String userId) {
        String sql = "SELECT DATE(recorded_at AT TIME ZONE 'Europe/Moscow') AS day, SUM(amount) AS total " +
                "FROM progress_history WHERE challenge_id = ? AND user_id = ? " +
                "GROUP BY day ORDER BY total DESC LIMIT 1";
        List<LocalDate> result = new ArrayList<>();
        jdbc.query(sql, rs -> {
            result.add(rs.getObject("day", LocalDate.class));
        }, challengeId, userId);
        return result.isEmpty() ? null : result.get(0);
    }

    /** Best single day across all users for a challenge (date + total). */
    public Map.Entry<LocalDate, Long> getBestDayAll(String challengeId) {
        String sql = "SELECT DATE(recorded_at AT TIME ZONE 'Europe/Moscow') AS day, SUM(amount) AS total " +
                "FROM progress_history WHERE challenge_id = ? " +
                "GROUP BY day ORDER BY total DESC LIMIT 1";
        List<Map.Entry<LocalDate, Long>> result = new ArrayList<>();
        jdbc.query(sql, rs -> {
            result.add(Map.entry(rs.getObject("day", LocalDate.class), rs.getLong("total")));
        }, challengeId);
        return result.isEmpty() ? null : result.get(0);
    }

    /** All records for a specific user in a challenge, ordered by recorded_at. */
    public List<ProgressRecord> getAllRecords(String challengeId, String userId) {
        String sql = "SELECT challenge_id, user_id, username, amount, " +
                "recorded_at AT TIME ZONE 'Europe/Moscow' AS recorded_at_local " +
                "FROM progress_history WHERE challenge_id = ? AND user_id = ? ORDER BY recorded_at";
        List<ProgressRecord> result = new ArrayList<>();
        jdbc.query(sql, rs -> {
            Timestamp ts = rs.getTimestamp("recorded_at_local");
            LocalDateTime ldt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
            result.add(new ProgressRecord(
                    rs.getString("challenge_id"),
                    rs.getString("user_id"),
                    rs.getString("username"),
                    rs.getLong("amount"),
                    ldt));
        }, challengeId, userId);
        return result;
    }

    /** Sum per user in the last 24 hours for a challenge. Returns Map<userId, total>. */
    public Map<String, Long> getUserTotalsLast24Hours(String challengeId) {
        String sql = "SELECT user_id, SUM(amount) AS total FROM progress_history " +
                "WHERE challenge_id = ? AND recorded_at >= NOW() - INTERVAL '24 hours' " +
                "GROUP BY user_id ORDER BY total DESC";
        Map<String, Long> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            result.put(rs.getString("user_id"), rs.getLong("total"));
        }, challengeId);
        return result;
    }
}
