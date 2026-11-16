package com.discord.challengebot.repository;

import com.discord.challengebot.config.DiscordConfig;
import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Реальный PostgreSQL запускается scripts/test-postgres.sh, прод-конфигурация не используется. */
@EnabledIfEnvironmentVariable(named = "CHALLENGE_TEST_DB_URL", matches = ".+")
class ChallengeInactivityPostgresTest {
    private PGSimpleDataSource ds;
    private JdbcTemplate jdbc;
    private String schema;
    private DataSourceTransactionManager tx;
    private ChallengeRepository challenges;
    private ChallengeProgressRepository progress;
    private ParticipantRepository participants;
    private ChallengeService service;
    private DiscordService discord;
    private ChallengeInactivityService inactivity;

    @BeforeEach
    void setup() {
        ds = new PGSimpleDataSource();
        ds.setURL(System.getenv("CHALLENGE_TEST_DB_URL"));
        ds.setUser(System.getenv("CHALLENGE_TEST_DB_USER"));
        jdbc = new JdbcTemplate(ds);
        schema = "inactivity_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("CREATE SCHEMA " + schema);
        ds.setCurrentSchema(schema);
        migrate();
        tx = new DataSourceTransactionManager(ds);
        challenges = new ChallengeRepository(jdbc);
        progress = new ChallengeProgressRepository(jdbc);
        participants = new ParticipantRepository(jdbc);
        service = transactional(new ChallengeService(challenges, progress,
                new ParticipantService(participants, challenges, new DiscordConfig()),
                new ProgressHistoryRepository(jdbc), mock(ApplicationEventPublisher.class)));
        discord = mock(DiscordService.class);
        when(discord.sendInactivityNotification(anyString(), anyString())).thenReturn(true);
        inactivity = transactional(new ChallengeInactivityService(challenges, progress, service, discord));
    }

    private void migrate() {
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(ds);
    }

