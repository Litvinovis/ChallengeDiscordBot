package com.discord.challengebot.service;

import com.discord.challengebot.event.AchievementUnlockedEvent;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AchievementServiceTest {

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
        challenge.setTargetValue(2000);
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
    void testAchievementNotAwardedTwice() {
        when(challengeService.getChallenge("test_challenge")).thenReturn(challenge);

        // Первый вызов — создаём нового участника и сохраняем достижение
        Participant participant = new Participant("user1", "user1");
        when(participantRepository.findById("user1"))
                .thenReturn(Optional.empty())          // loadAwardedAchievements (первый checkAndAward)
                .thenReturn(Optional.of(participant)); // persistAwardedAchievement

        achievementService.checkAndAwardAchievements("user1", "test_challenge", 100);

        // После первого вызова у участника уже есть достижение — второй вызов не должен его выдавать
        // @CacheEvict сбрасывает кэш, поэтому второй вызов загрузит данные снова из репозитория
        participant.getAwardedAchievements().add("user1:test_challenge:100_reps");
        when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

        achievementService.checkAndAwardAchievements("user1", "test_challenge", 150);

        // Только одно событие за всё время (только из первого вызова)
        verify(eventPublisher, times(1)).publishEvent(any(AchievementUnlockedEvent.class));
    }

    @Test
    void testHasAchievementReturnsFalseInitially() {
        boolean has = achievementService.hasAchievement("user1", "test_challenge", "100_reps");
        assertFalse(has);
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
}
