package com.discord.challengebot.service;

import com.discord.challengebot.command.CommandRegistry;
import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.event.AchievementUnlockedEvent;
import com.discord.challengebot.event.ChallengeHalfwayEvent;
import com.discord.challengebot.event.StreakMilestoneEvent;
import com.discord.challengebot.model.Challenge;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Сервис взаимодействия с Discord.
 * Инициализирует JDA, отправляет сообщения, обрабатывает Spring-события достижений и серий.
 * Циклические зависимости устранены через @EventListener вместо прямых вызовов.
 */
@Service
public class DiscordService implements IDiscordService {
	private static final Logger logger = LoggerFactory.getLogger(DiscordService.class);

	private final DiscordConfig discordConfig;
	private final ChallengeService challengeService;
	private final ParticipantService participantService;
	private final StatisticsService statisticsService;
	private final CommandRegistry commandRegistry;

	private JDA jda;

	/**
	 * Создаёт сервис Discord.
	 *
	 * @param discordConfig      конфигурация Discord бота
	 * @param challengeService   сервис управления испытаниями
	 * @param participantService сервис управления участниками
	 * @param statisticsService  сервис статистики
	 * @param commandRegistry    реестр команд
	 */
	public DiscordService(DiscordConfig discordConfig,
	                      ChallengeService challengeService,
	                      ParticipantService participantService,
	                      StatisticsService statisticsService,
	                      @Lazy CommandRegistry commandRegistry) {
		this.discordConfig = discordConfig;
		this.challengeService = challengeService;
		this.participantService = participantService;
		this.statisticsService = statisticsService;
		this.commandRegistry = commandRegistry;
	}

	/**
	 * Инициализирует Discord бота: создаёт JDA, регистрирует слушатель событий.
	 * При сетевых ошибках (DNS, отсутствие интернета) повторяет попытку с экспоненциальной
	 * задержкой (5→10→20→…→60 сек) до успешного подключения.
	 */
	@PostConstruct
	public void init() {
		logger.info("Инициализация Discord бота");
		int delaySec = 5;
		while (true) {
			JDA attempt = null;
			try {
				attempt = JDABuilder.createDefault(discordConfig.getToken())
								.enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
								.addEventListeners(new DiscordMessageListener(
												this, discordConfig, challengeService, participantService,
												statisticsService, commandRegistry))
								.build();
				attempt.awaitReady();
				jda = attempt;
				logger.info("Discord бот успешно инициализирован");
				return;
			} catch (InterruptedException e) {
				if (attempt != null) attempt.shutdownNow();
				Thread.currentThread().interrupt();
				logger.warn("Инициализация Discord бота прервана");
				return;
			} catch (Exception e) {
				if (attempt != null) attempt.shutdownNow();
				logger.warn("Не удалось подключиться к Discord ({}), повтор через {} сек", e.getMessage(), delaySec);
				try {
					Thread.sleep(delaySec * 1000L);
				} catch (InterruptedException _) {
					Thread.currentThread().interrupt();
					return;
				}
				delaySec = Math.min(delaySec * 2, 60);
			}
		}
	}

	/**
	 * Корректно завершает работу Discord бота при остановке приложения.
	 */
	@PreDestroy
	public void shutdown() {
		try {
			if (jda != null) {
				logger.info("Выключение Discord бота");
				jda.shutdown();
			}
		} catch (Exception e) {
			logger.error("Ошибка при выключении Discord бота", e);
		}
	}

	/**
	 * Возвращает экземпляр JDA.
	 */
	public JDA getJDA() {
		return jda;
	}

	// ---- Обработчики событий Spring ----

	/**
	 * Слушатель события разблокировки достижения.
	 * Отправляет поздравление в канал объявлений.
	 *
	 * @param event событие достижения
	 */
	@EventListener
	public void onAchievementUnlocked(AchievementUnlockedEvent event) {
		try {
			String channel = resolveAnnouncementChannel();
			String message = String.format(
							"🏆 <@%s> получил достижение **%s**! В испытании %s",
							event.userId(), event.achievementName(), event.challengeName());
			sendMessageToChannel(channel, message);
		} catch (Exception e) {
			logger.error("Ошибка отправки уведомления о достижении для пользователя {}", event.userId(), e);
		}
	}

