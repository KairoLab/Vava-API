package com.helper.vavahelper.exception;

/** Token de reset inexistente, expirado ou ja usado. A mensagem e generica de proposito. */
public class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException() {
        super("Invalid or expired token");
    }
}
