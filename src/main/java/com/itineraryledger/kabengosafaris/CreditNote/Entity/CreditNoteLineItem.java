package com.itineraryledger.kabengosafaris.CreditNote.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.CreditNote.Enums.CreditNoteItemType;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceLineItem;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Line item in a credit note — describes what is being credited and why.
 */
@Entity
@Table(name = "credit_note_line_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNoteLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_id", nullable = false)
    private CreditNote creditNote;

    /**
     * Optional link back to the original InvoiceLineItem this credit is against.
     * When set, the referenced item must belong to credit_note.invoice_id.
     * Left null for ad-hoc credits that don't correspond to a specific billed item
     * (e.g. goodwill gesture, restocking fee).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_line_item_id")
    private InvoiceLineItem invoiceLineItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CreditNoteItemType itemType;

    @Column(nullable = false, length = 300)
    private String itemName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "credit_note_line_item_prices", joinColumns = @JoinColumn(name = "credit_note_line_item_id"))
    @Builder.Default
    private List<Price> prices = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
