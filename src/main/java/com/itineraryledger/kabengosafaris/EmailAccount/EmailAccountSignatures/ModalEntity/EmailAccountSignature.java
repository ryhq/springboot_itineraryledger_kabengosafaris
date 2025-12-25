package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EmailSignature Entity - Stores email signature templates for email accounts
 *
 * Features:
 * - Multiple signatures per email account
 * - Only one signature per account can be marked as default (used for email appending)
 * - Support for variable substitution: {senderName}, {userAccountName}, etc.
 * - File-based storage with filename reference
 * - Variable definitions and default values
 *
 * Example:
 * - Signature Name: "Sales_Team"
 * - File: sales_sig_20241204.html
 * - Variables: [{name: "senderName", defaultValue: "Unknown"}, {name: "userAccountName", defaultValue: "Sales"}]
 */

@Entity
@Table(name = "email_account_signatures", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"email_account_id", "name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailAccountSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Email account this signature belongs to
     * CascadeType.ALL ensures that when an email account is deleted,
     * all associated signatures are also deleted
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_account_id", nullable = false)
    private EmailAccount emailAccount;

    /**
     * Name/label for this signature (e.g., "Sales", "Support", "Alerts")
     * Unique per email account
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Description of the signature's purpose
     */
    @Column(length = 500)
    private String description;

    /**
     * Filename where the signature HTML/content is stored
     * Format: {accountName}_{signatureName}_{timestamp}.html
     * Example: sales_sales_sig_20241204_143022.html
     */
    @Column(nullable = false, unique = true, length = 255)
    private String fileName;

    /**
     * Whether this is the default signature for the account
     * Only one signature per account should have this set to true
     * This signature will be automatically appended to emails sent from this account
     */
    @Column(nullable = false)
    private Boolean isDefault;

    /**
     * Whether this signature is enabled/active
     * Disabled signatures won't be used even if marked as default
     */
    @Column(nullable = false)
    private Boolean enabled;

    /**
     * Whether this is a system-generated default signature
     * System default signatures are created automatically when an email account is created
     * They can be modified but cannot be deleted (only deleted when the email account is deleted)
     * Users can restore this signature to its original default template if modified
     */
    @Column(nullable = false)
    private Boolean isSystemDefault;

    /**
     * JSON string storing variable definitions and their default values
     * Format: [{"name": "senderName", "defaultValue": "Unknown"}, {"name": "userAccountName", "defaultValue": "Sales"}]
     * Stored as @Lob to support large variable lists
     *
     * Variables supported:
     * - {senderName} - Name of the person sending the email
     * - {userAccountName} - Account name (email account)
     * - {currentDate} - Current date
     * - {currentTime} - Current time
     * - Custom variables defined by user
     */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String variablesJson;

    /**
     * File size of the signature in bytes
     */
    @Column(nullable = false)
    private Long fileSize;

    /**
     * Timestamp when this signature was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this signature was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.isDefault == null) this.isDefault = false;
        if (this.enabled == null) this.enabled = true;
        if (this.isSystemDefault == null) this.isSystemDefault = false;
        if (this.fileSize == null) this.fileSize = 0L;
        if (this.variablesJson == null) this.variablesJson = "[]";
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

    @Override
    public String toString() {
        return "EmailSignature{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", fileName='" + fileName + '\'' +
                ", isDefault=" + isDefault +
                ", enabled=" + enabled +
                ", fileSize=" + fileSize +
                ", createdAt=" + createdAt +
                '}';
    }
}
