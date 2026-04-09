package com.discord.challengebot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Менеджер подключения Apache Ignite 3 thin client для Spring Boot.
 *
 * <p>Хранит единственный экземпляр {@link IgniteClient} и предоставляет метод
 * {@link #reconnect()} для создания нового соединения при потере старого.
 * Репозитории, которые получают клиент через {@link #getClient()}, автоматически
 * начнут использовать новый клиент после переподключения — view сбрасываются
 * при смене объекта клиента.
 */
@Component
public class IgniteConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(IgniteConnectionManager.class);

    @Value("${ignite3.address:127.0.0.1:10300}")
    private String address;

    private volatile IgniteClient client;

    /**
     * Устанавливает начальное соединение при старте Spring-контекста.
     */
    @PostConstruct
    public void connect() {
        log.info("IgniteConnectionManager: подключение к Ignite 3 по адресу {}", address);
        client = buildClient();
        log.info("IgniteConnectionManager: подключение установлено");
    }

    /**
     * Закрывает соединение при остановке Spring-контекста.
     */
    @PreDestroy
    public void close() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("IgniteConnectionManager: ошибка при закрытии клиента: {}", e.getMessage());
            }
        }
    }

    /**
     * Возвращает текущий подключённый Ignite 3 thin client.
     *
     * @return актуальный экземпляр {@link IgniteClient}
     */
    public IgniteClient getClient() {
        return client;
    }

    /**
     * Переподключается к Ignite 3: закрывает старый клиент и создаёт новый.
     * Метод потокобезопасен — защищён монитором объекта.
     *
     * @return {@code true} если переподключение прошло успешно
     */
    public synchronized boolean reconnect() {
        log.info("IgniteConnectionManager: переподключение к {}...", address);
        try {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception ex) {
                    log.warn("IgniteConnectionManager: ошибка закрытия старого клиента: {}", ex.getMessage());
                }
            }
            client = buildClient();
            log.info("IgniteConnectionManager: переподключение успешно к {}", address);
            return true;
        } catch (Exception e) {
            log.error("IgniteConnectionManager: переподключение не удалось: {}", e.getMessage(), e);
            return false;
        }
    }

    private IgniteClient buildClient() {
        return IgniteClient.builder()
                .addresses(address)
                .build();
    }
}
