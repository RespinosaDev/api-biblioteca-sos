package com.biblioteca.exception;

/**
 * La petición es válida pero choca con el estado actual
 * (duplicados, libro ya prestado, usuario sancionado...) → 409 Conflict.
 */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
