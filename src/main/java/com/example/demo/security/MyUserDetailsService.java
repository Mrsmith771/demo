package com.example.demo.security;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class MyUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public MyUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> userOpt = userService.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        User user = userOpt.get();
        String role = user.getRole() != null ? user.getRole() : "ROLE_USER";

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(role)))
                .build();
    }
}

