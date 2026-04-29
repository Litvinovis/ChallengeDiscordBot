package com.discord.challengebot.service;

import com.discord.challengebot.dto.ChallengeStats;

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
}
