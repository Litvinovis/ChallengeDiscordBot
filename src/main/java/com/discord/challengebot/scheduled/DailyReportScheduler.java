package com.discord.challengebot.scheduled;

import com.discord.challengebot.service.DiscordService;
import com.discord.challengebot.service.ChallengeService;
import com.discord.challengebot.model.Challenge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Планировщик ежедневных отчетов
 */
@Component
public class DailyReportScheduler {
    private static final Logger logger = LoggerFactory.getLogger(DailyReportScheduler.class);
    
    @Autowired
    private DiscordService discordService;
    
    @Autowired
    private ChallengeService challengeService;

    /**
     * Отправка ежедневных отчетов о прогрессе в 7:00 утра
     */
    @Scheduled(cron = "${scheduled.cron.daily-report}")
    public void sendDailyProgressReports() {
        logger.info("Запуск отправки ежедневных отчетов о прогрессе");
        try {
            discordService.sendDailyReport();
            logger.info("Ежедневные отчеты успешно отправлены");
        } catch (Exception e) {
            logger.error("Ошибка при отправке ежедневных отчетов", e);
        }
    }

    /**
     * Проверка завершения испытаний каждый час
     */
    @Scheduled(cron = "0 0 * * * ?") // Каждый час
    public void checkChallengeCompletions() {
        logger.info("Проверка завершения испытаний");
        
        try {
            // Получаем все активные испытания
            List<Challenge> challenges = challengeService.getAllChallenges();
            LocalDateTime now = LocalDateTime.now();
            
            logger.debug("Получено {} активных испытаний для проверки завершения", challenges.size());
            
            int completedChallenges = 0;
            for (Challenge challenge : challenges) {
                if (challenge.isActive() && challenge.getEndDate().isBefore(now)) {
                    logger.info("Испытание '{}' завершено", challenge.getName());
                    // Испытание завершено
                    challengeService.completeChallenge(challenge);
                    discordService.sendChallengeCompletionNotification(challenge);
                    completedChallenges++;
                }
            }
            
            logger.info("Проверка завершения испытаний завершена. Завершено {} испытаний", completedChallenges);
        } catch (Exception e) {
            logger.error("Ошибка при проверке завершения испытаний", e);
        }
    }

    /**
     * Очистка старых данных каждый день в 2:00 ночи
     */
    @Scheduled(cron = "0 0 2 * * ?") // Каждый день в 2:00
    public void cleanupOldData() {
        logger.info("Очистка старых данных");
        try {
            // В реальной реализации здесь будет очистка старых данных
            // Например, удаление завершенных испытаний старше 30 дней
            logger.info("Очистка старых данных завершена");
        } catch (Exception e) {
            logger.error("Ошибка при очистке старых данных", e);
        }
    }
}