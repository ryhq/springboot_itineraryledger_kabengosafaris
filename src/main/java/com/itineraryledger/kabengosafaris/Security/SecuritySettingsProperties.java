package com.itineraryledger.kabengosafaris.Security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties class for Security settings.
 * These properties can be bound from both application.properties and the security_settings database table.
 *
 * Properties are prefixed with 'security' and are automatically injected as @ConfigurationProperties.
 *
 * Example usage:
 * - @Autowired private SecuritySettingsProperties securityProps;
 * - securityProps.getJwt().getExpirationTimeMinutes()
 * - securityProps.getObfuscation().getObfuscatedIdLength()
 */
@Component
@ConfigurationProperties(prefix = "security")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecuritySettingsProperties {

    /**
     * JWT Token Configuration
     */
    private Jwt jwt = new Jwt();

    /**
     * ID Obfuscation Configuration
     */
    private Obfuscation obfuscation = new Obfuscation();

    /**
     * Password Policy Configuration
     */
    private Password password = new Password();

    /**
     * Account Lockout Configuration
     */
    private AccountLockout accountLockout = new AccountLockout();

    /**
     * JWT Token Settings
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Jwt {
        /**
         * JWT token expiration time in minutes
         * Default: 180 (3 hours)
         */
        private long expirationTimeMinutes = 180;

        /**
         * JWT token refresh expiration time in minutes
         * Default: 10080 (7 days)
         */
        private long refreshExpirationTimeMinutes = 10080;

        /**
         * JWT secret key (preferably injected from secure vault)
         */
        private String secretKey = "";

        /**
         * Algorithm used for signing JWT tokens
         * Default: HS256
         */
        private String algorithm = "HS256";

        /**
         * JWT issuer claim
         * Default: kabengosafaris
         */
        private String issuer = "kabengosafaris";
    }

    /**
     * ID Obfuscation Settings
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Obfuscation {
        /**
         * Length of obfuscated IDs
         * Default: 70
         */
        private int obfuscatedIdLength = 70;

        /**
         * Length of the salt used for obfuscation
         * Default: 21
         */
        private int saltLength = 21;

        /**
         * Whether to enable ID obfuscation
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Algorithm used for ID obfuscation
         * Default: hashids
         */
        private String algorithm = "hashids";
    }

    /**
     * Password Policy Settings
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Password {
        /**
         * Minimum password length
         * Default: 8
         */
        private int minLength = 8;

        /**
         * Maximum password length
         * Default: 128
         */
        private int maxLength = 128;

        /**
         * Whether password must contain uppercase letters
         * Default: true
         */
        private boolean requireUppercase = true;

        /**
         * Whether password must contain lowercase letters
         * Default: true
         */
        private boolean requireLowercase = true;

        /**
         * Whether password must contain numbers
         * Default: true
         */
        private boolean requireNumbers = true;

        /**
         * Whether password must contain special characters
         * Default: true
         */
        private boolean requireSpecialCharacters = true;

        /**
         * Password expiration time in days (0 = no expiration)
         * Default: 90
         */
        private int expirationDays = 90;

        /**
         * Number of previous passwords to check for reuse
         * Default: 5
         */
        private int historyCount = 5;
    }

    /**
     * Account Lockout Settings
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountLockout {
        /**
         * Number of failed login attempts before lockout
         * Default: 5
         */
        private int maxFailedAttempts = 5;

        /**
         * Account lockout duration in minutes
         * Default: 30
         */
        private int lockoutDurationMinutes = 30;

        /**
         * Whether account lockout is enabled
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Failed attempt counter reset time in hours (0 = never reset)
         * Default: 24
         */
        private int counterResetHours = 24;
    }
}
