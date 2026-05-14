package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Интерфейс сервиса генерации визуализаций (графиков) для испытаний.
 * Методы выполняются асинхронно и возвращают PNG-изображения в виде байтового массива.
 */
public interface IVisualizationService {
	/**
	 * Асинхронно генерирует столбчатый график прогресса испытания.
	 *
	 * @param stats статистика испытания
	 * @return CompletableFuture с байтами PNG-изображения
	 */
	CompletableFuture<byte[]> generateProgressChart(ChallengeStats stats);

	/**
	 * Асинхронно генерирует столбчатый график процента выполнения испытания.
	 *
	 * @param stats статистика испытания
	 * @return CompletableFuture с байтами PNG-изображения
	 */
	CompletableFuture<byte[]> generatePercentageChart(ChallengeStats stats);

	/**
	 * Асинхронно генерирует столбчатый график динамики прогресса по дням.
	 *
	 * @param challengeName название испытания
	 * @param dailyTotals   карта дата -> суммарный прогресс за день
	 * @return CompletableFuture с байтами PNG-изображения
	 */
	CompletableFuture<byte[]> generateDailyProgressChart(String challengeName, Map<LocalDate, Long> dailyTotals);

	/**
	 * Асинхронно генерирует круговую диаграмму вклада участников.
	 *
	 * @param challengeName        название испытания
	 * @param participantProgress  карта userId -> суммарный прогресс
	 * @return CompletableFuture с байтами PNG-изображения
	 */
	CompletableFuture<byte[]> generateParticipationPieChart(String challengeName, Map<String, Long> participantProgress);
}
