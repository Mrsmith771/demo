package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/users/register", "/users/login").permitAll() // registration and user
                        .requestMatchers(HttpMethod.GET, "/users/**").authenticated()    // view users
                        .requestMatchers(HttpMethod.PUT, "/users/**").authenticated()    // update users
                        .requestMatchers(HttpMethod.DELETE, "/users/**").authenticated() // delete user
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic();

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance(); // just for test
    }
}

