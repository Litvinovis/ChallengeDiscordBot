package com.discord.challengebot.service;

import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.repository.ChallengeRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@DependsOn("schemaInitializer")
public class ProgressMigrationService {

    private static final Logger log = LoggerFactory.getLogger(ProgressMigrationService.class);

    private final ChallengeRepository challengeRepository;
    private final ChallengeProgressRepository progressRepository;

    public ProgressMigrationService(ChallengeRepository challengeRepository,
                                    ChallengeProgressRepository progressRepository) {
        this.challengeRepository = challengeRepository;
        this.progressRepository = progressRepository;
    }

    /**
     * Мигрирует прогресс участников из JSON-колонки participant_progress
     * в нормализованную таблицу challenge_progress, если данных там ещё нет (идемпотентно).
     * Читает JSON напрямую из БД, так как ChallengeRepository.mapChallenge намеренно
     * не загружает participant_progress в объект Challenge.
     * Запускается автоматически при старте после инициализации схемы.
     */
    @PostConstruct
    public void migrate() {
        log.info("Запуск миграции прогресса участников из JSON в challenge_progress...");

        Map<String, Map<String, Long>> legacyData = challengeRepository.findAllLegacyParticipantProgress();
        int totalMigrated = 0;
        int totalSkipped = 0;

        for (Map.Entry<String, Map<String, Long>> challengeEntry : legacyData.entrySet()) {
            String challengeId = challengeEntry.getKey();
            Map<String, Long> progress = challengeEntry.getValue();

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
            log.info("Испытание '{}': мигрировано {} записей прогресса", challengeId, count);
        }

        log.info("Миграция прогресса завершена. Перенесено записей: {}, пропущено испытаний: {}",
                totalMigrated, totalSkipped);
    }

}
