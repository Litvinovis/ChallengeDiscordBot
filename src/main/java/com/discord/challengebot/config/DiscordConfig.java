package com.discord.challengebot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Конфигурация Discord бота
 */
@Component
@ConfigurationProperties(prefix = "discord")
public class DiscordConfig {
    private String token;
    private String channel;
    private String channelId;
    private List<String> channelIds;
    private String guildId;
    private String adminUserId;
    private List<String> adminUserIds;
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

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public List<String> getChannelIds() {
        return channelIds;
    }

    public void setChannelIds(List<String> channelIds) {
        this.channelIds = channelIds;
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
    
    public List<String> getAdminUserIds() {
        return adminUserIds;
    }
    
    public void setAdminUserIds(List<String> adminUserIds) {
        this.adminUserIds = adminUserIds;
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