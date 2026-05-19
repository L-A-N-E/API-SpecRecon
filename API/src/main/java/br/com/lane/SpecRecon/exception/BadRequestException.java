package br.com.lane.SpecRecon.exception;

/**
 * Excecao para requisicoes invalidas.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}

