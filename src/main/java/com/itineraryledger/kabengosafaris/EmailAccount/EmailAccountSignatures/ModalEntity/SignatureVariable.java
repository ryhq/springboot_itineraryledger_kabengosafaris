package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SignatureVariable - Represents a variable used in email signatures
 *
 * Variables are placeholders that get replaced with actual values when the signature is used.
 * Example: {senderName} gets replaced with the actual sender's name
 *
 * Structure:
 * {
 *   "name": "senderName",
 *   "defaultValue": "Unknown Sender",
 *   "description": "Name of the person sending the email"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignatureVariable {

    /**
     * Variable name (without curly braces)
     * Used as: {senderName} in signature
     */
    private String name;

    /**
     * Default value if the variable is not provided at runtime
     * Example: "Unknown" for senderName
     */
    private String defaultValue;

    /**
     * Human-readable description of what this variable is
     * Example: "Name of the person sending the email"
     */
    private String description;

    /**
     * Whether this variable is required
     * If required and not provided, it will use the defaultValue
     */
    @Builder.Default
    private Boolean isRequired = false;

    /**
     * Predefined variables that Spring will automatically handle
     */
    public static final String SENDER_NAME = "senderName";
    public static final String USER_ACCOUNT_NAME = "userAccountName";
    public static final String CURRENT_DATE = "currentDate";
    public static final String CURRENT_TIME = "currentTime";

    /**
     * Get predefined variable with default settings
     */
    public static SignatureVariable getSenderNameVariable() {
        return SignatureVariable.builder()
                .name(SENDER_NAME)
                .defaultValue("Unknown Sender")
                .description("Name of the person sending the email")
                .isRequired(false)
                .build();
    }

    public static SignatureVariable getUserAccountNameVariable() {
        return SignatureVariable.builder()
                .name(USER_ACCOUNT_NAME)
                .defaultValue("Unknown Account")
                .description("Email account name")
                .isRequired(false)
                .build();
    }

    public static SignatureVariable getCurrentDateVariable() {
        return SignatureVariable.builder()
                .name(CURRENT_DATE)
                .defaultValue("N/A")
                .description("Current date in yyyy-MM-dd format")
                .isRequired(false)
                .build();
    }

    public static SignatureVariable getCurrentTimeVariable() {
        return SignatureVariable.builder()
                .name(CURRENT_TIME)
                .defaultValue("N/A")
                .description("Current time in HH:mm:ss format")
                .isRequired(false)
                .build();
    }

    @Override
    public String toString() {
        return "{" +
                "name='" + name + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", description='" + description + '\'' +
                ", isRequired=" + isRequired +
                '}';
    }
}
