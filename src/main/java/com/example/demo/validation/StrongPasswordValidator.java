package com.example.demo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        // Password must be at least 8 characters
        if (password.length() < 8) {
            return false;
        }

        // Must contain at least one digit
        if (!password.matches(".*\\d.*")) {
            return false;
        }

        // Must contain at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }

        // Must contain at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            return false;
        }

        return true;
    }
}
