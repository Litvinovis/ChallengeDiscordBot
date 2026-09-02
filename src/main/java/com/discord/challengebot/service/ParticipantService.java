package com.discord.challengebot.service;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.repository.ParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Сервис управления участниками Discord-бота.
 * Объединяет функциональность UserService и управление участниками испытаний.
 * Работает напрямую с ParticipantRepository и ChallengeRepository.
 */
@Service
public class ParticipantService implements IUserService {

	private static final Logger log = LoggerFactory.getLogger(ParticipantService.class);

	private final ParticipantRepository participantRepository;
	private final ChallengeRepository challengeRepository;
	private final DiscordConfig discordConfig;

	/**
	 * Создаёт сервис управления участниками.
	 *
	 * @param participantRepository репозиторий участников
	 * @param challengeRepository   репозиторий испытаний
	 * @param discordConfig         конфигурация Discord (для проверки прав администратора)
	 */
	public ParticipantService(ParticipantRepository participantRepository,
	                          ChallengeRepository challengeRepository,
	                          DiscordConfig discordConfig) {
		this.participantRepository = participantRepository;
		this.challengeRepository = challengeRepository;
		this.discordConfig = discordConfig;
	}

	/**
	 * {@inheritDoc}
	 * Регистрирует пользователя на испытание. Создаёт запись участника при необходимости.
	 */
	@Override
	public boolean registerForChallenge(String userId, String username, String challengeName) {
		try {
			if (userId == null || userId.isBlank()) {
				log.warn("Попытка регистрации с пустым ID пользователя");
				return false;
			}
			if (username == null || username.isBlank()) {
				log.warn("Попытка регистрации с пустым именем пользователя");
				return false;
			}
			if (challengeName == null || challengeName.isBlank()) {
				log.warn("Попытка регистрации на испытание с пустым названием");
				return false;
			}

			Participant participant = participantRepository.findById(userId).orElse(null);
			if (participant == null) {
				log.debug("Участник {} не найден, создаём нового: {}", userId, username);
				participant = new Participant(userId, username);
			} else if (!username.equals(participant.getUsername())) {
				log.debug("Обновляем имя участника {}: {} -> {}", userId, participant.getUsername(), username);
				participant.setUsername(username);
			}

			participant.addChallenge(challengeName);
			participantRepository.save(participant);
			log.info("Пользователь {} ({}) зарегистрирован на испытание {}", username, userId, challengeName);
			return true;
		} catch (Exception e) {
			log.error("Ошибка при регистрации пользователя {} на испытание {}", username, challengeName, e);
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * Отменяет регистрацию пользователя на испытание.
	 */
	@Override
	public boolean unregisterFromChallenge(String userId, String challengeName) {
		try {
			if (userId == null || userId.isBlank() || challengeName == null || challengeName.isBlank()) {
				return false;
			}
			Optional<Participant> opt = participantRepository.findById(userId);
			if (opt.isEmpty()) {
				log.warn("Участник {} не найден для отмены регистрации", userId);
				return false;
			}
			var participant = opt.get();
			participant.removeChallenge(challengeName);
			participantRepository.save(participant);
			log.info("Регистрация пользователя {} на испытание {} отменена", userId, challengeName);
			return true;
		} catch (Exception e) {
			log.error("Ошибка при отмене регистрации пользователя {} на испытание {}", userId, challengeName, e);
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * Возвращает данные участника по ID пользователя.
	 */
	@Override
	public Participant getParticipant(String userId) {
		try {
			if (userId == null || userId.isBlank()) return null;
			return participantRepository.findById(userId).orElse(null);
		} catch (Exception e) {
			log.error("Ошибка при получении участника {}", userId, e);
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 * Возвращает все испытания, на которые зарегистрирован пользователь.
	 */
	@Override
	public List<Challenge> getRegisteredChallenges(String userId) {
		try {
			if (userId == null || userId.isBlank()) return new ArrayList<>();
			Participant participant = participantRepository.findById(userId).orElse(null);
			if (participant == null) return new ArrayList<>();

			var allChallenges = challengeRepository.findAll();
			var registered = new ArrayList<Challenge>();
			for (var challenge : allChallenges) {
				if (participant.isRegisteredForChallenge(challenge.getName())) {
					registered.add(challenge);
				}
			}
			return registered;
		} catch (Exception e) {
			log.error("Ошибка при получении испытаний пользователя {}", userId, e);
			return new ArrayList<>();
		}
	}

	/**
	 * {@inheritDoc}
	 * Делегирует проверку прав в DiscordConfig.
	 */
	@Override
	public boolean isAdminUser(String userId) {
		try {
			if (userId == null || userId.isBlank()) return false;
			var adminIds = discordConfig.getAdminUserIds();
			if (adminIds != null && !adminIds.isEmpty()) {
				return adminIds.contains(userId);
			}
			// Поддержка legacy поля adminUserId
			return userId.equals(discordConfig.getAdminUserId());
		} catch (Exception e) {
			log.error("Ошибка при проверке прав администратора для пользователя {}", userId, e);
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * Обновляет имя пользователя в хранилище.
	 */
	@Override
	public boolean updateParticipantUsername(String userId, String username) {
		try {
			if (userId == null || userId.isBlank() || username == null || username.isBlank()) return false;
			var participant = participantRepository.findById(userId)
							.orElse(new Participant(userId, username));
			participant.setUsername(username);
			participantRepository.save(participant);
			log.info("Имя пользователя {} обновлено на {}", userId, username);
			return true;
		} catch (Exception e) {
			log.error("Ошибка при обновлении имени пользователя {}", userId, e);
			return false;
		}
	}

	// ---- Дополнительные методы управления участниками ----

	/**
	 * Возвращает всех зарегистрированных участников системы.
	 *
	 * @return список всех участников
	 */
	public List<Participant> getAllParticipants() {
		try {
			return participantRepository.findAll();
		} catch (Exception e) {
			log.error("Ошибка при получении всех участников", e);
			return new ArrayList<>();
		}
	}

	/**
	 * Сохраняет участника в хранилище.
	 *
	 * @param participant участник для сохранения
	 */
	public void saveParticipant(Participant participant) {
		if (participant != null) {
			participantRepository.save(participant);
		}
	}
}
