package com.example.demo.service;

import com.example.demo.dto.NoteDTO;
import com.example.demo.model.Note;
import com.example.demo.model.User;
import com.example.demo.repository.NoteRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public NoteService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Note createNote(NoteDTO noteDTO, Long userId) {
        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Note note = new Note();
        note.setTitle(noteDTO.getTitle());
        note.setContent(noteDTO.getContent());
        note.setUserId(userId);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        return noteRepository.save(note);
    }

    public List<Note> getAllNotesByUserId(Long userId) {
        return noteRepository.findByUserId(userId);
    }

    public Optional<Note> getNoteById(Long noteId, Long userId) {
        Optional<Note> note = noteRepository.findByIdAndUserId(noteId, userId);
        if (note.isEmpty()) {
            // Check if note exists but belongs to another user
            Optional<Note> anyNote = noteRepository.findById(noteId);
            if (anyNote.isPresent()) {
                throw new AccessDeniedException("You do not have access to this note");
            }
        }
        return note;
    }

    @Transactional
    public Note updateNote(Long noteId, NoteDTO noteDTO, Long userId) {
        Optional<Note> noteOpt = noteRepository.findByIdAndUserId(noteId, userId);
        
        if (noteOpt.isEmpty()) {
            // Check if note exists but belongs to another user
            Optional<Note> anyNote = noteRepository.findById(noteId);
            if (anyNote.isPresent()) {
                throw new AccessDeniedException("You do not have access to this note");
            }
            throw new IllegalArgumentException("Note not found");
        }

        Note note = noteOpt.get();
        note.setTitle(noteDTO.getTitle());
        note.setContent(noteDTO.getContent());
        note.setUpdatedAt(LocalDateTime.now());

        Note updated = noteRepository.save(note);
        if (updated == null) {
            throw new RuntimeException("Failed to update note");
        }
        return updated;
    }

    @Transactional
    public boolean deleteNote(Long noteId, Long userId) {
        // Check if note exists and belongs to user
        Optional<Note> noteOpt = noteRepository.findByIdAndUserId(noteId, userId);
        
        if (noteOpt.isEmpty()) {
            // Check if note exists but belongs to another user
            Optional<Note> anyNote = noteRepository.findById(noteId);
            if (anyNote.isPresent()) {
                throw new AccessDeniedException("You do not have access to this note");
            }
            return false; // Note doesn't exist
        }

        return noteRepository.deleteByIdAndUserId(noteId, userId);
    }

    public boolean noteExists(Long noteId) {
        return noteRepository.existsById(noteId);
    }

    public boolean noteBelongsToUser(Long noteId, Long userId) {
        Optional<Note> note = noteRepository.findByIdAndUserId(noteId, userId);
        return note.isPresent();
    }
}
