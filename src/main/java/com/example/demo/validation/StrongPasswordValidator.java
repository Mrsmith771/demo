package com.example.demo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final List<String> COMMON_PASSWORDS = Arrays.asList(
            "password", "12345678", "123456789", "1234567890", "qwerty123",
            "password123", "admin123", "letmein", "welcome", "monkey",
            "1234567", "sunshine", "princess", "dragon", "passw0rd",
            "master", "hello", "freedom", "whatever", "qazwsx"
    );

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Password cannot be null")
                    .addConstraintViolation();
            return false;
        }

        // Password must be at least 8 characters
        if (password.length() < 8) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Password must be at least 8 characters long")
                    .addConstraintViolation();
            return false;
        }

        // Check for common passwords (case-insensitive)
        String lowerPassword = password.toLowerCase();
        if (COMMON_PASSWORDS.contains(lowerPassword)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Password is too common. Please choose a more secure password")
                    .addConstraintViolation();
            return false;
        }

        // Must contain at least one digit
        if (!password.matches(".*\\d.*")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Password must contain at least one digit")
                    .addConstraintViolation();
            return false;
        }

        // Must contain at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Password must contain at least one uppercase letter")
                    .addConstraintViolation();
            return false;
        }

        // Must contain at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Password must contain at least one lowercase letter")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
