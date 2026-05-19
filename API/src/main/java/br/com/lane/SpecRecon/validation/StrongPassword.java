package br.com.lane.SpecRecon.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validador customizado para senhas fortes.
 * Requisitos:
 * - Mínimo 8 caracteres
 * - Pelo menos 1 letra maiúscula
 * - Pelo menos 1 letra minúscula
 * - Pelo menos 1 dígito
 * - Pelo menos 1 caractere especial (@, #, $, %, etc)
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "Senha deve ter mínimo 8 caracteres, 1 maiúscula, 1 minúscula, 1 dígito e 1 caractere especial";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
