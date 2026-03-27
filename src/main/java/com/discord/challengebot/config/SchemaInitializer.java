package com.discord.challengebot.config;

import jakarta.annotation.PostConstruct;
import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Инициализатор схемы базы данных для Apache Ignite 3.
 * Читает DDL из schema.sql и выполняет через IgniteClient SQL API при старте приложения.
 */
@Component
public class SchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private final IgniteClient igniteClient;

    /**
     * Создаёт инициализатор схемы.
     *
     * @param igniteClient подключённый Ignite 3 thin client
     */
    public SchemaInitializer(IgniteClient igniteClient) {
        this.igniteClient = igniteClient;
    }

    /**
     * Выполняет DDL-скрипт из classpath-ресурса schema.sql.
     * Операторы разделяются по ';'. Каждый выполняется отдельно.
     * Ошибки логируются, но не останавливают процесс (таблица может уже существовать).
     */
    @PostConstruct
    public void init() {
        log.info("Инициализация схемы Ignite 3...");
        String sql = loadSqlResource();
        if (sql == null || sql.isBlank()) {
            log.warn("DDL-скрипт schema.sql пуст или не найден — пропуск инициализации схемы");
            return;
        }

        // Разбиваем по ';', фильтруем комментарии и пустые строки
        String[] statements = sql.split(";");
        int ok = 0;
        int failed = 0;
        for (String raw : statements) {
            String stmt = Arrays.stream(raw.split("\n"))
                    .filter(line -> !line.trim().startsWith("--") && !line.trim().isEmpty())
                    .collect(Collectors.joining("\n"))
                    .trim();
            if (stmt.isEmpty()) continue;

            try {
                igniteClient.sql().execute(null, stmt);
                ok++;
            } catch (Exception e) {
                // IF NOT EXISTS — всё равно может выбросить при наличии объекта в части реализаций
                log.warn("DDL statement failed: {} | stmt: {}", e.getMessage(), stmt.replace("\n", " "));
                failed++;
            }
        }
        log.info("Схема Ignite 3 инициализирована: {} успешно, {} пропущено/ошибок", ok, failed);
    }

    private String loadSqlResource() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) return null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            log.error("Ошибка чтения schema.sql: {}", e.getMessage());
            return null;
        }
    }
}
