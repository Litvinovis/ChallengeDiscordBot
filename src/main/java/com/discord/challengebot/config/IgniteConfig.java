package com.discord.challengebot.config;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.nio.file.Paths;

/**
 * Конфигурация Apache Ignite
 */
@Configuration
public class IgniteConfig {

    private Ignite igniteInstance;

    @Bean
    public Ignite igniteInstance() {
        // Установка системных свойств для совместимости с новыми версиями Java
        System.setProperty("IGNITE_QUIET", "false");
        System.setProperty("IGNITE_NO_ASCII", "false");
        System.setProperty("IGNITE_UPDATE_NOTIFIER", "false");
        
        IgniteConfiguration cfg = new IgniteConfiguration();
        
        // Установка абсолютного пути к рабочей директории
        String workDir = Paths.get("ignite/work").toAbsolutePath().toString();
        cfg.setWorkDirectory(workDir);
        
        // Настройка персистентности (сохранение данных на диск)
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        storageCfg.setDefaultDataRegionConfiguration(
            new DataRegionConfiguration()
                .setName("default")
                .setPersistenceEnabled(true) // Включение персистентности
        );
        cfg.setDataStorageConfiguration(storageCfg);
        
        // Запуск Ignite в embedded режиме
        // В этом режиме Ignite запускается внутри приложения
        
        igniteInstance = Ignition.start(cfg);
        
        // Активация кластера после запуска, если используется персистентность
        if (!igniteInstance.cluster().active()) {
            igniteInstance.cluster().state(org.apache.ignite.cluster.ClusterState.ACTIVE);
        }
        
        return igniteInstance;
    }
    
    /**
     * Корректное закрытие экземпляра Ignite при завершении приложения
     */
    public void closeIgnite() {
        if (igniteInstance != null) {
            try {
                // Останавливаем экземпляр Ignite для корректного сохранения данных на диск
                igniteInstance.close();
            } catch (Exception e) {
                // Логируем ошибку, но не прерываем завершение приложения
                System.err.println("Ошибка при закрытии экземпляра Ignite: " + e.getMessage());
            }
        }
    }
}