package com.discord.challengebot.service;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Health check service for Apache Ignite connection
 */
@Component
public class IgniteHealthCheckService {
    private static final Logger logger = LoggerFactory.getLogger(IgniteHealthCheckService.class);

    @Autowired
    private Ignite ignite;

    private final AtomicBoolean healthy = new AtomicBoolean(true);

    /**
     * Периодически проверяет подключение к Ignite каждые 5 минут.
     * При недоступности логирует ERROR и пытается переподключиться.
     */
    @Scheduled(fixedDelay = 300000)
    public void checkIgniteHealth() {
        try {
            if (ignite == null) {
                logger.error("Ignite instance is null - connection lost");
                healthy.set(false);
                return;
            }
            // Simple health check: verify cluster is active
            boolean clusterActive = ignite.cluster().active();
            if (clusterActive) {
                if (!healthy.get()) {
                    logger.info("Ignite connection restored");
                }
                healthy.set(true);
            } else {
                logger.error("Ignite cluster is not active - attempting to verify state");
                healthy.set(false);
            }
        } catch (Exception e) {
            logger.error("Ignite health check failed - connection may be lost", e);
            healthy.set(false);
            attemptReconnect();
        }
    }

    /**
     * Attempt to reconnect to Ignite cluster
     */
    private void attemptReconnect() {
        try {
            logger.info("Attempting to reconnect to Ignite cluster...");
            if (ignite != null && !ignite.cluster().active()) {
                // Try to activate the cluster
                ignite.cluster().active(true);
                healthy.set(true);
                logger.info("Successfully reconnected to Ignite cluster");
            }
        } catch (Exception e) {
            logger.error("Failed to reconnect to Ignite cluster", e);
        }
    }

    /**
     * Returns true if Ignite is healthy and connected
     */
    public boolean isHealthy() {
        return healthy.get();
    }
}
