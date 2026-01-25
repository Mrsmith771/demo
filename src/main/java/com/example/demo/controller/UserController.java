package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.security.MyUserDetailsService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MyUserDetailsService userDetailsService;

    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider, MyUserDetailsService userDetailsService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(@RequestBody Map<String, String> userData, HttpServletRequest request) {
        String username = userData.get("username");
        String email = userData.get("email");
        String password = userData.get("password");

        Map<String, Object> response = new HashMap<>();

        if (username == null || email == null || password == null) {
            response.put("message", "All fields are required");
            return ResponseEntity.badRequest().body(response);
        }

        // IMPORTANT: Invalidate the old OAuth2 session
        HttpSession session = request.getSession(false);
        if (session != null) {
            System.out.println("Invalidating existing session for registration");
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        try {
            User user = userService.createUser(username, email, password);

            // Generate JWT token for the newly registered user
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            String role = user.getRole() != null ? user.getRole() : "ROLE_USER";
            String token = jwtTokenProvider.generateToken(userDetails, role);

            response.put("message", "User created successfully");
            response.put("token", token);  // ← ВАЖНО: отправляем токен!
            response.put("email", email);
            response.put("username", username);

            System.out.println("Registration successful for: " + email + ", token: " + token.substring(0, 20) + "...");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }
}