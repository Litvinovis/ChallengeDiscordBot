package com.discord.challengebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Главный класс приложения ChallengeDiscordBot.
 * Запускает Spring Boot приложение с поддержкой планировщика задач и асинхронного выполнения.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ChallengeBotApplication {

	/**
	 * Точка входа в приложение.
	 *
	 * @param args аргументы командной строки
	 */
	static void main(String[] args) {
		SpringApplication.run(ChallengeBotApplication.class, args);
	}
}
