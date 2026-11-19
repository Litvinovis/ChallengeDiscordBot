package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.repository.ParticipantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChallengeBackupService {
    private static final List<String> TABLES = List.of(
            "challenges", "challenge_progress", "challenge_participants", "progress_history", "challenge_archive");

    private final JdbcTemplate jdbc;
    private final ChallengeRepository challenges;
    private final ChallengeProgressRepository progress;
    private final ParticipantRepository participants;

    public ChallengeBackupService(JdbcTemplate jdbc, ChallengeRepository challenges,
                                  ChallengeProgressRepository progress, ParticipantRepository participants) {
        this.jdbc = jdbc;
        this.challenges = challenges;
        this.progress = progress;
        this.participants = participants;
    }

    public String exportSnapshot() {
        // Один SQL-запрос видит согласованный снимок всех таблиц, включая архив и историю.
        return jdbc.queryForObject("""
                SELECT jsonb_build_object(
                  'version', 2, 'exportedAt', now(),
                  'challenges', COALESCE((SELECT jsonb_agg(c ORDER BY id) FROM challenges c), '[]'::jsonb),
                  'challenge_progress', COALESCE((SELECT jsonb_agg(p ORDER BY challenge_id, user_id)
                                                  FROM challenge_progress p), '[]'::jsonb),
                  'challenge_participants', COALESCE((SELECT jsonb_agg(u ORDER BY user_id)
                                                       FROM challenge_participants u), '[]'::jsonb),
                  'progress_history', COALESCE((SELECT jsonb_agg(h ORDER BY id) FROM progress_history h), '[]'::jsonb),
                  'challenge_archive', COALESCE((SELECT jsonb_agg(a ORDER BY id) FROM challenge_archive a), '[]'::jsonb)
                )::text
                """, String.class);
    }

    @Transactional
    public int restore(JsonNode backup) {
        if (backup == null || !backup.isObject()) throw new IllegalArgumentException("Нужен JSON-бэкап спортивного бота.");
        int version = backup.path("version").asInt(1);
        return switch (version) {
            case 1 -> restoreLegacy(backup);
            case 2 -> restoreSnapshot(backup);
            default -> throw new IllegalArgumentException("Неподдерживаемая версия JSON-бэкапа.");
        };
    }

    private int restoreSnapshot(JsonNode backup) {
        for (String table : TABLES) {
            if (!backup.path(table).isArray()) throw new IllegalArgumentException("В JSON-бэкапе отсутствует раздел " + table + ".");
        }
        // Имена таблиц берём только из константы. Блокировки не дают командам или второму импорту
        // добавить строки между проверкой пустого хранилища и восстановлением.
        jdbc.execute("LOCK TABLE " + String.join(", ", TABLES) + " IN SHARE ROW EXCLUSIVE MODE");
        for (String table : TABLES) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM " + table + ")", Boolean.class))) {
                throw new IllegalArgumentException("Полный бэкап восстанавливается только в пустую базу.");
            }
        }
        for (String table : TABLES) {
            jdbc.update("INSERT INTO " + table + " SELECT * FROM jsonb_populate_recordset(NULL::" + table + ", ?::jsonb)",
                    backup.path(table).toString());
        }
        long nextId = jdbc.queryForObject("SELECT COALESCE(max(id), 0) + 1 FROM progress_history", Long.class);
        jdbc.execute("ALTER SEQUENCE progress_history_id_seq RESTART WITH " + nextId);
        return backup.path("challenges").size();
    }

    private int restoreLegacy(JsonNode backup) {
        if (!backup.path("challenges").isArray() || !backup.path("progress").isObject()) {
            throw new IllegalArgumentException("В старом бэкапе нужны испытания и прогресс.");
        }
        jdbc.execute("LOCK TABLE challenges IN SHARE ROW EXCLUSIVE MODE");
        int imported = 0;
        for (JsonNode row : backup.path("challenges")) {
            String name = row.path("name").asText("");
            String id = row.path("id").asText(name.toLowerCase(java.util.Locale.ROOT).replace(" ", "_"));
            long target = row.path("targetValue").asLong(0);
            if (id.isBlank() || name.isBlank() || target <= 0) throw new IllegalArgumentException("Некорректное испытание в бэкапе.");
            if (challenges.findById(id).isPresent() || challenges.findByName(name).isPresent()) continue;

            Challenge challenge = new Challenge(id, name, target,
                    ChallengeType.valueOf(row.path("type").asText("GROUP")), readDate(row.path("startDate")),
                    readLegacyDeadline(row.path("endDate")), row.path("description").asText(null), row.path("unit").asText(null));
            challenge.setActive(row.path("active").asBoolean(true));
            challenges.save(challenge);
            JsonNode members = backup.path("progress").path(id);
            if (!members.isMissingNode() && !members.isObject()) {
                throw new IllegalArgumentException("Некорректный прогресс в бэкапе.");
            }
            members.fields().forEachRemaining(entry -> {
                String userId = entry.getKey();
                if (userId.isBlank() || !entry.getValue().isIntegralNumber()
                        || !entry.getValue().canConvertToLong() || entry.getValue().longValue() < 0) {
                    throw new IllegalArgumentException("Некорректный прогресс в бэкапе.");
                }
                Participant participant = participants.findById(userId).orElseGet(() -> new Participant(userId, userId));
                participant.addChallenge(name);
                participants.save(participant);
                progress.upsert(id, userId, entry.getValue().longValue());
                challenges.addParticipant(id, userId);
            });
            challenges.refreshCurrentValue(id);
            imported++;
        }
        return imported;
    }

    private static LocalDateTime readDate(JsonNode value) {
        return value.isTextual() ? LocalDateTime.parse(value.asText()) : null;
    }

    private static LocalDateTime readLegacyDeadline(JsonNode value) {
        var date = readDate(value);
        return date != null && date.toLocalTime().equals(java.time.LocalTime.MIDNIGHT) ? date.plusDays(1) : date;
    }
}
