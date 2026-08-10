package com.itineraryledger.kabengosafaris.PdfDocument.Entity;

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
 * Entity representing system PDF document types that can be generated.
 * PDF documents are built-in types - users cannot create new ones, only templates for existing types.
 * Each document type can have multiple PDF templates associated with it.
 *
 * Examples: FULL_ITINERARY, SAFARI_QUOTE, BOOKING_CONFIRMATION, etc.
 */
@Entity
@Table(name = "pdf_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique name of the document type (e.g., "FULL_ITINERARY", "SAFARI_QUOTE")
     * This should be unique and immutable once created
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Human-readable display name for the document type
     */
    @Column(name = "display_name", length = 200)
    private String displayName;

    /**
     * Human-readable description of the document type
     */
    /*
     * Deliberately roomy. A 500-char cap was reached by a document type whose
     * description ran five characters over, and the failure surfaced not as a
     * validation error but as a crash loop at boot — the seeding insert broke
     * the startup transaction. Descriptions are prose written by people; give
     * them room rather than trusting everyone to count.
     */
    @Column(length = 2000)
    private String description;

    /**
     * The fully qualified class name of the DTO used for this document
     * e.g., "com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO"
     */
    @Column(name = "data_source_class", length = 500)
    private String dataSourceClass;

    /**
     * The root variable name used in templates
     * e.g., "itinerary" for FullItineraryDTO
     */
    @Column(name = "root_variable_name", length = 100)
    private String rootVariableName;

    /**
     * Whether this document type is enabled
     * When disabled, no PDFs can be generated for this type
     */
    @Column(nullable = false)
    private Boolean enabled;

    /**
     * System-defined variables/schema for this document type (immutable via API)
     * Defines what data fields are available when rendering PDF templates
     * Template creators can reference these variable paths in their templates
     * Format: JSON array of objects with keys: path, type, description, isRequired
     * Example: [{"path":"name","type":"String","description":"Itinerary name","isRequired":true}]
     */
    @Column(name = "variables_json", columnDefinition = "TEXT", nullable = false)
    private String variablesJson;

    /**
     * Timestamp when the document type was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the document type was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
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
     * Validates that variablesJson only contains allowed keys: path, type, description, isRequired
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
            Set<String> allowedKeys = Set.of("path", "type", "description", "isRequired", "children");

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
                "Invalid JSON format in variablesJson. Expected an array of objects with keys: path, type, description, isRequired, children. Error: " + e.getMessage()
            );
        }
    }
}
