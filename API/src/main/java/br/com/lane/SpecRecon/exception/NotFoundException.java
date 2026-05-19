package br.com.lane.SpecRecon.exception;

/**
 * Excecao para recursos nao encontrados.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}

