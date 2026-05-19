package br.com.lane.SpecRecon.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementação do validador de strings seguras contra XSS e injeção.
 */
public class SafeStringValidator implements ConstraintValidator<SafeString, String> {

    private int maxLength;

    @Override
    public void initialize(SafeString constraintAnnotation) {
        this.maxLength = constraintAnnotation.maxLength();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Deixar para @NotNull/NotBlank validar
        }

        // Verificar tamanho
        if (value.length() > maxLength) {
            return false;
        }

        // Caracteres perigosos que indicam XSS/injeção
        String dangerousPatterns = "(<|>|['\"]|--|;|\\||&|`|\\$|\\(|\\)|\\{|\\}|\\[|\\]|<script|javascript:|onerror|onload|onclick)";
        
        if (value.matches(".*" + dangerousPatterns + ".*")) {
            return false;
        }

        // Rejeitar sequências de bytes nulos ou caracteres de controle
        if (value.contains("\0") || value.contains("\n") || value.contains("\r")) {
            return false;
        }

        return true;
    }
}
