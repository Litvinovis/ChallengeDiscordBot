package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис для генерации визуализаций
 */
@Service
public class VisualizationService implements IVisualizationService {
	private static final Logger logger = LoggerFactory.getLogger(VisualizationService.class);

	/**
	 * Сгенерировать изображение прогресса испытания.
	 * Выполняется синхронно — нет смысла в отдельном пуле при 10 запросах в сутки.
	 */
	@Override
	public CompletableFuture<byte[]> generateProgressChart(ChallengeStats stats) {
		try {
			DefaultCategoryDataset dataset = new DefaultCategoryDataset();
			dataset.addValue(stats.currentValue(), "Выполнено", "");
			dataset.addValue(stats.remaining(), "Осталось", "");

			JFreeChart barChart = ChartFactory.createBarChart(
							"Прогресс по испытанию: " + stats.challengeName(),
							"Статус",
							"Количество",
							dataset,
							PlotOrientation.VERTICAL,
							true, true, false);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ChartUtils.writeChartAsPNG(out, barChart, 400, 300);
			return CompletableFuture.completedFuture(out.toByteArray());
		} catch (IOException e) {
			logger.error("Ошибка генерации изображения прогресса", e);
			return CompletableFuture.completedFuture(new byte[0]);
		}
	}

	/**
	 * Сгенерировать изображение процента выполнения.
	 * Выполняется синхронно — нет смысла в отдельном пуле при 10 запросах в сутки.
	 */
	@Override
	public CompletableFuture<byte[]> generatePercentageChart(ChallengeStats stats) {
		try {
			DefaultCategoryDataset dataset = new DefaultCategoryDataset();
			dataset.addValue(stats.percentage(), "Выполнено (%)", "");
			dataset.addValue(100 - stats.percentage(), "Осталось (%)", "");

			JFreeChart barChart = ChartFactory.createBarChart(
							"Процент выполнения: " + String.format("%.2f", stats.percentage()) + "%",
							"Процент",
							"Значение",
							dataset,
							PlotOrientation.VERTICAL,
							true, true, false);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ChartUtils.writeChartAsPNG(out, barChart, 400, 300);
			return CompletableFuture.completedFuture(out.toByteArray());
		} catch (IOException e) {
			logger.error("Ошибка генерации изображения процента выполнения", e);
			return CompletableFuture.completedFuture(new byte[0]);
		}
	}

	@Override
	public CompletableFuture<byte[]> generateDailyProgressChart(String challengeName, Map<LocalDate, Long> dailyTotals) {
		try {
			DefaultCategoryDataset dataset = new DefaultCategoryDataset();
			DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM");
			dailyTotals.entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.forEach(e -> dataset.addValue(e.getValue(), "Прогресс", e.getKey().format(fmt)));
			JFreeChart chart = ChartFactory.createBarChart(
					"Динамика: " + challengeName, "День", "Количество",
					dataset, PlotOrientation.VERTICAL, false, true, false);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ChartUtils.writeChartAsPNG(out, chart, 600, 400);
			return CompletableFuture.completedFuture(out.toByteArray());
		} catch (IOException e) {
			logger.error("Ошибка генерации графика динамики по дням", e);
			return CompletableFuture.completedFuture(new byte[0]);
		}
	}

	@Override
	public CompletableFuture<byte[]> generateParticipationPieChart(String challengeName, Map<String, Long> participantProgress) {
		try {
			DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
			participantProgress.forEach((userId, amount) -> {
				if (amount > 0) dataset.setValue(userId, amount);
			});
			JFreeChart chart = ChartFactory.createPieChart(
					"Вклад участников: " + challengeName, dataset, true, true, false);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ChartUtils.writeChartAsPNG(out, chart, 500, 400);
			return CompletableFuture.completedFuture(out.toByteArray());
		} catch (IOException e) {
			logger.error("Ошибка генерации круговой диаграммы участия", e);
			return CompletableFuture.completedFuture(new byte[0]);
		}
	}
}
