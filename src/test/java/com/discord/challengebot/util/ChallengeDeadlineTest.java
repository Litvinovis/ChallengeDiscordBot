package com.discord.challengebot.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeDeadlineTest {
    @ParameterizedTest
    @ValueSource(strings = {"29.02.2028", "2028-02-29"})
    void leapDayIsIncludedAndShownAsLastDay(String input) {
        LocalDateTime deadline = ChallengeDeadline.parseLastDay(input);
        assertEquals(LocalDateTime.of(2028, 3, 1, 0, 0), deadline);
        assertEquals("29.02.2028 включительно (МСК)", ChallengeDeadline.format(deadline));
    }

    @ParameterizedTest
    @ValueSource(strings = {"29.02.2027", "31.04.2028", "2028-02-30"})
    void rejectsNonexistentDates(String input) {
        assertThrows(DateTimeParseException.class, () -> ChallengeDeadline.parseLastDay(input));
    }

    @Test
    void displaysExactTimeAndMissingDeadline() {
        assertEquals("31.12.2028 18:30 МСК", ChallengeDeadline.format(LocalDateTime.of(2028, 12, 31, 18, 30)));
        assertEquals("—", ChallengeDeadline.format(null));
    }
}
