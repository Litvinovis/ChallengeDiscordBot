package com.discord.challengebot.service;

import com.discord.challengebot.config.IgniteConnectionManager;
import jakarta.annotation.PostConstruct;
import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Сервис проверки работоспособности подключения к Apache Ignite 3 с механизмом переподключения.
 *
 * <p>Каждые 5 минут (@Scheduled) проверяет доступность таблиц через IgniteClient.
 * При обнаружении сбоя немедленно запускает цикл переподключения с интервалом
 * {@value #RECONNECT_INTERVAL_SEC} секунд через отдельный планировщик.
 * После успешного переподключения цикл останавливается.
 */
@Component
public class IgniteHealthCheckService {
    private static final Logger logger = LoggerFactory.getLogger(IgniteHealthCheckService.class);

    private static final long RECONNECT_INTERVAL_SEC = 30;

    @Autowired
    private IgniteConnectionManager connectionManager;

    private final AtomicBoolean healthy = new AtomicBoolean(true);
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);

    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ignite-reconnect");
        t.setDaemon(true);
        return t;
    });

    /**
     * Немедленная проверка при старте Spring-контекста.
     * Если Ignite недоступен при запуске, сразу инициирует цикл переподключения.
     */
    @PostConstruct
    public void onStartup() {
        if (connectionManager.getClient() == null) {
            markUnhealthy("Ignite недоступен при старте приложения");
            scheduleReconnect();
        }
    }

    /**
     * Периодически (каждые 5 минут) проверяет состояние подключения к Ignite 3.
     * При обнаружении проблем запускает цикл переподключения.
     */
    @Scheduled(fixedDelay = 300000)
    public void checkIgniteHealth() {
        IgniteClient client = connectionManager.getClient();
        try {
            if (client == null) {
                markUnhealthy("IgniteClient instance is null");
                scheduleReconnect();
                return;
            }
            checkTable(client, "challenges");
            checkTable(client, "challenge_participants");

            if (!healthy.get()) {
                logger.info("Ignite 3 восстановлен и доступен");
            }
            healthy.set(true);
            reconnecting.set(false);
        } catch (Exception e) {
            markUnhealthy("Исключение при проверке: " + e.getMessage());
            scheduleReconnect();
        }
    }

    /**
     * Запускает цикл переподключения, если он ещё не активен.
     */
    private void scheduleReconnect() {
        if (reconnecting.compareAndSet(false, true)) {
            logger.info("IgniteHealthCheckService: запускаю цикл переподключения (интервал {}с)", RECONNECT_INTERVAL_SEC);
            reconnectScheduler.schedule(this::attemptReconnect, RECONNECT_INTERVAL_SEC, TimeUnit.SECONDS);
        }
    }

    /**
     * Одна попытка переподключения. При неудаче планирует следующую.
     */
    private void attemptReconnect() {
        logger.info("IgniteHealthCheckService: попытка переподключения к Ignite 3...");
        boolean ok = connectionManager.reconnect();
        if (ok) {
            IgniteClient fresh = connectionManager.getClient();
            try {
                checkTable(fresh, "challenges");
                healthy.set(true);
                reconnecting.set(false);
                logger.info("IgniteHealthCheckService: переподключение успешно, кластер доступен");
            } catch (Exception e) {
                logger.warn("IgniteHealthCheckService: переподключение выполнено, но верификация не прошла: {} — повтор через {}с",
                        e.getMessage(), RECONNECT_INTERVAL_SEC);
                reconnectScheduler.schedule(this::attemptReconnect, RECONNECT_INTERVAL_SEC, TimeUnit.SECONDS);
            }
        } else {
            logger.warn("IgniteHealthCheckService: переподключение не удалось — повтор через {}с", RECONNECT_INTERVAL_SEC);
            reconnectScheduler.schedule(this::attemptReconnect, RECONNECT_INTERVAL_SEC, TimeUnit.SECONDS);
        }
    }

    private void checkTable(IgniteClient client, String tableName) {
        try {
            var table = client.tables().table(tableName);
            if (table == null) {
                logger.warn("[IgniteHealth] Таблица '{}' недоступна (null)", tableName);
            }
        } catch (Exception e) {
            logger.warn("[IgniteHealth] Ошибка доступа к таблице '{}': {}", tableName, e.getMessage());
        }
    }

    private void markUnhealthy(String reason) {
        if (healthy.get()) {
            logger.warn("[IgniteHealth] WARNING: Apache Ignite 3 недоступен! Причина: {}", reason);
        } else {
            logger.warn("[IgniteHealth] Apache Ignite 3 всё ещё недоступен. Причина: {}", reason);
        }
        healthy.set(false);
    }

    /**
     * Возвращает состояние подключения к Ignite 3.
     *
     * @return {@code true}, если последняя проверка прошла успешно
     */
    public boolean isHealthy() {
        return healthy.get();
    }
}
