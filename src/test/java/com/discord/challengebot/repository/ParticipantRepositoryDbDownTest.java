package com.discord.challengebot.repository;

import com.discord.challengebot.model.Participant;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Проверяет поведение репозиториев при недоступности БД.
 *
 * ParticipantRepository намеренно не перехватывает DataAccessException —
 * исключение всплывает до DiscordMessageListener, который ловит любое Exception
 * и отправляет пользователю сообщение "⚠️ Произошла внутренняя ошибка".
 */
class ParticipantRepositoryDbDownTest {

	private JdbcTemplate brokenJdbc() throws SQLException {
		DataSource ds = mock(DataSource.class);
		when(ds.getConnection()).thenThrow(new SQLException("Connection refused: DB is down"));
		return new JdbcTemplate(ds);
	}

	@Test
	void findById_whenDbDown_throwsDataAccessException() throws SQLException {
		ParticipantRepository repo = new ParticipantRepository(brokenJdbc());
		assertThrows(DataAccessException.class, () -> repo.findById("user1"));
	}

	@Test
	void findAll_whenDbDown_throwsDataAccessException() throws SQLException {
		ParticipantRepository repo = new ParticipantRepository(brokenJdbc());
		assertThrows(DataAccessException.class, repo::findAll);
	}

	@Test
	void save_whenDbDown_throwsDataAccessException() throws SQLException {
		Participant p = new Participant();
		p.setUserId("user1");
		p.setUsername("TestUser");
		ParticipantRepository repo = new ParticipantRepository(brokenJdbc());
		assertThrows(DataAccessException.class, () -> repo.save(p));
	}

	@Test
	void existsById_whenDbDown_throwsDataAccessException() throws SQLException {
		ParticipantRepository repo = new ParticipantRepository(brokenJdbc());
		assertThrows(DataAccessException.class, () -> repo.existsById("user1"));
	}
}
