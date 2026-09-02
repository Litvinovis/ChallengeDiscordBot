package com.discord.challengebot.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Разбор аргументов команды с поддержкой кавычек.
 * Обычное разбиение по пробелам делало невозможными названия испытаний
 * из нескольких слов: {@code +новый бег утром 1000} читалось как название
 * «бег» и нечисловая цель «утром».
 */
public final class CommandArguments {

	private CommandArguments() {
	}

	/**
	 * Разбивает строку команды на аргументы по пробелам, сохраняя целыми
	 * фрагменты в кавычках: {@code "..."}, {@code «...»} и {@code “...”}.
	 * Незакрытая кавычка считается открытой до конца строки.
	 *
	 * @param input строка команды без префикса
	 * @return массив аргументов (пустой, если строка пуста)
	 */
	public static String[] split(String input) {
		List<String> args = new ArrayList<>();
		if (input == null) return new String[0];

		StringBuilder current = new StringBuilder();
		char closing = 0;

		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (closing != 0) {
				if (c == closing) closing = 0;
				else current.append(c);
			} else if (c == '"') {
				closing = '"';
			} else if (c == '«') {
				closing = '»';
			} else if (c == '“') {
				closing = '”';
			} else if (Character.isWhitespace(c)) {
				if (!current.isEmpty()) {
					args.add(current.toString());
					current.setLength(0);
				}
			} else {
				current.append(c);
			}
		}
		if (!current.isEmpty()) args.add(current.toString());

		return args.toArray(new String[0]);
	}
}
