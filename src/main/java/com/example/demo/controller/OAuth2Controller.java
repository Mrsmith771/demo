package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Controller
public class OAuth2Controller {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public OAuth2Controller(UserService userService, JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/oauth2/success")
    public RedirectView oauth2Success(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return new RedirectView("/?error=oauth2_failed");
        }

        try {
            // Get user data from OAuth2
            String email = principal.getAttribute("email");
            String name = principal.getAttribute("name");

            if (email == null || email.isEmpty()) {
                return new RedirectView("/?error=oauth2_failed");
            }

            System.out.println("OAuth2 Login successful:");
            System.out.println("Email: " + email);
            System.out.println("Name: " + name);

            // Check if user exists, if not create one
            Optional<User> userOpt = userService.findByEmail(email);
            User user;
            String username;
            if (userOpt.isEmpty()) {
                // Create user from OAuth2 data
                username = name != null ? name.split(" ")[0] : email.split("@")[0];
                // Generate a random password (user won't need it for OAuth2 login)
                String randomPassword = "oauth2_" + System.currentTimeMillis();
                user = userService.createUser(username, email, randomPassword);
            } else {
                user = userOpt.get();
                username = user.getUsername();
            }

            // Generate JWT token for the user
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            String role = user.getRole() != null ? user.getRole() : "ROLE_USER";
            String token = jwtTokenProvider.generateToken(userDetails, role);

            // Encode parameters for the URL
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
            String encodedName = URLEncoder.encode(name != null ? name : username, StandardCharsets.UTF_8);
            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);

            // Redirect to the main page with parameters including JWT token
            String redirectUrl = String.format("/?oauth2=success&email=%s&name=%s&token=%s",
                    encodedEmail, encodedName, encodedToken);

            return new RedirectView(redirectUrl);

        } catch (Exception e) {
            System.err.println("OAuth2 error: " + e.getMessage());
            e.printStackTrace();
            return new RedirectView("/?error=oauth2_failed");
        }
    }
}
