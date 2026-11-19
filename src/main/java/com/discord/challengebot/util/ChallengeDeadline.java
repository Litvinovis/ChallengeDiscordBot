package com.discord.challengebot.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class ChallengeDeadline {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.uuuu");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm");

    private ChallengeDeadline() {}

    /** Указанная дата включена до полуночи следующего дня. */
    public static LocalDateTime parseLastDay(String value) {
        LocalDate day;
        try {
            day = LocalDate.parse(value, DATE.withResolverStyle(ResolverStyle.STRICT));
        } catch (DateTimeParseException e) {
            day = LocalDate.parse(value);
        }
        return day.plusDays(1).atStartOfDay();
    }

    public static String format(LocalDateTime deadline) {
        if (deadline == null) return "—";
        if (deadline.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            return deadline.minusDays(1).format(DATE) + " включительно (МСК)";
        }
        return deadline.format(DATE_TIME) + " МСК";
    }
}
