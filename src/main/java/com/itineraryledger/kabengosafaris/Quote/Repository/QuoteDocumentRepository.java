package com.itineraryledger.kabengosafaris.Quote.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument.DocumentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for QuoteDocument entity.
 * Provides database operations for quote document management.
 */
@Repository
public interface QuoteDocumentRepository extends JpaRepository<QuoteDocument, Long>, JpaSpecificationExecutor<QuoteDocument> {

    /**
     * Find all documents for a quote
     */
    @Query("SELECT doc FROM QuoteDocument doc WHERE doc.quote.id = :quoteId ORDER BY doc.createdAt DESC")
    List<QuoteDocument> findByQuoteIdOrderByCreatedAtDesc(@Param("quoteId") Long quoteId);

    /**
     * Find all active documents for a quote
     */
    @Query("SELECT doc FROM QuoteDocument doc WHERE doc.quote.id = :quoteId AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<QuoteDocument> findActiveByQuoteId(@Param("quoteId") Long quoteId);

    /**
     * Find documents by quote and type
     */
    @Query("SELECT doc FROM QuoteDocument doc WHERE doc.quote.id = :quoteId AND doc.documentType = :documentType AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<QuoteDocument> findByQuoteIdAndDocumentType(@Param("quoteId") Long quoteId, @Param("documentType") DocumentType documentType);

    /**
     * Find currently valid documents for a quote
     */
    @Query("SELECT doc FROM QuoteDocument doc WHERE doc.quote.id = :quoteId AND doc.isActive = true AND (doc.validFrom IS NULL OR doc.validFrom <= :date) AND (doc.validTo IS NULL OR doc.validTo >= :date) ORDER BY doc.createdAt DESC")
    List<QuoteDocument> findCurrentlyValidByQuoteId(@Param("quoteId") Long quoteId, @Param("date") LocalDateTime date);

    /**
     * Find primary documents for a quote (QUOTE_PDF, PROPOSAL, CONTRACT)
     */
    @Query("SELECT doc FROM QuoteDocument doc WHERE doc.quote.id = :quoteId AND (doc.documentType = 'QUOTE_PDF' OR doc.documentType = 'PROPOSAL' OR doc.documentType = 'CONTRACT') AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<QuoteDocument> findPrimaryDocumentsByQuoteId(@Param("quoteId") Long quoteId);

    /**
     * Find payment documents for a quote (INVOICE, RECEIPT, PAYMENT_SCHEDULE, REFUND)
     */
    @Query("SELECT doc FROM QuoteDocument doc WHERE doc.quote.id = :quoteId AND (doc.documentType = 'INVOICE' OR doc.documentType = 'RECEIPT' OR doc.documentType = 'PAYMENT_SCHEDULE' OR doc.documentType = 'REFUND') AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<QuoteDocument> findPaymentDocumentsByQuoteId(@Param("quoteId") Long quoteId);

    /**
     * Count documents for a quote
     */
    @Query("SELECT COUNT(doc) FROM QuoteDocument doc WHERE doc.quote.id = :quoteId")
    long countByQuoteId(@Param("quoteId") Long quoteId);

    /**
     * Count active documents for a quote
     */
    @Query("SELECT COUNT(doc) FROM QuoteDocument doc WHERE doc.quote.id = :quoteId AND doc.isActive = true")
    long countActiveByQuoteId(@Param("quoteId") Long quoteId);

    /**
     * Check if filename exists
     */
    boolean existsByFileName(String fileName);

    /**
     * Find document by filename
     */
    Optional<QuoteDocument> findByFileName(String fileName);

    /**
     * Find documents with pagination
     */
    @Query("SELECT doc FROM QuoteDocument doc WHERE doc.quote.id = :quoteId")
    Page<QuoteDocument> findByQuoteIdPaginated(@Param("quoteId") Long quoteId, Pageable pageable);

    /**
     * Find all documents by type across all quotes (for admin/reporting)
     */
    @Query("SELECT doc FROM QuoteDocument doc WHERE doc.documentType = :documentType AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<QuoteDocument> findAllByDocumentType(@Param("documentType") DocumentType documentType);

    /**
     * Find expired documents (validTo in the past)
     */
    @Query("SELECT doc FROM QuoteDocument doc WHERE doc.isActive = true AND doc.validTo IS NOT NULL AND doc.validTo < :date ORDER BY doc.validTo ASC")
    List<QuoteDocument> findExpiredDocuments(@Param("date") LocalDateTime date);

    /**
     * Find documents expiring soon (within given days)
     */
    @Query("SELECT doc FROM QuoteDocument doc WHERE doc.isActive = true AND doc.validTo IS NOT NULL AND doc.validTo BETWEEN :startDate AND :endDate ORDER BY doc.validTo ASC")
    List<QuoteDocument> findDocumentsExpiringSoon(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT doc.id FROM QuoteDocument doc WHERE doc.id > :currentId ORDER BY doc.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT doc.id FROM QuoteDocument doc WHERE doc.id < :currentId ORDER BY doc.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT doc.id FROM QuoteDocument doc ORDER BY doc.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT doc.id FROM QuoteDocument doc ORDER BY doc.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
