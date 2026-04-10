package com.discord.challengebot.repository;

import com.discord.challengebot.config.IgniteConnectionManager;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * Репозиторий для нормализованного хранения прогресса участников.
 * Работает с таблицей challenge_progress через SQL API (составной ключ).
 * Заменяет JSON-сериализацию participant_progress в таблице challenges.
 */
@Repository
public class ChallengeProgressRepository {

    private static final Logger log = LoggerFactory.getLogger(ChallengeProgressRepository.class);

    private final IgniteConnectionManager connectionManager;

    /**
     * Создаёт репозиторий прогресса участников.
     *
     * @param connectionManager менеджер подключения к Ignite 3
     */
    public ChallengeProgressRepository(IgniteConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    private IgniteClient client() {
        IgniteClient c = connectionManager.getClient();
        if (c == null) {
            throw new IllegalStateException("Ignite 3 недоступен");
        }
        return c;
    }

    /**
     * Сохраняет или обновляет прогресс участника в испытании.
     * Операция идемпотентна: при повторном вызове с теми же ключами значение перезаписывается.
     *
     * @param challengeId идентификатор испытания
     * @param userId      идентификатор пользователя
     * @param progress    новое значение прогресса
     */
    public void upsert(String challengeId, String userId, long progress) {
        try {
            client().sql().execute(null,
                    "MERGE INTO challenge_progress (challenge_id, user_id, progress) VALUES (?, ?, ?)",
                    challengeId, userId, progress);
        } catch (Exception e) {
            log.error("Ошибка при сохранении прогресса: challengeId={}, userId={}", challengeId, userId, e);
        }
    }

    /**
     * Возвращает карту прогресса всех участников указанного испытания.
     *
     * @param challengeId идентификатор испытания
     * @return карта userId -> progress
     */
    public Map<String, Long> findByChallengeId(String challengeId) {
        Map<String, Long> result = new HashMap<>();
        try (ResultSet<SqlRow> rs = client().sql().execute(null,
                "SELECT user_id, progress FROM challenge_progress WHERE challenge_id = ?",
                challengeId)) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                result.put(row.stringValue("user_id"), row.longValue("progress"));
            }
        } catch (Exception e) {
            log.error("Ошибка при получении прогресса для испытания {}", challengeId, e);
        }
        return result;
    }

    /**
     * Возвращает карту прогресса пользователя по всем испытаниям.
     *
     * @param userId идентификатор пользователя
     * @return карта challengeId -> progress
     */
    public Map<String, Long> findByUserId(String userId) {
        Map<String, Long> result = new HashMap<>();
        try (ResultSet<SqlRow> rs = client().sql().execute(null,
                "SELECT challenge_id, progress FROM challenge_progress WHERE user_id = ?",
                userId)) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                result.put(row.stringValue("challenge_id"), row.longValue("progress"));
            }
        } catch (Exception e) {
            log.error("Ошибка при получении прогресса пользователя {}", userId, e);
        }
        return result;
    }

    /**
     * Удаляет запись о прогрессе конкретного участника в конкретном испытании.
     *
     * @param challengeId идентификатор испытания
     * @param userId      идентификатор пользователя
     */
    public void delete(String challengeId, String userId) {
        try {
            client().sql().execute(null,
                    "DELETE FROM challenge_progress WHERE challenge_id = ? AND user_id = ?",
                    challengeId, userId);
        } catch (Exception e) {
            log.error("Ошибка при удалении прогресса: challengeId={}, userId={}", challengeId, userId, e);
        }
    }

    /**
     * Удаляет все записи о прогрессе для указанного испытания.
     * Вызывается при удалении испытания.
     *
     * @param challengeId идентификатор испытания
     */
    public void deleteByChallengeId(String challengeId) {
        try {
            client().sql().execute(null,
                    "DELETE FROM challenge_progress WHERE challenge_id = ?",
                    challengeId);
        } catch (Exception e) {
            log.error("Ошибка при удалении прогресса для испытания {}", challengeId, e);
        }
    }

    /**
     * Проверяет, есть ли хоть одна запись прогресса для данного испытания.
     * Используется миграционным сервисом для идемпотентности.
     *
     * @param challengeId идентификатор испытания
     * @return true если записи существуют
     */
    public boolean existsByChallengeId(String challengeId) {
        try (ResultSet<SqlRow> rs = client().sql().execute(null,
                "SELECT COUNT(*) AS cnt FROM challenge_progress WHERE challenge_id = ?",
                challengeId)) {
            if (rs.hasNext()) {
                return rs.next().longValue("cnt") > 0;
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке наличия прогресса для испытания {}", challengeId, e);
        }
        return false;
    }
}
