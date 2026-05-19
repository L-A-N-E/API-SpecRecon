package br.com.lane.SpecRecon.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementação do validador de senhas fortes.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Deixar para @NotNull/NotBlank validar
        }

        // Requisitos de senha forte
        boolean hasMinLength = value.length() >= 8;
        boolean hasUpperCase = value.matches(".*[A-Z].*");
        boolean hasLowerCase = value.matches(".*[a-z].*");
        boolean hasDigit = value.matches(".*[0-9].*");
        boolean hasSpecialChar = value.matches(".*[@#$%^&+=!._-].*");

        return hasMinLength && hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar;
    }
}
