package com.discord.challengebot.config;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Конфигурация Apache Ignite
 */
@Configuration
public class IgniteConfig {
    
    @Value("${ignite.addresses:127.0.0.1:11800}")
    private List<String> igniteAddresses;
    
    @Value("${ignite.client-mode:true}")
    private boolean clientMode;

    @Bean
    public Ignite igniteInstance() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        
        // Настройка обнаружения узлов
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(igniteAddresses); // Используем правильный порт из конфигурации
        discoverySpi.setIpFinder(ipFinder);
        
        cfg.setDiscoverySpi(discoverySpi);
        cfg.setClientMode(clientMode);
        
        return Ignition.start(cfg);
    }
}