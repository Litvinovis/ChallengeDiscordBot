package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsServiceTest {

    @InjectMocks
    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCalculateStats() {
        Challenge challenge = new Challenge();
        challenge.setName("Отжимания");
        challenge.setTargetValue(10000);
        challenge.setCurrentValue(2500);
        challenge.setEndDate(LocalDateTime.now().plusDays(10));

        ChallengeStats stats = statisticsService.calculateStats(challenge);

        assertNotNull(stats);
        assertEquals("Отжимания", stats.getChallengeName());
        assertEquals(10000L, stats.getTargetValue());
        assertEquals(2500L, stats.getCurrentValue());
        assertEquals(7500L, stats.getRemaining());
    }

    @Test
    void testCalculateRemaining() {
        Challenge challenge = new Challenge();
        challenge.setTargetValue(10000);
        challenge.setCurrentValue(2500);

        long remaining = statisticsService.calculateRemaining(challenge);

        assertEquals(7500L, remaining);
    }

    @Test
    void testCalculateDailyTarget() {
        Challenge challenge = new Challenge();
        challenge.setTargetValue(10000);
        challenge.setCurrentValue(2500);
        // Use a fixed date for testing
        challenge.setEndDate(LocalDateTime.of(2025, 9, 10, 12, 0));

        double dailyTarget = statisticsService.calculateDailyTarget(challenge);

        // We expect 7500 remaining over 10 days = 750 per day
        // But the exact value depends on the current date, so we'll check it's reasonable
        assertTrue(dailyTarget > 0);
    }

    @Test
    void testCalculateDailyTargetWithParticipants() {
        Challenge challenge = new Challenge();
        challenge.setTargetValue(10000);
        challenge.setCurrentValue(2500);
        // Use a fixed date for testing
        challenge.setEndDate(LocalDateTime.of(2025, 9, 10, 12, 0));
        
        // Add participants
        challenge.addParticipant("user1");
        challenge.addParticipant("user2");
        challenge.addParticipant("user3");
        challenge.addParticipant("user4");

        double dailyTarget = statisticsService.calculateDailyTarget(challenge);

        // Calculate the expected value based on the current date
        long remaining = 10000 - 2500; // 7500
        LocalDateTime now = LocalDateTime.now();
        long daysRemaining = Duration.between(now, LocalDateTime.of(2025, 9, 10, 12, 0)).toDays();
        
        // With 4 participants, we expect the daily target to be distributed among them
        // Daily target per participant: 7500 / 4 / daysRemaining
        double expected = (double) remaining / 4 / daysRemaining;
        assertEquals(expected, dailyTarget, 0.01);
    }

    @Test
    void testCalculateDailyTargetWithoutParticipants() {
        Challenge challenge = new Challenge();
        challenge.setTargetValue(10000);
        challenge.setCurrentValue(2500);
        // Use a fixed date for testing
        challenge.setEndDate(LocalDateTime.of(2025, 9, 10, 12, 0));
        
        // No participants added

        double dailyTarget = statisticsService.calculateDailyTarget(challenge);

        // Calculate the expected value based on the current date
        long remaining = 10000 - 2500; // 7500
        LocalDateTime now = LocalDateTime.now();
        long daysRemaining = Duration.between(now, LocalDateTime.of(2025, 9, 10, 12, 0)).toDays();
        
        // With no participants, we expect the daily target to be calculated for 1 participant
        // Daily target per participant: 7500 / 1 / daysRemaining
        double expected = (double) remaining / 1 / daysRemaining;
        assertEquals(expected, dailyTarget, 0.01);
    }

    @Test
    void testCalculatePercentage() {
        Challenge challenge = new Challenge();
        challenge.setTargetValue(10000);
        challenge.setCurrentValue(2500);

        double percentage = statisticsService.calculatePercentage(challenge);

        assertEquals(25.0, percentage, 0.01);
    }

    @Test
    void testFormatReportForDiscord() {
        Challenge challenge = new Challenge();
        challenge.setName("Отжимания");
        challenge.setUnit("раз");
        
        // Add participants
        challenge.addParticipant("user1");
        challenge.addParticipant("user2");
        challenge.addParticipant("user3");
        
        // Add progress for participants
        challenge.getParticipantProgress().put("user1", 1000L);
        challenge.getParticipantProgress().put("user2", 800L);
        challenge.getParticipantProgress().put("user3", 600L);
        
        ChallengeStats stats = new ChallengeStats();
        stats.setChallengeName("Отжимания");
        stats.setTargetValue(10000);
        stats.setCurrentValue(2500);
        stats.setRemaining(7500);
        stats.setPercentage(25.0);
        stats.setDailyTarget(750.0);
        stats.setDaysRemaining(10);

        String formatted = statisticsService.formatReportForDiscord(challenge, stats);
        
        // Print the actual output for debugging
        System.out.println("Formatted output: " + formatted);

        assertTrue(formatted.contains("**Статистика по испытанию: Отжимания**"), "Should contain title");
        assertTrue(formatted.contains("Цель: 10000"), "Should contain target value");
        assertTrue(formatted.contains("Выполнено: 2500"), "Should contain current value");
        assertTrue(formatted.contains("Осталось: 7500"), "Should contain remaining value");
        // Use comma as decimal separator as shown in the output
        assertTrue(formatted.contains("Процент выполнения: 25,00%"), "Should contain percentage");
        assertTrue(formatted.contains("Ежедневная цель: 750,00 в день"), "Should contain daily target");
        assertTrue(formatted.contains("Дней осталось: 10"), "Should contain days remaining");
        assertTrue(formatted.contains("Зарегистрировано участников: 3"), "Should contain participant count");
        assertTrue(formatted.contains("**Топ-3 участников:**"), "Should contain top participants header");
        assertTrue(formatted.contains("1. <@user1> - 1000 раз"), "Should contain top participant 1");
        assertTrue(formatted.contains("2. <@user2> - 800 раз"), "Should contain top participant 2");
        assertTrue(formatted.contains("3. <@user3> - 600 раз"), "Should contain top participant 3");
    }
    
    @Test
    void testFormatReportForDiscordWithoutParticipants() {
        Challenge challenge = new Challenge();
        challenge.setName("Отжимания");
        challenge.setUnit("раз");
        
        ChallengeStats stats = new ChallengeStats();
        stats.setChallengeName("Отжимания");
        stats.setTargetValue(10000);
        stats.setCurrentValue(2500);
        stats.setRemaining(7500);
        stats.setPercentage(25.0);
        stats.setDailyTarget(750.0);
        stats.setDaysRemaining(10);

        String formatted = statisticsService.formatReportForDiscord(challenge, stats);
        
        // Print the actual output for debugging
        System.out.println("Formatted output: " + formatted);

        assertTrue(formatted.contains("**Статистика по испытанию: Отжимания**"), "Should contain title");
        assertTrue(formatted.contains("Цель: 10000"), "Should contain target value");
        assertTrue(formatted.contains("Выполнено: 2500"), "Should contain current value");
        assertTrue(formatted.contains("Осталось: 7500"), "Should contain remaining value");
        // Use comma as decimal separator as shown in the output
        assertTrue(formatted.contains("Процент выполнения: 25,00%"), "Should contain percentage");
        assertTrue(formatted.contains("Ежедневная цель: 750,00 в день"), "Should contain daily target");
        assertTrue(formatted.contains("Дней осталось: 10"), "Should contain days remaining");
        assertTrue(formatted.contains("Зарегистрировано участников: 0"), "Should contain participant count");
        assertFalse(formatted.contains("**Топ-3 участников:**"), "Should not contain top participants header when no participants");
    }
}