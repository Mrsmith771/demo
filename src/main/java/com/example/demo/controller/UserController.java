package com.example.demo.controller;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // user
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok("User created: " + user.getUsername());
    }

    // user login
    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@Valid @RequestBody CreateUserRequest request) {
        boolean auth = userService.authenticate(request.getEmail(), request.getPassword());
        if (auth) {
            return ResponseEntity.ok("Login successful!");
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    // profile usera
    @GetMapping("/profile")
    public ResponseEntity<String> getProfile(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String email = auth.getName();
        return ResponseEntity.ok("Logged in as: " + email);
    }

    // list of users
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // user id update
    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable Long id, @RequestBody CreateUserRequest request) {
        boolean updated = userService.updateUser(id, request.getUsername(), request.getEmail(), request.getPassword());
        if (updated) {
            return ResponseEntity.ok("User updated successfully");
        } else {
            return ResponseEntity.status(404).body("User not found");
        }
    }

    // delete user id
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            return ResponseEntity.ok("User deleted successfully");
        } else {
            return ResponseEntity.status(404).body("User not found");
        }
    }
}

