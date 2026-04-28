package com.discord.challengebot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конфигурация Discord бота.
 * Загружает параметры подключения из application.yml/properties с префиксом {@code discord}:
 * токен, идентификаторы каналов и серверов, идентификаторы администраторов, канал отчётов.
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

	/**
	 * Возвращает токен Discord бота.
	 *
	 * @return токен Discord бота
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Устанавливает токен Discord бота.
	 *
	 * @param token токен Discord бота
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * Возвращает имя канала, в котором бот слушает команды.
	 *
	 * @return имя канала
	 */
	public String getChannel() {
		return channel;
	}

	/**
	 * Устанавливает имя канала для команд.
	 *
	 * @param channel имя канала
	 */
	public void setChannel(String channel) {
		this.channel = channel;
	}

	/**
	 * Возвращает идентификатор одного канала (устаревший способ конфигурации).
	 *
	 * @return идентификатор канала
	 */
	public String getChannelId() {
		return channelId;
	}

	/**
	 * Устанавливает идентификатор одного канала.
	 *
	 * @param channelId идентификатор канала
	 */
	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	/**
	 * Возвращает список идентификаторов каналов, в которых бот слушает команды.
	 *
	 * @return список идентификаторов каналов
	 */
	public List<String> getChannelIds() {
		return channelIds;
	}

	/**
	 * Устанавливает список идентификаторов каналов.
	 *
	 * @param channelIds список идентификаторов каналов
	 */
	public void setChannelIds(List<String> channelIds) {
		this.channelIds = channelIds;
	}

	/**
	 * Возвращает идентификатор сервера Discord.
	 *
	 * @return идентификатор сервера (Guild ID)
	 */
	public String getGuildId() {
		return guildId;
	}

	/**
	 * Устанавливает идентификатор сервера Discord.
	 *
	 * @param guildId идентификатор сервера (Guild ID)
	 */
	public void setGuildId(String guildId) {
		this.guildId = guildId;
	}

	/**
	 * Возвращает идентификатор администратора (устаревший способ конфигурации).
	 *
	 * @return идентификатор пользователя-администратора
	 */
	public String getAdminUserId() {
		return adminUserId;
	}

	/**
	 * Устанавливает идентификатор администратора.
	 *
	 * @param adminUserId идентификатор пользователя-администратора
	 */
	public void setAdminUserId(String adminUserId) {
		this.adminUserId = adminUserId;
	}

	/**
	 * Возвращает список идентификаторов пользователей-администраторов.
	 *
	 * @return список идентификаторов администраторов
	 */
	public List<String> getAdminUserIds() {
		return adminUserIds;
	}

	/**
	 * Устанавливает список идентификаторов администраторов.
	 *
	 * @param adminUserIds список идентификаторов администраторов
	 */
	public void setAdminUserIds(List<String> adminUserIds) {
		this.adminUserIds = adminUserIds;
	}

	/**
	 * Возвращает имя канала для отправки ежедневных отчётов.
	 *
	 * @return имя канала отчётов
	 */
	public String getReportChannel() {
		return reportChannel;
	}

	/**
	 * Устанавливает имя канала отчётов.
	 *
	 * @param reportChannel имя канала отчётов
	 */
	public void setReportChannel(String reportChannel) {
		this.reportChannel = reportChannel;
	}

	/**
	 * Возвращает идентификатор сервера для отправки отчётов.
	 *
	 * @return идентификатор сервера отчётов
	 */
	public String getReportGuildId() {
		return reportGuildId;
	}

	/**
	 * Устанавливает идентификатор сервера для отправки отчётов.
	 *
	 * @param reportGuildId идентификатор сервера отчётов
	 */
	public void setReportGuildId(String reportGuildId) {
		this.reportGuildId = reportGuildId;
	}
}