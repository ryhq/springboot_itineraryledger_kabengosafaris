package com.itineraryledger.kabengosafaris.Security;

/**
 * Exception thrown when a token is used for an operation
 * that it is not authorized for (wrong token type)
 */
public class InvalidTokenTypeException extends RuntimeException {
    private final TokenType expectedType;
    private final TokenType actualType;

    public InvalidTokenTypeException(String message, TokenType expectedType, TokenType actualType) {
        super(message);
        this.expectedType = expectedType;
        this.actualType = actualType;
    }

    public TokenType getExpectedType() {
        return expectedType;
    }

    public TokenType getActualType() {
        return actualType;
    }
}
