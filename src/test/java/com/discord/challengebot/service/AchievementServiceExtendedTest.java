package com.discord.challengebot.service;

import com.discord.challengebot.event.AchievementUnlockedEvent;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Расширенные тесты для AchievementService:
 * проверяет бейджи, пороги 100/500/1000/5000, и публикацию событий.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AchievementServiceExtendedTest {

	@Mock
	private ParticipantRepository participantRepository;

	@Mock
	private IChallengeService challengeService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private AchievementService achievementService;

	private Challenge challenge;

	@BeforeEach
	void setUp() {
		challenge = new Challenge();
		challenge.setId("test_challenge");
		challenge.setName("Test Challenge");
		challenge.setTargetValue(10000);
		challenge.setCurrentValue(0);
		challenge.setType(ChallengeType.GROUP);
		challenge.setStartDate(LocalDateTime.now().minusDays(10));
		challenge.setEndDate(LocalDateTime.now().plusDays(30));
		challenge.setUnit("раз");

		// По умолчанию участник не найден — нет персистентных достижений
		when(participantRepository.findById(anyString())).thenReturn(Optional.empty());
	}

	@Test
	void testNoAchievementBelowThreshold() {
		when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
		achievementService.checkAndAwardAchievements("user1", "test_challenge", 50);
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void testAchievementAt100() {
		when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
		achievementService.checkAndAwardAchievements("user1", "test_challenge", 100);
		verify(eventPublisher, times(1)).publishEvent(any(AchievementUnlockedEvent.class));
	}

	@Test
	void testAchievementAt100_containsName() {
		when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
		achievementService.checkAndAwardAchievements("user1", "test_challenge", 100);

		ArgumentCaptor<AchievementUnlockedEvent> captor = ArgumentCaptor.forClass(AchievementUnlockedEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertTrue(captor.getValue().achievementName().contains("100"), "Achievement name must mention 100");
	}

	@Test
	void testAchievementAt500() {
		when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
		achievementService.checkAndAwardAchievements("user1", "test_challenge", 500);
		// Должны выдаться бейджи 100 и 500
		verify(eventPublisher, times(2)).publishEvent(any(AchievementUnlockedEvent.class));
	}

	@Test
	void testAchievementAt1000() {
		when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
		achievementService.checkAndAwardAchievements("user1", "test_challenge", 1000);
		// Должны выдаться бейджи 100, 500 и 1000
		verify(eventPublisher, times(3)).publishEvent(any(AchievementUnlockedEvent.class));
	}

	@Test
	void testAchievementAt5000() {
		when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
		achievementService.checkAndAwardAchievements("user1", "test_challenge", 5000);
		// Должны выдаться бейджи 100, 500, 1000 и 5000
		verify(eventPublisher, times(4)).publishEvent(any(AchievementUnlockedEvent.class));
	}

	@Test
	void testAchievementAt5000_containsMilestone() {
		when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
		achievementService.checkAndAwardAchievements("user1", "test_challenge", 5000);

		ArgumentCaptor<AchievementUnlockedEvent> captor = ArgumentCaptor.forClass(AchievementUnlockedEvent.class);
		verify(eventPublisher, times(4)).publishEvent(captor.capture());
		// Последнее событие должно содержать "5000" в имени достижения
		boolean hasMilestone = captor.getAllValues().stream()
						.anyMatch(e -> e.achievementName().contains("5000"));
		assertTrue(hasMilestone, "At least one event must mention 5000");
	}

	@Test
	void testAchievementNotAwardedTwice() {
		when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);

		achievementService.checkAndAwardAchievements("user1", "test_challenge", 100);

		// Имитируем состояние после выдачи: участник с уже записанным достижением
		Participant participant = new Participant("user1", "user1");
		participant.getAwardedAchievements().add("user1:test_challenge:100_reps");
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		achievementService.checkAndAwardAchievements("user1", "test_challenge", 150);

		// Только одно событие (из первого вызова)
		verify(eventPublisher, times(1)).publishEvent(any(AchievementUnlockedEvent.class));
	}

	@Test
	void testHasAchievementReturnsFalseInitially() {
		assertFalse(achievementService.hasAchievement("user1", "test_challenge", "100_reps"));
	}

	@Test
	void testHasAchievementReturnsTrueAfterAwarding() {
		when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);
		achievementService.checkAndAwardAchievements("user1", "test_challenge", 100);

		// Имитируем сохранённое достижение в репозитории
		Participant participant = new Participant("user1", "user1");
		participant.getAwardedAchievements().add("user1:test_challenge:100_reps");
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		assertTrue(achievementService.hasAchievement("user1", "test_challenge", "100_reps"));
	}

	@Test
	void testNullUserDoesNotThrow() {
		assertDoesNotThrow(() ->
						achievementService.checkAndAwardAchievements(null, "test_challenge", 500));
	}

	@Test
	void testNullChallengeIdDoesNotThrow() {
		assertDoesNotThrow(() ->
						achievementService.checkAndAwardAchievements("user1", null, 500));
	}

	@Test
	void testFourDistinctAchievementsDefinedInService() {
		assertEquals(4, AchievementService.ACHIEVEMENTS.size());
	}

	@Test
	void testAchievementThresholds() {
		int[] expectedThresholds = {100, 500, 1000, 5000};
		for (int i = 0; i < expectedThresholds.length; i++) {
			assertEquals(expectedThresholds[i], AchievementService.ACHIEVEMENTS.get(i).threshold());
		}
	}

	/**
	 * Регрессионный тест: достижение уже сохранено в БД (после перезапуска бота).
	 * При повторном вызове checkAndAwardAchievements достижение НЕ должно выдаваться снова.
	 */
	@Test
	void testAlreadyPersistedAchievementIsNotReawardedAfterRestart() {
		// Имитируем состояние после перезапуска: у Participant уже записано достижение
		Participant participant = new Participant("user1", "TestUser");
		Set<String> alreadyAwarded = new HashSet<>();
		alreadyAwarded.add("user1:test_challenge:100_reps");
		participant.setAwardedAchievements(alreadyAwarded);

		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));
		when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);

		// Прогресс 150 (выше порога 100), но достижение уже выдано
		achievementService.checkAndAwardAchievements("user1", "test_challenge", 150);

		// Никакого события — достижение уже было выдано
		verify(eventPublisher, never()).publishEvent(any());
	}

	/**
	 * Проверяет, что при повторном вызове в той же сессии достижение не выдаётся дважды.
	 */
	@Test
	void testAlreadyAwardedInSessionNotReawarded() {
		when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);

		// Первый вызов — достижение выдаётся
		achievementService.checkAndAwardAchievements("user1", "test_challenge", 100);

		// Имитируем персистированное достижение для последующих вызовов
		Participant participant = new Participant("user1", "user1");
		participant.getAwardedAchievements().add("user1:test_challenge:100_reps");
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		// Повторные вызовы — достижение НЕ должно выдаваться снова
		achievementService.checkAndAwardAchievements("user1", "test_challenge", 100);
		achievementService.checkAndAwardAchievements("user1", "test_challenge", 120);

		// Только одно событие за всё время
		verify(eventPublisher, times(1)).publishEvent(any(AchievementUnlockedEvent.class));
	}
}
