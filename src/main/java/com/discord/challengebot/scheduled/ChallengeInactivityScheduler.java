package com.discord.challengebot.scheduled;

import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.service.ChallengeInactivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ChallengeInactivityScheduler {
    private static final Logger log = LoggerFactory.getLogger(ChallengeInactivityScheduler.class);
    private final ChallengeRepository challenges;
    private final ChallengeInactivityService inactivity;

    public ChallengeInactivityScheduler(ChallengeRepository challenges, ChallengeInactivityService inactivity) {
        this.challenges = challenges;
        this.inactivity = inactivity;
    }

    // Проверяем до утреннего отчёта, в том же часовом поясе, что и игровая логика.
    @Scheduled(cron = "0 45 6 * * *", zone = "Europe/Moscow")
    public void checkInactivity() {
        Instant now = Instant.now();
        for (var challenge : challenges.findAll()) {
            if (!challenge.isActive()) continue;
            try {
                inactivity.checkChallenge(challenge.getId(), now);
            } catch (Exception e) {
                log.error("Не удалось проверить неактивность в испытании {}", challenge.getId(), e);
            }
        }
    }
}
