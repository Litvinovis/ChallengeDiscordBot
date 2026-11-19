package com.discord.challengebot.service;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.repository.ChallengeProgressRepository.InactivityState;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.util.TimeZones;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChallengeInactivityService {
    private static final Duration FIRST_WARNING_DELAY = Duration.ofDays(14);
    private static final Duration NEXT_STEP_DELAY = Duration.ofDays(7);

    private final ChallengeRepository challenges;
    private final ChallengeProgressRepository progress;
    private final ChallengeService challengeService;
    private final DiscordService discord;
    private final TransactionTemplate transactions;

    public ChallengeInactivityService(ChallengeRepository challenges, ChallengeProgressRepository progress,
                                     ChallengeService challengeService, DiscordService discord,
                                     PlatformTransactionManager transactionManager) {
        this.challenges = challenges;
        this.progress = progress;
        this.challengeService = challengeService;
        this.discord = discord;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    private enum Stage { FIRST, SECOND, REMOVE }

    // Один экземпляр планировщика не отправляет параллельно одно и то же предупреждение.
    public synchronized void checkChallenge(String id, Instant now) {
        var states = transactions.execute(tx -> {
            if (eligibleChallenge(id, now) == null) return List.<InactivityState>of();
            var members = progress.findInactivityStates(id);
            if (members.isEmpty()) challenges.stopIfEmpty(id);
            return members;
        });
        for (var state : states) {
            Stage stage = dueStage(state, now);
            if (stage == null) continue;
            String message = transactions.execute(tx -> prepareMessage(id, now, state, stage));
            if (message == null) continue;

            // Discord может отвечать секунды. Блокировка строки уже снята и команды прогресса работают.
            if (discord.sendInactivityNotification(state.userId(), message) && stage != Stage.REMOVE) {
                transactions.executeWithoutResult(tx -> {
                    if (eligibleChallenge(id, now) != null && stillCurrent(id, state)) {
                        progress.markInactivityWarning(id, state.userId(), stage == Stage.SECOND, now);
                    }
                });
            }
        }
    }

    private Challenge eligibleChallenge(String id, Instant now) {
        challenges.lockById(id);
        Challenge challenge = challenges.findById(id).orElse(null);
        if (challenge == null || !challenge.isActive()) return null;
        if (challenge.getEndDate() != null
                && !challenge.getEndDate().isAfter(LocalDateTime.ofInstant(now, TimeZones.MOSCOW))) return null;
        if (challenge.getTargetValue() > 0 && challenge.getCurrentValue() >= challenge.getTargetValue()) return null;
        return challenge;
    }

    private boolean stillCurrent(String id, InactivityState state) {
        return progress.findInactivityState(id, state.userId()).contains(state);
    }

    private static Stage dueStage(InactivityState state, Instant now) {
        if (state.firstWarningAt() == null) {
            return now.isBefore(state.inactiveSince().plus(FIRST_WARNING_DELAY)) ? null : Stage.FIRST;
        }
        if (state.secondWarningAt() == null) {
            return now.isBefore(state.firstWarningAt().plus(NEXT_STEP_DELAY)) ? null : Stage.SECOND;
        }
        return now.isBefore(state.secondWarningAt().plus(NEXT_STEP_DELAY)) ? null : Stage.REMOVE;
    }

    private String prepareMessage(String id, Instant now, InactivityState state, Stage stage) {
        Challenge challenge = eligibleChallenge(id, now);
        if (challenge == null || !stillCurrent(id, state)) return null;

        String mention = "<@" + state.userId() + ">";
        String name = challenge.getName();
        return switch (stage) {
            case FIRST -> "⚠️ " + mention + ", вы 2 недели не вносили прогресс в испытание «" + name
                    + "». Необходимо принимать участие и записывать прогресс, иначе вы будете исключены из испытания."
                    + " Через неделю придёт последнее напоминание, ещё через неделю без прогресса — исключение.";
            case SECOND -> "⚠️ " + mention + ", последнее напоминание об испытании «" + name
                    + "»: после первого предупреждения прогресса нет. Внесите прогресс в течение недели,"
                    + " иначе вы будете исключены из испытания.";
            case REMOVE -> {
                challengeService.removeParticipant(challenge, state.userId());
                yield "⛔ " + mention + ", вы исключены из испытания «" + name
                        + "»: после двух напоминаний прогресс не был внесён."
                        + (challenge.isActive() ? "" : " Участников не осталось, испытание остановлено.");
            }
        };
    }
}
