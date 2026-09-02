package com.discord.challengebot.repository;

import com.discord.challengebot.model.Challenge;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChallengeArchiveRepository {

    private final JdbcTemplate jdbc;

    public ChallengeArchiveRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void archive(Challenge challenge) {
        if (challenge == null) return;
        jdbc.update(
                "INSERT INTO challenge_archive (id, name, target_value, current_value, chal_type, " +
                "start_date, end_date, description, unit) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (id) DO NOTHING",
                challenge.getId(),
                challenge.getName(),
                challenge.getTargetValue(),
                challenge.getCurrentValue(),
                challenge.getType() != null ? challenge.getType().name() : null,
                challenge.getStartDate(),
                challenge.getEndDate(),
                challenge.getDescription(),
                challenge.getUnit());
    }
}
