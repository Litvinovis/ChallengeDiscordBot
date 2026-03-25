package com.discord.challengebot.model;

import java.io.Serializable;

/**
 * Модель достижения пользователя.
 * Достижение выдаётся при достижении определённого порогового значения прогресса в испытании.
 */
public class Achievement implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;        // e.g. "100_reps", "500_reps", "1000_reps"
    private String name;
    private String description;
    private int threshold;    // 100, 500, 1000

    /**
     * Конструктор по умолчанию.
     */
    public Achievement() {}

    /**
     * Конструктор с параметрами.
     *
     * @param id          уникальный идентификатор достижения (например, "100_reps")
     * @param name        отображаемое название достижения
     * @param description описание достижения
     * @param threshold   пороговое значение прогресса для получения достижения
     */
    public Achievement(String id, String name, String description, int threshold) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.threshold = threshold;
    }

    /**
     * Возвращает уникальный идентификатор достижения.
     *
     * @return идентификатор достижения
     */
    public String getId() { return id; }

    /**
     * Устанавливает уникальный идентификатор достижения.
     *
     * @param id идентификатор достижения
     */
    public void setId(String id) { this.id = id; }

    /**
     * Возвращает отображаемое название достижения.
     *
     * @return название достижения
     */
    public String getName() { return name; }

    /**
     * Устанавливает название достижения.
     *
     * @param name название достижения
     */
    public void setName(String name) { this.name = name; }

    /**
     * Возвращает описание достижения.
     *
     * @return описание достижения
     */
    public String getDescription() { return description; }

    /**
     * Устанавливает описание достижения.
     *
     * @param description описание достижения
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Возвращает пороговое значение для получения достижения.
     *
     * @return пороговое значение
     */
    public int getThreshold() { return threshold; }

    /**
     * Устанавливает пороговое значение для получения достижения.
     *
     * @param threshold пороговое значение
     */
    public void setThreshold(int threshold) { this.threshold = threshold; }
}
