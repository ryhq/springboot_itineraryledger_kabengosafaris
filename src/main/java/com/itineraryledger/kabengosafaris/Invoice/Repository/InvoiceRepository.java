package com.itineraryledger.kabengosafaris.Invoice.Repository;

import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
