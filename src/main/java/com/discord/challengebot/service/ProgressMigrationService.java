package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.repository.ChallengeRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.List;
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
     * Мигрирует прогресс участников из JSON-поля Challenge.participantProgress
     * в нормализованную таблицу challenge_progress, если данных там ещё нет (идемпотентно).
     * Запускается автоматически при старте после инициализации схемы.
     */
    @PostConstruct
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

}
