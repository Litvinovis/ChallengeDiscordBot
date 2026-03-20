package com.discord.challengebot.model;

import java.io.Serializable;

/**
 * Model representing a user achievement
 */
public class Achievement implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;        // e.g. "100_reps", "500_reps", "1000_reps"
    private String name;
    private String description;
    private int threshold;    // 100, 500, 1000

    public Achievement() {}

    public Achievement(String id, String name, String description, int threshold) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.threshold = threshold;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getThreshold() { return threshold; }
    public void setThreshold(int threshold) { this.threshold = threshold; }
}
