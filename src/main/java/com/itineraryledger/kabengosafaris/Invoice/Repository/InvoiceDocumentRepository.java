package com.itineraryledger.kabengosafaris.Invoice.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument.DocumentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for InvoiceDocument entity.
 * Provides database operations for invoice document management.
 */
@Repository
public interface InvoiceDocumentRepository extends JpaRepository<InvoiceDocument, Long>, JpaSpecificationExecutor<InvoiceDocument> {

    /**
     * Find all documents for an invoice
     */
    @Query("SELECT doc FROM InvoiceDocument doc WHERE doc.invoice.id = :invoiceId ORDER BY doc.createdAt DESC")
    List<InvoiceDocument> findByInvoiceIdOrderByCreatedAtDesc(@Param("invoiceId") Long invoiceId);

    /**
     * Find all active documents for an invoice
     */
    @Query("SELECT doc FROM InvoiceDocument doc WHERE doc.invoice.id = :invoiceId AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<InvoiceDocument> findActiveByInvoiceId(@Param("invoiceId") Long invoiceId);

    /**
     * Find documents by invoice and type
     */
    @Query("SELECT doc FROM InvoiceDocument doc WHERE doc.invoice.id = :invoiceId AND doc.documentType = :documentType AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<InvoiceDocument> findByInvoiceIdAndDocumentType(@Param("invoiceId") Long invoiceId, @Param("documentType") DocumentType documentType);

    /**
     * Find currently valid documents for an invoice
     */
    @Query("SELECT doc FROM InvoiceDocument doc WHERE doc.invoice.id = :invoiceId AND doc.isActive = true AND (doc.validFrom IS NULL OR doc.validFrom <= :date) AND (doc.validTo IS NULL OR doc.validTo >= :date) ORDER BY doc.createdAt DESC")
    List<InvoiceDocument> findCurrentlyValidByInvoiceId(@Param("invoiceId") Long invoiceId, @Param("date") LocalDateTime date);

    /**
     * Find primary documents for an invoice (INVOICE_PDF, TAX_INVOICE, CONTRACT)
     */
    @Query("SELECT doc FROM InvoiceDocument doc WHERE doc.invoice.id = :invoiceId AND (doc.documentType = 'INVOICE_PDF' OR doc.documentType = 'TAX_INVOICE' OR doc.documentType = 'CONTRACT') AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<InvoiceDocument> findPrimaryDocumentsByInvoiceId(@Param("invoiceId") Long invoiceId);

    /**
     * Find payment documents for an invoice (PAYMENT_RECEIPT, PAYMENT_SCHEDULE, PAYMENT_CONFIRMATION, REFUND_RECEIPT, CREDIT_NOTE, DEBIT_NOTE)
     */
    @Query("SELECT doc FROM InvoiceDocument doc WHERE doc.invoice.id = :invoiceId AND (doc.documentType = 'PAYMENT_RECEIPT' OR doc.documentType = 'PAYMENT_SCHEDULE' OR doc.documentType = 'PAYMENT_CONFIRMATION' OR doc.documentType = 'REFUND_RECEIPT' OR doc.documentType = 'CREDIT_NOTE' OR doc.documentType = 'DEBIT_NOTE') AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<InvoiceDocument> findPaymentDocumentsByInvoiceId(@Param("invoiceId") Long invoiceId);

    /**
     * Count documents for an invoice
     */
    @Query("SELECT COUNT(doc) FROM InvoiceDocument doc WHERE doc.invoice.id = :invoiceId")
    long countByInvoiceId(@Param("invoiceId") Long invoiceId);

    /**
     * Count active documents for an invoice
     */
    @Query("SELECT COUNT(doc) FROM InvoiceDocument doc WHERE doc.invoice.id = :invoiceId AND doc.isActive = true")
    long countActiveByInvoiceId(@Param("invoiceId") Long invoiceId);

    /**
     * Check if filename exists
     */
    boolean existsByFileName(String fileName);

    /**
     * Find document by filename
     */
    Optional<InvoiceDocument> findByFileName(String fileName);

    /**
     * Find documents with pagination
     */
    @Query("SELECT doc FROM InvoiceDocument doc WHERE doc.invoice.id = :invoiceId")
    Page<InvoiceDocument> findByInvoiceIdPaginated(@Param("invoiceId") Long invoiceId, Pageable pageable);

    /**
     * Find all documents by type across all invoices (for admin/reporting)
     */
    @Query("SELECT doc FROM InvoiceDocument doc WHERE doc.documentType = :documentType AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<InvoiceDocument> findAllByDocumentType(@Param("documentType") DocumentType documentType);

    /**
     * Find expired documents (validTo in the past)
     */
    @Query("SELECT doc FROM InvoiceDocument doc WHERE doc.isActive = true AND doc.validTo IS NOT NULL AND doc.validTo < :date ORDER BY doc.validTo ASC")
    List<InvoiceDocument> findExpiredDocuments(@Param("date") LocalDateTime date);

    /**
     * Find documents expiring soon (within given days)
     */
    @Query("SELECT doc FROM InvoiceDocument doc WHERE doc.isActive = true AND doc.validTo IS NOT NULL AND doc.validTo BETWEEN :startDate AND :endDate ORDER BY doc.validTo ASC")
    List<InvoiceDocument> findDocumentsExpiringSoon(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT doc.id FROM InvoiceDocument doc WHERE doc.id > :currentId ORDER BY doc.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT doc.id FROM InvoiceDocument doc WHERE doc.id < :currentId ORDER BY doc.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT doc.id FROM InvoiceDocument doc ORDER BY doc.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT doc.id FROM InvoiceDocument doc ORDER BY doc.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
