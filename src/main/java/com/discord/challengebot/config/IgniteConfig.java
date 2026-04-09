package com.discord.challengebot.config;

/**
 * Конфигурация Apache Ignite 3.
 *
 * <p>Управление подключением вынесено в {@link IgniteConnectionManager},
 * который является Spring-компонентом и предоставляет актуальный клиент
 * через {@link IgniteConnectionManager#getClient()}, а также поддерживает
 * переподключение через {@link IgniteConnectionManager#reconnect()}.
 */
public class IgniteConfig {
    // Конфигурация в IgniteConnectionManager (@Component с @PostConstruct/@PreDestroy)
}
