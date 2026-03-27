package com.discord.challengebot.service;

import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Сервис проверки работоспособности подключения к Apache Ignite 3.
 * Периодически (каждые 5 минут) проверяет доступность таблиц через IgniteClient.
 */
@Component
public class IgniteHealthCheckService {
    private static final Logger logger = LoggerFactory.getLogger(IgniteHealthCheckService.class);

    @Autowired
    private IgniteClient igniteClient;

    private final AtomicBoolean healthy = new AtomicBoolean(true);

    /**
     * Периодически (каждые 5 минут) проверяет состояние подключения к Ignite 3.
     * При обнаружении проблем логирует предупреждение.
     */
    @Scheduled(fixedDelay = 300000)
    public void checkIgniteHealth() {
        try {
            if (igniteClient == null) {
                markUnhealthy("IgniteClient instance is null");
                return;
            }
            // Проверяем доступность основных таблиц
            checkTable("challenges");
            checkTable("challenge_participants");

            if (!healthy.get()) {
                logger.info("Ignite 3 восстановлен и доступен");
            }
            healthy.set(true);
        } catch (Exception e) {
            markUnhealthy("Исключение при проверке: " + e.getMessage());
        }
    }

    private void checkTable(String tableName) {
        try {
            var table = igniteClient.tables().table(tableName);
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