	/**
	 * Слушатель события достижения порогового значения серии активности.
	 * Отправляет поздравление в канал уведомлений.
	 *
	 * @param event событие серии
	 */
	@EventListener
	public void onStreakMilestone(StreakMilestoneEvent event) {
		try {
			String message = switch (event.streak()) {
				case 30 -> String.format("🏆 <@%s> — Месяц без пропуска! Легенда!", event.userId());
				case 7 -> String.format("🔥🔥 <@%s> — Неделя без пропуска!", event.userId());
				case 3 -> String.format("🔥 <@%s> — 3-дневная серия!", event.userId());
				default -> null;
			};
			if (message != null) {
				sendMessageToChannel(resolveAnnouncementChannel(), message);
			}
		} catch (Exception e) {
			logger.error("Ошибка отправки уведомления о серии для пользователя {}", event.userId(), e);
		}
	}

	/**
	 * Слушатель события достижения 50% прогресса по испытанию.
	 *
	 * @param event событие достижения половины цели
	 */
	@EventListener
	public void onChallengeHalfway(ChallengeHalfwayEvent event) {
		try {
			Challenge challenge = event.getChallenge();
			if (challenge == null) return;
			String message = String.format(
					"🎉 Испытание **%s** достигло **50%%** прогресса! (%d / %d). Продолжаем!",
					challenge.getName(), challenge.getCurrentValue(), challenge.getTargetValue());
			sendMessageToChannel(resolveAnnouncementChannel(), message);
		} catch (Exception e) {
			logger.error("Ошибка отправки уведомления о 50% прогресса", e);
		}
	}

