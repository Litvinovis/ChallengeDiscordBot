package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.util.TimeZones;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
public class ChallengeInactivityService {
    private static final Duration FIRST_WARNING_DELAY = Duration.ofDays(14);
    private static final Duration NEXT_STEP_DELAY = Duration.ofDays(7);

    private final ChallengeRepository challenges;
    private final ChallengeProgressRepository progress;
    private final ChallengeService challengeService;
    private final DiscordService discord;

    public ChallengeInactivityService(ChallengeRepository challenges, ChallengeProgressRepository progress,
                                     ChallengeService challengeService, DiscordService discord) {
        this.challenges = challenges;
        this.progress = progress;
        this.challengeService = challengeService;
        this.discord = discord;
    }

    /** Блокировка общая с внесением прогресса: свежий результат не теряется при исключении. */
    @Transactional
    public void checkChallenge(String challengeId, Instant now) {
        challenges.lockById(challengeId);
        Challenge challenge = challenges.findById(challengeId).orElse(null);
        if (challenge == null || !challenge.isActive()) return;
        if (challenge.getEndDate() != null && !challenge.getEndDate().isAfter(LocalDateTime.ofInstant(now, TimeZones.MOSCOW))) return;
        if (challenge.getTargetValue() > 0 && challenge.getCurrentValue() >= challenge.getTargetValue()) return;

        var states = progress.findInactivityStates(challengeId);
        if (states.isEmpty()) {
            challenges.stopIfEmpty(challengeId);
            return;
        }
        for (var state : states) {
            String mention = "<@" + state.userId() + ">";
            if (state.firstWarningAt() == null) {
                if (now.isBefore(state.inactiveSince().plus(FIRST_WARNING_DELAY))) continue;
                String message = "⚠️ " + mention + ", вы 2 недели не вносили прогресс в испытание «" + challenge.getName()
                        + "». Необходимо принимать участие и записывать прогресс, иначе вы будете исключены из испытания."
                        + " Через неделю придёт последнее напоминание, ещё через неделю без прогресса — исключение.";
                if (discord.sendInactivityNotification(state.userId(), message)) {
                    progress.markInactivityWarning(challengeId, state.userId(), false, now);
                }
            } else if (state.secondWarningAt() == null) {
                // После простоя бота не пропускаем предупреждения: даём неделю от доставленного сообщения.
                if (now.isBefore(state.firstWarningAt().plus(NEXT_STEP_DELAY))) continue;
                String message = "⚠️ " + mention + ", последнее напоминание об испытании «" + challenge.getName()
                        + "»: после первого предупреждения прогресса нет. Внесите прогресс в течение недели,"
                        + " иначе вы будете исключены из испытания.";
                if (discord.sendInactivityNotification(state.userId(), message)) {
                    progress.markInactivityWarning(challengeId, state.userId(), true, now);
                }
            } else if (!now.isBefore(state.secondWarningAt().plus(NEXT_STEP_DELAY))) {
                challengeService.removeParticipant(challenge, state.userId());
                discord.sendInactivityNotification(state.userId(), "⛔ " + mention + ", вы исключены из испытания «"
                        + challenge.getName() + "»: после двух напоминаний прогресс не был внесён."
                        + (!challenge.isActive() ? " Участников не осталось, испытание остановлено." : ""));
            }
        }
    }
}