    @SuppressWarnings("unchecked")
    private <T> T transactional(T target) {
        var factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(tx, new AnnotationTransactionAttributeSource()));
        return (T) factory.getProxy();
    }

    @AfterEach
    void cleanup() {
        if (schema != null) jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
    }

    private Challenge enroll(String id) {
        Challenge challenge = service.createChallenge(id, 10000, LocalDateTime.of(2030, 1, 1, 0, 0), ChallengeType.GROUP, "", "раз");
        service.addParticipantWithUsername(challenge, "123", "Участник");
        return challenge;
    }

    private void lastProgress(String id, Instant at) {
        jdbc.update("UPDATE challenge_progress SET last_progress_at = ? WHERE challenge_id = ?", Timestamp.from(at), id);
    }

    @Test
    void twoWarningsSurviveRestartThenExclusionStopsLastParticipantsChallenge() {
        Challenge challenge = enroll("pushups");
        new ProgressHistoryRepository(jdbc).insert("pushups", "123", "Участник", 10);
        Instant start = Instant.parse("2026-10-01T06:00:00Z");
        lastProgress("pushups", start);
        inactivity.checkChallenge("pushups", start.plus(Duration.ofDays(14)));
        // Новый сервис моделирует перезапуск: никаких счётчиков в памяти нет.
        inactivity = transactional(new ChallengeInactivityService(challenges, progress, service, discord));
        inactivity.checkChallenge("pushups", start.plus(Duration.ofDays(15)));
        verify(discord, times(1)).sendInactivityNotification(eq("123"), anyString());
        inactivity.checkChallenge("pushups", start.plus(Duration.ofDays(21)));
        assertNotNull(progress.findInactivityStates("pushups").getFirst().secondWarningAt());
        inactivity.checkChallenge("pushups", start.plus(Duration.ofDays(28)));
        assertTrue(progress.findByChallengeId("pushups").isEmpty());
        assertFalse(challenges.findById("pushups").orElseThrow().isActive());
        assertTrue(challenges.findById("pushups").orElseThrow().getParticipants().isEmpty());
        assertTrue(participants.findById("123").orElseThrow().getRegisteredChallenges().isEmpty());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM progress_history", Integer.class));
        verify(discord, times(3)).sendInactivityNotification(eq("123"), anyString());
    }

    @Test
    void progressResetsOnlyThisChallengesWarningsAndAllowsNewCycle() {
        Challenge a = enroll("a");
        enroll("b");
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        lastProgress("a", now.minus(Duration.ofDays(14)));
        lastProgress("b", now.minus(Duration.ofDays(14)));
        inactivity.checkChallenge("a", now);
        inactivity.checkChallenge("b", now);
        service.addProgress(a, "123", "Участник", 5);
        assertNull(progress.findInactivityStates("a").getFirst().firstWarningAt());
        assertNotNull(progress.findInactivityStates("b").getFirst().firstWarningAt());
        inactivity.checkChallenge("a", now.plus(Duration.ofDays(13)));
        assertNull(progress.findInactivityStates("a").getFirst().firstWarningAt());
        inactivity.checkChallenge("a", now.plus(Duration.ofDays(15)));
        assertNotNull(progress.findInactivityStates("a").getFirst().firstWarningAt());
    }

    @Test
    void registrationZeroAndSubtractionDoNotResetWarningsButAdminIncreaseDoes() {
        Challenge challenge = enroll("a");
        service.addProgress(challenge, "123", "Участник", 10);
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        lastProgress("a", now.minus(Duration.ofDays(14)));
        inactivity.checkChallenge("a", now);
        var before = progress.findInactivityStates("a").getFirst();
        service.addParticipantWithUsername(staleChallenge("a"), "123", "Участник");
        progress.addAmount("a", "123", 0);
        service.subtractProgress(challenge, "123", "Участник", 1);
        service.setParticipantProgress(challenge, "123", 8);
        assertEquals(before, progress.findInactivityStates("a").getFirst());
        assertEquals(8L, progress.findByChallengeId("a").get("123"));
        service.setParticipantProgress(challenge, "123", 20);
        assertNull(progress.findInactivityStates("a").getFirst().firstWarningAt());
        assertTrue(progress.findInactivityStates("a").getFirst().inactiveSince().isAfter(before.inactiveSince()));
    }

    @Test
    void migrationBackfillsPositiveHistoryOnlyAndIsSafeOnRepeatedStartupAndRejoin() {
        Challenge challenge = enroll("a");
        enroll("b");
        jdbc.update("INSERT INTO progress_history (challenge_id,user_id,amount,recorded_at) VALUES ('a','123',10,?), ('a','123',-5,?)",
                Timestamp.from(Instant.parse("2026-09-01T00:00:00Z")), Timestamp.from(Instant.parse("2026-10-01T00:00:00Z")));
        jdbc.execute("ALTER TABLE challenge_progress DROP COLUMN joined_at, DROP COLUMN last_progress_at, DROP COLUMN first_inactivity_warning_at, DROP COLUMN second_inactivity_warning_at");
        Instant before = Instant.now().minusSeconds(1);
        migrate();
        assertEquals(Instant.parse("2026-09-01T00:00:00Z"), progress.findInactivityStates("a").getFirst().inactiveSince());
        assertTrue(progress.findInactivityStates("b").getFirst().inactiveSince().isAfter(before));
        progress.markInactivityWarning("a", "123", false, Instant.parse("2026-10-01T00:00:00Z"));
        migrate();
        assertNotNull(progress.findInactivityStates("a").getFirst().firstWarningAt());
        service.removeParticipant(challenge, "123");
        service.addParticipantWithUsername(challenge, "123", "Участник");
        migrate();
        assertTrue(progress.findInactivityStates("a").getFirst().inactiveSince().isAfter(before));
        assertNull(progress.findInactivityStates("a").getFirst().firstWarningAt());
    }

    @Test
    void exclusionRollsBackIfRegistrationCleanupFails() {
        Challenge challenge = enroll("a");
        service.addProgress(challenge, "123", "Участник", 10);
        ParticipantService broken = mock(ParticipantService.class);
        doThrow(new IllegalStateException("DB failure")).when(broken).removeChallengeRegistration("123", "a");
        ChallengeService failing = transactional(new ChallengeService(challenges, progress, broken,
                new ProgressHistoryRepository(jdbc), mock(ApplicationEventPublisher.class)));
        assertThrows(IllegalStateException.class, () -> failing.removeParticipant(challenge, "123"));
        assertEquals(10L, progress.findByChallengeId("a").get("123"));
        assertTrue(challenges.findById("a").orElseThrow().isActive());
        assertTrue(challenges.findById("a").orElseThrow().getParticipants().contains("123"));
    }

    @Test
    void progressCommittedBeforeCheckPreventsConcurrentExclusion() throws Exception {
        Challenge challenge = enroll("a");
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        lastProgress("a", now.minus(Duration.ofDays(28)));
        progress.markInactivityWarning("a", "123", false, now.minus(Duration.ofDays(14)));
        progress.markInactivityWarning("a", "123", true, now.minus(Duration.ofDays(7)));
        try (var executor = Executors.newSingleThreadExecutor()) {
            Future<?>[] future = new Future<?>[1];
            new TransactionTemplate(tx).executeWithoutResult(status -> {
                challenges.lockById("a");
                future[0] = executor.submit(() -> inactivity.checkChallenge("a", now));
                service.addProgress(challenge, "123", "Участник", 1);
            });
            future[0].get(10, TimeUnit.SECONDS);
        }
        assertEquals(1L, progress.findByChallengeId("a").get("123"));
        assertNull(progress.findInactivityStates("a").getFirst().firstWarningAt());
        assertTrue(challenges.findById("a").orElseThrow().isActive());
        verifyNoInteractions(discord);
    }

    @Test
    void removingOneParticipantKeepsOtherMembersAndTheirRegistrations() {
        Challenge challenge = enroll("a");
        enroll("b");
        service.addParticipantWithUsername(challenge, "456", "Второй");
        service.removeParticipant(challenge, "123");
        assertTrue(challenges.findById("a").orElseThrow().isActive());
        assertEquals(java.util.Set.of("456"), progress.findByChallengeId("a").keySet());
        assertEquals(java.util.List.of("b"), participants.findById("123").orElseThrow().getRegisteredChallenges());
    }

    @Test
    void stoppedEmptyChallengeRejectsProgressFromStaleCommandSnapshot() {
        Challenge challenge = enroll("a");
        service.removeParticipant(challenge, "123");
        Challenge stale = staleChallenge("a");
        stale.setActive(true);
        assertThrows(IllegalStateException.class, () -> service.addProgress(stale, "123", "Участник", 1));
        assertTrue(progress.findByChallengeId("a").isEmpty());
        assertFalse(challenges.findById("a").orElseThrow().isActive());
    }

    @Test
    void explicitRejoinUsesDatabaseEvenIfCommandWasReadBeforeExclusion() {
        Challenge challenge = enroll("a");
        service.addProgress(challenge, "123", "Участник", 10);
        Challenge stale = service.getChallenge("a");
        service.removeParticipant(challenge, "123");
        service.addParticipantWithUsername(stale, "123", "Участник");
        assertEquals(0L, progress.findByChallengeId("a").get("123"));
        assertNull(progress.findInactivityStates("a").getFirst().firstWarningAt());
        assertTrue(participants.findById("123").orElseThrow().getRegisteredChallenges().contains("a"));
    }

    private static Challenge staleChallenge(String id) {
        Challenge challenge = new Challenge();
        challenge.setId(id); challenge.setName(id);
        return challenge;
    }
}
