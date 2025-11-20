package com.itineraryledger.kabengosafaris.User.Services.LoginServices;

public class LoginException extends RuntimeException {
    public LoginException(String message) {
        super(message);
    }

    public LoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
