package com.discord.challengebot.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Модель достижения пользователя.
 * Достижение выдаётся при достижении определённого порогового значения прогресса в испытании.
 */
public record Achievement(String id, String name, String description, int threshold) implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
}
