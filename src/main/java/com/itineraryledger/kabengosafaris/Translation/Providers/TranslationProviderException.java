package com.itineraryledger.kabengosafaris.Translation.Providers;

/**
 * Exception thrown by translation providers when translation operations fail.
 */
public class TranslationProviderException extends Exception {

    private final ErrorType errorType;

    public enum ErrorType {
        SERVICE_DISABLED,
        CONNECTION_ERROR,
        API_ERROR,
        INVALID_LANGUAGE,
        TEXT_TOO_LONG,
        RATE_LIMITED
    }

    public TranslationProviderException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public TranslationProviderException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
