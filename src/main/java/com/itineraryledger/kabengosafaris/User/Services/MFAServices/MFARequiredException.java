package com.itineraryledger.kabengosafaris.User.Services.MFAServices;

public class MFARequiredException extends RuntimeException {
    private String tempToken;

    public MFARequiredException(String message, String tempToken) {
        super(message);
        this.tempToken = tempToken;
    }

    public String getTempToken() {
        return tempToken;
    }

    public void setTempToken(String tempToken) {
        this.tempToken = tempToken;
    }
}
