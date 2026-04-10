package com.discord.challengebot.repository;

import com.discord.challengebot.config.IgniteConnectionManager;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
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
import java.util.function.Function;

/**
 * Репозиторий испытаний для Apache Ignite 3.
 * Маппит Challenge (с Map и List полями) в/из строковых JSON-колонок таблицы challenges.
 * Получает клиент через {@link IgniteConnectionManager} — view автоматически сбрасывается
 * при смене клиента после переподключения.
 * <p>
 * Колонки participant_progress и participants оставлены в таблице для обратной совместимости
 * данных, но НЕ читаются при маппинге (прогресс хранится в таблице challenge_progress).
 */
@Repository
public class ChallengeRepository {

    private static final Logger log = LoggerFactory.getLogger(ChallengeRepository.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final IgniteConnectionManager connectionManager;
    private volatile IgniteClient lastClient;
    private volatile KeyValueView<Tuple, Tuple> view;

    /**
     * Создаёт репозиторий испытаний.
     *
     * @param connectionManager менеджер подключения Ignite 3
     */
    public ChallengeRepository(IgniteConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    private KeyValueView<Tuple, Tuple> view() {
        IgniteClient current = connectionManager.getClient();
        if (current == null) {
            throw new IllegalStateException("Ignite 3 недоступен — соединение ещё не установлено");
        }
        if (view == null || current != lastClient) {
            synchronized (this) {
                current = connectionManager.getClient();
                if (current == null) {
                    throw new IllegalStateException("Ignite 3 недоступен — соединение ещё не установлено");
                }
                if (view == null || current != lastClient) {
                    view = current.tables().table("challenges").keyValueView();
                    lastClient = current;
                }
            }
        }
        return view;
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
        view().put(null, key, val);
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
        Tuple row = view().get(null, key);
        if (row == null) return Optional.empty();
        return Optional.of(mapChallenge(id, col -> row.value(col.toUpperCase())));
    }

    /**
     * Возвращает все испытания из таблицы.
     *
     * @return список всех испытаний
     */
    public List<Challenge> findAll() {
        List<Challenge> result = new ArrayList<>();
        IgniteClient client = connectionManager.getClient();
        if (client == null) {
            log.warn("findAll: Ignite 3 недоступен — возврат пустого списка");
            return result;
        }
        try (ResultSet<SqlRow> rs = client.sql().execute(null,
                "SELECT id, name, target_value, current_value, chal_type, " +
                "start_date, end_date, active, description, unit, " +
                "participant_progress, participants FROM challenges ORDER BY name")) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                // Маппинг через унифицированный метод: SqlRow — регистронезависимый доступ по имени
                result.add(mapChallenge(row.stringValue("id"), col -> {
                    try { return row.value(col.toLowerCase()); } catch (Exception e) {
                        try { return row.value(col.toUpperCase()); } catch (Exception e2) { return null; }
                    }
                }));
            }
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
        view().remove(null, key);
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
        return view().contains(null, key);
    }

    // ---- унифицированный маппинг ----

    /**
     * Унифицированный метод маппинга Challenge из источника данных.
     * Принимает функцию-геттер для извлечения значения по имени колонки,
     * что позволяет использовать один код и для Tuple-based (KeyValueView), и для SqlRow.
     *
     * @param id     идентификатор испытания
     * @param getter функция получения значения по имени колонки (регистр не важен)
     * @return смаппированный объект Challenge
     */
    private Challenge mapChallenge(String id, Function<String, Object> getter) {
        Challenge ch = new Challenge();
        ch.setId(id);
        ch.setName(asString(getter.apply("NAME")));

        Object tv = getter.apply("TARGET_VALUE");
        if (tv instanceof Number n) ch.setTargetValue(n.longValue());

        Object cv = getter.apply("CURRENT_VALUE");
        if (cv instanceof Number n) ch.setCurrentValue(n.longValue());

        String typeStr = asString(getter.apply("CHAL_TYPE"));
        if (typeStr != null && !typeStr.isBlank()) {
            try {
                ch.setType(ChallengeType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                ch.setType(ChallengeType.INDIVIDUAL);
            }
        } else {
            ch.setType(ChallengeType.INDIVIDUAL);
        }

        String startDateStr = asString(getter.apply("START_DATE"));
        if (startDateStr != null && !startDateStr.isBlank()) {
            try { ch.setStartDate(LocalDateTime.parse(startDateStr)); }
            catch (Exception e) { log.warn("Не удалось распарсить start_date для испытания {}: {}", id, startDateStr); }
        }

        String endDateStr = asString(getter.apply("END_DATE"));
        if (endDateStr != null && !endDateStr.isBlank()) {
            try { ch.setEndDate(LocalDateTime.parse(endDateStr)); }
            catch (Exception e) { log.warn("Не удалось распарсить end_date для испытания {}: {}", id, endDateStr); }
        }

        Object active = getter.apply("ACTIVE");
        ch.setActive(Boolean.TRUE.equals(active));
        ch.setDescription(asString(getter.apply("DESCRIPTION")));
        ch.setUnit(asString(getter.apply("UNIT")));

        // participant_progress и participants оставлены в таблице для обратной совместимости,
        // но маппируются во вспомогательные поля Challenge только для целей миграции.
        // Основная работа с прогрессом ведётся через ChallengeProgressRepository.
        String progressJson = asString(getter.apply("PARTICIPANT_PROGRESS"));
        ch.setParticipantProgress(fromJsonToMapStringLong(progressJson));

        String participantsJson = asString(getter.apply("PARTICIPANTS"));
        ch.setParticipants(fromJsonToListString(participantsJson));

        return ch;
    }

    private static String asString(Object v) {
        return v instanceof String s ? s : (v != null ? v.toString() : null);
    }

    private Tuple challengeToRow(Challenge ch) {
        return Tuple.create()
                .set("name", ch.getName())
                .set("target_value", ch.getTargetValue())
                .set("current_value", ch.getCurrentValue())
                .set("chal_type", ch.getType() != null ? ch.getType().name() : ChallengeType.INDIVIDUAL.name())
                .set("start_date", ch.getStartDate() != null ? ch.getStartDate().toString() : null)
                .set("end_date", ch.getEndDate() != null ? ch.getEndDate().toString() : null)
                .set("active", ch.isActive())
                .set("description", ch.getDescription())
                .set("unit", ch.getUnit())
                // Сохраняем participant_progress/participants для обратной совместимости
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
