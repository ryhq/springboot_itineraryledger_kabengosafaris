package com.itineraryledger.kabengosafaris.User.Services.PasswordResetServices;

/**
 * Exception thrown when password reset operations fail.
 */
public class PasswordResetException extends RuntimeException {

    public PasswordResetException(String message) {
        super(message);
    }

    public PasswordResetException(String message, Throwable cause) {
        super(message, cause);
    }
}
