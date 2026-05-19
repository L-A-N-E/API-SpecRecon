package br.com.lane.SpecRecon.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resposta padrao de erro da API.
 *
 * @param timestamp data/hora do erro.
 * @param status codigo HTTP.
 * @param error descricao curta do erro.
 * @param message mensagem detalhada.
 * @param path caminho da requisicao.
 * @param fields erros de validacao (quando aplicavel).
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldValidationError> fields
) {
}

