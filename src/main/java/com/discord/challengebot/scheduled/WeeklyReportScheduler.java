package com.discord.challengebot.scheduled;

import com.discord.challengebot.service.DiscordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WeeklyReportScheduler {
    private static final Logger logger = LoggerFactory.getLogger(WeeklyReportScheduler.class);

    @Autowired
    private DiscordService discordService;

    @Scheduled(cron = "0 0 20 ? * SUN")
    public void sendWeeklyProgressReports() {
        logger.info("Запуск отправки еженедельных отчётов о прогрессе");
        try {
            discordService.sendWeeklyReport();
            logger.info("Еженедельные отчёты успешно отправлены");
        } catch (Exception e) {
            logger.error("Ошибка при отправке еженедельных отчётов", e);
        }
    }
}
