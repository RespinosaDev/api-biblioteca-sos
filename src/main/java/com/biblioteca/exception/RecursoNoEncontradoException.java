package com.biblioteca.exception;

/** El recurso pedido (usuario, libro o préstamo) no existe → 404 Not Found. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
