package com.discord.challengebot.repository;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Репозиторий испытаний для Apache Ignite 3.
 * Маппит Challenge (с Map и List полями) в/из строковых JSON-колонок таблицы challenges.
 */
@Repository
public class ChallengeRepository {

    private static final Logger log = LoggerFactory.getLogger(ChallengeRepository.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final KeyValueView<Tuple, Tuple> view;

    /**
     * Создаёт репозиторий испытаний.
     *
     * @param igniteClient подключённый Ignite 3 thin client
     */
    public ChallengeRepository(IgniteClient igniteClient) {
        this.view = igniteClient.tables().table("challenges").keyValueView();
    }

    /**
     * Сохраняет или обновляет испытание в таблице.
     *
     * @param challenge объект Challenge
     */
    public void save(Challenge challenge) {
        if (challenge == null || challenge.getId() == null) {
            log.warn("ChallengeRepository.save: challenge или id равен null — пропуск");
            return;
        }
        Tuple key = Tuple.create().set("id", challenge.getId());
        Tuple val = challengeToRow(challenge);
        view.put(null, key, val);
    }

    /**
     * Возвращает испытание по ID.
     *
     * @param id идентификатор испытания
     * @return Optional с объектом Challenge или пустой Optional
     */
    public Optional<Challenge> findById(String id) {
        if (id == null) return Optional.empty();
        Tuple key = Tuple.create().set("id", id);
        Tuple row = view.get(null, key);
        if (row == null) return Optional.empty();
        return Optional.of(rowToChallenge(id, row));
    }

    /**
     * Возвращает все испытания из таблицы.
     *
     * @return список всех испытаний
     */
    public List<Challenge> findAll() {
        List<Challenge> result = new ArrayList<>();
        try {
            // Используем SQL для получения всех записей
            view.query(null, null).forEachRemaining(entry -> {
                String id = entry.getKey().stringValue("ID");
                result.add(rowToChallenge(id, entry.getValue()));
            });
        } catch (Exception e) {
            log.error("Ошибка при получении всех испытаний из Ignite 3: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Удаляет испытание по ID.
     *
     * @param id идентификатор испытания
     */
    public void deleteById(String id) {
        if (id == null) return;
        Tuple key = Tuple.create().set("id", id);
        view.remove(null, key);
    }

    /**
     * Проверяет наличие испытания по ID.
     *
     * @param id идентификатор испытания
     * @return true если испытание существует
     */
    public boolean existsById(String id) {
        if (id == null) return false;
        Tuple key = Tuple.create().set("id", id);
        return view.contains(null, key);
    }

    // ---- маппинг ----

    private Challenge rowToChallenge(String id, Tuple row) {
        Challenge ch = new Challenge();
        ch.setId(id);
        ch.setName(row.stringValue("NAME"));
        ch.setTargetValue(row.longValue("TARGET_VALUE"));
        ch.setCurrentValue(row.longValue("CURRENT_VALUE"));

        String typeStr = row.stringValue("CHALLENGE_TYPE");
        if (typeStr != null && !typeStr.isBlank()) {
            try {
                ch.setType(ChallengeType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                ch.setType(ChallengeType.INDIVIDUAL);
            }
        } else {
            ch.setType(ChallengeType.INDIVIDUAL);
        }

        String startDateStr = row.stringValue("START_DATE");
        if (startDateStr != null && !startDateStr.isBlank()) {
            try {
                ch.setStartDate(LocalDateTime.parse(startDateStr));
            } catch (Exception e) {
                log.warn("Не удалось распарсить start_date для испытания {}: {}", id, startDateStr);
            }
        }

        String endDateStr = row.stringValue("END_DATE");
        if (endDateStr != null && !endDateStr.isBlank()) {
            try {
                ch.setEndDate(LocalDateTime.parse(endDateStr));
            } catch (Exception e) {
                log.warn("Не удалось распарсить end_date для испытания {}: {}", id, endDateStr);
            }
        }

        ch.setActive(Boolean.TRUE.equals(row.value("ACTIVE")));
        ch.setDescription(row.stringValue("DESCRIPTION"));
        ch.setUnit(row.stringValue("UNIT"));

        String progressJson = row.stringValue("PARTICIPANT_PROGRESS");
        ch.setParticipantProgress(fromJsonToMapStringLong(progressJson));

        String participantsJson = row.stringValue("PARTICIPANTS");
        ch.setParticipants(fromJsonToListString(participantsJson));

        return ch;
    }

    private Tuple challengeToRow(Challenge ch) {
        return Tuple.create()
                .set("name", ch.getName())
                .set("target_value", ch.getTargetValue())
                .set("current_value", ch.getCurrentValue())
                .set("challenge_type", ch.getType() != null ? ch.getType().name() : ChallengeType.INDIVIDUAL.name())
                .set("start_date", ch.getStartDate() != null ? ch.getStartDate().toString() : null)
                .set("end_date", ch.getEndDate() != null ? ch.getEndDate().toString() : null)
                .set("active", ch.isActive())
                .set("description", ch.getDescription())
                .set("unit", ch.getUnit())
                .set("participant_progress", toJson(ch.getParticipantProgress() != null ? ch.getParticipantProgress() : new HashMap<>()))
                .set("participants", toJson(ch.getParticipants() != null ? ch.getParticipants() : new ArrayList<>()));
    }

    // ---- JSON helpers ----

    private static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static Map<String, Long> fromJsonToMapStringLong(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Long>>() {});
        } catch (Exception e) {
            return new HashMap<>();
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
}