	// ---- Методы отправки сообщений ----

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendMessage(String channelId, String message) {
		try {
			if (channelId == null || channelId.isBlank() || message == null || message.isBlank()) return;
			TextChannel channel = jda.getTextChannelById(channelId);
			if (channel != null) {
				channel.sendMessage(message).queue();
			} else {
				logger.warn("Канал с ID {} не найден", channelId);
			}
		} catch (Exception e) {
			logger.error("Ошибка отправки сообщения в канал {}", channelId, e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendMessageToChannel(String channelName, String message) {
		try {
			if (channelName == null || channelName.isBlank() || message == null || message.isBlank()) return;
			TextChannel channel = null;
			if (discordConfig.getReportGuildId() != null && !discordConfig.getReportGuildId().isBlank()) {
				Guild guild = jda.getGuildById(discordConfig.getReportGuildId());
				if (guild != null) {
					channel = guild.getTextChannelsByName(channelName, true).stream().findFirst().orElse(null);
				}
			}
			if (channel == null) {
				channel = jda.getTextChannelsByName(channelName, true).stream().findFirst().orElse(null);
			}
			if (channel != null) {
				channel.sendMessage(message).queue();
			} else {
				logger.warn("Канал с именем {} не найден", channelName);
			}
		} catch (Exception e) {
			logger.error("Ошибка отправки сообщения в канал {}", channelName, e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendMessageWithVisualization(String channelId, String message, byte[] image) {
		try {
			if (channelId == null || channelId.isBlank()) return;
			TextChannel channel = jda.getTextChannelById(channelId);
			if (channel != null) {
				if (image != null && image.length > 0) {
					channel.sendMessage(message).addFiles(FileUpload.fromData(image, "chart.png")).queue();
				} else {
					channel.sendMessage(message).queue();
				}
			}
		} catch (Exception e) {
			logger.error("Ошибка отправки сообщения с визуализацией в канал {}", channelId, e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateHelpMessage() {
		return generateHelpMessage(null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateHelpMessage(String userId) {
		try {
			var sb = new StringBuilder();
			sb.append("""
							**Справка по командам бота**
							
							**Основные команды:**
							`+<испытание> <количество>` - Добавить прогресс к испытанию (например: `+отжимания 10`)
							`+статистика` - Показать статистику по всем испытаниям
							`+статистика <испытание>` - Показать статистику по конкретному испытанию
							`+испытания` - Показать список всех активных испытаний
							`+помощь` - Показать эту справку
							
							""");
			boolean isAdmin = userId != null && participantService.isAdminUser(userId);
			if (isAdmin) {
				sb.append("""
								**Команды управления испытаниями (только для администраторов):**
								`+новый <название> <цель> [дата окончания] [тип]` - Создать новое испытание
								`+удалить <название>` - Удалить испытание
								`+остановить <название>` - Остановить активное испытание
								`+продолжить <название>` - Продолжить остановленное испытание
								`+изменить <название> <новая цель>` - Изменить цель испытания
								`+изменить_дату <название> <новая дата>` - Изменить дату окончания испытания
								`+установить_прогресс <испытание> <пользователь> <количество>` - Установить прогресс участника
								`+добавить_участника <испытание> <пользователь>` - Добавить участника в испытание
								`+удалить_участника <испытание> <пользователь>` - Удалить участника из испытания
								
								""");
			}
			sb.append("""
							**Команды пользователя:**
							`+мои` - Показать личные испытания
							`+топ <испытание> [количество]` - Показать таблицу лидеров по испытанию
							`+прогресс <испытание>` - Показать личный прогресс по испытанию
							`+прогноз <испытание>` - Прогноз даты завершения при текущем темпе
							`+график <испытание>` - Показать PNG-график прогресса по испытанию
							`+регистрация <название>` - Зарегистрироваться на испытание
							`+обновить_имя [новое имя]` - Обновить ваше имя в системе
							""");
			return sb.toString();
		} catch (Exception e) {
			logger.error("Ошибка при генерации сообщения справки", e);
			return "**Ошибка при генерации справки. Пожалуйста, попробуйте позже.**";
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendDailyReport() {
		try {
			logger.info("Отправка ежедневного отчёта");
			List<Challenge> challenges = challengeService.getAllChallenges().stream()
							.filter(Challenge::isActive).toList();
			if (challenges.isEmpty()) {
				sendMessageToChannel(discordConfig.getReportChannel(), "📋 Активных испытаний нет.");
				return;
			}
			sendMessageToChannel(discordConfig.getReportChannel(),
							"📋 **Ежедневный отчёт** · " + challenges.size() + " активных испытаний");
			for (var challenge : challenges) {
				ChallengeStats stats = challengeService.getChallengeStats(challenge);
				if (stats == null) continue;
				var top3 = challengeService.getTopParticipants(challenge, 3);
				String message = statisticsService.formatDailyReportForDiscord(challenge, stats, top3);
				if (!message.isBlank()) {
					sendMessageToChannel(discordConfig.getReportChannel(), message);
				}
			}
		} catch (Exception e) {
			logger.error("Ошибка при отправке ежедневного отчёта", e);
		}
	}

	/**
	 * Формирует и отправляет еженедельный отчёт о прогрессе всех активных испытаний.
	 */
	public void sendWeeklyReport() {
		try {
			List<Challenge> challenges = challengeService.getActiveChallenges();
			if (challenges.isEmpty()) return;
			for (Challenge challenge : challenges) {
				try {
					ChallengeStats stats = statisticsService.calculateStats(challenge);
					if (stats == null) continue;
					String report = formatWeeklyReport(challenge, stats);
					sendMessageToChannel(discordConfig.getReportChannel(), report);
				} catch (Exception e) {
					logger.error("Ошибка при отправке недельного отчёта для испытания {}", challenge.getName(), e);
				}
			}
		} catch (Exception e) {
			logger.error("Ошибка при отправке недельных отчётов", e);
		}
	}

	/**
	 * Формирует и отправляет ежемесячный отчёт о прогрессе всех активных испытаний.
	 */
	public void sendMonthlyReport() {
		try {
			List<Challenge> challenges = challengeService.getActiveChallenges();
			String month = LocalDateTime.now(ZoneId.of("Europe/Moscow"))
					.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru")));
			sendMessageToChannel(discordConfig.getReportChannel(),
					"**📅 Месячный итог — " + month + "**");
			if (challenges.isEmpty()) {
				sendMessageToChannel(discordConfig.getReportChannel(), "Активных испытаний нет.");
				return;
			}
			for (Challenge challenge : challenges) {
				try {
					ChallengeStats stats = statisticsService.calculateStats(challenge);
					if (stats == null) continue;
					var top3 = challengeService.getTopParticipants(challenge, 3);
					String report = statisticsService.formatDailyReportForDiscord(challenge, stats, top3);
					if (!report.isBlank()) {
						sendMessageToChannel(discordConfig.getReportChannel(), report);
					}
				} catch (Exception e) {
					logger.error("Ошибка при отправке месячного отчёта для испытания {}", challenge.getName(), e);
				}
			}
		} catch (Exception e) {
			logger.error("Ошибка при отправке ежемесячных отчётов", e);
		}
	}

	private String formatWeeklyReport(Challenge challenge, ChallengeStats stats) {
		StringBuilder sb = new StringBuilder();
		sb.append("📅 **Итоги недели — ").append(challenge.getName()).append("**\n");
		double pct = stats.percentage();
		sb.append(String.format("📊 Прогресс: **%d / %d** (%.1f%%)\n",
			challenge.getCurrentValue(), challenge.getTargetValue(), pct));
		if (challenge.getEndDate() != null) {
			long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
				java.time.LocalDateTime.now(java.time.ZoneId.of("Europe/Moscow")), challenge.getEndDate());
			sb.append("⏳ До конца: **").append(Math.max(0, daysLeft)).append(" дн.**\n");
		}
		sb.append("💪 Продолжайте в том же духе!");
		return sb.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendChallengeCompletionNotification(Challenge challenge) {
		try {
			if (challenge == null) return;
			String message = String.format(
							"**Испытание завершено!**\nИспытание \"%s\" успешно завершено!\nПоздравляем всех участников!",
							challenge.getName());
			sendMessageToChannel(discordConfig.getReportChannel(), message);
			var leaderboard = challengeService.getTopParticipants(challenge, 5);
			if (!leaderboard.isEmpty()) {
				sendMessageToChannel(discordConfig.getReportChannel(),
								statisticsService.formatLeaderboardForDiscord(challenge, leaderboard));
			}
		} catch (Exception e) {
			logger.error("Ошибка отправки уведомления о завершении испытания: {}",
							challenge != null ? challenge.getName() : "null", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendChallengeFailureNotification(Challenge challenge) {
		try {
			if (challenge == null) return;
			String message = String.format(
							"**Испытание завершено!**\nИспытание \"%s\" завершено, но цель не достигнута.\nНе расстраивайтесь, в следующий раз обязательно получится!",
							challenge.getName());
			sendMessageToChannel(discordConfig.getReportChannel(), message);
			var leaderboard = challengeService.getTopParticipants(challenge, 5);
			if (!leaderboard.isEmpty()) {
				sendMessageToChannel(discordConfig.getReportChannel(),
								statisticsService.formatLeaderboardForDiscord(challenge, leaderboard));
			}
		} catch (Exception e) {
			logger.error("Ошибка отправки уведомления о провале испытания: {}",
							challenge != null ? challenge.getName() : "null", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String formatChallengeStats(Challenge challenge, ChallengeStats stats) {
		try {
			return statisticsService.formatReportForDiscord(challenge, stats);
		} catch (Exception e) {
			logger.error("Ошибка при форматировании статистики испытания", e);
			return "**Ошибка при форматировании статистики.**";
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAuthorizedUser(String userId, String command) {
		try {
			if (userId == null || userId.isBlank()) return false;
			if (command == null || command.isBlank()) return true;
			if (command.startsWith("новый") || command.startsWith("удалить") ||
							command.startsWith("остановить") || command.startsWith("продолжить") ||
							command.startsWith("изменить") || command.startsWith("изменить_дату") ||
							command.startsWith("установить_прогресс") ||
							command.startsWith("добавить_участника") || command.startsWith("удалить_участника")) {
				return participantService.isAdminUser(userId);
			}
			return true;
		} catch (Exception e) {
			logger.error("Ошибка при проверке авторизации {} для команды {}", userId, command, e);
			return false;
		}
	}

	// ---- вспомогательные методы ----

	/**
	 * Определяет канал для публикации объявлений.
	 * Приоритет: report-channel из конфига, иначе "announcements".
	 */
	private String resolveAnnouncementChannel() {
		String reportChannel = discordConfig.getReportChannel();
		if (reportChannel != null && !reportChannel.isBlank()) return reportChannel;
		return "announcements";
	}
}
