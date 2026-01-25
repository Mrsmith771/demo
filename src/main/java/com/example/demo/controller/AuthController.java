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
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

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

        // ВАЖНО: Инвалидируем старую OAuth2 сессию
        HttpSession session = request.getSession(false);
        if (session != null) {
            System.out.println("Invalidating existing session for JWT login");
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

                System.out.println("JWT login successful for: " + email + ", token: " + token.substring(0, 20) + "...");
                return ResponseEntity.ok(response);
            }
        }

        response.put("message", "Invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}