package com.example.demo.repository;

import com.example.demo.model.Note;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class NoteRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Note> noteRowMapper = (rs, rowNum) -> {
        Note note = new Note();
        note.setId(rs.getLong("id"));
        note.setTitle(rs.getString("title"));
        note.setContent(rs.getString("content"));
        note.setUserId(rs.getLong("user_id"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            note.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            note.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return note;
    };

    public NoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Note save(Note note) {
        if (note.getId() == null) {
            return insert(note);
        } else {
            return update(note);
        }
    }

    private Note insert(Note note) {
        String sql = "INSERT INTO notes (title, content, user_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setLong(3, note.getUserId());
            ps.setTimestamp(4, Timestamp.valueOf(note.getCreatedAt() != null ? note.getCreatedAt() : LocalDateTime.now()));
            ps.setTimestamp(5, Timestamp.valueOf(note.getUpdatedAt() != null ? note.getUpdatedAt() : LocalDateTime.now()));
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
        note.setId(id);
        return note;
    }

    private Note update(Note note) {
        String sql = "UPDATE notes SET title = ?, content = ?, updated_at = ? WHERE id = ? AND user_id = ?";
        
        int updated = jdbcTemplate.update(sql, ps -> {
            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(4, note.getId());
            ps.setLong(5, note.getUserId());
        });

        if (updated == 0) {
            return null;
        }
        return note;
    }

    public Optional<Note> findById(Long id) {
        String sql = "SELECT * FROM notes WHERE id = ?";
        List<Note> notes = jdbcTemplate.query(sql, ps -> ps.setLong(1, id), noteRowMapper);
        return notes.isEmpty() ? Optional.empty() : Optional.of(notes.get(0));
    }

    public Optional<Note> findByIdAndUserId(Long id, Long userId) {
        String sql = "SELECT * FROM notes WHERE id = ? AND user_id = ?";
        List<Note> notes = jdbcTemplate.query(sql, ps -> {
            ps.setLong(1, id);
            ps.setLong(2, userId);
        }, noteRowMapper);
        return notes.isEmpty() ? Optional.empty() : Optional.of(notes.get(0));
    }

    public List<Note> findByUserId(Long userId) {
        String sql = "SELECT * FROM notes WHERE user_id = ? ORDER BY updated_at DESC";
        return jdbcTemplate.query(sql, ps -> ps.setLong(1, userId), noteRowMapper);
    }

    public boolean deleteById(Long id) {
        String sql = "DELETE FROM notes WHERE id = ?";
        int deleted = jdbcTemplate.update(sql, ps -> ps.setLong(1, id));
        return deleted > 0;
    }

    public boolean deleteByIdAndUserId(Long id, Long userId) {
        String sql = "DELETE FROM notes WHERE id = ? AND user_id = ?";
        int deleted = jdbcTemplate.update(sql, ps -> {
            ps.setLong(1, id);
            ps.setLong(2, userId);
        });
        return deleted > 0;
    }

    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM notes WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}
