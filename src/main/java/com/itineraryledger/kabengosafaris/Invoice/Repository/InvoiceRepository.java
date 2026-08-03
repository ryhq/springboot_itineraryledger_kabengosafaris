package com.itineraryledger.kabengosafaris.Invoice.Repository;

import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Invoice entity.
 * Provides database operations for invoice management.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    /**
     * Find invoice by invoice code
     */
    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    /**
     * Find invoice by invoice code (case-insensitive)
     */
    Optional<Invoice> findByInvoiceCodeIgnoreCase(String invoiceCode);

    /**
     * Check if invoice code exists
     */
    boolean existsByInvoiceCode(String invoiceCode);

    /**
     * Check if invoice code exists for a different invoice
     */
    boolean existsByInvoiceCodeAndIdNot(String invoiceCode, Long id);

    /**
     * Count invoices by status
     */
    long countByStatus(InvoiceStatus status);

    /**
     * Count active invoices
     */
    long countByIsActiveTrue();

    /**
     * Count invoices by customer
     */
    long countByCustomerId(Long customerId);

    /**
     * Count invoices by safari
     */
    long countBySafariId(Long safariId);

    /**
     * Find all invoices by status
     */
    java.util.List<Invoice> findByStatus(InvoiceStatus status);

    /**
     * Find all invoices by status in list
     */
    java.util.List<Invoice> findByStatusIn(java.util.List<InvoiceStatus> statuses);

    /**
     * Find all invoices linked to a safari
     */
    java.util.List<Invoice> findBySafariId(Long safariId);

    /**
     * Check if an invoice already exists for a safari (one invoice per safari)
     */
    boolean existsBySafariId(Long safariId);

    /**
     * Check if a NON-cancelled invoice already exists for a safari. Cancelled
     * invoices are kept for audit but must not block generating a fresh one,
     * so the "one invoice per safari" rule only counts live invoices.
     */
    boolean existsBySafariIdAndStatusNot(Long safariId, InvoiceStatus status);

    /**
     * Find all unpaid invoices for a safari (statuses that indicate money is still owed)
     */
    @Query("SELECT i FROM Invoice i WHERE i.safari.id = :safariId AND i.status IN ('DRAFT', 'SENT', 'PARTIALLY_PAID', 'OVERDUE')")
    java.util.List<Invoice> findUnpaidBySafariId(@Param("safariId") Long safariId);

    // Navigation queries for next/previous
    @Query("SELECT e.id FROM Invoice e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Invoice e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Invoice e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM Invoice e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
