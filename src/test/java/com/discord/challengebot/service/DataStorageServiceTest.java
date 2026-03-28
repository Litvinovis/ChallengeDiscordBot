package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataStorageServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @InjectMocks
    private DataStorageService dataStorageService;

    @BeforeEach
    void setUp() {
        // Репозитории готовы — дополнительная инициализация не нужна
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
        verify(challengeRepository).save(challenge);
    }

    @Test
    void testGetChallenge_found() {
        Challenge challenge = buildChallenge("Отжимания");
        when(challengeRepository.findById("отжимания")).thenReturn(Optional.of(challenge));

        Challenge result = dataStorageService.getChallenge("Отжимания");
        assertNotNull(result);
        assertEquals("Отжимания", result.getName());
    }

    @Test
    void testGetChallenge_notFound() {
        when(challengeRepository.findById(anyString())).thenReturn(Optional.empty());
        when(challengeRepository.findAll()).thenReturn(Collections.emptyList());

        Challenge result = dataStorageService.getChallenge("НесуществующееИспытание");
        assertNull(result);
    }

    @Test
    void testDeleteChallenge_found() {
        when(challengeRepository.existsById("отжимания")).thenReturn(true);

        boolean deleted = dataStorageService.deleteChallenge("Отжимания");
        assertTrue(deleted);
        verify(challengeRepository).deleteById("отжимания");
    }

    @Test
    void testDeleteChallenge_notFound() {
        when(challengeRepository.existsById(anyString())).thenReturn(false);
        when(challengeRepository.findAll()).thenReturn(Collections.emptyList());

        boolean deleted = dataStorageService.deleteChallenge("НесуществующееИспытание");
        assertFalse(deleted);
    }

    @Test
    void testSaveParticipant() {
        Participant participant = new Participant("user1", "TestUser");
        dataStorageService.saveParticipant(participant);
        verify(participantRepository).save(participant);
    }

    @Test
    void testGetParticipant_found() {
        Participant participant = new Participant("user1", "TestUser");
        when(participantRepository.findById("user1")).thenReturn(Optional.of(participant));

        Participant result = dataStorageService.getParticipant("user1");
        assertNotNull(result);
        assertEquals("TestUser", result.getUsername());
    }

    @Test
    void testGetParticipant_notFound() {
        when(participantRepository.findById(anyString())).thenReturn(Optional.empty());

        Participant result = dataStorageService.getParticipant("unknownUser");
        assertNull(result);
    }

    @Test
    void testDeleteParticipant_found() {
        when(participantRepository.existsById("user1")).thenReturn(true);
        boolean deleted = dataStorageService.deleteParticipant("user1");
        assertTrue(deleted);
        verify(participantRepository).deleteById("user1");
    }

    @Test
    void testDeleteParticipant_notFound() {
        when(participantRepository.existsById(anyString())).thenReturn(false);
        boolean deleted = dataStorageService.deleteParticipant("unknownUser");
        assertFalse(deleted);
    }

    @Test
    void testGetAllChallenges() {
        when(challengeRepository.findAll()).thenReturn(Collections.emptyList());
        List<Challenge> challenges = dataStorageService.getAllChallenges();
        assertNotNull(challenges);
        assertTrue(challenges.isEmpty());
    }

    @Test
    void testGetAllParticipants() {
        when(participantRepository.findAll()).thenReturn(Collections.emptyList());
        List<Participant> participants = dataStorageService.getAllParticipants();
        assertNotNull(participants);
        assertTrue(participants.isEmpty());
    }

    @Test
    void testSaveNull() {
        // Should not throw
        assertDoesNotThrow(() -> dataStorageService.saveChallenge(null));
        assertDoesNotThrow(() -> dataStorageService.saveParticipant(null));
    }
}
