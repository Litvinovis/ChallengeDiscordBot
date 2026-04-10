package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.repository.ChallengeRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Сервис миграции прогресса участников из JSON-колонки challenges.participant_progress
 * в нормализованную таблицу challenge_progress.
 * <p>
 * Запускается автоматически при старте приложения (@PostConstruct).
 * Идемпотентен: пропускает испытания, для которых данные уже мигрированы.
 * Исходные данные в challenges.participant_progress НЕ удаляются (обратная совместимость).
 */
@Service
public class ProgressMigrationService {

    private static final Logger log = LoggerFactory.getLogger(ProgressMigrationService.class);

    private final ChallengeRepository challengeRepository;
    private final ChallengeProgressRepository progressRepository;

    /**
     * Создаёт сервис миграции прогресса.
     *
     * @param challengeRepository  репозиторий испытаний
     * @param progressRepository   репозиторий прогресса участников
     */
    public ProgressMigrationService(ChallengeRepository challengeRepository,
                                    ChallengeProgressRepository progressRepository) {
        this.challengeRepository = challengeRepository;
        this.progressRepository = progressRepository;
    }

    /**
     * Выполняет миграцию при старте: для каждого испытания переносит participant_progress
     * из JSON в таблицу challenge_progress, если данных там ещё нет (идемпотентно).
     */
    @PostConstruct
    public void migrate() {
        try {
            log.info("Запуск миграции прогресса участников из JSON в challenge_progress...");
            List<Challenge> challenges = challengeRepository.findAll();
            int totalMigrated = 0;
            int totalSkipped = 0;

            for (Challenge challenge : challenges) {
                Map<String, Long> progress = challenge.getParticipantProgress();
                if (progress == null || progress.isEmpty()) {
                    continue;
                }

                String challengeId = challenge.getId();

                // Идемпотентность: если данные уже мигрированы — пропускаем
                if (progressRepository.existsByChallengeId(challengeId)) {
                    log.debug("Прогресс для испытания '{}' уже существует в challenge_progress — пропуск", challengeId);
                    totalSkipped++;
                    continue;
                }

                // Переносим каждую запись прогресса
                int count = 0;
                for (Map.Entry<String, Long> entry : progress.entrySet()) {
                    progressRepository.upsert(challengeId, entry.getKey(), entry.getValue());
                    count++;
                }
                totalMigrated += count;
                log.info("Испытание '{}': мигрировано {} записей прогресса", challenge.getName(), count);
            }

            log.info("Миграция прогресса завершена. Перенесено записей: {}, пропущено испытаний: {}",
                    totalMigrated, totalSkipped);
        } catch (Exception e) {
            log.error("Ошибка при миграции прогресса участников: {}", e.getMessage(), e);
        }
    }
}
