package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.security.MyUserDetailsService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    // Logger instead System.out.println
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MyUserDetailsService userDetailsService;

    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider, MyUserDetailsService userDetailsService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials, HttpServletRequest request) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        Map<String, Object> response = new HashMap<>();

        if (email == null || password == null) {
            response.put("message", "Email and password required");
            return ResponseEntity.badRequest().body(response);
        }

        // Clear OAuth2 session
        HttpSession session = request.getSession(false);
        if (session != null) {
            logger.debug("Invalidating existing session for JWT login");
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        boolean authenticated = userService.authenticate(email, password);

        if (authenticated) {
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                String role = user.getRole() != null ? user.getRole() : "ROLE_USER";
                String token = jwtTokenProvider.generateToken(userDetails, role);

                response.put("message", "Login successful");
                response.put("token", token);
                response.put("email", email);
                response.put("username", user.getUsername());

                // token
                logger.info("Successful login for user: {}", email);

                return ResponseEntity.ok(response);
            }
        }

        // failed login attempts
        logger.warn("Failed login attempt for email: {}", email);

        response.put("message", "Invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}