package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.repository.ProgressHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ChallengeProgressRepository progressRepository;

    @Mock
    private ParticipantService participantService;

    @Mock
    private ProgressHistoryRepository progressHistoryRepository;

    @InjectMocks
    private ChallengeService challengeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateChallenge() {
        String name = "Отжимания";
        long targetValue = 10000;
        LocalDateTime endDate = LocalDateTime.now().plusDays(365);
        ChallengeType type = ChallengeType.GROUP;
        String description = "Испытание по отжиманиям";
        String unit = "раз";

        Challenge challenge = challengeService.createChallenge(name, targetValue, endDate, type, description, unit);

        assertNotNull(challenge);
        assertEquals(name, challenge.getName());
        assertEquals(targetValue, challenge.getTargetValue());
        assertEquals(type, challenge.getType());
        assertEquals(description, challenge.getDescription());
        assertEquals(unit, challenge.getUnit());
        assertTrue(challenge.isActive());
        assertNotNull(challenge.getStartDate());
        assertEquals(endDate, challenge.getEndDate());

        // Проверяем что challengeRepository.save был вызван
        verify(challengeRepository).save(any(Challenge.class));
    }

    @Test
    void testAddProgress() {
        String userId = "12345";
        String username = "testuser";
        long amount = 10;

        Challenge challenge = new Challenge();
        challenge.setId("отжимания");
        challenge.setName("Отжимания");
        challenge.setCurrentValue(100L);
        challenge.setTargetValue(1000L);
        challenge.setActive(true);
        challenge.setEndDate(LocalDateTime.now().plusDays(30));

        // Состояние в БД после атомарного инкремента нового участника
        Map<String, Long> progressAfter = new HashMap<>();
        progressAfter.put(userId, 10L);
        when(progressRepository.findByChallengeId("отжимания")).thenReturn(progressAfter);
        when(participantService.registerForChallenge(userId, username, "Отжимания")).thenReturn(true);

        Challenge updatedChallenge = challengeService.addProgress(challenge, userId, username, amount);

        assertNotNull(updatedChallenge);
        verify(participantService).registerForChallenge(userId, username, "Отжимания");
        verify(progressRepository).addAmount("отжимания", userId, 10L);
        verify(challengeRepository).addParticipant("отжимания", userId);
        verify(challengeRepository).refreshCurrentValue("отжимания");
    }

    /**
     * addProgress не должен бросать NPE при null challenge.
     */
    @Test
    void testAddProgressWithNullChallenge() {
        Challenge result = challengeService.addProgress(null, "user1", "username", 10);
        assertNull(result, "addProgress should return null for null challenge without throwing NPE");
    }

    /**
     * setParticipantProgress не должен бросать NPE при null challenge.
     */
    @Test
    void testSetParticipantProgressWithNullChallenge() {
        Challenge result = challengeService.setParticipantProgress(null, "user1", 10);
        assertNull(result, "setParticipantProgress should return null for null challenge without throwing NPE");
    }

    /**
     * participantProgress должен быть инициализирован при создании испытания.
     */
    @Test
    void testChallengeParticipantProgressIsInitialized() {
        Challenge challenge = challengeService.createChallenge(
                "ConcurrencyTest", 1000L, LocalDateTime.now().plusDays(30),
                ChallengeType.GROUP, "desc", "rep");

        assertNotNull(challenge);
        assertNotNull(challenge.getParticipantProgress(),
                "participantProgress must be initialized");
    }

    /**
     * Конкурентные вызовы addProgress от разных участников не должны терять данные.
     */
    @Test
    void testConcurrentProgressUpdates() throws InterruptedException {
        Challenge challenge = new Challenge();
        challenge.setId("concurrent_test");
        challenge.setName("concurrent_test");
        challenge.setTargetValue(10000L);
        challenge.setCurrentValue(0L);
        challenge.setActive(true);
        challenge.setEndDate(LocalDateTime.now().plusDays(30));
        challenge.setUnit("rep");

        when(participantService.registerForChallenge(anyString(), anyString(), anyString())).thenReturn(true);
        // Каждый поток получает свою карту прогресса
        when(progressRepository.findByChallengeId(anyString())).thenAnswer(inv -> new HashMap<>());

        int threads = 10;
        int incrementsPerThread = 50;
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final String userId = "user" + i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        challengeService.addProgress(challenge, userId, userId, 1L);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // У каждого участника должен быть прогресс в in-memory карте
        for (int i = 0; i < threads; i++) {
            assertNotNull(challenge.getParticipantProgress().get("user" + i));
        }
    }

    /**
     * hasParticipant должен работать корректно (Set-семантика).
     */
    @Test
    void testHasParticipantUsesSetSemantics() {
        Challenge challenge = new Challenge();
        challenge.setId("set_test");
        challenge.setName("set_test");

        assertFalse(challenge.hasParticipant("user1"), "User1 should not be present initially");
        challenge.addParticipant("user1");
        assertTrue(challenge.hasParticipant("user1"), "User1 should be present after addParticipant");

        // Двойное добавление не создаёт дубликатов
        challenge.addParticipant("user1");
        assertEquals(1, challenge.getParticipants().size(), "Participants list should not contain duplicates");

        challenge.removeParticipant("user1");
        assertFalse(challenge.hasParticipant("user1"), "User1 should be absent after removeParticipant");
    }

    @Test
    void testGetChallengeStats() {
        Challenge challenge = new Challenge();
        challenge.setId("отжимания");
        challenge.setName("Отжимания");
        challenge.setTargetValue(10000L);
        challenge.setCurrentValue(2500L);
        challenge.setEndDate(LocalDateTime.now().plusDays(10));

        when(progressRepository.findByChallengeId("отжимания")).thenReturn(Map.of("user1", 2500L));

        ChallengeStats stats = challengeService.getChallengeStats(challenge);

        assertNotNull(stats);
        assertEquals("Отжимания", stats.challengeName());
        assertEquals(10000L, stats.targetValue());
        assertEquals(2500L, stats.currentValue());
        assertEquals(7500L, stats.remaining());
        assertEquals(25.0, stats.percentage(), 0.01);
    }

    @Test
    void testAddProgress_PropagatesDbErrorSoTransactionRollsBack() {
        Challenge challenge = new Challenge();
        challenge.setId("бег");
        challenge.setName("Бег");
        challenge.setTargetValue(1000);
        doThrow(new RuntimeException("db down"))
                .when(progressRepository).addAmount("бег", "user1", 10L);

        assertThrows(RuntimeException.class,
                () -> challengeService.addProgress(challenge, "user1", "Alice", 10L),
                "Ошибка БД должна выходить наружу, иначе @Transactional не откатит изменения");

        verify(challengeRepository, never()).refreshCurrentValue(anyString());
    }

    @Test
    void testCreateChallenge_RejectsExistingName() {
        Challenge existing = new Challenge();
        existing.setId("отжимания");
        existing.setName("Отжимания");
        existing.setTargetValue(10000);
        when(challengeRepository.findById("отжимания")).thenReturn(Optional.of(existing));

        Challenge result = challengeService.createChallenge("Отжимания", 500,
                LocalDateTime.now().plusDays(30), ChallengeType.GROUP, "описание", "раз");

        assertNull(result, "Повторное создание не должно возвращать испытание");
        verify(challengeRepository, never()).save(any());
    }

    @Test
    void testCreateChallenge_RejectsExistingNameFoundByLegacyId() {
        Challenge existing = new Challenge();
        existing.setId("legacy-id");
        existing.setName("Отжимания");
        when(challengeRepository.findById(anyString())).thenReturn(Optional.empty());
        when(challengeRepository.findByName("Отжимания")).thenReturn(Optional.of(existing));

        Challenge result = challengeService.createChallenge("Отжимания", 500,
                LocalDateTime.now().plusDays(30), ChallengeType.GROUP, "описание", "раз");

        assertNull(result);
        verify(challengeRepository, never()).save(any());
    }
}
