package com.discord.challengebot.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChallengeRenewalServiceTest {
    @ParameterizedTest
    @CsvSource({
            "1000, true, 1300",
            "1000, false, 700",
            "574, true, 750",
            "140, false, 100",
            "10, true, 20",
            "10, false, 7",
            "1, false, 1",
            "1, true, 2"
    })
    void roundsTargetAndKeepsDirection(long oldTarget, boolean reached, long expected) {
        assertEquals(expected, ChallengeRenewalService.roundTarget(oldTarget, reached));
    }
}
