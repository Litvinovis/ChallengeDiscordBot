package com.discord.challengebot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Конфигурация Spring Cache с Caffeine в качестве провайдера.
 * Кэш "achievements" хранит выданные достижения пользователей в памяти,
 * уменьшая обращения к Ignite при частых проверках.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Создаёт менеджер кэша с Caffeine.
     * Параметры: максимум 500 записей, TTL — 1 час после записи.
     *
     * @return настроенный CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("achievements");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(500));
        return manager;
    }
}
