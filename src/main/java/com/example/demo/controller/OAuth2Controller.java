package com.example.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class OAuth2Controller {

    @GetMapping("/oauth2/success")
    public RedirectView oauth2Success(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            // If the user is not authorized, redirect to the main page with an error
            return new RedirectView("/?error=oauth2_failed");
        }

        try {
            // Get user data from OAuth2
            String email = principal.getAttribute("email");
            String name = principal.getAttribute("name");

            System.out.println("OAuth2 Login successful:");
            System.out.println("Email: " + email);
            System.out.println("Name: " + name);

            // Encode parameters for the URL
            String encodedEmail = URLEncoder.encode(email != null ? email : "", StandardCharsets.UTF_8.toString());
            String encodedName = URLEncoder.encode(name != null ? name : "", StandardCharsets.UTF_8.toString());

            // Redirect to the main page with parameters
            String redirectUrl = String.format("/?oauth2=success&email=%s&name=%s",
                    encodedEmail, encodedName);

            return new RedirectView(redirectUrl);

        } catch (UnsupportedEncodingException e) {
            System.err.println("Error encoding URL parameters: " + e.getMessage());
            return new RedirectView("/?error=oauth2_failed");
        } catch (Exception e) {
            System.err.println("OAuth2 error: " + e.getMessage());
            e.printStackTrace();
            return new RedirectView("/?error=oauth2_failed");
        }
    }
}
