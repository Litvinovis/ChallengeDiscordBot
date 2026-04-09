package com.discord.challengebot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Утилита экспорта и импорта данных для миграции Apache Ignite 3.0.0 → 3.1.0.
 *
 * <p>Использование:</p>
 * <pre>
 *   # Экспорт из работающего Ignite 3.0.0:
 *   java -cp challenge-bot-1.0.0.jar com.discord.challengebot.util.DataExport export /backup/ignite-data 127.0.0.1:10300
 *
 *   # Импорт в новый Ignite 3.1.0:
 *   java -cp challenge-bot-1.0.0.jar com.discord.challengebot.util.DataExport import /backup/ignite-data 127.0.0.1:10300
 *
 *   # Только проверка количества строк:
 *   java -cp challenge-bot-1.0.0.jar com.discord.challengebot.util.DataExport verify /backup/ignite-data 127.0.0.1:10300
 * </pre>
 */
public class DataExport {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String[] TABLES = {"challenges", "challenge_participants"};

    private static final Map<String, String> TABLE_QUERIES = Map.of(
        "challenges",
            "SELECT id, name, target_value, current_value, chal_type, " +
            "start_date, end_date, active, description, unit, " +
            "participant_progress, participants FROM challenges",
        "challenge_participants",
            "SELECT user_id, username, join_date, registered_challenges, " +
            "current_streak, longest_streak, last_activity_date, awarded_achievements " +
            "FROM challenge_participants"
    );

    private static final Map<String, String> TABLE_INSERT = Map.of(
        "challenges",
            "INSERT INTO challenges (id, name, target_value, current_value, chal_type, " +
            "start_date, end_date, active, description, unit, participant_progress, participants) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        "challenge_participants",
            "INSERT INTO challenge_participants (user_id, username, join_date, registered_challenges, " +
            "current_streak, longest_streak, last_activity_date, awarded_achievements) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    );

    private static final Map<String, String[]> TABLE_COLUMNS = Map.of(
        "challenges",
            new String[]{"id", "name", "target_value", "current_value", "chal_type",
                         "start_date", "end_date", "active", "description", "unit",
                         "participant_progress", "participants"},
        "challenge_participants",
            new String[]{"user_id", "username", "join_date", "registered_challenges",
                         "current_streak", "longest_streak", "last_activity_date", "awarded_achievements"}
    );

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        String mode = args[0];
        String backupDir = args[1];
        String address = args.length >= 3 ? args[2] : "127.0.0.1:10300";

        System.out.println("[DataExport] Mode: " + mode);
        System.out.println("[DataExport] Directory: " + backupDir);
        System.out.println("[DataExport] Ignite address: " + address);

        switch (mode) {
            case "export" -> export(backupDir, address);
            case "import" -> importData(backupDir, address);
            case "verify" -> verify(backupDir, address);
            default -> {
                System.err.println("Unknown mode: " + mode);
                printUsage();
                System.exit(1);
            }
        }
    }

    // ---- EXPORT ----

    private static void export(String backupDir, String address) throws Exception {
        Path dir = Paths.get(backupDir);
        Files.createDirectories(dir);

        try (IgniteClient client = IgniteClient.builder().addresses(address).build()) {
            System.out.println("[Export] Connected to Ignite at " + address);

            for (String table : TABLES) {
                int count = exportTable(client, table, dir);
                System.out.printf("[Export] %-30s → %d rows%n", table, count);
            }

            System.out.println("[Export] Done. Files written to: " + dir.toAbsolutePath());
        }
    }

