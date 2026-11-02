package com.example.searchengine.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

@Component
public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    private final PasswordValidator passwordValidator;

    public PasswordConstraintValidator(PasswordValidator passwordValidator) {
        this.passwordValidator = passwordValidator;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return passwordValidator.isValid(value);
    }
}
