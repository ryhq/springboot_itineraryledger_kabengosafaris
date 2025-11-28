package com.itineraryledger.kabengosafaris.Security.SecuritySettings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;

/**
 * Initializer for Security Settings.
 * Runs at application startup and initializes default security settings in the database.
 *
 * This ensures that the database has the required security settings even if they're
 * not explicitly created by the user.
 *
 * Properties can be overridden via application.properties but this initializer loads them into the database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecuritySettingsInitializer implements ApplicationRunner, Ordered {

    private final SecuritySettingsRepository securitySettingsRepository;

    /**
     * Injected values from application.properties
     * These serve as fallback/default values if not present in the database
     */
    @Value("${security.jwt.expiration.time.minutes:180}")
    private Long jwtExpirationTimeMinutes;

    @Value("${security.mfa.jwt.expiration.time.seconds:180}")
    private Long mfaJwtExpirationTimeSeconds;

    @Value("${security.jwt.refresh.expiration.time.minutes:10080}")
    private Long jwtRefreshExpirationTimeMinutes;

    @Value("${security.idObfuscator.obfuscated.length:70}")
    private Integer obfuscatedIdLength;

    @Value("${security.idObfuscator.salt.length:21}")
    private Integer saltLength;

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

    @Value("${security.account-lockout.max-failed-attempts:5}")
    private Integer maxFailedAttempts;

    @Value("${security.account-lockout.lockout-duration-minutes:30}")
    private Integer lockoutDurationMinutes;

    @Value("${security.account-lockout.counter-reset-hours:24}")
    private Integer lockoutCounterResetHours;

    @Value("${security.account-lockout.enabled:true}")
    private Boolean accountLockoutEnabled;

    @Value("${security.login-rate-limit.max-capacity:5}")
    private Integer loginRateLimitMaxCapacity;

    @Value("${security.login-rate-limit.refill-rate:5}")
    private Integer loginRateLimitRefillRate;

    @Value("${security.login-rate-limit.refill-duration-minutes:1}")
    private Integer loginRateLimitRefillDurationMinutes;

    @Value("${security.login-rate-limit.enabled:true}")
    private Boolean loginRateLimitEnabled;

    /**
     * Run initialization at application startup with highest priority
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        System.out.println("\n\n\n");
        log.info("=============================================");
        log.info("Starting Security Settings initialization...");
        log.info("=============================================");
        System.out.println("\n");

        try {
            log.info("Initializing Security Settings from database...");
            initializeSecuritySettings();
            log.info("Security Settings initialization completed successfully");
            System.out.println("\n");
            log.info("=============================================");
            log.info("✓ Security Settings initialization Complete");
            log.info("=============================================");
            System.out.println("\n");
        } catch (Exception e) {
            log.error("Error initializing Security Settings", e);
        }
    }

    /**
     * Set order to HIGHEST_PRECEDENCE so this initializer runs first
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * Initialize or update security settings in the database
     */
    private void initializeSecuritySettings() {
        // JWT Settings
        createOrUpdateSetting(
                "jwt.expiration.time.minutes",
                String.valueOf(jwtExpirationTimeMinutes),
                SettingDataType.LONG,
                "JWT token expiration time in minutes",
                SecuritySetting.Category.JWT,
                false
        );

        createOrUpdateSetting(
                "mfa.jwt.expiration.time.seconds",
                String.valueOf(mfaJwtExpirationTimeSeconds),
                SettingDataType.LONG,
                "MFA JWT token expiration time in seconds",
                SecuritySetting.Category.JWT,
                false
        );

        createOrUpdateSetting(
                "jwt.refresh.expiration.time.minutes",
                String.valueOf(jwtRefreshExpirationTimeMinutes),
                SettingDataType.LONG,
                "JWT refresh token expiration time in minutes",
                SecuritySetting.Category.JWT,
                false
        );

        // ID Obfuscation Settings
        createOrUpdateSetting(
                "idObfuscator.obfuscated.length",
                String.valueOf(obfuscatedIdLength),
                SettingDataType.INTEGER,
                "Length of obfuscated IDs",
                SecuritySetting.Category.OBFUSCATION,
                false
        );

        createOrUpdateSetting(
                "idObfuscator.salt.length",
                String.valueOf(saltLength),
                SettingDataType.INTEGER,
                "Length of the salt used for ID obfuscation",
                SecuritySetting.Category.OBFUSCATION,
                false
        );

        // Password Policy Settings
        createOrUpdateSetting(
                "password.minLength",
                String.valueOf(passwordMinLength),
                SettingDataType.INTEGER,
                "Minimum password length",
                SecuritySetting.Category.PASSWORD,
                false
        );

        createOrUpdateSetting(
                "password.maxLength",
                String.valueOf(passwordMaxLength),
                SettingDataType.INTEGER,
                "Maximum password length",
                SecuritySetting.Category.PASSWORD,
                false
        );

        createOrUpdateSetting(
                "password.requireUppercase",
                String.valueOf(passwordRequireUppercase),
                SettingDataType.BOOLEAN,
                "Whether password must contain uppercase letters",
                SecuritySetting.Category.PASSWORD,
                false
        );

        createOrUpdateSetting(
                "password.requireLowercase",
                String.valueOf(passwordRequireLowercase),
                SettingDataType.BOOLEAN,
                "Whether password must contain lowercase letters",
                SecuritySetting.Category.PASSWORD,
                false
        );

        createOrUpdateSetting(
                "password.requireNumbers",
                String.valueOf(passwordRequireNumbers),
                SettingDataType.BOOLEAN,
                "Whether password must contain numbers",
                SecuritySetting.Category.PASSWORD,
                false
        );

        createOrUpdateSetting(
                "password.requireSpecialCharacters",
                String.valueOf(passwordRequireSpecialCharacters),
                SettingDataType.BOOLEAN,
                "Whether password must contain special characters",
                SecuritySetting.Category.PASSWORD,
                false
        );

        createOrUpdateSetting(
                "password.expirationDays",
                String.valueOf(passwordExpirationDays),
                SettingDataType.INTEGER,
                "Password expiration time in days (0 = no expiration)",
                SecuritySetting.Category.PASSWORD,
                false
        );
       
        // Account Lockout Settings
        createOrUpdateSetting(
                "accountLockout.maxFailedAttempts",
                String.valueOf(maxFailedAttempts),
                SettingDataType.INTEGER,
                "Number of failed login attempts before lockout",
                SecuritySetting.Category.ACCOUNT_LOCKOUT,
                false
        );

        /* accountLockout.lockoutDurationMinutes
         * What it does: How long an account stays locked after exceeding failed attempts
         * Use case: Account lockout for failed credentials
         * When it applies: After a user makes specified quantity of failed login attempts, their account is locked for specified minutes
         * Example: User fails 5 times → account locked until 30 minutes pass → automatically unlocked
         * So on each failed login attempt we increment failedAttempt count
         * Once failedAttempt >= accountLockout.maxFailedAttempts, we set accountLocked = true and accountLocked
         */
        createOrUpdateSetting(
                "accountLockout.lockoutDurationMinutes",
                String.valueOf(lockoutDurationMinutes),
                SettingDataType.INTEGER,
                "Account lockout duration in minutes",
                SecuritySetting.Category.ACCOUNT_LOCKOUT,
                false
        );

        createOrUpdateSetting(
                "accountLockout.enabled",
                String.valueOf(accountLockoutEnabled),
                SettingDataType.BOOLEAN,
                "Whether account lockout is enabled",
                SecuritySetting.Category.ACCOUNT_LOCKOUT,
                false
        );

        /* accountLockout.counterResetHours
         * What it does: How long before the failed attempt counter resets to zero
         * Use case: Resets attempt counter after a period of time
         * When it applies: If a user hasn't attempted login in 24 hours, their failed attempt counter resets
         * Example: User fails 3 times → waits 24 hours → counter resets to 0 → they get 5 fresh attempts again
         */
        createOrUpdateSetting(
                "accountLockout.counterResetHours",
                String.valueOf(lockoutCounterResetHours),
                SettingDataType.INTEGER,
                "Failed attempt counter reset time in hours (0 = never reset)",
                SecuritySetting.Category.ACCOUNT_LOCKOUT,
                false
        );

        // Login Rate Limiting Settings
        createOrUpdateSetting(
                "loginAttempts.maxCapacity",
                String.valueOf(loginRateLimitMaxCapacity),
                SettingDataType.INTEGER,
                "Maximum login attempts (token bucket capacity)",
                SecuritySetting.Category.RATE_LIMIT,
                false
        );

        createOrUpdateSetting(
                "loginAttempts.refillRate",
                String.valueOf(loginRateLimitRefillRate),
                SettingDataType.INTEGER,
                "Number of tokens to refill for login attempts",
                SecuritySetting.Category.RATE_LIMIT,
                false
        );

        createOrUpdateSetting(
                "loginAttempts.refillDurationMinutes",
                String.valueOf(loginRateLimitRefillDurationMinutes),
                SettingDataType.INTEGER,
                "Duration in minutes for login attempt token refill",
                SecuritySetting.Category.RATE_LIMIT,
                false
        );

        // Enable Login Rate Limiting Setting
        createOrUpdateSetting(
                "loginAttempts.enabled",
                String.valueOf(loginRateLimitEnabled),
                SettingDataType.BOOLEAN,
                "Whether login rate limiting is enabled",
                SecuritySetting.Category.RATE_LIMIT,
                false
        );

        log.info("All security settings have been initialized");
    }

    /**
     * Create or update a security setting
     * If the setting already exists (by key), it will not be overwritten
     * This preserves any user modifications to settings
     *
     * @param settingKey the setting key
     * @param settingValue the setting value
     * @param dataType the data type
     * @param description the description
     * @param category the category
     * @param requiresRestart whether changing this setting requires restart
     */
    private void createOrUpdateSetting(String settingKey, String settingValue,
                                        SettingDataType dataType,
                                        String description, SecuritySetting.Category category,
                                        Boolean requiresRestart) {
        try {
            if (securitySettingsRepository.existsBySettingKey(settingKey)) {
                log.debug("Setting already exists, skipping: {}", settingKey);
                return;
            }

            SecuritySetting setting = SecuritySetting.builder()
                    .settingKey(settingKey)
                    .settingValue(settingValue)
                    .dataType(dataType)
                    .description(description)
                    .category(category)
                    .active(true)
                    .isSystemDefault(true)
                    .requiresRestart(requiresRestart)
                    .build();
                    
            securitySettingsRepository.save(setting);
            log.info("Security setting initialized: {} = {} (category: {})", settingKey, settingValue, category);

        } catch (Exception e) {
            log.warn("Failed to initialize security setting {}: {}", settingKey, e.getMessage());
        }
    }
}
