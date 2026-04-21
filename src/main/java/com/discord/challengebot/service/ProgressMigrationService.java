package com.discord.challengebot.service;

import com.discord.challengebot.config.IgniteConnectionManager;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.repository.ChallengeRepository;
import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProgressMigrationService {

    private static final Logger log = LoggerFactory.getLogger(ProgressMigrationService.class);

    private final IgniteConnectionManager connectionManager;
    private final ChallengeRepository challengeRepository;
    private final ChallengeProgressRepository progressRepository;

    public ProgressMigrationService(IgniteConnectionManager connectionManager,
                                    ChallengeRepository challengeRepository,
                                    ChallengeProgressRepository progressRepository) {
        this.connectionManager = connectionManager;
        this.challengeRepository = challengeRepository;
        this.progressRepository = progressRepository;
    }

    private IgniteClient client() {
        IgniteClient c = connectionManager.getClient();
        if (c == null) throw new IllegalStateException("Ignite 3 недоступен");
        return c;
    }

    /**
     * Мигрирует прогресс участников из JSON-поля Challenge.participantProgress
     * в нормализованную таблицу challenge_progress, если данных там ещё нет (идемпотентно).
     */
    public void migrate() {
        log.info("Запуск миграции прогресса участников из JSON в challenge_progress...");

        List<Challenge> challenges = challengeRepository.findAll();
        int totalMigrated = 0;
        int totalSkipped = 0;

        for (Challenge challenge : challenges) {
            String challengeId = challenge.getId();
            Map<String, Long> progress = challenge.getParticipantProgress();

            if (progress == null || progress.isEmpty()) {
                totalSkipped++;
                continue;
            }

            if (progressRepository.existsByChallengeId(challengeId)) {
                log.debug("Прогресс для испытания '{}' уже существует в challenge_progress — пропуск", challengeId);
                totalSkipped++;
                continue;
            }

            int count = 0;
            for (Map.Entry<String, Long> entry : progress.entrySet()) {
                try {
                    progressRepository.upsert(challengeId, entry.getKey(), entry.getValue());
                    count++;
                } catch (Exception e) {
                    log.error("Ошибка при миграции прогресса: challengeId={}, userId={}",
                            challengeId, entry.getKey(), e);
                }
            }
            totalMigrated += count;
            log.info("Испытание '{}': мигрировано {} записей прогресса", challenge.getName(), count);
        }

        log.info("Миграция прогресса завершена. Перенесено записей: {}, пропущено испытаний: {}",
                totalMigrated, totalSkipped);
    }

    /**
     * Создаёт таблицу challenge_progress если она не существует.
     */
    private void createChallengeProgressTable() {
        try {
            client().sql().execute(null,
                "CREATE TABLE IF NOT EXISTS challenge_progress (" +
                "  challenge_id VARCHAR NOT NULL," +
                "  user_id VARCHAR NOT NULL," +
                "  progress BIGINT NOT NULL DEFAULT 0," +
                "  PRIMARY KEY (challenge_id, user_id)" +
                ") ZONE challengebot");
            log.info("Таблица challenge_progress создана успешно");
        } catch (Exception e) {
            log.error("Ошибка при создании таблицы challenge_progress", e);
            throw new RuntimeException("Не удалось создать таблицу challenge_progress", e);
        }
    }
}
