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

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис для генерации визуализаций
 */
@Service
public class VisualizationService implements IVisualizationService {
	private static final Logger logger = LoggerFactory.getLogger(VisualizationService.class);

	private static final Color[] ACTIVITY_COLORS = {
			new Color(0x263242), new Color(0x135348), new Color(0x12745B),
			new Color(0x26A675), new Color(0x5CDBA3)
	};
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("LLL", Locale.forLanguageTag("ru"));
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	@Override
	public CompletableFuture<byte[]> generateActivityCalendar(LocalDate start, LocalDate end, Map<LocalDate, Long> dailyTotals) {
		LocalDate firstMonday = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		int columns = (int) ChronoUnit.WEEKS.between(firstMonday, end) + 1;
		int step = 18;
		int cell = 14;
		int left = 52;
		int top = 82;
		int width = Math.max(520, left + columns * step + 36);
		BufferedImage image = new BufferedImage(width, 254, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();

		// Записи за пределами видимых дней не должны менять цвета календаря.
		long max = start.datesUntil(end.plusDays(1))
				.mapToLong(day -> dailyTotals.getOrDefault(day, 0L))
				.max().orElse(0L);
		long activeDays = 0;
		Color muted = new Color(0xABBBCB);
		try {
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphics.setColor(new Color(0x131C28));
			graphics.fillRect(0, 0, width, image.getHeight());
			graphics.setColor(new Color(0xEFF5FC));
			graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
			graphics.drawString("Календарь активности", 20, 32);
			graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
			graphics.setColor(muted);
			graphics.drawString(start.format(DATE) + " — " + end.format(DATE) + " · мск", 20, 54);

			String[] weekdays = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
			for (int row = 0; row < weekdays.length; row++) {
				graphics.drawString(weekdays[row], 20, top + row * step + 12);
			}

			for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
				int x = left + (int) ChronoUnit.WEEKS.between(firstMonday, day) * step;
				int y = top + (day.getDayOfWeek().getValue() - 1) * step;
				long total = dailyTotals.getOrDefault(day, 0L);
				int level = 0;
				if (total > 0) {
					activeDays++;
					level = (int) Math.ceil((ACTIVITY_COLORS.length - 1) * (double) total / max);
				}
				graphics.setColor(ACTIVITY_COLORS[level]);
				graphics.fillRoundRect(x, y, cell, cell, 4, 4);
				if (day.getDayOfMonth() == 1) {
					graphics.setColor(muted);
					graphics.drawString(day.format(MONTH), x, top - 8);
				}
			}

			graphics.setColor(muted);
			graphics.drawString("Дней с прогрессом: " + activeDays, 20, 233);
			int legendX = width - 244;
			graphics.drawString("Меньше", legendX, 233);
			for (int i = 0; i < ACTIVITY_COLORS.length; i++) {
				graphics.setColor(ACTIVITY_COLORS[i]);
				graphics.fillRoundRect(legendX + 70 + i * step, 221, cell, cell, 4, 4);
			}
			graphics.setColor(muted);
			graphics.drawString("Больше", legendX + 165, 233);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "png", out);
			return CompletableFuture.completedFuture(out.toByteArray());
		} catch (IOException e) {
			logger.error("Ошибка генерации календаря активности", e);
			return CompletableFuture.completedFuture(new byte[0]);
		} finally {
			graphics.dispose();
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
