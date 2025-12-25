package com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entity representing system email events that trigger email notifications.
 * Email events are never deleted from the system but can be deactivated.
 * Each event can have multiple email templates associated with it.
 *
 * Examples: USER_REGISTRATION, PASSWORD_RESET, EMAIL_VERIFICATION, etc.
 */
@Entity
@Table(name = "email_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique name of the event (e.g., "USER_REGISTRATION", "PASSWORD_RESET")
     * This should be unique and immutable once created
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Human-readable description of the event
     */
    @Column(length = 500)
    private String description;

    /**
     * Whether this event is enabled
     * When disabled, no emails will be sent for this event
     */
    @Column(nullable = false)
    private Boolean enabled;

    /**
     * System-defined variables for this event (immutable via API)
     * Defines what data the system will provide when rendering email templates
     * Template creators must use these exact variable names in their templates
     * Format: JSON array of objects with keys: name, description, isRequired, defaultValue
     * Example: [{"name":"username","description":"User's username","isRequired":true}]
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String variablesJson;

    /**
     * Timestamp when the event was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the event was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Initialize default values before persisting
     */
    @PrePersist
    protected void onCreate() {
        if (this.enabled == null) {
            this.enabled = true;
        }
        if (this.variablesJson == null) {
            this.variablesJson = "[]";
        }
        validateVariablesJson();
    }

    /**
     * Validate variables JSON before updating
     */
    @PreUpdate
    protected void onUpdate() {
        validateVariablesJson();
    }

    /**
     * Validates that variablesJson only contains allowed keys: name, defaultValue, description, isRequired
     *
     * @throws IllegalArgumentException if variablesJson contains invalid structure or keys
     */
    private void validateVariablesJson() {
        if (this.variablesJson == null || this.variablesJson.isBlank() || "[]".equals(this.variablesJson.trim())) {
            return; // Empty or null is valid
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> variables = objectMapper.readValue(
                this.variablesJson,
                new TypeReference<List<Map<String, Object>>>() {}
            );

            // Define allowed keys
            Set<String> allowedKeys = Set.of("name", "defaultValue", "description", "isRequired");

            // Validate each variable object
            for (int i = 0; i < variables.size(); i++) {
                Map<String, Object> variable = variables.get(i);

                // Check for invalid keys
                for (String key : variable.keySet()) {
                    if (!allowedKeys.contains(key)) {
                        throw new IllegalArgumentException(
                            String.format(
                                "Invalid key '%s' in variablesJson at index %d. Only allowed keys are: %s",
                                key, i, allowedKeys
                            )
                        );
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors
            throw e;
        } catch (Exception e) {
            // JSON parsing error
            throw new IllegalArgumentException(
                "Invalid JSON format in variablesJson. Expected an array of objects with keys: name, defaultValue, description, isRequired. Error: " + e.getMessage()
            );
        }
    }
}
