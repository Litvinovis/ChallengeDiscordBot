package com.discord.challengebot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Конфигурация асинхронного выполнения задач.
 * Определяет пул потоков для генерации визуализаций (графиков прогресса).
 */
@Configuration
public class AsyncConfig {

    /**
     * Создаёт пул потоков для генерации визуализаций.
     * Параметры: 2 базовых потока, максимум 4 потока, очередь на 50 задач.
     *
     * @return настроенный исполнитель задач
     */
    @Bean(name = "visualizationExecutor")
    public Executor visualizationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("visualization-");
        executor.initialize();
        return executor;
    }
}
