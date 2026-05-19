package br.com.lane.SpecRecon.exception;

/**
 * Excecao para conflitos de negocio.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

