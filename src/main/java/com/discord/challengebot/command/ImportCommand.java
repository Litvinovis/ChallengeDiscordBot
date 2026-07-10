package com.discord.challengebot.command;

import com.discord.challengebot.model.ChallengeType;
import com.discord.challengebot.repository.ChallengeProgressRepository;
import com.discord.challengebot.repository.ChallengeRepository;
import com.discord.challengebot.service.IChallengeService;
import com.discord.challengebot.service.IUserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Команда {@code +импорт} — импорт испытаний и прогресса из прикреплённого JSON-файла (только для администраторов).
 */
@Component
@Order(1)
public class ImportCommand extends BaseCommand {
    private static final Logger logger = LoggerFactory.getLogger(ImportCommand.class);

    @Autowired
    private IChallengeService challengeService;
    @Autowired
    private ChallengeProgressRepository progressRepository;
    @Autowired
    private ChallengeRepository challengeRepository;
    @Autowired
    private IUserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public boolean canHandle(String cmd) {
        return "импорт".equals(cmd);
    }

    @Override
    public void execute(MessageReceivedEvent event, String[] args, String authorId, String username) {
        try {
            if (!userService.isAdminUser(authorId)) {
                replyError(event, "Недостаточно прав для выполнения команды");
                return;
            }
            List<Message.Attachment> attachments = event.getMessage().getAttachments();
            if (attachments.isEmpty()) {
                replyError(event, "Прикрепите JSON-файл бэкапа к сообщению.");
                return;
            }
            byte[] jsonBytes = attachments.get(0).getProxy().download()
                    .get(30, TimeUnit.SECONDS).readAllBytes();
            JsonNode root = objectMapper.readTree(jsonBytes);
            int imported = 0;

            JsonNode challengesNode = root.path("challenges");
            if (challengesNode.isArray()) {
                for (JsonNode cn : challengesNode) {
                    String name = cn.path("name").asText(null);
                    long target = cn.path("targetValue").asLong(0);
                    String endDateStr = cn.path("endDate").asText(null);
                    String typeStr = cn.path("type").asText("GROUP");
                    String description = cn.path("description").asText(null);
                    String unit = cn.path("unit").asText(null);
                    if (name == null || target <= 0 || endDateStr == null) continue;
                    if (challengeService.getChallenge(name) != null) continue;
                    try {
                        LocalDateTime endDate = LocalDateTime.parse(endDateStr);
                        ChallengeType type;
                        try {
                            type = ChallengeType.valueOf(typeStr);
                        } catch (Exception ex) {
                            type = ChallengeType.GROUP;
                        }
                        challengeService.createChallenge(name, target, endDate, type, description, unit);
                        imported++;
                    } catch (Exception e) {
                        logger.warn("Не удалось импортировать испытание {}: {}", name, e.getMessage());
                    }
                }
            }

            JsonNode progressNode = root.path("progress");
            Set<String> touchedChallengeIds = new HashSet<>();
            if (progressNode.isObject()) {
                progressNode.fields().forEachRemaining(challengeEntry -> {
                    String challengeId = challengeEntry.getKey();
                    JsonNode userProgress = challengeEntry.getValue();
                    userProgress.fields().forEachRemaining(userEntry -> {
                        String userId = userEntry.getKey();
                        long amount = userEntry.getValue().asLong(0);
                        try {
                            progressRepository.upsert(challengeId, userId, amount);
                            touchedChallengeIds.add(challengeId);
                        } catch (Exception e) {
                            logger.warn("Не удалось импортировать прогресс {}/{}: {}", challengeId, userId, e.getMessage());
                        }
                    });
                });
            }

            // Пересчитываем общий прогресс и список участников испытаний по импортированным данным
            for (String challengeId : touchedChallengeIds) {
                try {
                    challengeRepository.findById(challengeId).ifPresent(ch -> {
                        Map<String, Long> allProgress = progressRepository.findByChallengeId(challengeId);
                        ch.setCurrentValue(allProgress.values().stream().mapToLong(Long::longValue).sum());
                        allProgress.keySet().forEach(ch::addParticipant);
                        challengeRepository.save(ch);
                    });
                } catch (Exception e) {
                    logger.warn("Не удалось пересчитать прогресс испытания {}: {}", challengeId, e.getMessage());
                }
            }

            reply(event, "✅ Импорт завершён. Загружено испытаний: " + imported);
        } catch (Exception e) {
            logger.error("Ошибка обработки команды +импорт", e);
            replyError(event, "Произошла ошибка при импорте данных.");
        }
    }
}
