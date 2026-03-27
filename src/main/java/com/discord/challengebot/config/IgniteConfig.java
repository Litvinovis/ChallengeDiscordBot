package com.discord.challengebot.config;

import org.apache.ignite.client.IgniteClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Apache Ignite 3 thin client.
 * Создаёт IgniteClient, подключающийся к Ignite 3 узлу по адресу из application.yml.
 */
@Configuration
public class IgniteConfig {

    @Value("${ignite3.address:127.0.0.1:10300}")
    private String ignite3Address;

    /**
     * Создаёт и возвращает подключённый Ignite 3 thin client.
     * Spring автоматически вызовет close() при завершении контекста.
     *
     * @return подключённый экземпляр {@link IgniteClient}
     */
    @Bean(destroyMethod = "close")
    public IgniteClient igniteClient() {
        return IgniteClient.builder()
                .addresses(ignite3Address)
                .build();
    }
}
