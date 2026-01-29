package com.example.demo.controller;

import com.example.demo.dto.CreateUserRequest;
import com.example.demo.model.User;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.security.MyUserDetailsService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MyUserDetailsService userDetailsService;

    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider, MyUserDetailsService userDetailsService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    // use CreateUserRequest DTO with validation
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest) {

        Map<String, Object> response = new HashMap<>();

        // Invalidate the old OAuth2 session
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            logger.debug("Invalidating existing session for registration");
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        try {
            User user = userService.createUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword()
            );

            // Generate JWT token for the newly registered user
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            String role = user.getRole() != null ? user.getRole() : "ROLE_USER";
            String token = jwtTokenProvider.generateToken(userDetails, role);

            response.put("message", "User created successfully");
            response.put("token", token);
            response.put("email", request.getEmail());
            response.put("username", request.getUsername());

            // token
            logger.info("Successful registration for user: {}", request.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed for email {}: {}", request.getEmail(), e.getMessage());
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }
}