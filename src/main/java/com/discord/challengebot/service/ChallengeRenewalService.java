package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeArchiveRepository;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.util.TimeZones;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChallengeRenewalService {
    private static final Duration DEFAULT_DURATION = Duration.ofDays(365);

    private final ChallengeRepository challenges;
    private final ChallengeProgressRepository progress;
    private final ChallengeArchiveRepository archive;

    public ChallengeRenewalService(ChallengeRepository challenges, ChallengeProgressRepository progress,
                                   ChallengeArchiveRepository archive) {
        this.challenges = challenges;
        this.progress = progress;
        this.archive = archive;
    }

    public record Renewal(Challenge previous, long nextTarget, LocalDateTime nextDeadline, boolean goalReached) {}

    @Transactional
    public Optional<Renewal> renewIfDue(String id, LocalDateTime now) {
        // Прогресс и продление используют одну блокировку. После ожидания перечитываем срок и результат.
        challenges.lockById(id);
        var previous = challenges.findById(id).orElse(null);
        if (previous == null || !previous.isActive() || previous.getEndDate() == null
                || previous.getEndDate().isAfter(now)) {
            return Optional.empty();
        }
        if (challenges.stopIfEmpty(id)) {
            return Optional.empty();
        }

        LocalDateTime deadline = previous.getEndDate();
        LocalDateTime start = previous.getStartDate();
        Duration duration = start != null && start.isBefore(deadline)
                ? Duration.between(start, deadline) : DEFAULT_DURATION;
        boolean reached = previous.getCurrentValue() >= previous.getTargetValue();
        long nextTarget = roundTarget(previous.getTargetValue(), reached);
        LocalDateTime nextDeadline = now.plus(duration);

        // История активности остаётся по прежнему идентификатору. Для итога каждого цикла нужен свой архивный ID.
        archive.archive(previous, UUID.randomUUID().toString());
        progress.resetForRenewal(id, now.atZone(TimeZones.MOSCOW).toInstant());
        challenges.startNextCycle(id, nextTarget, now, nextDeadline);
        return Optional.of(new Renewal(previous, nextTarget, nextDeadline, reached));
    }

    static long roundTarget(long target, boolean reached) {
        BigDecimal adjusted = BigDecimal.valueOf(target).multiply(BigDecimal.valueOf(reached ? 13 : 7, 1));
        int digits = adjusted.precision() - adjusted.scale();
        int places = digits >= 2 ? Math.max(1, digits - 2) : 0;
        BigDecimal rounded = adjusted.setScale(-places, RoundingMode.HALF_UP);
        BigDecimal original = BigDecimal.valueOf(target);

        // Для маленьких целей округление может поглотить изменение. Оставляем круглый шаг.
        BigDecimal step = BigDecimal.ONE.scaleByPowerOfTen(places);
        if (reached && rounded.compareTo(original) <= 0) {
            rounded = original.divide(step, 0, RoundingMode.FLOOR).add(BigDecimal.ONE).multiply(step);
        } else if (!reached && rounded.compareTo(original) >= 0) {
            rounded = original.divide(step, 0, RoundingMode.CEILING).subtract(BigDecimal.ONE).multiply(step);
        }
        return rounded.max(BigDecimal.ONE).min(BigDecimal.valueOf(Long.MAX_VALUE)).longValueExact();
    }
}
