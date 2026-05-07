package com.itineraryledger.kabengosafaris.Expense.Entity;

import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.Vendor.Entity.Vendor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Expense — money paid (or owed) to a Vendor, optionally for a specific Safari.
 *
 * Mirrors Invoice in reverse: it is a parent document with line items and
 * cross-currency payment tracking via ExpensePayment.
 */
@Entity
@Table(name = "expenses",
    indexes = {
        @Index(name = "idx_expense_code", columnList = "expense_code"),
        @Index(name = "idx_expense_status", columnList = "status"),
        @Index(name = "idx_expense_vendor_id", columnList = "vendor_id"),
        @Index(name = "idx_expense_safari_id", columnList = "safari_id"),
        @Index(name = "idx_expense_expense_date", columnList = "expense_date"),
        @Index(name = "idx_expense_due_date", columnList = "due_date"),
        @Index(name = "idx_expense_is_active", columnList = "is_active")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_expense_code", columnNames = {"expense_code"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique reference, format EXP-NNNNNN. */
    @Column(name = "expense_code", nullable = false, unique = true, length = 50)
    private String expenseCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** REQUIRED. Whom we paid. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    /**
     * NULLABLE. Safari the expense is attributed to. Null = operational
     * expense (rent, monthly insurance, generic fuel stock, etc).
     * ON DELETE SET NULL so historical expenses survive a safari deletion.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "safari_id", nullable = true)
    private Safari safari;

    @OneToMany(mappedBy = "expense", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenseLineItem> lineItems = new ArrayList<>();

    // =========================================================================
    // Multi-currency totals (computed by ExpenseTotalsCalculationService).
    // No discounts column — vendors don't typically discount themselves.
    // =========================================================================

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expense_subtotals", joinColumns = @JoinColumn(name = "expense_id"))
    @Builder.Default
    private List<Price> subtotals = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expense_taxes", joinColumns = @JoinColumn(name = "expense_id"))
    @Builder.Default
    private List<Price> taxes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expense_grand_totals", joinColumns = @JoinColumn(name = "expense_id"))
    @Builder.Default
    private List<Price> grandTotals = new ArrayList<>();

    /** VAT charged by the vendor, applied uniformly across line items. */
    @Column(precision = 5, scale = 2)
    private BigDecimal taxPercentage;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    /** When the vendor expects payment by. Nullable — many cash purchases have no due date. */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Vendor's own invoice / receipt number — important for reconciliation. */
    @Column(name = "reference_number", length = 200)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ExpenseStatus status = ExpenseStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String internalNotes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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

    @Transient
    public String generateCode() {
        if (id == null) return null;
        return String.format("EXP-%06d", id);
    }

    @Transient
    public boolean isOverdue() {
        return dueDate != null
                && LocalDate.now().isAfter(dueDate)
                && status != null
                && !status.isFinalState();
    }

    @Transient
    public boolean isEditable() {
        return status != null && status.isEditable();
    }

    @Transient
    public boolean isDeletable() {
        return status != null && status.isDeletable();
    }
}
