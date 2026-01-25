package com.example.demo.controller;

import com.example.demo.dto.NoteDTO;
import com.example.demo.model.Note;
import com.example.demo.model.User;
import com.example.demo.service.NoteService;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final NoteService noteService;
    private final UserService userService;

    public NoteController(NoteService noteService, UserService userService) {
        this.noteService = noteService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN', 'OIDC_USER')")
    public ResponseEntity<Map<String, Object>> createNote(
            @Valid @RequestBody NoteDTO noteDTO,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        System.out.println("Creating note for userId: " + userId);
        Note note = noteService.createNote(noteDTO, userId);
        System.out.println("Created note - id: " + note.getId() + ", userId: " + note.getUserId() + ", title: " + note.getTitle());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Note created successfully");
        response.put("id", note.getId());
        response.put("title", note.getTitle());
        response.put("content", note.getContent());
        response.put("userId", note.getUserId());
        response.put("createdAt", note.getCreatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN', 'OIDC_USER')")
    public ResponseEntity<Map<String, Object>> getAllNotes(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        System.out.println("Getting notes for userId: " + userId);
        System.out.println("Authentication name: " + authentication.getName());
        List<Note> notes = noteService.getAllNotesByUserId(userId);
        System.out.println("Found " + notes.size() + " notes for userId: " + userId);

        Map<String, Object> response = new HashMap<>();
        response.put("notes", notes);
        response.put("total", notes.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN', 'OIDC_USER')")
    public ResponseEntity<Map<String, Object>> getNoteById(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);

        try {
            Optional<Note> noteOpt = noteService.getNoteById(id, userId);

            if (noteOpt.isPresent()) {
                Note note = noteOpt.get();
                Map<String, Object> response = new HashMap<>();
                response.put("id", note.getId());
                response.put("title", note.getTitle());
                response.put("content", note.getContent());
                response.put("userId", note.getUserId());
                response.put("createdAt", note.getCreatedAt());
                response.put("updatedAt", note.getUpdatedAt());

                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Note not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (AccessDeniedException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN', 'OIDC_USER')")
    public ResponseEntity<Map<String, Object>> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody NoteDTO noteDTO,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);

        try {
            Note updatedNote = noteService.updateNote(id, noteDTO, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Note updated successfully");
            response.put("id", updatedNote.getId());
            response.put("title", updatedNote.getTitle());
            response.put("content", updatedNote.getContent());
            response.put("updatedAt", updatedNote.getUpdatedAt());

            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN', 'OIDC_USER')")
    public ResponseEntity<Map<String, Object>> deleteNote(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);

        try {
            boolean deleted = noteService.deleteNote(id, userId);

            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Note deleted successfully");
                response.put("id", id);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Note not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (AccessDeniedException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated");
        }

        String email = null;
        
        // Handle OAuth2 authentication
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            org.springframework.security.oauth2.core.user.OAuth2User oauth2User = 
                (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
            email = oauth2User.getAttribute("email");
            System.out.println("OAuth2 authentication - email: " + email);
        } else {
            // Handle regular JWT/username authentication
            email = authentication.getName();
            System.out.println("JWT authentication - email: " + email);
        }

        if (email == null || email.isEmpty()) {
            throw new AccessDeniedException("Email not found in authentication");
        }

        Optional<User> userOpt = userService.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new AccessDeniedException("User not found for email: " + email);
        }

        User user = userOpt.get();
        System.out.println("Found user - id: " + user.getId() + ", email: " + user.getEmail());
        return user.getId();
    }
}
