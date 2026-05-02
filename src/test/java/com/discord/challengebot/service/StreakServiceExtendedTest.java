package com.discord.challengebot.service;

import com.discord.challengebot.event.StreakMilestoneEvent;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Расширенные тесты для StreakService:
 * проверяет обновление серий и публикацию событий при достижении 3, 7, 30 дней.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StreakServiceExtendedTest {

	private static final ZoneId ZONE = ZoneId.of("Europe/Moscow");

	@Mock
	private ParticipantRepository participantRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private StreakService streakService;

	// ---- Базовое поведение серии ----

	@Test
	void testFirstActivitySetsStreakTo1() {
		Participant participant = new Participant("user1", "TestUser");
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		streakService.recordActivity("user1");

		assertEquals(1, participant.getCurrentStreak());
	}

	@Test
	void testConsecutiveDayIncrementsStreak() {
		Participant participant = new Participant("user1", "TestUser");
		participant.setCurrentStreak(2);
		participant.setLastActivityDate(LocalDate.now(ZONE).minusDays(1));
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		streakService.recordActivity("user1");

		assertEquals(3, participant.getCurrentStreak());
	}

	@Test
	void testSameDayDoesNotChangeStreak() {
		Participant participant = new Participant("user1", "TestUser");
		participant.setCurrentStreak(5);
		participant.setLastActivityDate(LocalDate.now(ZONE));
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		streakService.recordActivity("user1");

		assertEquals(5, participant.getCurrentStreak());
	}

	@Test
	void testMissedDayResetsStreakTo1() {
		Participant participant = new Participant("user1", "TestUser");
		participant.setCurrentStreak(10);
		participant.setLastActivityDate(LocalDate.now(ZONE).minusDays(3));
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		streakService.recordActivity("user1");

		assertEquals(1, participant.getCurrentStreak());
	}

	// ---- Публикация событий при достижении порогов ----

	@Test
	void testEventPublishedAt3Days() {
		Participant participant = new Participant("user1", "TestUser");
		participant.setCurrentStreak(2);
		participant.setLastActivityDate(LocalDate.now(ZONE).minusDays(1));
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		streakService.recordActivity("user1");

		assertEquals(3, participant.getCurrentStreak());
		ArgumentCaptor<StreakMilestoneEvent> captor = ArgumentCaptor.forClass(StreakMilestoneEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertEquals(3, captor.getValue().streak());
		assertEquals("user1", captor.getValue().userId());
	}

	@Test
	void testEventPublishedAt7Days() {
		Participant participant = new Participant("user1", "TestUser");
		participant.setCurrentStreak(6);
		participant.setLastActivityDate(LocalDate.now(ZONE).minusDays(1));
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		streakService.recordActivity("user1");

		assertEquals(7, participant.getCurrentStreak());
		ArgumentCaptor<StreakMilestoneEvent> captor = ArgumentCaptor.forClass(StreakMilestoneEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertEquals(7, captor.getValue().streak());
	}

	@Test
	void testEventPublishedAt30Days() {
		Participant participant = new Participant("user1", "TestUser");
		participant.setCurrentStreak(29);
		participant.setLastActivityDate(LocalDate.now(ZONE).minusDays(1));
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		streakService.recordActivity("user1");

		assertEquals(30, participant.getCurrentStreak());
		ArgumentCaptor<StreakMilestoneEvent> captor = ArgumentCaptor.forClass(StreakMilestoneEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		assertEquals(30, captor.getValue().streak());
	}

	@Test
	void testNoEventAt5Days() {
		Participant participant = new Participant("user1", "TestUser");
		participant.setCurrentStreak(4);
		participant.setLastActivityDate(LocalDate.now(ZONE).minusDays(1));
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		streakService.recordActivity("user1");

		assertEquals(5, participant.getCurrentStreak());
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void testNoEventOnFirstActivity() {
		Participant participant = new Participant("user1", "TestUser");
		when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

		streakService.recordActivity("user1");

		// Серия 1 — нет уведомления (previousStreak=0, newStreak=1 — не пороговое значение)
		verify(eventPublisher, never()).publishEvent(any());
	}

	// ---- Мультипликаторы ----

	@Test
	void testMultiplierBelow7() {
		assertEquals(1.0, streakService.getStreakMultiplier(3), 0.001);
	}

	@Test
	void testMultiplierAt7() {
		assertEquals(1.1, streakService.getStreakMultiplier(7), 0.001);
	}

	@Test
	void testMultiplierAt30() {
		assertEquals(1.2, streakService.getStreakMultiplier(30), 0.001);
	}

	// ---- Прочее ----

	@Test
	void testNullUserDoesNotThrow() {
		assertDoesNotThrow(() -> streakService.recordActivity(null));
	}

	@Test
	void testEmptyUserDoesNotThrow() {
		assertDoesNotThrow(() -> streakService.recordActivity(""));
	}

	@Test
	void testGetCurrentStreakForUnknownUser() {
		when(participantRepository.findById("unknown")).thenReturn(Optional.empty());
		assertEquals(0, streakService.getCurrentStreak("unknown"));
	}
}
