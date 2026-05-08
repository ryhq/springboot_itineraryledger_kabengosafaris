package com.itineraryledger.kabengosafaris.Expense.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ExpenseDocument Entity — Manages reference documents for expenses.
 *
 * Stores files supplied by the vendor (or our own internal records) that
 * document a payment, such as:
 *   - Vendor receipts (PDF or photo)
 *   - Vendor invoices issued to us
 *   - Bank-transfer / wire-transfer confirmations
 *   - Mobile-money transaction screenshots
 *   - Tax invoices, contracts, purchase orders, delivery notes
 *
 * Mirrors the CustomerDocument shape so the same FE patterns apply.
 */
@Entity
@Table(name = "expense_documents", indexes = {
    @Index(name = "idx_expense_document_expense_id", columnList = "expense_id"),
    @Index(name = "idx_expense_document_payment_id", columnList = "expense_payment_id"),
    @Index(name = "idx_expense_document_type", columnList = "document_type"),
    @Index(name = "idx_expense_document_is_active", columnList = "is_active"),
    @Index(name = "idx_expense_document_valid_from", columnList = "valid_from"),
    @Index(name = "idx_expense_document_valid_to", columnList = "valid_to")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    /**
     * Optional. When set, this proof attaches to a specific ExpensePayment row
     * (e.g., the receipt for one cash payment). Null = applies to the expense
     * as a whole.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_payment_id")
    private ExpensePayment expensePayment;

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

    /** Vendor's reference / receipt number printed on the document. */
    @Column(name = "document_number", length = 100)
    private String documentNumber;

    @Column(name = "version", length = 50)
    private String version;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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
     * Expense-specific document type enumeration.
     * Oriented around proof-of-payment artefacts the vendor issues to us.
     */
    public enum DocumentType {
        RECEIPT("Receipt", "Vendor's receipt acknowledging the payment"),
        VENDOR_INVOICE("Vendor Invoice", "Invoice issued to us by the vendor"),
        TAX_INVOICE("Tax Invoice", "Tax invoice with VAT details"),
        BANK_TRANSFER_PROOF("Bank Transfer Proof", "Bank statement entry or wire-transfer confirmation"),
        MOBILE_MONEY_RECEIPT("Mobile Money Receipt", "M-Pesa, Tigo Pesa or Airtel Money confirmation"),
        CASH_RECEIPT("Cash Receipt", "Photo of a paper cash receipt"),
        CHEQUE_COPY("Cheque Copy", "Scan or photo of the issued cheque"),
        CARD_RECEIPT("Card Receipt", "Credit / debit card slip"),
        PURCHASE_ORDER("Purchase Order", "Purchase order document"),
        DELIVERY_NOTE("Delivery Note", "Proof of delivery / goods-received note"),
        CONTRACT("Contract", "Service contract or agreement with the vendor"),
        QUOTATION("Quotation", "Vendor's original quote / pro-forma"),
        STATEMENT("Statement", "Vendor account statement"),
        REFUND_RECEIPT("Refund Receipt", "Receipt for refund issued by the vendor"),
        CORRESPONDENCE("Correspondence", "Email or letter relating to the expense"),
        SUPPORTING_DOCUMENT("Supporting Document", "Supporting documentation"),
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

    /** Whether the document is in date and active. */
    public boolean isCurrentlyValid() {
        LocalDateTime now = LocalDateTime.now();
        if (validFrom != null && now.isBefore(validFrom)) return false;
        if (validTo != null && now.isAfter(validTo)) return false;
        return Boolean.TRUE.equals(isActive);
    }

    public boolean isValidForDate(LocalDateTime date) {
        if (validFrom != null && date.isBefore(validFrom)) return false;
        if (validTo != null && date.isAfter(validTo)) return false;
        return Boolean.TRUE.equals(isActive);
    }

    public String getFileExtension() {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /** True for the canonical proof-of-payment document types. */
    public boolean isProofOfPayment() {
        return documentType == DocumentType.RECEIPT
            || documentType == DocumentType.BANK_TRANSFER_PROOF
            || documentType == DocumentType.MOBILE_MONEY_RECEIPT
            || documentType == DocumentType.CASH_RECEIPT
            || documentType == DocumentType.CHEQUE_COPY
            || documentType == DocumentType.CARD_RECEIPT;
    }
}
