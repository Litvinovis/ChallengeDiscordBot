package com.discord.challengebot.e2e;

import com.discord.challengebot.ChallengeBotApplication;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.scheduled.ChallengeInactivityScheduler;
import com.discord.challengebot.scheduled.DailyReportScheduler;
import com.discord.challengebot.scheduled.WeeklyReportScheduler;
import com.discord.challengebot.util.TimeZones;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.discord.challengebot.e2e.DiscordTestTransport.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@EnabledIfEnvironmentVariable(named = "CHALLENGE_TEST_DB_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(30)
class BotDiscordE2eTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private ConfigurableApplicationContext app;
    private DiscordTestTransport discord;
    private JdbcTemplate db;
    private JdbcTemplate fixtureAdmin;
    private String schema;

    @BeforeAll
    @Timeout(45)
    void startApplication() {
        String url = System.getenv("CHALLENGE_TEST_DB_URL");
        // Используем базу из scripts/test-postgres.sh и отдельную схему для каждого запуска набора.
        assertTrue(url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[1-9][0-9]*/challenge_test"));
        assertEquals("inactivity_test", System.getenv("CHALLENGE_TEST_DB_USER"));
        var fixture = new PGSimpleDataSource();
        fixture.setURL(url);
        fixture.setUser("inactivity_test");
        fixtureAdmin = new JdbcTemplate(fixture);
        schema = "e2e_" + UUID.randomUUID().toString().replace("-", "");
        fixtureAdmin.execute("CREATE SCHEMA " + schema);
        launch(url + "?currentSchema=" + schema);
    }

    private void launch(String url) {
        discord = new DiscordTestTransport();
        try (var connection = discord.interceptConnection()) {
            app = new SpringApplicationBuilder(ChallengeBotApplication.class)
                    .web(WebApplicationType.NONE)
                    // Задания вызываются вручную, чтобы тесты не зависели от минуты запуска.
                    .initializers(ctx -> ((GenericApplicationContext) ctx)
                            .registerBean(TaskScheduler.class, () -> mock(TaskScheduler.class)))
                    .run("--spring.datasource.url=" + url, "--spring.datasource.username=inactivity_test",
                            "--spring.datasource.password=", "--discord.token=local-e2e",
                            "--discord.channel-ids[0]=" + CHANNEL_ID, "--discord.admin-user-ids[0]=" + ADMIN,
                            "--discord.report-guild-id=" + GUILD_ID, "--discord.report-channel=" + CHANNEL_NAME,
                            "--spring.main.banner-mode=off");
        }
        db = app.getBean(JdbcTemplate.class);
    }

    @AfterAll
    void stopApplication() {
        if (app != null) app.close();
        if (fixtureAdmin != null && schema != null) {
            fixtureAdmin.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @BeforeEach
    void clearFixture() {
        db.execute("""
                TRUNCATE challenges, challenge_progress, challenge_participants, progress_history, challenge_archive
                RESTART IDENTITY
                """);
    }

    @Test
    void routesOnlyGameMessagesAndShowsHelp() {
        assertTrue(discord.botMessage("+помощь").isEmpty());
        assertTrue(discord.wrongChannel("+помощь").isEmpty());
        assertTrue(discord.command("привет", ALICE, null).isEmpty());
        assertReply(send(ALICE, "+помощь"), "+регистрация");
        assertReply(send(ALICE, "+новый запрет 100"), "нет прав");
        assertEquals(0, db.queryForObject("SELECT count(*) FROM challenges", Integer.class));
        assertReply(send(ALICE, "+неизвестно 10"), "не найдено");
        assertReply(send(ADMIN, "+новый неверная 100 31.02.2030"), "Неверная дата");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+новый", "+удалить", "+остановить", "+продолжить", "+изменить", "+изменить_дату",
            "+установить_прогресс", "+добавить_участника", "+удалить_участника",
            "+регистрация", "+прогресс", "+прогноз", "+топ", "+топ-день",
            "+детали", "+график", "+динамика", "+участие", "+экспорт"
    })
    void commandsWithRequiredArgumentsGiveUsageWithoutWriting(String message) {
        var response = text(send(ADMIN, message));
        assertTrue(response.contains("Использ") || response.contains("Пример"),
                () -> "Команда " + message + " не объяснила параметры: " + response);
        assertEquals(0, db.queryForObject("SELECT count(*) FROM challenges", Integer.class));
        assertEquals(0, db.queryForObject("SELECT count(*) FROM progress_history", Integer.class));
    }

    @Test
    void invalidProgressAndZeroDoNotChangeTotalsOrHistory() {
        create("пресс", 200);
        assertReply(send(ALICE, "+пресс много"), "Количество должно быть числом");
        assertReply(send(ALICE, "+пресс 0"), "не может быть равно нулю");
        assertEquals(0L, current("пресс"));
        assertEquals(0, db.queryForObject("SELECT count(*) FROM progress_history", Integer.class));
    }

    @Test
    void recordsProgressAndShowsPersonalAndTeamViews() {
        create("жим лёжа", 200);
        assertReply(send(ALICE, "+регистрация \"жим лёжа\""), "зарегистрированы");
        var crossing = send(ALICE, "+\"жим лёжа\" 120");
        assertReply(crossing, "всего: 120/200");
        assertReply(crossing, "50%");
        assertReply(crossing, "получил достижение");
        assertReply(send(BOB, "+\"жим лёжа\" 20"), "всего: 140/200");
        assertReply(send(ALICE, "+\"жим лёжа\" -5"), "у тебя: 115");
        assertEquals(135L, current("жим_лёжа"));
        assertEquals(1, db.queryForObject(
                "SELECT current_streak FROM challenge_participants WHERE user_id=?", Integer.class, ALICE));
        assertEquals(3, db.queryForObject("SELECT count(*) FROM progress_history", Integer.class));

        assertReply(send(ALICE, "+испытания"), "жим лёжа");
        assertReply(send(ALICE, "+мои"), "жим лёжа");
        assertReply(send(ALICE, "+прогресс \"жим лёжа\""), "115");
        assertReply(send(ALICE, "+детали \"жим лёжа\""), "115");
        assertReply(send(ALICE, "+статистика"), "жим лёжа");
        assertReply(send(ALICE, "+статистика \"жим лёжа\""), "135");
        assertReply(send(ALICE, "+топ \"жим лёжа\" 1"), "Алиса");
        assertFalse(text(send(ALICE, "+топ \"жим лёжа\" 1")).contains("Боб"));
        assertReply(send(BOB, "+топ-день \"жим лёжа\""), "115");
        assertReply(send(ALICE, "+прогноз \"жим лёжа\""), "Осталось: 65");
        assertReply(send(ALICE, "+обновить_имя Алиса Спорт"), "Алиса Спорт");
        assertReply(send(BOB, "+топ \"жим лёжа\""), "Алиса Спорт");
    }

    @Test
    void individualForecastUsesOwnProgressInsteadOfTeamTotal() {
        var date = LocalDate.now(TimeZones.MOSCOW).plusDays(30);
        assertReply(send(ADMIN, "+новый велосипед 200 " + date + " индивидуальное"), "успешно создано");
        assertReply(send(ALICE, "+велосипед 50"), "у тебя: 50");
        assertReply(send(BOB, "+велосипед 25"), "у тебя: 25");
        assertReply(send(ALICE, "+прогноз велосипед"), "Осталось: 150");
        assertReply(send(BOB, "+прогноз велосипед"), "Осталось: 175");
    }

    @Test
    void handlesAdminStatusDatesParticipantsAndDeletion() {
        create("бег", 200);
        assertReply(send(ADMIN, "+добавить_участника бег <@" + ALICE + ">"), "добавлен");
        assertReply(send(ADMIN, "+установить_прогресс бег <@" + ALICE + "> 55"), "установлен на 55");
        assertEquals(55L, current("бег"));
        assertEquals("ADJUSTMENT", db.queryForObject("SELECT kind FROM progress_history", String.class));
        assertReply(send(ADMIN, "+изменить бег 350"), "350");
        assertEquals(350L, db.queryForObject("SELECT target_value FROM challenges WHERE id='бег'", Long.class));
        assertReply(send(ADMIN, "+изменить_дату бег 31.12.2030"), "31.12.2030 включительно");
        assertEquals(LocalDateTime.of(2031, 1, 1, 0, 0), db.queryForObject(
                "SELECT end_date FROM challenges WHERE id='бег'", LocalDateTime.class));
        assertReply(send(ADMIN, "+остановить бег"), "остановлено");
        assertReply(send(ALICE, "+бег 3"), "не активно");
        assertEquals(55L, current("бег"));
        assertReply(send(ADMIN, "+продолжить бег"), "возобновлено");
        assertReply(send(ALICE, "+бег 3"), "58/350");
        assertReply(send(ADMIN, "+удалить_участника бег <@" + ALICE + ">"), "удален");
        assertEquals(0L, current("бег"));
        assertFalse(db.queryForObject("SELECT active FROM challenges WHERE id='бег'", Boolean.class));
        assertReply(send(ADMIN, "+удалить бег"), "удалено");
        assertEquals(0, db.queryForObject("SELECT count(*) FROM progress_history", Integer.class));
    }

    @Test
    void attachesReadableChartsAndCsvWithAdjustment() throws Exception {
        create("ходьба", 1000);
        assertReply(send(ALICE, "+ходьба 25"), "25/1000");
        assertReply(send(ADMIN, "+установить_прогресс ходьба <@" + ALICE + "> 40"), "установлен на 40");

        assertPng(onlyAttachment(send(ALICE, "+график ходьба")), "activity-calendar.png");
        assertPng(onlyAttachment(send(ALICE, "+динамика ходьба")), "daily_chart.png");
        assertPng(onlyAttachment(send(ALICE, "+участие ходьба")), "participation.png");
        var csv = onlyAttachment(send(ALICE, "+экспорт ходьба"));
        assertTrue(csv.name().endsWith(".csv"));
        String content = new String(csv.bytes(), StandardCharsets.UTF_8);
        assertTrue(content.contains("Дата,Количество,Тип"));
        assertTrue(content.contains(",25,Тренировка"));
        assertTrue(content.contains(",15,Корректировка"));
    }

    @Test
    void roundTripsBackupThroughDiscordAndRefusesToOverwrite() throws Exception {
        create("плавание", 1000);
        assertReply(send(ALICE, "+плавание 80"), "80/1000");
        assertReply(send(ADMIN, "+остановить плавание"), "остановлено");
        byte[] snapshot = onlyAttachment(send(ADMIN, "+бэкап")).bytes();
        assertEquals(2, JSON.readTree(snapshot).path("version").asInt());

        assertReply(discord.command("+импорт", ADMIN, snapshot), "пустую базу");
        assertEquals(80L, current("плавание"));

        clearFixture();
        assertReply(discord.command("+импорт", ADMIN, snapshot), "Загружено испытаний: 1");
        assertEquals(80L, current("плавание"));
        assertFalse(db.queryForObject("SELECT active FROM challenges WHERE id='плавание'", Boolean.class));
        assertEquals(80L, db.queryForObject("SELECT amount FROM progress_history", Long.class));
        assertReply(send(ALICE, "+детали плавание"), "80");
    }

    @Test
    void executesRenewalOnceAndPublishesPriorCycleResult() {
        create("планка", 1000);
        assertReply(send(ALICE, "+планка 1050"), "1050/1000");
        var schedule = app.getBean(DailyReportScheduler.class);
        assertTrue(discord.scheduled(schedule::checkChallengeCompletions).isEmpty());
        LocalDateTime deadline = LocalDateTime.now(TimeZones.MOSCOW).minusMinutes(2);
        db.update("UPDATE challenges SET start_date=?, end_date=? WHERE id='планка'", deadline.minusDays(7), deadline);

        var responses = discord.scheduled(schedule::checkChallengeCompletions);
        assertReply(responses, "цель выполнена: 1050 из 1000");
        assertReply(responses, "Новая цель — **1300**");
        assertEquals(0L, current("планка"));
        assertEquals(1050L, db.queryForObject("SELECT current_value FROM challenge_archive", Long.class));
        assertEquals(1, db.queryForObject("SELECT count(*) FROM progress_history", Integer.class));
        assertTrue(discord.scheduled(schedule::checkChallengeCompletions).isEmpty());
        assertReply(send(ALICE, "+планка 9"), "9/1300");
    }

    @Test
    void lowersFailedGoalAndKeepsScheduleAfterRestart() {
        create("йога", 1070);
        assertReply(send(ALICE, "+йога 25"), "25/1070");
        LocalDateTime deadline = LocalDateTime.now(TimeZones.MOSCOW).minusMinutes(1);
        db.update("UPDATE challenges SET start_date=?, end_date=? WHERE id='йога'", deadline.minusDays(5), deadline);
        assertReply(discord.scheduled(app.getBean(DailyReportScheduler.class)::checkChallengeCompletions),
                "Новая цель — **750**");
        LocalDateTime nextDeadline = db.queryForObject("SELECT end_date FROM challenges WHERE id='йога'", LocalDateTime.class);

        app.close();
        launch(System.getenv("CHALLENGE_TEST_DB_URL") + "?currentSchema=" + schema);

        assertEquals(nextDeadline, db.queryForObject("SELECT end_date FROM challenges WHERE id='йога'", LocalDateTime.class));
        assertTrue(discord.scheduled(app.getBean(DailyReportScheduler.class)::checkChallengeCompletions).isEmpty());
        assertEquals(1, db.queryForObject("SELECT count(*) FROM challenge_archive", Integer.class));
        assertReply(send(ALICE, "+йога 10"), "10/750");
    }

    @Test
    void archivesCompletedChallengeWithoutDeletingPausedChallengeOrWorkouts() {
        create("спринт", 10);
        create("растяжка", 100);
        assertReply(send(ALICE, "+спринт 10"), "10/10");
        assertReply(send(ALICE, "+растяжка 20"), "20/100");
        assertReply(send(ADMIN, "+остановить растяжка"), "остановлено");
        db.update("UPDATE challenges SET end_date=NULL WHERE id='спринт'");
        var schedule = app.getBean(DailyReportScheduler.class);
        assertReply(discord.scheduled(schedule::checkChallengeCompletions), "Испытание завершено!");
        db.update("UPDATE challenges SET completed_at=? WHERE id='спринт'",
                LocalDateTime.now(TimeZones.MOSCOW).minusDays(31));
        discord.scheduled(schedule::cleanupOldData);

        assertEquals(0, db.queryForObject("SELECT count(*) FROM challenges WHERE id='спринт'", Integer.class));
        assertEquals(20L, current("растяжка"));
        assertEquals(10L, db.queryForObject("""
                SELECT h.amount FROM progress_history h JOIN challenge_archive a ON a.id=h.challenge_id
                """, Long.class));
        assertEquals(2, db.queryForObject("SELECT count(*) FROM progress_history", Integer.class));
    }

    @Test
    void emitsStreakMilestoneOnceOnTheSameMoscowDay() {
        create("подтягивания", 1000);
        assertReply(send(ALICE, "+подтягивания 3"), "3/1000");
        db.update("UPDATE challenge_participants SET current_streak=2, longest_streak=2, last_activity_date=? WHERE user_id=?",
                LocalDate.now(TimeZones.MOSCOW).minusDays(1), ALICE);
        assertReply(send(ALICE, "+подтягивания 3"), "3-дневная серия");
        assertFalse(text(send(ALICE, "+подтягивания 3")).contains("3-дневная серия"));
        assertEquals(3, db.queryForObject("SELECT current_streak FROM challenge_participants WHERE user_id=?", Integer.class, ALICE));
    }

    @Test
    void sendsDailyWeeklyAndMonthlyReportsFromWorkoutHistory() {
        create("зарядка", 1000);
        assertReply(send(ALICE, "+регистрация зарядка"), "зарегистрированы");

        var daily = app.getBean(DailyReportScheduler.class);
        assertTrue(discord.scheduled(daily::sendDailyProgressReports).isEmpty());
        assertReply(send(ALICE, "+зарядка 50"), "50/1000");
        assertReply(discord.scheduled(daily::sendDailyProgressReports), "Ежедневный отчёт");

        LocalDate today = LocalDate.now(TimeZones.MOSCOW);
        LocalDate previousWeek = today.with(DayOfWeek.MONDAY).minusWeeks(1);
        LocalDate previousMonth = today.withDayOfMonth(1).minusMonths(1);
        historicWorkout("зарядка", previousWeek, 23);
        historicWorkout("зарядка", previousMonth, 41);
        var weekly = app.getBean(WeeklyReportScheduler.class);
        var weeklyResponses = discord.scheduled(weekly::sendWeeklyProgressReports);
        assertReply(weeklyResponses, "Итоги недели");
        assertReply(weeklyResponses, "Текущий цикл: **50 / 1000**");
        var monthlyResponses = discord.scheduled(daily::sendMonthlyReport);
        assertReply(monthlyResponses, "Итоги месяца");
        assertReply(monthlyResponses, "Текущий цикл: **50 / 1000**");
        assertReply(weeklyResponses, "за период: **23**");
        // В начале месяца предыдущая неделя может попадать в отчёт за предыдущий месяц.
        assertReply(monthlyResponses, "за период: **" + (previousWeek.isBefore(today.withDayOfMonth(1)) ? 64 : 41) + "**");
    }

    @Test
    void inactivitySchedulerDeliversBothWarningsBeforeRemovingParticipant() {
        create("скакалка", 1000);
        assertReply(send(ALICE, "+регистрация скакалка"), "зарегистрированы");
        var scheduler = app.getBean(ChallengeInactivityScheduler.class);
        Instant now = Instant.now();
        db.update("UPDATE challenge_progress SET joined_at=? WHERE challenge_id='скакалка'",
                Timestamp.from(now.minusSeconds(15 * 86400L)));
        assertReply(discord.scheduled(scheduler::checkInactivity), "2 недели");
        assertTrue(discord.scheduled(scheduler::checkInactivity).isEmpty());
        db.update("UPDATE challenge_progress SET first_inactivity_warning_at=? WHERE challenge_id='скакалка'",
                Timestamp.from(now.minusSeconds(8 * 86400L)));
        assertReply(discord.scheduled(scheduler::checkInactivity), "последнее напоминание");
        db.update("UPDATE challenge_progress SET second_inactivity_warning_at=? WHERE challenge_id='скакалка'",
                Timestamp.from(now.minusSeconds(8 * 86400L)));
        assertReply(discord.scheduled(scheduler::checkInactivity), "вы исключены");
        assertTrue(app.getBean(ChallengeProgressRepository.class).findByChallengeId("скакалка").isEmpty());
        assertFalse(db.queryForObject("SELECT active FROM challenges WHERE id='скакалка'", Boolean.class));
    }

    private void historicWorkout(String id, LocalDate day, long amount) {
        db.update("INSERT INTO progress_history (challenge_id,user_id,amount,recorded_at) VALUES (?,?,?,?)",
                id, ALICE, amount, Timestamp.from(day.atTime(12, 0).atZone(TimeZones.MOSCOW).toInstant()));
    }

    private void create(String name, long goal) {
        String date = LocalDate.now(TimeZones.MOSCOW).plusDays(30).toString();
        assertReply(send(ADMIN, "+новый \"" + name + "\" " + goal + " " + date), "успешно создано");
    }

    private long current(String id) {
        return db.queryForObject("SELECT current_value FROM challenges WHERE id=?", Long.class, id);
    }

    private List<Response> send(String author, String message) {
        return discord.command(message, author, null);
    }

    private static String text(List<Response> responses) {
        return String.join("\n", responses.stream().map(Response::text).toList());
    }

    private static void assertReply(List<Response> responses, String expected) {
        String actual = text(responses);
        assertTrue(actual.contains(expected), () -> "Ожидалось: " + expected + "\nПолучено: " + actual);
    }

    private static Attachment onlyAttachment(List<Response> responses) {
        var files = responses.stream().flatMap(r -> r.files().stream()).toList();
        assertEquals(1, files.size(), () -> text(responses));
        return files.getFirst();
    }

    private static void assertPng(Attachment file, String filename) throws Exception {
        assertEquals(filename, file.name());
        var image = ImageIO.read(new ByteArrayInputStream(file.bytes()));
        assertNotNull(image);
        assertTrue(image.getWidth() > 0 && image.getHeight() > 0);
    }
}
