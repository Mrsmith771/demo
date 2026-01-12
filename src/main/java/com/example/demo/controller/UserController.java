package com.example.demo.controller;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // User registration with JSON response
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {

        // Log user agent for security tracking
        System.out.println("Registration attempt from: " + userAgent);

        User user = userService.createUser(request.getUsername(), request.getEmail(), request.getPassword());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User created successfully");
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // User login with JSON response
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginUser(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress) {

        // Log login attempt with user agent and IP
        System.out.println("Login attempt from: " + userAgent + " | IP: " + ipAddress);

        boolean auth = userService.authenticate(request.getEmail(), request.getPassword());

        if (auth) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("email", request.getEmail());
            response.put("token", "generated-token-" + System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Invalid credentials");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // Get profile with JSON response
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(
            Authentication auth,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {

        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String email = auth.getName();
        Optional<User> userOpt = userService.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("email", user.getEmail());
            response.put("username", user.getUsername());
            response.put("id", user.getId());

            return ResponseEntity.ok(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // Get all users with JSON response
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<User> users = userService.getAllUsers();

        Map<String, Object> response = new HashMap<>();
        response.put("users", users);
        response.put("total", users.size());
        response.put("page", page);
        response.put("size", size);

        return ResponseEntity.ok(response);
    }

    // Update user with JSON response
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody CreateUserRequest request) {

        boolean updated = userService.updateUser(id, request.getUsername(), request.getEmail(), request.getPassword());

        if (updated) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User updated successfully");
            response.put("userId", id);

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User not found");

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // Partial update with JSON response
    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> patchUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        Optional<User> userOpt = userService.patchUser(id, updates);

        if (userOpt.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User updated successfully");
            response.put("userId", id);

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User not found");

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // Delete user with JSON response
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);

        if (deleted) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            response.put("userId", id);

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User not found");

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // OPTIONS method for CORS preflight
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok()
                .header("Allow", "GET, POST, PUT, PATCH, DELETE, OPTIONS")
                .build();
    }
}