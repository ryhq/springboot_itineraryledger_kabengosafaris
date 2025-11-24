package com.itineraryledger.kabengosafaris.Security.SecuritySettings;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLog;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Security.JwtTokenProvider;

@Service
public class SecuritySettingsServices {

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

    @Value("${security.jwt.refresh.expiration.time.minutes:1440}")
    private long jwtRefreshExpirationMinutes;

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

    @Autowired
    @Lazy
    private IdObfuscator idObfuscator;

    @Autowired
    @Lazy
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SecuritySettingsGetterServices securitySettingsGetterServices;

    @Autowired
    private SecuritySettingsRepository securitySettingsRepository;

    @Autowired
    private AuditLogService auditLogService;

    private SecuritySettingDTO getSecuritySettingDTO(SecuritySetting securitySetting) {
        return new SecuritySettingDTO(
            idObfuscator.encodeId(securitySetting.getId()),
            securitySetting.getCategory().getDisplayName(),
            securitySetting.getSettingKey(),
            securitySetting.getSettingValue(),
            securitySetting.getDataType(),
            securitySetting.getDescription(),
            securitySetting.getActive(),
            securitySetting.getIsSystemDefault(),
            securitySetting.getCategory(),
            securitySetting.getRequiresRestart(),
            securitySetting.getCreatedAt(),
            securitySetting.getUpdatedAt()
        );
    }

    private List<SecuritySettingDTO> getSecuritySettingDTOs(List<SecuritySetting> securitySettings) {
        return securitySettings.stream().map(this::getSecuritySettingDTO).toList();
    }

    public ResponseEntity<?> getAllSecuritySettings() {
        List<SecuritySetting> securitySettings = securitySettingsRepository.findAll();
        List<SecuritySettingDTO> securitySettingDTOs = getSecuritySettingDTOs(securitySettings);
        return ResponseEntity.ok(ApiResponse.success(200, "Security Settings retrieved successfully.", securitySettingDTOs));
    }
    
    public ResponseEntity<?> updateSecuritySetting(String obfuscatedId, UpdateSecuritySettingDTO updateSecuritySettingDTO) {
        Long id;

        try {
            id = idObfuscator.decodeId(obfuscatedId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    "Invalid Security Setting ID provided.",
                    "VALIDATION_ERROR"
                )
            );
        }

