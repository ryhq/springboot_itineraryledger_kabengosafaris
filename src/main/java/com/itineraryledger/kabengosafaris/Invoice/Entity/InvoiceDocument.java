package com.itineraryledger.kabengosafaris.Invoice.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * InvoiceDocument Entity - Manages reference documents for invoices
 *
 * Stores various document files related to invoices:
 * - Invoice PDF/documents
 * - Payment receipts
 * - Payment schedules
 * - Credit notes
 * - Contracts and agreements
 * - Supporting documents
 */
@Entity
@Table(name = "invoice_documents", indexes = {
    @Index(name = "idx_invoice_document_invoice_id", columnList = "invoice_id"),
    @Index(name = "idx_invoice_document_type", columnList = "document_type"),
    @Index(name = "idx_invoice_document_is_active", columnList = "is_active"),
    @Index(name = "idx_invoice_document_valid_from", columnList = "valid_from"),
    @Index(name = "idx_invoice_document_valid_to", columnList = "valid_to"),
    @Index(name = "idx_invoice_document_is_generated", columnList = "is_generated")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "version", length = 50)
    private String version;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_generated", nullable = false)
    private Boolean isGenerated = false;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Invoice-specific document type enumeration
     */
    public enum DocumentType {
        INVOICE_PDF("Invoice PDF", "Main invoice document in PDF format"),
        PAYMENT_RECEIPT("Payment Receipt", "Receipt for payment received"),
        PAYMENT_SCHEDULE("Payment Schedule", "Payment terms and schedule"),
        PROFORMA_INVOICE("Proforma Invoice", "Preliminary invoice"),
        COMMERCIAL_INVOICE("Commercial Invoice", "Commercial invoice for customs"),
        CREDIT_NOTE("Credit Note", "Credit note for refund or adjustment"),
        DEBIT_NOTE("Debit Note", "Debit note for additional charges"),
        TAX_INVOICE("Tax Invoice", "Tax invoice with tax details"),
        CONTRACT("Contract", "Service contract or agreement"),
        TERMS_AND_CONDITIONS("Terms & Conditions", "Terms and conditions document"),
        PURCHASE_ORDER("Purchase Order", "Customer purchase order"),
        DELIVERY_NOTE("Delivery Note", "Proof of delivery"),
        BANK_DETAILS("Bank Details", "Bank transfer information"),
        PAYMENT_CONFIRMATION("Payment Confirmation", "Confirmation of payment"),
        REFUND_RECEIPT("Refund Receipt", "Receipt for refund issued"),
        STATEMENT("Statement", "Account statement"),
        CORRESPONDENCE("Correspondence", "Email or letter correspondence"),
        SUPPORTING_DOCUMENT("Supporting Document", "Supporting documentation"),
        INSURANCE("Insurance", "Travel insurance documents"),
        VISA_SUPPORT("Visa Support", "Visa application support letter"),
        ACCOMMODATION_VOUCHER("Accommodation Voucher", "Hotel/lodge voucher"),
        ACTIVITY_VOUCHER("Activity Voucher", "Activity booking voucher"),
        TRANSPORT_VOUCHER("Transport Voucher", "Transport/transfer voucher"),
        CANCELLATION("Cancellation", "Cancellation document"),
        AMENDMENT("Amendment", "Invoice amendment or revision"),
        OTHER("Other", "Other document type");

        private final String displayName;
        private final String description;

        DocumentType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Check if document is currently valid
     */
    public boolean isCurrentlyValid() {
        LocalDateTime now = LocalDateTime.now();

        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }

        if (validTo != null && now.isAfter(validTo)) {
            return false;
        }

        return isActive;
    }

    /**
     * Check if document is valid for a specific date
     */
    public boolean isValidForDate(LocalDateTime date) {
        if (validFrom != null && date.isBefore(validFrom)) {
            return false;
        }

        if (validTo != null && date.isAfter(validTo)) {
            return false;
        }

        return isActive;
    }

    /**
     * Get file extension from file name
     */
    public String getFileExtension() {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Check if document is a primary invoice document
     */
    public boolean isPrimaryInvoiceDocument() {
        return documentType == DocumentType.INVOICE_PDF ||
               documentType == DocumentType.TAX_INVOICE ||
               documentType == DocumentType.CONTRACT;
    }

    /**
     * Check if document is payment-related
     */
    public boolean isPaymentDocument() {
        return documentType == DocumentType.PAYMENT_RECEIPT ||
               documentType == DocumentType.PAYMENT_SCHEDULE ||
               documentType == DocumentType.PAYMENT_CONFIRMATION ||
               documentType == DocumentType.REFUND_RECEIPT ||
               documentType == DocumentType.CREDIT_NOTE ||
               documentType == DocumentType.DEBIT_NOTE;
    }

    /**
     * Check if document is a voucher
     */
    public boolean isVoucherDocument() {
        return documentType == DocumentType.ACCOMMODATION_VOUCHER ||
               documentType == DocumentType.ACTIVITY_VOUCHER ||
               documentType == DocumentType.TRANSPORT_VOUCHER;
    }
}
