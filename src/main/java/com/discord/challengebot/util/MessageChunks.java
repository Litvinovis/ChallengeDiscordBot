package com.discord.challengebot.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Разбиение длинного текста на сообщения Discord.
 * Discord отклоняет сообщения длиннее 2000 символов, и JDA бросает исключение
 * ещё до отправки — пользователь не получал вообще ничего.
 */
public final class MessageChunks {

	/** Максимальная длина одного сообщения Discord. */
	public static final int DISCORD_LIMIT = 2000;

	private MessageChunks() {
	}

	/**
	 * Делит текст на части не длиннее {@code limit}, стараясь резать по переводам строк.
	 * Строка длиннее лимита режется жёстко.
	 *
	 * @param text  исходный текст
	 * @param limit максимальная длина части
	 * @return непустые части в исходном порядке
	 */
	public static List<String> split(String text, int limit) {
		List<String> chunks = new ArrayList<>();
		if (text == null || text.isEmpty()) return chunks;

		int start = 0;
		while (text.length() - start > limit) {
			int cut = text.lastIndexOf('\n', start + limit - 1);
			if (cut < start) {
				cut = start + limit;
				chunks.add(text.substring(start, cut));
				start = cut;
			} else {
				chunks.add(text.substring(start, cut));
				start = cut + 1;
			}
		}
		chunks.add(text.substring(start));
		chunks.removeIf(String::isBlank);
		return chunks;
	}
}
