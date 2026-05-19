package br.com.lane.SpecRecon.exception;

/**
 * Representa um erro de validacao de campo.
 *
 * @param field nome do campo.
 * @param message mensagem de erro.
 */
public record FieldValidationError(
        String field,
        String message
) {
}

