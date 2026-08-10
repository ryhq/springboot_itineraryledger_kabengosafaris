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
            case "FULL_INVOICE" -> loadSchema("full-invoice-schema.json");
            case "FULL_CREDIT_NOTE" -> loadSchema("full-credit-note-schema.json");
            case "PAYMENT_RECEIPT" -> loadSchema("payment-receipt-schema.json");
            case "FULL_COST_ESTIMATION" -> loadSchema("full-cost-estimation-schema.json");
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
            case "FULL_INVOICE" -> "Full Safari Invoice";
            case "FULL_CREDIT_NOTE" -> "Full Credit Note";
            case "PAYMENT_RECEIPT" -> "Payment Receipt";
            case "FULL_COST_ESTIMATION" -> "Full Cost Estimation";
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
            case "FULL_INVOICE" -> "Complete invoice document with line items, multi-currency support, payment tracking (amounts paid, balances), tax and discount calculations, bank account details, and due dates. Ideal for billing and payment collection.";
            case "FULL_CREDIT_NOTE" -> "Complete credit note document with credited line items, multi-currency support, original invoice reference, credit reason, and customer details. Ideal for refund and credit documentation.";
            case "PAYMENT_RECEIPT" -> "Payment receipt document with payment amount, method, reference, invoice details, customer information, and remaining balance. Ideal for payment confirmation and record-keeping.";
            case "FULL_COST_ESTIMATION" -> "Complete costing for an itinerary (later a safari): every chargeable line day by day AND grouped by passenger band, in both operator (STO) and published (rack) prices, with per-currency totals, gross profit and margin, per-head figures, the pricing gaps that could not be resolved, and the last saved summary for comparison. Ships an internal costing template and a client price sheet — the internal figures are marked INTERNAL in the schema so a client-facing template can be built without printing them.";
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
            case "FULL_INVOICE" -> "com.itineraryledger.kabengosafaris.Invoice.DTOs.FullInvoiceDTO";
            case "FULL_CREDIT_NOTE" -> "com.itineraryledger.kabengosafaris.CreditNote.DTOs.CreditNoteDTO";
            case "PAYMENT_RECEIPT" -> "com.itineraryledger.kabengosafaris.Invoice.DTOs.PaymentReceiptDTO";
            case "FULL_COST_ESTIMATION" -> "com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.FullCostEstimationDTO";
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
            // From FullInvoiceDTO
            case "FULL_INVOICE" -> "invoice";
            case "FULL_CREDIT_NOTE" -> "creditNote";
            case "PAYMENT_RECEIPT" -> "receipt";
            // deliberately not "itinerary": a safari costing fills the same document
            case "FULL_COST_ESTIMATION" -> "costing";
            default -> "data";
        };
    }

    /**
     * Get all supported PDF document type names
     *
     * @return Array of PDF document type names that have defined variables
     */
    /**
     * Extra templates shipped with a document type, beyond its system default.
     *
     * Each row is {resource suffix, name, description}. The file is expected at
     * {@code templates/pdf-templates/<document>_<suffix>.html}. Seeding these
     * matters when a document type is useless with one template — a costing has
     * two audiences, and the difference between them is which figures are
     * printed, not how they are laid out.
     */
    public static String[][] getExtraTemplates(String documentName) {
        return switch (documentName) {
            case "FULL_COST_ESTIMATION" -> new String[][]{
                {
                    "client_price_sheet",
                    "Client Price Sheet",
                    "Published (rack) prices only — no operator cost, no margin, no pricing gaps. "
                        + "Safe to send to a client. States no total while anything is unpriced."
                }
            };
            default -> new String[0][];
        };
    }

    public static String[] getSupportedDocuments() {
        return new String[]{
            "FULL_ITINERARY",
            "FULL_QUOTE",
            "FULL_SAFARI",
            "FULL_INVOICE",
            "FULL_CREDIT_NOTE",
            "PAYMENT_RECEIPT",
            "FULL_COST_ESTIMATION"
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
