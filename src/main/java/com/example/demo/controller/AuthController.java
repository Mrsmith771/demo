package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.model.User;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        boolean authenticated = userService.authenticate(request.getEmail(), request.getPassword());

        if (!authenticated) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Load user details to generate token with role
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        Optional<User> userOpt = userService.findByEmail(request.getEmail());

        String role = "ROLE_USER";
        if (userOpt.isPresent()) {
            role = userOpt.get().getRole() != null ? userOpt.get().getRole() : "ROLE_USER";
        }

        String token = jwtTokenProvider.generateToken(userDetails, role);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("token", token);
        response.put("email", request.getEmail());
        response.put("role", role);

        return ResponseEntity.ok(response);
    }
}
