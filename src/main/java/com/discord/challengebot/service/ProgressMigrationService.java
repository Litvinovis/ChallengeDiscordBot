    /**
     * Мигрирует прогресс участников из JSON-поля Challenge.progressParticipants
     * в нормализованную таблицу challenge_progress, если данных там ещё нет (идемпотентно).
     */
    @Transactional
    public void migrate() {
        log.info("Запуск миграции прогресса участников из JSON в challenge_progress...");
        
        // Проверяем существование таблицы challenge_progress
        try {
            client().sql().execute(null, "SELECT COUNT(*) FROM challenge_progress LIMIT 1");
            log.info("Таблица challenge_progress существует");
        } catch (Exception e) {
            log.warn("Таблица challenge_progress не существует, создаём...");
            createChallengeProgressTable();
        }
        
        List<Challenge> challenges = challengeRepository.findAll();
        int totalMigrated = 0;
        int totalSkipped = 0;

        for (Challenge challenge : challenges) {
            String challengeId = challenge.getId();
            Map<String, Long> progress = challenge.getProgressParticipants();

            if (progress == null || progress.isEmpty()) {
                totalSkipped++;
                continue;
            }

            // Проверяем, есть ли уже записи для этого испытания
            boolean hasExistingProgress = false;
            try {
                Long existingCount = client().sql().execute(null,
                        "SELECT COUNT(*) FROM challenge_progress WHERE challenge_id = ?",
                        challengeId).get(0).get(0, Long.class);
                hasExistingProgress = existingCount > 0;
            } catch (Exception e) {
                log.debug("Ошибка при проверке существующих записей для challengeId={}", challengeId, e);
            }

            if (hasExistingProgress) {
                log.debug("Прогресс для испытания '{}' уже существует в challenge_progress — пропуск", challengeId);
                totalSkipped++;
                continue;
            }

            // Переносим каждую запись прогресса
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
                ")");
            log.info("Таблица challenge_progress создана успешно");
        } catch (Exception e) {
            log.error("Ошибка при создании таблицы challenge_progress", e);
            throw new RuntimeException("Не удалось создать таблицу challenge_progress", e);
        }
    }