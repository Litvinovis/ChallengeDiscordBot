package com.discord.challengebot.command;

import com.discord.challengebot.model.Challenge;
import com.discord.challengebot.model.ProgressRecord;
import com.discord.challengebot.repository.ProgressHistoryRepository;
import com.discord.challengebot.service.IChallengeService;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Команда {@code +детали <испытание>} — персональная статистика прогресса по дням.
 */
@Component
@Order(1)
public class MyDetailsCommand extends BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(MyDetailsCommand.class);

    @Autowired
    private IChallengeService challengeService;
    @Autowired
    private ProgressHistoryRepository progressHistoryRepository;

    @Override
    public boolean canHandle(String cmd) {
        return "детали".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            if (args.length < 2) {
                replyError(event, "Укажите название испытания. Пример: `+детали Отжимания`");
                return;
            }
            String challengeName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            Challenge challenge = challengeService.getChallenge(challengeName);
            if (challenge == null) {
                replyError(event, "Испытание \"" + challengeName + "\" не найдено.");
                return;
            }
            Map<LocalDate, Long> daily = progressHistoryRepository.getDailyTotals(challenge.getId(), authorId, 30);
            if (daily.isEmpty()) {
                reply(event, "Нет данных о вашем прогрессе по испытанию \"" + challengeName + "\" за последние 30 дней.");
                return;
            }
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM");
            long total = daily.values().stream().mapToLong(Long::longValue).sum();
            long avgPerDay = daily.isEmpty() ? 0 : total / daily.size();
            LocalDate bestDay = daily.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            StringBuilder sb = new StringBuilder();
            sb.append("**Ваш прогресс по испытанию: ").append(challengeName).append("** (последние 30 дней)\n");
            daily.entrySet().stream()
                    .sorted(Map.Entry.<LocalDate, Long>comparingByKey().reversed())
                    .limit(30)
                    .forEach(e -> sb.append("📅 ").append(e.getKey().format(fmt))
                            .append(": ").append(e.getValue()).append("\n"));
            sb.append("\n📊 Всего: **").append(total).append("**");
            sb.append(" | Среднее/день: **").append(avgPerDay).append("**");
            if (bestDay != null) {
                sb.append(" | Лучший день: **").append(bestDay.format(fmt)).append("**");
            }
            reply(event, sb.toString());
        } catch (Exception e) {
            logger.error("Ошибка обработки команды +детали", e);
            replyError(event, "Произошла ошибка при получении деталей.");
        }
    }
}
