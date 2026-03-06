package com.itineraryledger.kabengosafaris.Invoice.Repository;

import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for InvoiceLineItem entity.
 * Provides database operations for invoice line item management.
 */
@Repository
public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, Long>, JpaSpecificationExecutor<InvoiceLineItem> {

    /**
     * Find all line items for an invoice
     */
    List<InvoiceLineItem> findByInvoiceIdOrderByDisplayOrderAsc(Long invoiceId);

    /**
     * Find all active line items for an invoice
     */
    List<InvoiceLineItem> findByInvoiceIdAndIsActiveTrueOrderByDisplayOrderAsc(Long invoiceId);

    /**
     * Find maximum display order for an invoice
     */
    @Query("SELECT COALESCE(MAX(i.displayOrder), -1) FROM InvoiceLineItem i WHERE i.invoice.id = :invoiceId")
    Integer findMaxDisplayOrderByInvoiceId(@Param("invoiceId") Long invoiceId);

    /**
     * Count line items for an invoice
     */
    long countByInvoiceId(Long invoiceId);

    /**
     * Delete all line items for an invoice
     */
    void deleteByInvoiceId(Long invoiceId);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT i.id FROM InvoiceLineItem i WHERE i.id > :currentId ORDER BY i.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT i.id FROM InvoiceLineItem i WHERE i.id < :currentId ORDER BY i.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT i.id FROM InvoiceLineItem i ORDER BY i.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT i.id FROM InvoiceLineItem i ORDER BY i.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
