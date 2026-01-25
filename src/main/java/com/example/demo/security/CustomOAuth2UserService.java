package com.example.demo.security;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    public CustomOAuth2UserService(@Lazy UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found in OAuth2 user attributes");
        }

        // Check if user exists, if not create one
        Optional<User> userOpt = userService.findByEmail(email);
        User user;
        if (userOpt.isEmpty()) {
            // Create user from OAuth2 data
            String username = name != null ? name.split(" ")[0] : email.split("@")[0];
            // Generate a random password (user won't need it for OAuth2 login)
            String randomPassword = "oauth2_" + System.currentTimeMillis();
            user = userService.createUser(username, email, randomPassword);
        } else {
            user = userOpt.get();
        }

        // Get user role
        String role = user.getRole() != null ? user.getRole() : "ROLE_USER";

        // Create authorities with ROLE_USER for OAuth2 users
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        // Add ROLE_USER first to ensure it's included
        authorities.add(new SimpleGrantedAuthority(role));
        // Add existing OAuth2 authorities
        authorities.addAll(oauth2User.getAuthorities());

        System.out.println("CustomOAuth2UserService - Adding role: " + role);
        System.out.println("CustomOAuth2UserService - All authorities: " + authorities);

        // Return OAuth2User with additional ROLE_USER authority
        // Use email as the name attribute key so authentication.getName() returns email
        DefaultOAuth2User oauth2UserWithRole = new DefaultOAuth2User(
                authorities,
                oauth2User.getAttributes(),
                "email" // Use email as the name attribute key
        );
        
        System.out.println("CustomOAuth2UserService - Created OAuth2User with authorities: " + oauth2UserWithRole.getAuthorities());
        
        return oauth2UserWithRole;
    }
}
