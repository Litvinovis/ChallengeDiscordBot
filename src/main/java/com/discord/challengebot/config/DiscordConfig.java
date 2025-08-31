package com.discord.challengebot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурация Discord бота
 */
@Component
@ConfigurationProperties(prefix = "discord")
public class DiscordConfig {
    private String token;
    private String channel;
    private String guildId;
    private String adminUserId;
    private String reportChannel;
    private String reportGuildId;

    // Getters and setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getGuildId() {
        return guildId;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public String getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(String adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getReportChannel() {
        return reportChannel;
    }

    public void setReportChannel(String reportChannel) {
        this.reportChannel = reportChannel;
    }

    public String getReportGuildId() {
        return reportGuildId;
    }

    public void setReportGuildId(String reportGuildId) {
        this.reportGuildId = reportGuildId;
    }
}