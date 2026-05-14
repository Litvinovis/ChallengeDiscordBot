package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.Participant;
import com.discord.challengebot.repository.ProgressHistoryRepository;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.ParticipantService;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * Команда {@code +топ-день <испытание>} — топ участников по приросту за последние 24 часа.
 */
@Component
@Order(1)
public class TopDayCommand extends BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(TopDayCommand.class);

    @Autowired
    private IChallengeService challengeService;
    @Autowired
    private ProgressHistoryRepository progressHistoryRepository;
    @Autowired
    private ParticipantService participantService;

    @Override
    public boolean canHandle(String cmd) {
        return "топ-день".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            if (args.length < 2) {
                replyError(event, "Укажите название испытания. Пример: `+топ-день Отжимания`");
                return;
            }
            String challengeName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            Challenge challenge = challengeService.getChallenge(challengeName);
            if (challenge == null) {
                replyError(event, "Испытание \"" + challengeName + "\" не найдено.");
                return;
            }
            Map<String, Long> totals = progressHistoryRepository.getUserTotalsLast24Hours(challenge.getId());
            if (totals.isEmpty()) {
                reply(event, "Нет активности за последние 24 часа по испытанию \"" + challengeName + "\".");
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("**🏆 Топ за 24ч — ").append(challengeName).append(":**\n");
            String[] medals = {"🥇", "🥈", "🥉"};
            int[] rank = {0};
            totals.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(e -> {
                        String name = resolveUsername(e.getKey());
                        String medal = rank[0] < medals.length ? medals[rank[0]] : (rank[0] + 1) + ".";
                        sb.append(medal).append(" ").append(name)
                                .append(" — ").append(e.getValue())
                                .append(" ").append(challenge.getUnit() != null ? challenge.getUnit() : "").append("\n");
                        rank[0]++;
                    });
            reply(event, sb.toString());
        } catch (Exception e) {
            logger.error("Ошибка обработки команды +топ-день", e);
            replyError(event, "Произошла ошибка при получении топа.");
        }
    }

    private String resolveUsername(String userId) {
        try {
            Participant p = participantService.getParticipant(userId);
            if (p != null && p.getUsername() != null && !p.getUsername().isBlank()) {
                return p.getUsername();
            }
        } catch (Exception ignored) {}
        return userId;
    }
}