        return updateSecuritySettingById(id, updateSecuritySettingDTO);
    }

    @AuditLogAnnotation(action = "Update Security Setting", description = "Updates a security setting by ID", entityIdParamName = "id", entityType = "SecuritySetting")
    private ResponseEntity<?> updateSecuritySettingById(Long id, UpdateSecuritySettingDTO updateSecuritySettingDTO) {
        Boolean active = updateSecuritySettingDTO.getActive();
        String description = updateSecuritySettingDTO.getDescription();
        String settingValue = updateSecuritySettingDTO.getSettingValue();
        
        // Validation all
        if (active == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400, 
                    "Active status must be provided.", 
                    "VALIDATION_ERROR"
                )
            );
        }
        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400, 
                    "Description must be provided.", 
                    "VALIDATION_ERROR"
                )
            );
        }
        if (settingValue == null || settingValue.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400, 
                    "Setting Value must be provided.", 
                    "VALIDATION_ERROR"
                )
            );
        }

        SecuritySetting securitySetting = securitySettingsRepository.findById(id).orElse(null);
        if (securitySetting == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(
                    404, 
                    "Security Setting not found.", 
                    "NOT_FOUND"
                )
            );
        }

        // Store old values before changes
        Boolean oldActive = securitySetting.getActive();
        String oldDescription = securitySetting.getDescription();
        String oldSettingValue = securitySetting.getSettingValue();

        // Detect changes
        boolean activeChanged = !active.equals(oldActive);
        boolean descriptionChanged = !description.equals(oldDescription);
        boolean settingValueChanged = !settingValue.equals(oldSettingValue);

        // Check if any changes are being made
        if (!activeChanged && !descriptionChanged && !settingValueChanged) {
            return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(
                    200,
                    "No changes detected. Security Setting remains unchanged.",
                    null
                )
            );
        }

        securitySetting.setActive(active);
        securitySetting.setDescription(description);
        securitySetting.setSettingValue(settingValue);

        securitySetting = securitySettingsRepository.save(securitySetting);

        // Log each individual field change with old and new values
        logFieldChanges(
            securitySetting.getId(), 
            activeChanged, 
            descriptionChanged, 
            settingValueChanged,
            oldActive, 
            active, 
            oldDescription, 
            description, 
            oldSettingValue, 
            settingValue
        );

        // Build message with details about which fields changed
        String changeDetails = buildChangeDetails(activeChanged, descriptionChanged, settingValueChanged);

        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "Security Setting updated successfully. " + changeDetails,
                getSecuritySettingDTO(securitySetting)
            )
        );
    }

    private void logFieldChanges(
        Long entityId,
        boolean activeChanged,
        boolean descriptionChanged,
        boolean settingValueChanged,
        Boolean oldActive,
        Boolean newActive,
        String oldDescription,
        String newDescription,
        String oldSettingValue,
        String newSettingValue
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        String username = "SYSTEM";

        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            username = authentication.getName();
            Object principal = authentication.getPrincipal();
            if (principal instanceof com.itineraryledger.kabengosafaris.User.User) {
                userId = ((com.itineraryledger.kabengosafaris.User.User) principal).getId();
            }
        }

        if (activeChanged) {
            AuditLog log = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action("Update Security Setting Field")
                    .entityType("SecuritySetting")
                    .entityId(entityId)
                    .description("Changed active field from " + oldActive + " to " + newActive)
                    .status("SUCCESS")
                    .oldValues("{\"active\": " + oldActive + "}")
                    .newValues("{\"active\": " + newActive + "}")
                    .build();
            auditLogService.logActionSync(log);
        }

        if (descriptionChanged) {
            AuditLog log = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action("Update Security Setting Field")
                    .entityType("SecuritySetting")
                    .entityId(entityId)
                    .description("Changed description field from \"" + oldDescription + "\" to \"" + newDescription + "\"")
                    .status("SUCCESS")
                    .oldValues("{\"description\": \"" + escapeJson(oldDescription) + "\"}")
                    .newValues("{\"description\": \"" + escapeJson(newDescription) + "\"}")
                    .build();
            auditLogService.logActionSync(log);
        }

        if (settingValueChanged) {
            AuditLog log = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action("Update Security Setting Field")
                    .entityType("SecuritySetting")
                    .entityId(entityId)
                    .description("Changed settingValue field from \"" + oldSettingValue + "\" to \"" + newSettingValue + "\"")
                    .status("SUCCESS")
                    .oldValues("{\"settingValue\": \"" + escapeJson(oldSettingValue) + "\"}")
                    .newValues("{\"settingValue\": \"" + escapeJson(newSettingValue) + "\"}")
                    .build();
            auditLogService.logActionSync(log);
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "null";
        }
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private String buildChangeDetails(boolean activeChanged, boolean descriptionChanged, boolean settingValueChanged) {
        StringBuilder details = new StringBuilder("Updated fields: ");
        java.util.List<String> changedFields = new java.util.ArrayList<>();

        if (activeChanged) {
            changedFields.add("active");
        }
        if (descriptionChanged) {
            changedFields.add("description");
        }
        if (settingValueChanged) {
            changedFields.add("settingValue");
        }

        details.append(String.join(", ", changedFields));
        return details.toString();
    }

    /**
     * 
     * ##############################################################
     * ### ID Obfuscation Configurations Reload and Reset Methods ###
     * ##############################################################
     */

    public ResponseEntity<?> reloadIdObfuscationConfigurations() {
        idObfuscator.reloadConfig(securitySettingsGetterServices);
        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "ID Obfuscation Settings reloaded successfully.",
                null
            )
        );
    }

    public ResponseEntity<?> testIdObfuscationConfigurations() {
        String obfuscatedId;
        try {
            obfuscatedId = idObfuscator.encodeId(0l);
            // Return encoded
            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "ID Obfuscation test successful.",
                    obfuscatedId
                )
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                ApiResponse.error(
                    500,
                    "ID Obfuscation test failed: " + e.getMessage(),
                    "INTERNAL_SERVER_ERROR"
                )
            );
        }
    }

    public ResponseEntity<?> resetIdObfuscationConfigurations() {
        // Store default values in the database
        SecuritySetting obfuscatedLengthSetting = securitySettingsRepository.findBySettingKey("idObfuscator.obfuscated.length").orElse(null);
        if (obfuscatedLengthSetting != null) {
            obfuscatedLengthSetting.setSettingValue(String.valueOf(defaultObfuscatedLength));
            securitySettingsRepository.save(obfuscatedLengthSetting);
        }
        SecuritySetting saltLengthSetting = securitySettingsRepository.findBySettingKey("idObfuscator.salt.length").orElse(null);
        if (saltLengthSetting != null) {
            saltLengthSetting.setSettingValue(String.valueOf(defaultSaltLength));
            securitySettingsRepository.save(saltLengthSetting);
        }
        // Reload configurations
        idObfuscator.reloadConfig(securitySettingsGetterServices);
        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "ID Obfuscation Settings reset to default values successfully.",
                null
            )
        );
    }

    /**
     *
     * ###########################################################
     * ### Password Policy Configurations Reload and Reset Methods ###
     * ###########################################################
     */

    public ResponseEntity<?> resetPasswordPolicyConfigurations() {
        // Store default values in the database
        updateSettingIfExists("password.minLength", String.valueOf(passwordMinLength));
        updateSettingIfExists("password.maxLength", String.valueOf(passwordMaxLength));
        updateSettingIfExists("password.requireUppercase", String.valueOf(passwordRequireUppercase));
        updateSettingIfExists("password.requireLowercase", String.valueOf(passwordRequireLowercase));
        updateSettingIfExists("password.requireNumbers", String.valueOf(passwordRequireNumbers));
        updateSettingIfExists("password.requireSpecialCharacters", String.valueOf(passwordRequireSpecialCharacters));
        updateSettingIfExists("password.expirationDays", String.valueOf(passwordExpirationDays));

        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "Password Policy Settings reset to default values successfully.",
                null
            )
        );
    }

    /**
     *
     * #######################################################
     * ### JWT Token Configurations Reload and Reset Methods ###
     * #######################################################
     */

    public ResponseEntity<?> reloadJwtTokenConfigurations() {
        jwtTokenProvider.reloadConfig(securitySettingsGetterServices);
        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "JWT Token Settings reloaded successfully.",
                null
            )
        );
    }

    public ResponseEntity<?> resetJwtTokenConfigurations() {
        // Store default values in the database
        updateSettingIfExists("jwt.expiration.time.minutes", String.valueOf(jwtExpirationMinutes));
        updateSettingIfExists("jwt.refresh.expiration.time.minutes", String.valueOf(jwtRefreshExpirationMinutes));
        // Reload configurations
        jwtTokenProvider.reloadConfig(securitySettingsGetterServices);
        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "JWT Token Settings reset to default values successfully.",
                null
            )
        );
    }

    /**
     *
     * ########################################################
     * ### Login Rate Limit Configurations Reset Methods ###
     * ########################################################
     */

    public ResponseEntity<?> resetLoginRateLimitConfigurations() {
        // Store default values in the database
        updateSettingIfExists("loginAttempts.maxCapacity", String.valueOf(loginRateLimitMaxCapacity));
        updateSettingIfExists("loginAttempts.refillRate", String.valueOf(loginRateLimitRefillRate));
        updateSettingIfExists("loginAttempts.refillDurationMinutes", String.valueOf(loginRateLimitRefillDurationMinutes));
        updateSettingIfExists("loginAttempts.enabled", String.valueOf(loginRateLimitEnabled));

        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "Login Rate Limit Settings reset to default values successfully.",
                null
            )
        );
    }

    /**
     *
     * ############################################################
     * ### Account Lockout Policy Configurations Reset Methods ###
     * ############################################################
     */
    
    public ResponseEntity<?> resetAccountLockoutPolicyConfigurations() {
        // Store default values in the database
        updateSettingIfExists("accountLockout.maxFailedAttempts", String.valueOf(maxFailedAttempts));
        updateSettingIfExists("accountLockout.lockoutDurationMinutes", String.valueOf(lockoutDurationMinutes));
        updateSettingIfExists("accountLockout.counterResetHours", String.valueOf(lockoutCounterResetHours));
        updateSettingIfExists("accountLockout.enabled", String.valueOf(accountLockoutEnabled));

        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "Account Lockout Policy Settings reset to default values successfully.",
                null
            )
        );
    }

    /**
     *
     * ##################################################
     * ### General Configurations Reload and Reset Methods ###
     * ##################################################
     */

    public ResponseEntity<?> reloadAllConfigurations() {
        reloadIdObfuscationConfigurations();
        reloadJwtTokenConfigurations();

        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "All Security Settings reloaded successfully.",
                null
            )
        );
    }

    public ResponseEntity<?> resetAllConfigurations() {
        resetIdObfuscationConfigurations();
        resetPasswordPolicyConfigurations();
        resetJwtTokenConfigurations();
        resetLoginRateLimitConfigurations();
        resetAccountLockoutPolicyConfigurations();

        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "All Security Settings reset to default values successfully.",
                null
            )
        );
    }

    /**
     * Helper method to update a security setting if it exists in the database
     */
    private void updateSettingIfExists(String settingKey, String settingValue) {
        SecuritySetting setting = securitySettingsRepository.findBySettingKey(settingKey).orElse(null);
        if (setting != null) {
            setting.setSettingValue(settingValue);
            securitySettingsRepository.save(setting);
        }
    }
}
