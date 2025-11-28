package com.itineraryledger.kabengosafaris.Security;

/**
 * Enum to define different types of JWT tokens
 * Each token type has specific usage restrictions
 */
public enum TokenType {
    ACCESS("access"),      // Can access any endpoint
    REFRESH("refresh"),    // Only for obtaining new access tokens
    MFA("mfa");            // Only for MFA verification

    private final String type;

    TokenType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static TokenType fromString(String type) {
        for (TokenType tokenType : TokenType.values()) {
            if (tokenType.type.equals(type)) {
                return tokenType;
            }
        }
        throw new IllegalArgumentException("Invalid token type: " + type);
    }
}
