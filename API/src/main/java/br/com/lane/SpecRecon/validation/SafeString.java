package br.com.lane.SpecRecon.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validador customizado para sanitização de strings contra XSS e injeção.
 * Rejeita strings com caracteres perigosos ou padrões suspeitos.
 */
@Documented
@Constraint(validatedBy = SafeStringValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeString {
    String message() default "Campo contém caracteres ou padrões não permitidos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int maxLength() default 255;
}
