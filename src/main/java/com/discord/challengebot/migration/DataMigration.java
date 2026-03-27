package com.discord.challengebot.migration;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.repository.ParticipantRepository;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import java.util.List;

/**
 * Инструмент миграции данных из Apache Ignite 2.x в Apache Ignite 3.x.
 *
 * <p>Подключается к Ignite 2.x (client mode) через TCP discovery,
 * читает кэши challenges и participants, записывает данные в таблицы Ignite 3.
 *
 * <p>Запуск (с профилем migration):
 * <pre>
 *   mvn package -Pmigration -DskipTests
 *   java -cp target/challenge-bot-1.0.0.jar \
 *     com.discord.challengebot.migration.DataMigration \
 *     [ignite2_discovery_address] [ignite3_client_address]
 * </pre>
 *
 * <p>Или через system properties:
 * <pre>
 *   java -Dignite2.address=192.168.1.120:47650..47659 \
 *        -Dignite3.address=127.0.0.1:10300 \
 *        -cp target/... com.discord.challengebot.migration.DataMigration
 * </pre>
 */
public class DataMigration {

    private static final Logger log = LoggerFactory.getLogger(DataMigration.class);

    /** Имя Ignite 2 клиентского узла */
    private static final String IGNITE2_INSTANCE_NAME = "challenge-migrator";

    public static void main(String[] args) {
        String ignite2Address = System.getProperty("ignite2.address",
                args.length > 0 ? args[0] : "127.0.0.1:47650..47659");
        String ignite3Address = System.getProperty("ignite3.address",
                args.length > 1 ? args[1] : "127.0.0.1:10300");

        log.info("=== ChallengeDiscordBot Data Migration: Ignite 2 -> Ignite 3 ===");
        log.info("Ignite 2.x discovery: {}", ignite2Address);
        log.info("Ignite 3.x client:    {}", ignite3Address);

        // Подключаемся к Ignite 2.x
        log.info("Подключение к Ignite 2.x...");
        Ignite ignite2 = connectIgnite2(ignite2Address);
        log.info("Ignite 2.x подключён.");

        // Подключаемся к Ignite 3.x
        log.info("Подключение к Ignite 3.x...");
        IgniteClient ignite3 = connectIgnite3(ignite3Address);
        log.info("Ignite 3.x подключён.");

        // Репозитории Ignite 3
        ChallengeRepository challengeRepo = new ChallengeRepository(ignite3);
        ParticipantRepository participantRepo = new ParticipantRepository(ignite3);

        int total = 0;

        // === Миграция Challenges ===
        log.info("Миграция challenges...");
        int challenges = 0;
        try (var cursor = ignite2.cache("challenges").query(new ScanQuery<>())) {
            for (Object raw : cursor) {
                @SuppressWarnings("unchecked")
                Cache.Entry<String, Challenge> entry = (Cache.Entry<String, Challenge>) raw;
                try {
                    challengeRepo.save(entry.getValue());
                    challenges++;
                } catch (Exception e) {
                    log.warn("Ошибка миграции challenge {}: {}", entry.getKey(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при чтении кэша challenges из Ignite 2.x: {}", e.getMessage());
        }
        log.info("Мигрировано challenges: {}", challenges);
        total += challenges;

        // === Миграция Participants ===
        log.info("Миграция participants...");
        int participants = 0;
        try (var cursor = ignite2.cache("participants").query(new ScanQuery<>())) {
            for (Object raw : cursor) {
                @SuppressWarnings("unchecked")
                Cache.Entry<String, Participant> entry = (Cache.Entry<String, Participant>) raw;
                try {
                    participantRepo.save(entry.getValue());
                    participants++;
                } catch (Exception e) {
                    log.warn("Ошибка миграции participant {}: {}", entry.getKey(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при чтении кэша participants из Ignite 2.x: {}", e.getMessage());
        }
        log.info("Мигрировано participants: {}", participants);
        total += participants;

        log.info("=== Миграция завершена. Всего записей: {} ===", total);

        ignite3.close();
        Ignition.stop(IGNITE2_INSTANCE_NAME, true);
    }

    private static Ignite connectIgnite2(String discoveryAddress) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName(IGNITE2_INSTANCE_NAME);
        cfg.setClientMode(true);

        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(List.of(discoveryAddress));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        return Ignition.start(cfg);
    }

    private static IgniteClient connectIgnite3(String clientAddress) {
        return IgniteClient.builder()
                .addresses(clientAddress)
                .build();
    }
}
