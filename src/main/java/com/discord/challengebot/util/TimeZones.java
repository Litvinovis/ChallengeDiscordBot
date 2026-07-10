package com.discord.challengebot.util;

import java.time.ZoneId;

/**
 * Единый часовой пояс бота.
 * Все расчёты границ суток, дат окончания и интервалов ведутся в московском времени,
 * независимо от часового пояса сервера.
 */
public final class TimeZones {

	/** Московский часовой пояс — единственный используемый ботом. */
	public static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

	private TimeZones() {
	}
}
