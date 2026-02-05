package com.itineraryledger.kabengosafaris.BankAccount.Entity;

import com.itineraryledger.kabengosafaris.User.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts",
        indexes = {
                @Index(name = "idx_bank_account_currency", columnList = "currency"),
                @Index(name = "idx_bank_account_is_default", columnList = "is_default"),
                @Index(name = "idx_bank_account_is_active", columnList = "is_active")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bank_account_code", columnNames = {"account_code"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // IDENTIFICATION
    @Column(name = "account_code", length = 50, unique = true, nullable = false)
    private String accountCode;

    @Column(name = "account_name", length = 200, nullable = false)
    private String accountName;

    @Column(columnDefinition = "TEXT")
    private String description;

    // BANK DETAILS
    @Column(name = "bank_name", length = 200, nullable = false)
    private String bankName;

    @Column(name = "bank_branch", length = 200)
    private String bankBranch;

    @Column(name = "branch_address", columnDefinition = "TEXT")
    private String branchAddress;

    @Column(name = "branch_city", length = 100)
    private String branchCity;

    @Column(name = "branch_country", length = 100)
    private String branchCountry;

    // ACCOUNT DETAILS
    @Column(name = "account_number", length = 100, nullable = false)
    private String accountNumber;

    @Column(name = "account_holder_name", length = 200, nullable = false)
    private String accountHolderName;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    // INTERNATIONAL CODES
    @Column(name = "swift_bic_code", length = 20)
    private String swiftBicCode;

    @Column(name = "iban", length = 50)
    private String iban;

    @Column(name = "routing_number", length = 50)
    private String routingNumber;

    @Column(name = "sort_code", length = 20)
    private String sortCode;

    @Column(name = "intermediary_bank_name", length = 200)
    private String intermediaryBankName;

    @Column(name = "intermediary_swift_code", length = 20)
    private String intermediarySwiftCode;

    // SETTINGS
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // METADATA
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "invoice_display_notes", columnDefinition = "TEXT")
    private String invoiceDisplayNotes;

    // AUDIT FIELDS
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // HELPER METHODS
    @Transient
    public String generateCode() {
        if (id == null) return null;
        return String.format("BANK-%06d", id + 100);
    }

    @Transient
    public String getDisplayName() {
        return String.format("%s (%s)", accountName, currency);
    }
}
