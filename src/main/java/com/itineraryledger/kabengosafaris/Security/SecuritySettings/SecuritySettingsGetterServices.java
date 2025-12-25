package com.itineraryledger.kabengosafaris.Security.SecuritySettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SecuritySettingsGetterServices {

    // Fallback values from application.properties

    /**
     * ######################################
     * ###  ID Obfuscation Configurations ###
     * ######################################
     */
    @Value("${security.idObfuscator.obfuscated.length:70}")
    private int defaultObfuscatedLength;

    @Value("${security.idObfuscator.salt.length:21}")
    private int defaultSaltLength;

    /**
     * ######################################
     * ### Password Policy Configurations ###
     * ######################################
     */

    @Value("${security.password.min.length:8}")
    private Integer passwordMinLength;

    @Value("${security.password.max.length:128}")
    private Integer passwordMaxLength;

    @Value("${security.password.require.uppercase:true}")
    private Boolean passwordRequireUppercase;

    @Value("${security.password.require.lowercase:true}")
    private Boolean passwordRequireLowercase;

    @Value("${security.password.require.numbers:true}")
    private Boolean passwordRequireNumbers;

    @Value("${security.password.require.special.characters:true}")
    private Boolean passwordRequireSpecialCharacters;

    @Value("${security.password.expiration.days:90}")
    private Integer passwordExpirationDays;

    /**
     * ################################
     * ### JWT Token Configurations ###
     * ################################
     */
    @Value("${security.jwt.expiration.time.minutes:180}")
    private long jwtExpirationMinutes;

    @Value("${security.mfa.jwt.expiration.time.seconds:180}")
    private Long mfaJwtExpirationTimeSeconds;

    @Value("${security.jwt.refresh.expiration.time.minutes:1440}")
    private long jwtRefreshExpirationMinutes;

    @Value("${security.registration.jwt.expiration.time.minutes:60}")
    private long registrationJwtExpirationMinutes;

    /**
     * #######################################
     * ### Login Rate Limit Configurations ###
     * #######################################
     */

    @Value("${security.login-rate-limit.max-capacity:5}")
    private Integer loginRateLimitMaxCapacity;

    @Value("${security.login-rate-limit.refill-rate:5}")
    private Integer loginRateLimitRefillRate;

    @Value("${security.login-rate-limit.refill-duration-minutes:1}")
    private Integer loginRateLimitRefillDurationMinutes;

    @Value("${security.login-rate-limit.enabled:true}")
    private Boolean loginRateLimitEnabled;

    /**
     * #############################################
     * ### Account Lockout Policy Configurations ###
     * #############################################
     */

    @Value("${security.account-lockout.max-failed-attempts:5}")
    private Integer maxFailedAttempts;

    @Value("${security.account-lockout.lockout-duration-minutes:30}")
    private Integer lockoutDurationMinutes;

    @Value("${security.account-lockout.counter-reset-hours:24}")
    private Integer lockoutCounterResetHours;

    @Value("${security.account-lockout.enabled:true}")
    private Boolean accountLockoutEnabled;

    /**
     * Repository to access security settings from database
     */

    @Autowired
    private SecuritySettingsRepository securitySettingsRepository;

    /**
     * ############################################
     * ### ID Obfuscation Configuration Getters ###
     * ############################################
     */

    /**
     * Get ID obfuscation length
     * @return obfuscated ID length
     */
    public int getIdObfuscationLength() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("idObfuscator.obfuscated.length").orElse(null);
        if (securitySetting == null) {
            // Fallsback to default value from application.properties
            return defaultObfuscatedLength;
        } 

        if (securitySetting.getActive() == false) {
            return defaultObfuscatedLength;
        }

        try {
            return Integer.parseInt(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return defaultObfuscatedLength;
        }
    }

    /**
     * Get ID obfuscation salt length
     * @return salt length for ID obfuscation
     */
    public int getIdObfuscationSaltLength() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("idObfuscator.salt.length").orElse(null);
        if (securitySetting == null) {
            // Fallsback to default value from application.properties
            return defaultSaltLength;
        } 

        if (securitySetting.getActive() == false) {
            return defaultSaltLength;
        }

        try {
            return Integer.parseInt(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return defaultSaltLength;
        }
    }

    /**
     * #############################################
     * ### Password Policy Configuration Getters ###
     * #############################################
     */

    /**
     * Get password minimum length
     * @return minimum password length
     */
    public Integer getPasswordMinLength() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("password.minLength").orElse(null);
        if (securitySetting == null) {
            return passwordMinLength;
        }
        if (securitySetting.getActive() == false) {
            return passwordMinLength;
        }
        try {
            return Integer.parseInt(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return passwordMinLength;
        }
    }

    /**
     * Get password maximum length
     * @return maximum password length
     */
    public Integer getPasswordMaxLength() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("password.maxLength").orElse(null);
        if (securitySetting == null) {
            return passwordMaxLength;
        }
        if (securitySetting.getActive() == false) {
            return passwordMaxLength;
        }
        try {
            return Integer.parseInt(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return passwordMaxLength;
        }
    }

    /**
     * Get password require uppercase
     * @return whether uppercase characters are required
     */
    public Boolean getPasswordRequireUppercase() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("password.requireUppercase").orElse(null);
        if (securitySetting == null) {
            return passwordRequireUppercase;
        }
        if (securitySetting.getActive() == false) {
            return passwordRequireUppercase;
        }
        try {
            return Boolean.parseBoolean(securitySetting.getSettingValue());
        } catch (Exception e) {
            return passwordRequireUppercase;
        }
    }

    /**
     * Get password require lowercase
     * @return whether lowercase characters are required
     */
    public Boolean getPasswordRequireLowercase() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("password.requireLowercase").orElse(null);
        if (securitySetting == null) {
            return passwordRequireLowercase;
        }
        if (securitySetting.getActive() == false) {
            return passwordRequireLowercase;
        }
        try {
            return Boolean.parseBoolean(securitySetting.getSettingValue());
        } catch (Exception e) {
            return passwordRequireLowercase;
        }
    }

    /**
     * Get password require numbers
     * @return whether numbers are required
     */
    public Boolean getPasswordRequireNumbers() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("password.requireNumbers").orElse(null);
        if (securitySetting == null) {
            return passwordRequireNumbers;
        }
        if (securitySetting.getActive() == false) {
            return passwordRequireNumbers;
        }
        try {
            return Boolean.parseBoolean(securitySetting.getSettingValue());
        } catch (Exception e) {
            return passwordRequireNumbers;
        }
    }

    /**
     * Get password require special characters
     * @return whether special characters are required
     */
    public Boolean getPasswordRequireSpecialCharacters() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("password.requireSpecialCharacters").orElse(null);
        if (securitySetting == null) {
            return passwordRequireSpecialCharacters;
        }
        if (securitySetting.getActive() == false) {
            return passwordRequireSpecialCharacters;
        }
        try {
            return Boolean.parseBoolean(securitySetting.getSettingValue());
        } catch (Exception e) {
            return passwordRequireSpecialCharacters;
        }
    }

    /**
     * Get password expiration days
     * @return number of days before password expires
     */
    public Integer getPasswordExpirationDays() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("password.expirationDays").orElse(null);
        if (securitySetting == null) {
            return passwordExpirationDays;
        }
        if (securitySetting.getActive() == false) {
            return passwordExpirationDays;
        }
        try {
            return Integer.parseInt(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return passwordExpirationDays;
        }
    }

    /**
     * #######################################
     * ### JWT Token Configuration Getters ###
     * #######################################
     */
    public long getJwtExpirationMinutes() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("jwt.expiration.time.minutes").orElse(null);
        if (securitySetting == null) {
            return jwtExpirationMinutes;
        }
        if (securitySetting.getActive() == false) {
            return jwtExpirationMinutes;
        }
        try {
            return Long.parseLong(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return jwtExpirationMinutes;
        }
    }
    
    public long getMFAJwtExpirationMinutes() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("mfa.jwt.expiration.time.seconds").orElse(null);
        if (securitySetting == null) {
            return mfaJwtExpirationTimeSeconds;
        }
        if (securitySetting.getActive() == false) {
            return mfaJwtExpirationTimeSeconds;
        }
        try {
            return Long.parseLong(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return mfaJwtExpirationTimeSeconds;
        }
    }

    public long getRegistrationJwtExpirationMinutes() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("registration.jwt.expiration.time.minutes").orElse(null);
        if (securitySetting == null) {
            return registrationJwtExpirationMinutes;
        }
        if (securitySetting.getActive() == false) {
            return registrationJwtExpirationMinutes;
        }
        try {
            return Long.parseLong(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return registrationJwtExpirationMinutes;
        }
    }
    
    public long getJwtRefreshExpirationMinutes() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("jwt.refresh.expiration.time.minutes").orElse(null);
        if (securitySetting == null) {
            return jwtRefreshExpirationMinutes;
        }
        if (securitySetting.getActive() == false) {
            return jwtRefreshExpirationMinutes;
        }
        try {
            return Long.parseLong(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return jwtRefreshExpirationMinutes;
        }
    }

    public long getJwtExpirationTimeMillis() {
        return getJwtExpirationMinutes() * 60 * 1000;
    }

    public long getJwtRefreshExpirationTimeMillis() {
        return getJwtRefreshExpirationMinutes() * 60 * 1000;
    }

    /**
     * #############################################
     * ### Login Rate Limit Configuration Getters ###
     * #############################################
     */
    public Integer getLoginRateLimitMaxCapacity() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("loginAttempts.maxCapacity").orElse(null);
        if (securitySetting == null) {
            return loginRateLimitMaxCapacity;
        }
        if (securitySetting.getActive() == false) {
            return loginRateLimitMaxCapacity;
        }
        try {
            return Integer.parseInt(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return loginRateLimitMaxCapacity;
        }
    }

    public Integer getLoginRateLimitRefillRate() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("loginAttempts.refillRate").orElse(null);
        if (securitySetting == null) {
            return loginRateLimitRefillRate;
        }
        if (securitySetting.getActive() == false) {
            return loginRateLimitRefillRate;
        }
        try {
            return Integer.parseInt(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return loginRateLimitRefillRate;
        }
    }

    public Integer getLoginRateLimitRefillDurationMinutes() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("loginAttempts.refillDurationMinutes").orElse(null);
        if (securitySetting == null) {
            return loginRateLimitRefillDurationMinutes;
        }
        if (securitySetting.getActive() == false) {
            return loginRateLimitRefillDurationMinutes;
        }
        try {
            return Integer.parseInt(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return loginRateLimitRefillDurationMinutes;
        }
    }

    public Boolean getLoginRateLimitEnabled() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("loginAttempts.enabled").orElse(null);
        if (securitySetting == null) {
            return loginRateLimitEnabled;
        }
        if (securitySetting.getActive() == false) {
            return loginRateLimitEnabled;
        }
        try {
            return Boolean.parseBoolean(securitySetting.getSettingValue());
        } catch (Exception e) {
            return loginRateLimitEnabled;
        }
    }

    /**
     * #############################################
     * ### Account Lockout Configuration Getters ###
     * #############################################
     */
    public Integer getMaxFailedAttempts() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("accountLockout.maxFailedAttempts").orElse(null);
        if (securitySetting == null) {
            return maxFailedAttempts;
        }
        if (securitySetting.getActive() == false) {
            return maxFailedAttempts;
        }
        try {
            return Integer.parseInt(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return maxFailedAttempts;
        }
    }
    public Integer getLockoutDurationMinutes() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("accountLockout.lockoutDurationMinutes").orElse(null);
        if (securitySetting == null) {
            return lockoutDurationMinutes;
        }
        if (securitySetting.getActive() == false) {
            return lockoutDurationMinutes;
        }
        try {
            return Integer.parseInt(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return lockoutDurationMinutes;
        }
    }

    public Integer getLockoutCounterResetHours() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("accountLockout.counterResetHours").orElse(null);
        if (securitySetting == null) {
            return lockoutCounterResetHours;
        }
        if (securitySetting.getActive() == false) {
            return lockoutCounterResetHours;
        }
        try {
            return Integer.parseInt(securitySetting.getSettingValue());
        } catch (NumberFormatException e) {
            return lockoutCounterResetHours;
        }
    }

    public Boolean getAccountLockoutEnabled() {
        SecuritySetting securitySetting = securitySettingsRepository.findBySettingKey("accountLockout.enabled").orElse(null);
        if (securitySetting == null) {
            return accountLockoutEnabled;
        }
        if (securitySetting.getActive() == false) {
            return accountLockoutEnabled;
        }
        try {
            return Boolean.parseBoolean(securitySetting.getSettingValue());
        } catch (Exception e) {
            return accountLockoutEnabled;
        }
    }
}
