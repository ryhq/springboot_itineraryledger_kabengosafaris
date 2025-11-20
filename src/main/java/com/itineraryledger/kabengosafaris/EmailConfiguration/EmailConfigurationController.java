package com.itineraryledger.kabengosafaris.EmailConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for managing email configurations
 * All endpoints require ADMIN role or specific EMAIL_CONFIG permissions
 *
 * Endpoints:
 * - POST   /api/email-config                    Create new configuration
 * - GET    /api/email-config                    List all configurations
 * - GET    /api/email-config/{id}               Get configuration by ID
 * - GET    /api/email-config/name/{name}        Get configuration by name
 * - GET    /api/email-config/default             Get default configuration
 * - PUT    /api/email-config/{id}               Update configuration
 * - DELETE /api/email-config/{id}               Delete configuration
 * - POST   /api/email-config/{id}/test          Test SMTP connection
 * - POST   /api/email-config/{id}/grant-user    Grant user access
 * - POST   /api/email-config/{id}/grant-role    Grant role access
 * - DELETE /api/email-config/{id}/revoke-user   Revoke user access
 * - GET    /api/email-config/{id}/permissions   Get configuration permissions
 */
@RestController
@RequestMapping("/api/email-config")
@RequiredArgsConstructor
@Slf4j
public class EmailConfigurationController {

    private final EmailConfigurationService emailConfigService;

    /**
     * Create a new email configuration
     * Requires: ADMIN role or EMAIL_CONFIG_CREATE permission
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_email_config_create')")
    public ResponseEntity<Map<String, Object>> createConfiguration(
            @RequestBody EmailConfiguration config) {
        log.info("Creating new email configuration: {}", config.getName());

        try {
            EmailConfiguration created = emailConfigService.createConfiguration(config);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email configuration created successfully");
            response.put("data", created);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Failed to create email configuration", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Get all email configurations
     * Requires: ADMIN role or EMAIL_CONFIG_READ permission
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_email_config_read')")
    public ResponseEntity<Map<String, Object>> getAllConfigurations() {
        log.info("Retrieving all email configurations");

        try {
            List<EmailConfiguration> configs = emailConfigService.getAllConfigurations();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", configs.size());
            response.put("data", configs);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to retrieve email configurations", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get configuration by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_email_config_read')")
    public ResponseEntity<Map<String, Object>> getConfiguration(@PathVariable Long id) {
        log.info("Retrieving email configuration: {}", id);

        try {
            EmailConfiguration config = emailConfigService.getConfiguration(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", config);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to retrieve email configuration", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Get configuration by name
     */
    @GetMapping("/name/{name}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_email_config_read')")
    public ResponseEntity<Map<String, Object>> getConfigurationByName(@PathVariable String name) {
        log.info("Retrieving email configuration by name: {}", name);

        try {
            EmailConfiguration config = emailConfigService.getConfigurationByName(name);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", config);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to retrieve email configuration by name", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Get the default email configuration
     */
    @GetMapping("/default")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_email_config_read')")
    public ResponseEntity<Map<String, Object>> getDefaultConfiguration() {
        log.info("Retrieving default email configuration");

        try {
            EmailConfiguration config = emailConfigService.getDefaultConfiguration();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", config);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to retrieve default email configuration", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Update email configuration
     * Requires: ADMIN role or EMAIL_CONFIG_UPDATE permission
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_email_config_update')")
    public ResponseEntity<Map<String, Object>> updateConfiguration(
            @PathVariable Long id,
            @RequestBody EmailConfiguration configUpdates) {
        log.info("Updating email configuration: {}", id);

        try {
            EmailConfiguration updated = emailConfigService.updateConfiguration(id, configUpdates);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email configuration updated successfully");
            response.put("data", updated);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to update email configuration", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Delete email configuration
     * Requires: ADMIN role or EMAIL_CONFIG_DELETE permission
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_email_config_delete')")
    public ResponseEntity<Map<String, Object>> deleteConfiguration(@PathVariable Long id) {
        log.info("Deleting email configuration: {}", id);

        try {
            emailConfigService.deleteConfiguration(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email configuration deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to delete email configuration", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Test SMTP connection for a configuration
     */
    @PostMapping("/{id}/test")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_email_config_test')")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long id) {
        log.info("Testing email configuration connection: {}", id);

        try {
            EmailConfiguration config = emailConfigService.getConfiguration(id);
            boolean success = emailConfigService.testEmailConnection(config);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);

            if (success) {
                response.put("message", "Email connection test successful");
                response.put("testedAt", config.getLastTestedAt());
            } else {
                response.put("message", "Email connection test failed");
                response.put("error", config.getLastErrorMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to test email connection", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Grant a user access to use an email configuration
     */
    @PostMapping("/{id}/grant-user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_email_config_grant')")
    public ResponseEntity<Map<String, Object>> grantUserAccess(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String grantedBy) {
        log.info("Granting user {} access to email configuration {}", userId, id);

        try {
            EmailConfigurationPermission permission = emailConfigService.grantUserAccess(
                    id, userId, reason, grantedBy);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User access granted successfully");
            response.put("data", permission);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Failed to grant user access", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Grant a role access to use an email configuration
     */
    @PostMapping("/{id}/grant-role/{roleId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_email_config_grant')")
    public ResponseEntity<Map<String, Object>> grantRoleAccess(
            @PathVariable Long id,
            @PathVariable Long roleId,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String grantedBy) {
        log.info("Granting role {} access to email configuration {}", roleId, id);

        try {
            EmailConfigurationPermission permission = emailConfigService.grantRoleAccess(
                    id, roleId, reason, grantedBy);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Role access granted successfully");
            response.put("data", permission);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Failed to grant role access", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Revoke user access to an email configuration
     */
    @DeleteMapping("/{id}/revoke-user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PERM_email_config_revoke')")
    public ResponseEntity<Map<String, Object>> revokeUserAccess(
            @PathVariable Long id,
            @PathVariable Long userId) {
        log.info("Revoking user {} access to email configuration {}", userId, id);

        try {
            emailConfigService.revokeUserAccess(id, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User access revoked successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to revoke user access", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
