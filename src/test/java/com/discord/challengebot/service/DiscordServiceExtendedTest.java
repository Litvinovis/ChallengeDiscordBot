package com.discord.challengebot.service;

import com.discord.challengebot.command.CommandRegistry;
import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.dto.ChallengeStats;
import com.discord.challengebot.model.Challenge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Расширенные тесты для DiscordService: generateHelpMessage, isAuthorizedUser, форматирование.
 */
class DiscordServiceExtendedTest {

    @Mock
    private DiscordConfig discordConfig;

    @Mock
    private ChallengeService challengeService;

    @Mock
    private ParticipantService participantService;

    @Mock
    private StatisticsService statisticsService;

    @Mock
    private CommandRegistry commandRegistry;

    @InjectMocks
    private DiscordService discordService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---- generateHelpMessage (без аргументов) ----

    @Test
    void generateHelpMessage_noArg_containsBasicCommands() {
        String help = discordService.generateHelpMessage();
        assertNotNull(help);
        assertTrue(help.contains("+помощь"), "Must contain help command");
        assertTrue(help.contains("+статистика"), "Must contain statistics command");
        assertTrue(help.contains("+испытания"), "Must contain list command");
    }

    // ---- generateHelpMessage (с userId) ----

    @Test
    void generateHelpMessage_adminUser_includesAdminSection() {
        when(participantService.isAdminUser("admin1")).thenReturn(true);

        String help = discordService.generateHelpMessage("admin1");

        assertTrue(help.contains("+новый"), "Admin must see admin commands");
        assertTrue(help.contains("+удалить"));
    }

    @Test
    void generateHelpMessage_regularUser_excludesAdminSection() {
        when(participantService.isAdminUser("regular")).thenReturn(false);

        String help = discordService.generateHelpMessage("regular");

        assertFalse(help.contains("+новый"), "Regular user must not see admin commands");
        // Базовые команды всё равно должны быть
        assertTrue(help.contains("+помощь"));
        assertTrue(help.contains("+мои"));
    }

    @Test
    void generateHelpMessage_nullUserId_returnsHelpWithoutAdminSection() {
        String help = discordService.generateHelpMessage(null);
        assertNotNull(help);
        assertFalse(help.contains("+новый"));
        assertTrue(help.contains("+помощь"));
    }

    // ---- isAuthorizedUser — граничные случаи ----

    @Test
    void isAuthorizedUser_nullUserId_returnsFalse() {
        assertFalse(discordService.isAuthorizedUser(null, "помощь"));
    }

    @Test
    void isAuthorizedUser_emptyUserId_returnsFalse() {
        assertFalse(discordService.isAuthorizedUser("", "помощь"));
    }

    @Test
    void isAuthorizedUser_nullCommand_returnsTrue() {
        assertTrue(discordService.isAuthorizedUser("user1", null));
    }

    @Test
    void isAuthorizedUser_emptyCommand_returnsTrue() {
        assertTrue(discordService.isAuthorizedUser("user1", ""));
    }

    @Test
    void isAuthorizedUser_adminCommandsRequireAdminRole() {
        String[] adminCommands = {
            "новый испытание", "удалить испытание", "остановить испытание",
            "продолжить испытание", "изменить испытание", "изменить_дату испытание",
            "установить_прогресс испытание user1 100",
            "добавить_участника испытание user1",
            "удалить_участника испытание user1"
        };

        when(participantService.isAdminUser("regular")).thenReturn(false);

        for (String cmd : adminCommands) {
            assertFalse(discordService.isAuthorizedUser("regular", cmd),
                    "Regular user must not be authorized for: " + cmd);
        }
    }

    @Test
    void isAuthorizedUser_nonAdminCommands_allowedForAll() {
        String[] publicCommands = {"помощь", "статистика", "испытания", "мои", "топ"};

        for (String cmd : publicCommands) {
            assertTrue(discordService.isAuthorizedUser("anyuser", cmd),
                    "Public command must be allowed: " + cmd);
        }
        verify(participantService, never()).isAdminUser(anyString());
    }

    // ---- formatChallengeStats ----

    @Test
    void formatChallengeStats_delegatesToStatisticsService() {
        Challenge challenge = new Challenge();
        ChallengeStats stats = new ChallengeStats(null, 0L, 0L, 0L, 0.0, 0.0, 0);
        when(statisticsService.formatReportForDiscord(challenge, stats)).thenReturn("formatted");

        String result = discordService.formatChallengeStats(challenge, stats);

        assertEquals("formatted", result);
        verify(statisticsService).formatReportForDiscord(challenge, stats);
    }
}