    private static int exportTable(IgniteClient client, String table, Path dir) throws Exception {
        ArrayNode rows = MAPPER.createArrayNode();
        String[] columns = TABLE_COLUMNS.get(table);

        try (ResultSet<SqlRow> rs = client.sql().execute(null, TABLE_QUERIES.get(table))) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                ObjectNode node = MAPPER.createObjectNode();
                for (String col : columns) {
                    Object val = row.value(col);
                    if (val == null) {
                        node.putNull(col);
                    } else if (val instanceof Boolean b) {
                        node.put(col, b);
                    } else if (val instanceof Long l) {
                        node.put(col, l);
                    } else if (val instanceof Integer i) {
                        node.put(col, i);
                    } else {
                        node.put(col, val.toString());
                    }
                }
                rows.add(node);
            }
        }

        File outFile = dir.resolve(table + ".json").toFile();
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(outFile, rows);
        return rows.size();
    }

    // ---- IMPORT ----

    private static void importData(String backupDir, String address) throws Exception {
        Path dir = Paths.get(backupDir);

        try (IgniteClient client = IgniteClient.builder().addresses(address).build()) {
            System.out.println("[Import] Connected to Ignite at " + address);

            // Сначала создаём схему, затем импортируем
            initSchema(client);

            for (String table : TABLES) {
                File file = dir.resolve(table + ".json").toFile();
                if (!file.exists()) {
                    System.out.println("[Import] Skipping " + table + " — file not found: " + file);
                    continue;
                }
                int count = importTable(client, table, file);
                System.out.printf("[Import] %-30s ← %d rows%n", table, count);
            }

            System.out.println("[Import] Done.");
        }
    }

    private static int importTable(IgniteClient client, String table, File file) throws Exception {
        var rows = MAPPER.readTree(file);
        String[] columns = TABLE_COLUMNS.get(table);
        String insertSql = TABLE_INSERT.get(table);
        int count = 0;

        for (var row : rows) {
            Object[] params = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) {
                var node = row.get(columns[i]);
                if (node == null || node.isNull()) {
                    params[i] = null;
                } else if (node.isBoolean()) {
                    params[i] = node.booleanValue();
                } else if (node.isLong() || node.isInt()) {
                    params[i] = node.longValue();
                } else {
                    params[i] = node.asText();
                }
            }
            try {
                client.sql().execute(null, insertSql, params);
                count++;
            } catch (Exception e) {
                // Пропускаем дубликаты (ON DUPLICATE KEY)
                if (!e.getMessage().contains("duplicate") && !e.getMessage().contains("primary key")) {
                    System.err.printf("[Import] Row insert error in %s: %s%n", table, e.getMessage());
                }
            }
        }
        return count;
    }

    private static void initSchema(IgniteClient client) {
        String[] ddl = {
            "CREATE ZONE IF NOT EXISTS challengebot WITH STORAGE_PROFILES='default', REPLICAS=1, PARTITIONS=25",
            "CREATE TABLE IF NOT EXISTS challenges (" +
                "id VARCHAR PRIMARY KEY, name VARCHAR NOT NULL, " +
                "target_value BIGINT NOT NULL DEFAULT 0, current_value BIGINT NOT NULL DEFAULT 0, " +
                "chal_type VARCHAR NOT NULL DEFAULT 'INDIVIDUAL', " +
                "start_date VARCHAR, end_date VARCHAR, active BOOLEAN NOT NULL DEFAULT true, " +
                "description VARCHAR, unit VARCHAR, " +
                "participant_progress VARCHAR NOT NULL DEFAULT '{}', " +
                "participants VARCHAR NOT NULL DEFAULT '[]'" +
                ") ZONE challengebot",
            "CREATE TABLE IF NOT EXISTS challenge_participants (" +
                "user_id VARCHAR PRIMARY KEY, username VARCHAR, join_date VARCHAR, " +
                "registered_challenges VARCHAR NOT NULL DEFAULT '[]', " +
                "current_streak INT NOT NULL DEFAULT 0, longest_streak INT NOT NULL DEFAULT 0, " +
                "last_activity_date VARCHAR, awarded_achievements VARCHAR NOT NULL DEFAULT '[]'" +
                ") ZONE challengebot"
        };

        for (String stmt : ddl) {
            try {
                client.sql().execute(null, stmt);
            } catch (Exception e) {
                System.out.println("[Import] DDL skipped (already exists): " + e.getMessage());
            }
        }
    }

    // ---- VERIFY ----

    private static void verify(String backupDir, String address) throws Exception {
        Path dir = Paths.get(backupDir);
        boolean allMatch = true;

        try (IgniteClient client = IgniteClient.builder().addresses(address).build()) {
            System.out.println("[Verify] Connected to Ignite at " + address);

            for (String table : TABLES) {
                File file = dir.resolve(table + ".json").toFile();
                if (!file.exists()) {
                    System.out.println("[Verify] Backup file missing: " + file);
                    allMatch = false;
                    continue;
                }

                int backupCount = MAPPER.readTree(file).size();
                int liveCount = 0;
                try (ResultSet<SqlRow> rs = client.sql().execute(null,
                        "SELECT COUNT(*) AS cnt FROM " + table)) {
                    if (rs.hasNext()) {
                        liveCount = (int) rs.next().longValue("cnt");
                    }
                }

                String status = backupCount == liveCount ? "OK" : "MISMATCH";
                System.out.printf("[Verify] %-30s backup=%d live=%d [%s]%n",
                        table, backupCount, liveCount, status);
                if (!status.equals("OK")) allMatch = false;
            }
        }

        if (allMatch) {
            System.out.println("[Verify] All tables match.");
        } else {
            System.out.println("[Verify] WARNING: Some tables do not match!");
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  DataExport export  <backup-dir> [ignite-address]   — export from running Ignite");
        System.out.println("  DataExport import  <backup-dir> [ignite-address]   — import into new Ignite");
        System.out.println("  DataExport verify  <backup-dir> [ignite-address]   — compare backup vs live row counts");
        System.out.println();
        System.out.println("Default ignite-address: 127.0.0.1:10300");
    }
}
