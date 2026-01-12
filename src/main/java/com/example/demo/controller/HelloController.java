package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public ResponseEntity<Map<String, Object>> hello(
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello, user!");
        response.put("userAgent", userAgent != null ? userAgent : "Unknown");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}