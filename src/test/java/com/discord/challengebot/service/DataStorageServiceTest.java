package com.discord.challengebot.service;

import com.discord.challengebot.config.IgniteConfig;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.model.Participant;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.cache.Cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataStorageServiceTest {

    @Mock
    private Ignite ignite;

    @Mock
    private IgniteConfig igniteConfig;

    @Mock
    private IgniteCache<String, Challenge> challengesCache;

    @Mock
    private IgniteCache<String, Participant> participantsCache;

    @InjectMocks
    private DataStorageService dataStorageService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(ignite.getOrCreateCache(any(CacheConfiguration.class)))
                .thenReturn((IgniteCache) challengesCache)
                .thenReturn((IgniteCache) participantsCache);
        // Empty iterators by default
        when(challengesCache.iterator()).thenReturn(java.util.Collections.emptyIterator());
        when(participantsCache.iterator()).thenReturn(java.util.Collections.emptyIterator());
        dataStorageService.init();
    }

    private Challenge buildChallenge(String name) {
        Challenge challenge = new Challenge();
        challenge.setId(name.toLowerCase().replace(" ", "_"));
        challenge.setName(name);
        challenge.setTargetValue(10000);
        challenge.setCurrentValue(0);
        challenge.setType(ChallengeType.GROUP);
        challenge.setStartDate(LocalDateTime.now());
        challenge.setEndDate(LocalDateTime.now().plusDays(365));
        challenge.setActive(true);
        challenge.setDescription("Test description");
        challenge.setUnit("раз");
        return challenge;
    }

    @Test
    void testSaveChallenge() {
        Challenge challenge = buildChallenge("Отжимания");
        dataStorageService.saveChallenge(challenge);
        verify(challengesCache).put("отжимания", challenge);
    }

    @Test
    void testGetChallenge_found() {
        Challenge challenge = buildChallenge("Отжимания");
        when(challengesCache.get("отжимания")).thenReturn(challenge);

        Challenge result = dataStorageService.getChallenge("Отжимания");
        assertNotNull(result);
        assertEquals("Отжимания", result.getName());
    }

    @Test
    void testGetChallenge_notFound() {
        when(challengesCache.get(anyString())).thenReturn(null);
        // Empty iterator for fallback scan
        when(challengesCache.iterator()).thenReturn(java.util.Collections.emptyIterator());

        Challenge result = dataStorageService.getChallenge("НесуществующееИспытание");
        assertNull(result);
    }

    @Test
    void testDeleteChallenge_found() {
        when(challengesCache.remove("отжимания")).thenReturn(true);

        boolean deleted = dataStorageService.deleteChallenge("Отжимания");
        assertTrue(deleted);
    }

    @Test
    void testDeleteChallenge_notFound() {
        when(challengesCache.remove(anyString())).thenReturn(false);
        when(challengesCache.iterator()).thenReturn(java.util.Collections.emptyIterator());

        boolean deleted = dataStorageService.deleteChallenge("НесуществующееИспытание");
        assertFalse(deleted);
    }

    @Test
    void testSaveParticipant() {
        Participant participant = new Participant("user1", "TestUser");
        dataStorageService.saveParticipant(participant);
        verify(participantsCache).put("user1", participant);
    }

    @Test
    void testGetParticipant_found() {
        Participant participant = new Participant("user1", "TestUser");
        when(participantsCache.get("user1")).thenReturn(participant);
        when(participantsCache.iterator()).thenReturn(java.util.Collections.emptyIterator());

        Participant result = dataStorageService.getParticipant("user1");
        assertNotNull(result);
        assertEquals("TestUser", result.getUsername());
    }

    @Test
    void testGetParticipant_notFound() {
        when(participantsCache.get(anyString())).thenReturn(null);
        when(participantsCache.iterator()).thenReturn(java.util.Collections.emptyIterator());

        Participant result = dataStorageService.getParticipant("unknownUser");
        assertNull(result);
    }

    @Test
    void testDeleteParticipant_found() {
        when(participantsCache.remove("user1")).thenReturn(true);
        boolean deleted = dataStorageService.deleteParticipant("user1");
        assertTrue(deleted);
    }

    @Test
    void testDeleteParticipant_notFound() {
        when(participantsCache.remove(anyString())).thenReturn(false);
        boolean deleted = dataStorageService.deleteParticipant("unknownUser");
        assertFalse(deleted);
    }

    @Test
    void testGetAllChallenges() {
        when(challengesCache.iterator()).thenReturn(java.util.Collections.emptyIterator());
        List<Challenge> challenges = dataStorageService.getAllChallenges();
        assertNotNull(challenges);
        assertTrue(challenges.isEmpty());
    }

    @Test
    void testSaveNull() {
        // Should not throw
        assertDoesNotThrow(() -> dataStorageService.saveChallenge(null));
        assertDoesNotThrow(() -> dataStorageService.saveParticipant(null));
    }

    @Test
    void testDestroyCallsIgniteConfig() {
        dataStorageService.destroy();
        verify(igniteConfig).closeIgnite();
    }
}
