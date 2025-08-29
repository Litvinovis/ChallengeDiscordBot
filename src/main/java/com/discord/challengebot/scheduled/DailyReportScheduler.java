package com.discord.challengebot.scheduled;

import com.discord.challengebot.service.DiscordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Планировщик ежедневных отчетов
 */
@Component
public class DailyReportScheduler {
    private static final Logger logger = LoggerFactory.getLogger(DailyReportScheduler.class);
    
    @Autowired
    private DiscordService discordService;

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
        // В реальной реализации здесь будет проверка завершения испытаний
    }

    /**
     * Очистка старых данных каждый день в 2:00 ночи
     */
    @Scheduled(cron = "0 0 2 * * ?") // Каждый день в 2:00
    public void cleanupOldData() {
        logger.info("Очистка старых данных");
        // В реальной реализации здесь будет очистка старых данных
    }
}