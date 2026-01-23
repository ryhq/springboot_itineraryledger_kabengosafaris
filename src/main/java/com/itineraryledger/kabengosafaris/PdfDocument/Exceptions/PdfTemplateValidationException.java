package com.itineraryledger.kabengosafaris.PdfDocument.Exceptions;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when PDF template validation fails.
 * Contains detailed information about validation errors found in the template.
 */
@Getter
public class PdfTemplateValidationException extends RuntimeException {

    private final List<ValidationError> errors;
    private final String templateName;

    public PdfTemplateValidationException(String message, String templateName, List<ValidationError> errors) {
        super(message);
        this.templateName = templateName;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public PdfTemplateValidationException(String message, String templateName) {
        this(message, templateName, new ArrayList<>());
    }

    public PdfTemplateValidationException(String message) {
        this(message, null, new ArrayList<>());
    }

    /**
     * Represents a single validation error with location and suggestion
     */
    @Getter
    public static class ValidationError {
        private final String errorType;
        private final String message;
        private final Integer lineNumber;
        private final Integer columnNumber;
        private final String suggestion;
        private final String context;

        public ValidationError(String errorType, String message, Integer lineNumber,
                               Integer columnNumber, String suggestion, String context) {
            this.errorType = errorType;
            this.message = message;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.suggestion = suggestion;
            this.context = context;
        }

        public static ValidationError of(String errorType, String message, String suggestion) {
            return new ValidationError(errorType, message, null, null, suggestion, null);
        }

        public static ValidationError ofWithLine(String errorType, String message,
                                                  int lineNumber, String suggestion) {
            return new ValidationError(errorType, message, lineNumber, null, suggestion, null);
        }

        public static ValidationError ofWithContext(String errorType, String message,
                                                     String context, String suggestion) {
            return new ValidationError(errorType, message, null, null, suggestion, context);
        }
    }
}
