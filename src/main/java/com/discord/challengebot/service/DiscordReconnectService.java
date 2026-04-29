package com.discord.challengebot.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Сервис автоматического переподключения Discord бота для Challenge Bot.
 */
@Service
public class DiscordReconnectService {

	private static final Logger log = LoggerFactory.getLogger(DiscordReconnectService.class);

	@Value("${discord.token}")
	private String discordToken;

	@Value("${discord.auto-reconnect:true}")
	private boolean autoReconnect;

	@Value("${discord.reconnect-delay:10}")
	private int reconnectDelaySeconds;

	@Value("${discord.max-reconnect-attempts:10}")
	private int maxReconnectAttempts;

	@Value("${discord.health-check-interval:60}")
	private int healthCheckIntervalSeconds;

	private final AtomicReference<JDA> jdaRef = new AtomicReference<>();
	private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private volatile boolean shutdownRequested = false;

	/**
	 * Инициализация бота и запуск мониторинга.
	 */
	@PostConstruct
	public void init() {
		if (discordToken == null || discordToken.isEmpty()) {
			log.error("Discord токен не настроен. Бот не будет запущен.");
			return;
		}

		connect();

		if (autoReconnect) {
			startHealthMonitor();
			log.info("Автоматическое переподключение включено. Интервал проверки: {} секунд",
							healthCheckIntervalSeconds);
		}
	}

	/**
	 * Подключение к Discord.
	 */
	private void connect() {
		try {
			log.info("Подключение Challenge Bot к Discord...");

			JDA jda = JDABuilder.createDefault(discordToken)
							.enableIntents(
											GatewayIntent.GUILD_MESSAGES,
											GatewayIntent.MESSAGE_CONTENT,
											GatewayIntent.GUILD_MEMBERS
							)
							.disableCache(
											CacheFlag.ACTIVITY,
											CacheFlag.VOICE_STATE,
											CacheFlag.EMOJI,
											CacheFlag.STICKER
							)
							.setAutoReconnect(true)
							.build();

			jda.awaitReady();
			jdaRef.set(jda);
			reconnectAttempts.set(0);

			log.info("Challenge Bot успешно подключен. ID: {}", jda.getSelfUser().getId());
			log.info("Бот находится на {} серверах", jda.getGuilds().size());

		} catch (Exception e) {
			log.error("Ошибка подключения Challenge Bot к Discord", e);
			scheduleReconnect();
		}
	}

	/**
	 * Запуск мониторинга здоровья подключения.
	 */
	private void startHealthMonitor() {
		scheduler.scheduleAtFixedRate(() -> {
			try {
				checkConnectionHealth();
			} catch (Exception e) {
				log.error("Ошибка в health check", e);
			}
		}, healthCheckIntervalSeconds, healthCheckIntervalSeconds, TimeUnit.SECONDS);

		log.debug("Health monitor запущен для Challenge Bot");
	}

	/**
	 * Проверка состояния подключения.
	 */
	private void checkConnectionHealth() {
		JDA jda = jdaRef.get();

		if (jda == null) {
			log.warn("Challenge Bot: JDA не инициализирован. Попытка переподключения...");
			reconnect();
			return;
		}

		try {
			JDA.Status status = jda.getStatus();

			switch (status) {
				case CONNECTED:
					if (reconnectAttempts.get() > 0) {
						log.info("Challenge Bot: подключение восстановлено после {} попыток", reconnectAttempts.get());
						reconnectAttempts.set(0);
					}
					break;

				case DISCONNECTED:
				case FAILED_TO_LOGIN:
				case SHUTDOWN:
					log.warn("Challenge Bot отключен. Статус: {}", status);
					reconnect();
					break;

				default:
					// Другие статусы - просто логируем
					break;
			}

		} catch (Exception e) {
			log.error("Ошибка при проверке состояния Challenge Bot", e);
		}
	}

	/**
	 * Переподключение с экспоненциальной задержкой.
	 */
	private synchronized void reconnect() {
		if (shutdownRequested) {
			return;
		}

		int attempt = reconnectAttempts.incrementAndGet();

		if (attempt > maxReconnectAttempts) {
			log.error("Challenge Bot: достигнут лимит попыток переподключения ({}). Остановка.", maxReconnectAttempts);
			return;
		}

		long delay = reconnectDelaySeconds * (1L << (attempt - 1));
		delay = Math.min(delay, 300); // Макс 5 минут

		log.info("Challenge Bot: попытка переподключения #{}. Задержка: {} секунд", attempt, delay);

		scheduler.schedule(() -> {
			try {
				JDA oldJda = jdaRef.get();
				if (oldJda != null) {
					oldJda.shutdown();
				}

				connect();

			} catch (Exception e) {
				log.error("Ошибка при переподключении Challenge Bot", e);
			}
		}, delay, TimeUnit.SECONDS);
	}

	/**
	 * Планирование переподключения.
	 */
	public void scheduleReconnect() {
		if (autoReconnect) {
			scheduler.execute(this::reconnect);
		}
	}

	/**
	 * Принудительное переподключение.
	 */
	public void forceReconnect() {
		log.info("Challenge Bot: принудительное переподключение...");
		reconnectAttempts.set(0);
		reconnect();
	}

	/**
	 * Получение текущего экземпляра JDA.
	 */
	public JDA getJda() {
		return jdaRef.get();
	}

	/**
	 * Проверка, подключен ли бот.
	 */
	public boolean isConnected() {
		JDA jda = jdaRef.get();
		return jda != null && jda.getStatus() == JDA.Status.CONNECTED;
	}

	/**
	 * Остановка сервиса.
	 */
	@PreDestroy
	public void shutdown() {
		shutdownRequested = true;

		log.info("Остановка DiscordReconnectService для Challenge Bot...");

		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}

		JDA jda = jdaRef.get();
		if (jda != null) {
			jda.shutdown();
		}

		log.info("DiscordReconnectService для Challenge Bot остановлен");
	}
}