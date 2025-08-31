package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

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
        assertEquals(25.0, stats.getPercentage(), 0.01);
        assertEquals(10, stats.getDaysRemaining());
        assertEquals(750.0, stats.getDailyTarget(), 0.01);
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
        challenge.setEndDate(LocalDateTime.now().plusDays(10));

        double dailyTarget = statisticsService.calculateDailyTarget(challenge);

        assertEquals(750.0, dailyTarget, 0.01);
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
        ChallengeStats stats = new ChallengeStats();
        stats.setChallengeName("Отжимания");
        stats.setTargetValue(10000);
        stats.setCurrentValue(2500);
        stats.setRemaining(7500);
        stats.setPercentage(25.0);
        stats.setDailyTarget(750.0);
        stats.setDaysRemaining(10);

        String formatted = statisticsService.formatReportForDiscord(stats);

        assertTrue(formatted.contains("**Статистика по испытанию: Отжимания**"));
        assertTrue(formatted.contains("Цель: 10000"));
        assertTrue(formatted.contains("Выполнено: 2500"));
        assertTrue(formatted.contains("Осталось: 7500"));
        assertTrue(formatted.contains("Процент выполнения: 25,00%"));
        assertTrue(formatted.contains("Ежедневная цель: 750,00 в день"));
        assertTrue(formatted.contains("Дней осталось: 10"));
    }
}