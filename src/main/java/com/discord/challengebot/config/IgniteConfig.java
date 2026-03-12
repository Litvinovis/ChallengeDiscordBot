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
 * Конфигурация Apache Ignite (client mode, shared node)
 */
@Configuration
public class IgniteConfig {

    @Value("${ignite.local-address:192.168.1.120}")
    private String localAddress;

    @Value("${ignite.discovery-addresses:192.168.1.120:47650..47659}")
    private List<String> discoveryAddresses;

    @Value("${ignite.work-dir:/tmp/ignite-challenge-client}")
    private String workDir;

    private Ignite igniteInstance;

    @Bean
    public Ignite igniteInstance() {
        System.setProperty("IGNITE_QUIET", "false");
        System.setProperty("IGNITE_NO_ASCII", "false");
        System.setProperty("IGNITE_UPDATE_NOTIFIER", "false");
        System.setProperty("IGNITE_DISABLE_ACCESS_CHECK", "true");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("challenge-client");
        cfg.setClientMode(true);
        cfg.setWorkDirectory(workDir);

        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        discoverySpi.setLocalAddress(localAddress);
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(discoveryAddresses);
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        igniteInstance = Ignition.start(cfg);
        return igniteInstance;
    }

    public void closeIgnite() {
        if (igniteInstance != null) {
            try {
                igniteInstance.close();
            } catch (Exception ignored) {
            }
        }
    }
}
