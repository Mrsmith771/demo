package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("=== JwtAuthFilter ===");
        System.out.println("Request URI: " + request.getRequestURI());
        System.out.println("Request Method: " + request.getMethod());

        // Skip JWT filter for OAuth2 endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/")) {
            System.out.println("Skipping JWT filter - OAuth2 endpoint");
            filterChain.doFilter(request, response);
            return;
        }

        // Check if there's already an authentication (e.g., from OAuth2 session)
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            String authName = SecurityContextHolder.getContext().getAuthentication().getName();
            System.out.println("Existing authentication found: " + authName);
            System.out.println("Authentication class: " + SecurityContextHolder.getContext().getAuthentication().getClass().getName());

            if (!authName.equals("anonymousUser")) {
                System.out.println("Using existing OAuth2/session authentication, skipping JWT");
                filterChain.doFilter(request, response);
                return;
            }
        }

        String authHeader = request.getHeader("Authorization");
        System.out.println("Authorization header: " + authHeader);

        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            System.out.println("JWT token found: " + token.substring(0, Math.min(20, token.length())) + "...");

            try {
                username = jwtTokenProvider.extractUsername(token);
                System.out.println("Extracted username from JWT: " + username);
            } catch (Exception e) {
                System.out.println("JWT token validation failed: " + e.getMessage());
                logger.debug("JWT token validation failed: " + e.getMessage());
            }
        } else {
            System.out.println("No JWT token in Authorization header");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                System.out.println("Loaded UserDetails for: " + userDetails.getUsername());

                if (jwtTokenProvider.validateToken(token, userDetails)) {
                    System.out.println("JWT token is valid");

                    String role = jwtTokenProvider.getRoleFromToken(token);
                    if (role == null) {
                        role = "ROLE_USER";
                    }
                    System.out.println("User role: " + role);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(role))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    System.out.println("JWT authentication set successfully for: " + username);
                } else {
                    System.out.println("JWT token validation failed");
                }
            } catch (Exception e) {
                System.out.println("Authentication failed: " + e.getMessage());
                logger.debug("Authentication failed: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (username == null) {
                System.out.println("No username extracted from JWT");
            }
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                System.out.println("Authentication already exists, not setting JWT auth");
            }
        }

        System.out.println("=== END JwtAuthFilter ===");
        filterChain.doFilter(request, response);
    }
}
