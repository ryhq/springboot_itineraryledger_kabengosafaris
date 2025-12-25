package com.itineraryledger.kabengosafaris.EmailEvent;

/**
 * Defines system-wide variables for each email event.
 * These variables are immutable and cannot be modified via API.
 * Template creators MUST use these exact variable names in their templates.
 *
 * Variable Format:
 * - name: The variable name used in templates as {{variableName}}
 * - description: Human-readable description of what the variable represents
 * - isRequired: Whether the variable must be provided when sending emails
 * - defaultValue: Optional default value if not provided
 */
public class EmailEventVariables {

    /**
     * Get the system-defined variables for a specific email event
     *
     * @param eventName The name of the email event (e.g., "USER_REGISTRATION")
     * @return JSON string containing variable definitions
     */
    public static String getVariablesForEvent(String eventName) {
        return switch (eventName) {
            case "USER_REGISTRATION" -> """
                [
                    {
                        "name":"username",
                        "description":"User's username",
                        "isRequired":true
                    },
                    {
                        "name":"email",
                        "description":"User's email address",
                        "isRequired":false
                    },
                    {
                        "name":"firstName",
                        "description":"User's first name",
                        "isRequired":false
                    },
                    {
                        "name":"lastName",
                        "description":"User's last name",
                        "isRequired":false
                    },
                    {
                        "name":"phoneNumber",
                        "description":"User's phone number",
                        "isRequired":false
                    },
                    {
                        "name":"enabled",
                        "description":"Whether the user account is enabled",
                        "isRequired":false
                    },
                    {
                        "name":"accountLocked",
                        "description":"Whether the user account is locked",
                        "isRequired":false
                    },
                    {
                        "name":"createdAt",
                        "description":"Date and time when the user account was created",
                        "isRequired":false
                    },
                    {
                        "name":"activationToken",
                        "description":"Account activation token (JWT)",
                        "isRequired":true
                    },
                    {
                        "name":"activationLink",
                        "description":"Full account activation URL with token",
                        "isRequired":true
                    },
                    {
                        "name":"expirationHours",
                        "description":"Number of hours until activation link expires",
                        "isRequired":true
                    },
                    {
                        "name":"expirationDateTime",
                        "description":"Exact date and time when activation link expires (yyyy-MM-dd HH:mm:ss format)",
                        "isRequired":true
                    }
                ]
                """;

            default -> "[]";
        };
    }

    /**
     * Get all supported email event names
     *
     * @return Array of email event names that have defined variables
     */
    public static String[] getSupportedEvents() {
        return new String[]{
            "USER_REGISTRATION"
        };
    }
}
