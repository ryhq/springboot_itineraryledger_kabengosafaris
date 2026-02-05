package com.itineraryledger.kabengosafaris.Invoice.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceItemType;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * InvoiceLineItem entity representing an individual line item in an invoice.
 *
 * Each item can have multiple prices (one per currency) to accommodate
 * different payment methods and currency preferences.
 *
 * Key features:
 * - Multi-currency pricing support
 * - Display ordering
 * - Flexible item description
 */
@Entity
@Table(name = "invoice_line_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The invoice this line item belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // =====================================================================
    // ITEM DETAILS
    // =====================================================================

    /**
     * Type of item (Accommodation, Park Fee, Activity, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InvoiceItemType itemType;

    /**
     * Display name/title of the item
     * Example: "Safari Package - 5 Days Serengeti", "Park Entry Fees"
     */
    @Column(nullable = false, length = 300)
    private String itemName;

    /**
     * Detailed description of the item
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Display order/sequence for this item in the invoice
     * Lower numbers appear first
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    // =====================================================================
    // MULTI-CURRENCY PRICING
    // =====================================================================

    /**
     * Prices for this item in different currencies
     * Each Price object contains: {currency, quantity, unitPrice, totalPrice, breakdown}
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "invoice_line_item_prices", joinColumns = @JoinColumn(name = "invoice_line_item_id"))
    @Builder.Default
    private List<Price> prices = new ArrayList<>();

    /**
     * Whether this item is active
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // =====================================================================
    // AUDIT FIELDS
    // =====================================================================

    /**
     * Timestamp when this item was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this item was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
