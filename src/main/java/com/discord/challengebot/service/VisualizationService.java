package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.jfree.chart.ChartUtils;

/**
 * Сервис для генерации визуализаций
 */
@Service
public class VisualizationService {
    private static final Logger logger = LoggerFactory.getLogger(VisualizationService.class);

    /**
     * Сгенерировать изображение прогресса испытания
     */
    public byte[] generateProgressChart(ChallengeStats stats) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            dataset.addValue(stats.getCurrentValue(), "Выполнено", "");
            dataset.addValue(stats.getRemaining(), "Осталось", "");

            JFreeChart barChart = ChartFactory.createBarChart(
                    "Прогресс по испытанию: " + stats.getChallengeName(),
                    "Статус",
                    "Количество",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true, true, false);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(out, barChart, 400, 300);
            return out.toByteArray();
        } catch (IOException e) {
            logger.error("Ошибка генерации изображения прогресса", e);
            return new byte[0];
        }
    }

    /**
     * Сгенерировать изображение процента выполнения
     */
    public byte[] generatePercentageChart(ChallengeStats stats) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            dataset.addValue(stats.getPercentage(), "Выполнено (%)", "");
            dataset.addValue(100 - stats.getPercentage(), "Осталось (%)", "");

            JFreeChart barChart = ChartFactory.createBarChart(
                    "Процент выполнения: " + String.format("%.2f", stats.getPercentage()) + "%",
                    "Процент",
                    "Значение",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true, true, false);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(out, barChart, 400, 300);
            return out.toByteArray();
        } catch (IOException e) {
            logger.error("Ошибка генерации изображения процента выполнения", e);
            return new byte[0];
        }
    }
}