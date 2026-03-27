package com.discord.challengebot.repository;

import com.discord.challengebot.model.Participant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Репозиторий участников для Apache Ignite 3.
 * Маппит Participant (с List и Set полями) в/из строковых JSON-колонок таблицы challenge_participants.
 */
@Repository
public class ParticipantRepository {

    private static final Logger log = LoggerFactory.getLogger(ParticipantRepository.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final KeyValueView<Tuple, Tuple> view;

    /**
     * Создаёт репозиторий участников.
     *
     * @param igniteClient подключённый Ignite 3 thin client
     */
    public ParticipantRepository(IgniteClient igniteClient) {
        this.view = igniteClient.tables().table("challenge_participants").keyValueView();
    }

    /**
     * Сохраняет или обновляет участника в таблице.
     *
     * @param participant объект Participant
     */
    public void save(Participant participant) {
        if (participant == null || participant.getUserId() == null) {
            log.warn("ParticipantRepository.save: participant или userId равен null — пропуск");
            return;
        }
        Tuple key = Tuple.create().set("user_id", participant.getUserId());
        Tuple val = participantToRow(participant);
        view.put(null, key, val);
    }

    /**
     * Возвращает участника по userId.
     *
     * @param userId идентификатор пользователя Discord
     * @return Optional с объектом Participant или пустой Optional
     */
    public Optional<Participant> findById(String userId) {
        if (userId == null) return Optional.empty();
        Tuple key = Tuple.create().set("user_id", userId);
        Tuple row = view.get(null, key);
        if (row == null) return Optional.empty();
        return Optional.of(rowToParticipant(userId, row));
    }

    /**
     * Возвращает всех участников из таблицы.
     *
     * @return список всех участников
     */
    public List<Participant> findAll() {
        List<Participant> result = new ArrayList<>();
        try {
            view.query(null, null).forEachRemaining(entry -> {
                String userId = entry.getKey().stringValue("USER_ID");
                result.add(rowToParticipant(userId, entry.getValue()));
            });
        } catch (Exception e) {
            log.error("Ошибка при получении всех участников из Ignite 3: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Удаляет участника по userId.
     *
     * @param userId идентификатор пользователя Discord
     */
    public void deleteById(String userId) {
        if (userId == null) return;
        Tuple key = Tuple.create().set("user_id", userId);
        view.remove(null, key);
    }

    /**
     * Проверяет наличие участника по userId.
     *
     * @param userId идентификатор пользователя Discord
     * @return true если участник существует
     */
    public boolean existsById(String userId) {
        if (userId == null) return false;
        Tuple key = Tuple.create().set("user_id", userId);
        return view.contains(null, key);
    }

    // ---- маппинг ----

    private Participant rowToParticipant(String userId, Tuple row) {
        Participant p = new Participant();
        p.setUserId(userId);
        p.setUsername(row.stringValue("USERNAME"));

        String joinDateStr = row.stringValue("JOIN_DATE");
        if (joinDateStr != null && !joinDateStr.isBlank()) {
            try {
                p.setJoinDate(LocalDateTime.parse(joinDateStr));
            } catch (Exception e) {
                log.warn("Не удалось распарсить join_date для участника {}: {}", userId, joinDateStr);
            }
        }

        String challengesJson = row.stringValue("REGISTERED_CHALLENGES");
        p.setRegisteredChallenges(fromJsonToListString(challengesJson));

        p.setCurrentStreak(row.intValue("CURRENT_STREAK"));
        p.setLongestStreak(row.intValue("LONGEST_STREAK"));

        String lastActivityStr = row.stringValue("LAST_ACTIVITY_DATE");
        if (lastActivityStr != null && !lastActivityStr.isBlank()) {
            try {
                p.setLastActivityDate(LocalDate.parse(lastActivityStr));
            } catch (Exception e) {
                log.warn("Не удалось распарсить last_activity_date для участника {}: {}", userId, lastActivityStr);
            }
        }

        String achievementsJson = row.stringValue("AWARDED_ACHIEVEMENTS");
        p.setAwardedAchievements(fromJsonToSetString(achievementsJson));

        return p;
    }

    private Tuple participantToRow(Participant p) {
        return Tuple.create()
                .set("username", p.getUsername())
                .set("join_date", p.getJoinDate() != null ? p.getJoinDate().toString() : null)
                .set("registered_challenges", toJson(p.getRegisteredChallenges() != null ? p.getRegisteredChallenges() : new ArrayList<>()))
                .set("current_streak", p.getCurrentStreak())
                .set("longest_streak", p.getLongestStreak())
                .set("last_activity_date", p.getLastActivityDate() != null ? p.getLastActivityDate().toString() : null)
                .set("awarded_achievements", toJson(p.getAwardedAchievements() != null ? p.getAwardedAchievements() : new HashSet<>()));
    }

    // ---- JSON helpers ----

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
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static Set<String> fromJsonToSetString(String json) {
        if (json == null || json.isBlank()) return new HashSet<>();
        try {
            return MAPPER.readValue(json, new TypeReference<Set<String>>() {});
        } catch (Exception e) {
            return new HashSet<>();
        }
    }
}
