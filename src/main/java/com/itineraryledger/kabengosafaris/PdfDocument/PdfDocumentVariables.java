package com.itineraryledger.kabengosafaris.PdfDocument;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines system-wide variables/schema for each PDF document type.
 * These variables are immutable and cannot be modified via API.
 * Template creators can reference these paths using Thymeleaf syntax: ${itinerary.name}
 *
 * Variable Format:
 * - path: The variable path in the DTO (e.g., "name", "days[].title")
 * - type: The Java type (String, Integer, List, etc.)
 * - description: Human-readable description
 * - isRequired: Whether the variable is always present
 * - children: Nested fields for complex types
 */
public class PdfDocumentVariables {

    /**
     * Cache for loaded schemas to avoid repeated file reads
     */
    private static final Map<String, String> SCHEMA_CACHE = new HashMap<>();

    /**
     * Get the system-defined variables schema for a specific PDF document type
     *
     * @param documentName The name of the PDF document type (e.g., "FULL_ITINERARY")
     * @return JSON string containing variable definitions
     */
    public static String getVariablesForDocument(String documentName) {
        return switch (documentName) {
            case "FULL_ITINERARY" -> loadSchema("full-itinerary-schema.json");
            case "FULL_QUOTE" -> loadSchema("full-quote-schema.json");
            case "FULL_SAFARI" -> loadSchema("full-safari-schema.json");
            default -> "[]";
        };
    }

    /**
     * Get the display name for a document type
     */
    public static String getDisplayName(String documentName) {
        return switch (documentName) {
            case "FULL_ITINERARY" -> "Full Safari Itinerary";
            case "FULL_QUOTE" -> "Full Safari Quote";
            case "FULL_SAFARI" -> "Full Safari Document";
            default -> documentName;
        };
    }

    /**
     * Get the description for a document type
     */
    public static String getDescription(String documentName) {
        return switch (documentName) {
            case "FULL_ITINERARY" -> "Complete itinerary document with all days, parks, activities, accommodations, and passenger configurations. Ideal for client proposals and booking confirmations.";
            case "FULL_QUOTE" -> "Complete quote/quotation document with itemized pricing in multiple currencies, customer information, payment terms, and validity period. Ideal for price quotations and proposals.";
            case "FULL_SAFARI" -> "Complete safari booking document with actual dates, customer information, real-time tracking (current phase, day number), operational fields (payment tracking, waiver management), and all nested data. Ideal for confirmed safari bookings and operational management.";
            default -> "";
        };
    }

    /**
     * Get the DTO class name for a document type
     */
    public static String getDataSourceClass(String documentName) {
        return switch (documentName) {
            case "FULL_ITINERARY" -> "com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO";
            case "FULL_QUOTE" -> "com.itineraryledger.kabengosafaris.Quote.DTOs.FullQuoteDTO";
            case "FULL_SAFARI" -> "com.itineraryledger.kabengosafaris.Safari.DTOs.FullSafariDTO";
            default -> "";
        };
    }

    /**
     * Get the root variable name for templates
     */
    public static String getRootVariableName(String documentName) {
        return switch (documentName) {
            // From FullItineraryDTO
            case "FULL_ITINERARY" -> "itinerary";
            // From FullQuoteDTO
            case "FULL_QUOTE" -> "quote";
            // From FullSafariDTO
            case "FULL_SAFARI" -> "safari";
            default -> "data";
        };
    }

    /**
     * Get all supported PDF document type names
     *
     * @return Array of PDF document type names that have defined variables
     */
    public static String[] getSupportedDocuments() {
        return new String[]{
            "FULL_ITINERARY",
            "FULL_QUOTE",
            "FULL_SAFARI"
            // Future document types:
            // "BOOKING_CONFIRMATION",
            // "INVOICE"
        };
    }

    /**
     * Load schema from JSON file in classpath resources
     *
     * @param filename The schema filename (e.g., "full-itinerary-schema.json")
     * @return JSON string containing the schema
     */
    private static String loadSchema(String filename) {
        // Check cache first
        if (SCHEMA_CACHE.containsKey(filename)) {
            return SCHEMA_CACHE.get(filename);
        }

        try {
            ClassPathResource resource = new ClassPathResource("schemas/pdf-documents/" + filename);
            String schema = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Cache the schema for future use
            SCHEMA_CACHE.put(filename, schema);

            return schema;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load PDF document schema: " + filename, e);
        }
    }

}
