package com.itineraryledger.kabengosafaris.User.Services.RegistrationServices;

public class RegistrationException extends RuntimeException {
        public RegistrationException(String message) {
            super(message);
        }

        public RegistrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }