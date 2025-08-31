package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataStorageServiceTest {

    @InjectMocks
    private DataStorageService dataStorageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSaveChallenge() {
        String name = "Отжимания";
        long targetValue = 10000;
        LocalDateTime endDate = LocalDateTime.now().plusDays(365);
        ChallengeType type = ChallengeType.GROUP;
        String description = "Испытание по отжиманиям";
        String unit = "раз";

        Challenge challenge = new Challenge();
        challenge.setId(name.toLowerCase().replace(" ", "_"));
        challenge.setName(name);
        challenge.setTargetValue(targetValue);
        challenge.setCurrentValue(0);
        challenge.setType(type);
        challenge.setStartDate(LocalDateTime.now());
        challenge.setEndDate(endDate);
        challenge.setActive(true);
        challenge.setDescription(description);
        challenge.setUnit(unit);

        // Проверяем, что метод не вызывает исключений
        assertDoesNotThrow(() -> dataStorageService.saveChallenge(challenge));
    }

    @Test
    void testGetChallenge() {
        String challengeName = "Отжимания";
        
        // Метод возвращает null в текущей реализации
        assertNull(dataStorageService.getChallenge(challengeName));
    }

    @Test
    void testGetAllChallenges() {
        List<Challenge> challenges = dataStorageService.getAllChallenges();
        
        // Метод возвращает пустой список в текущей реализации
        assertNotNull(challenges);
        assertTrue(challenges.isEmpty());
    }

    @Test
    void testDeleteChallenge() {
        String challengeName = "Отжимания";
        
        // Метод возвращает true в текущей реализации
        assertTrue(dataStorageService.deleteChallenge(challengeName));
    }
    
    @Test
    void testInitAndDestroy() {
        // Проверяем, что методы инициализации и завершения не вызывают исключений
        assertDoesNotThrow(() -> dataStorageService.init());
        assertDoesNotThrow(() -> dataStorageService.destroy());
    }
}