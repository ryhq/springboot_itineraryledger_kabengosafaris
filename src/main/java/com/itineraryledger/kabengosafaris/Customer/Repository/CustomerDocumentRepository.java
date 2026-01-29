package com.itineraryledger.kabengosafaris.Customer.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument.DocumentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CustomerDocument entity.
 * Provides database operations for customer document management.
 */
@Repository
public interface CustomerDocumentRepository extends JpaRepository<CustomerDocument, Long>, JpaSpecificationExecutor<CustomerDocument> {

    /**
     * Find all documents for a customer
     */
    @Query("SELECT doc FROM CustomerDocument doc WHERE doc.customer.id = :customerId ORDER BY doc.createdAt DESC")
    List<CustomerDocument> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") Long customerId);

    /**
     * Find all active documents for a customer
     */
    @Query("SELECT doc FROM CustomerDocument doc WHERE doc.customer.id = :customerId AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<CustomerDocument> findActiveByCustomerId(@Param("customerId") Long customerId);

    /**
     * Find documents by customer and type
     */
    @Query("SELECT doc FROM CustomerDocument doc WHERE doc.customer.id = :customerId AND doc.documentType = :documentType AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<CustomerDocument> findByCustomerIdAndDocumentType(@Param("customerId") Long customerId, @Param("documentType") DocumentType documentType);

    /**
     * Find currently valid documents for a customer
     */
    @Query("SELECT doc FROM CustomerDocument doc WHERE doc.customer.id = :customerId AND doc.isActive = true AND (doc.validFrom IS NULL OR doc.validFrom <= :date) AND (doc.validTo IS NULL OR doc.validTo >= :date) ORDER BY doc.createdAt DESC")
    List<CustomerDocument> findCurrentlyValidByCustomerId(@Param("customerId") Long customerId, @Param("date") LocalDateTime date);

    /**
     * Find identity documents for a customer
     */
    @Query("SELECT doc FROM CustomerDocument doc WHERE doc.customer.id = :customerId AND (doc.documentType = 'PASSPORT' OR doc.documentType = 'ID_CARD' OR doc.documentType = 'DRIVERS_LICENSE') AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<CustomerDocument> findIdentityDocumentsByCustomerId(@Param("customerId") Long customerId);

    /**
     * Count documents for a customer
     */
    @Query("SELECT COUNT(doc) FROM CustomerDocument doc WHERE doc.customer.id = :customerId")
    long countByCustomerId(@Param("customerId") Long customerId);

    /**
     * Count active documents for a customer
     */
    @Query("SELECT COUNT(doc) FROM CustomerDocument doc WHERE doc.customer.id = :customerId AND doc.isActive = true")
    long countActiveByCustomerId(@Param("customerId") Long customerId);

    /**
     * Check if filename exists
     */
    boolean existsByFileName(String fileName);

    /**
     * Find document by filename
     */
    Optional<CustomerDocument> findByFileName(String fileName);

    /**
     * Find documents with pagination
     */
    @Query("SELECT doc FROM CustomerDocument doc WHERE doc.customer.id = :customerId")
    Page<CustomerDocument> findByCustomerIdPaginated(@Param("customerId") Long customerId, Pageable pageable);

    /**
     * Find all documents by type across all customers (for admin/reporting)
     */
    @Query("SELECT doc FROM CustomerDocument doc WHERE doc.documentType = :documentType AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<CustomerDocument> findAllByDocumentType(@Param("documentType") DocumentType documentType);

    /**
     * Find expired documents (validTo in the past)
     */
    @Query("SELECT doc FROM CustomerDocument doc WHERE doc.isActive = true AND doc.validTo IS NOT NULL AND doc.validTo < :date ORDER BY doc.validTo ASC")
    List<CustomerDocument> findExpiredDocuments(@Param("date") LocalDateTime date);

    /**
     * Find documents expiring soon (within given days)
     */
    @Query("SELECT doc FROM CustomerDocument doc WHERE doc.isActive = true AND doc.validTo IS NOT NULL AND doc.validTo BETWEEN :startDate AND :endDate ORDER BY doc.validTo ASC")
    List<CustomerDocument> findDocumentsExpiringSoon(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Delete all documents for a customer
     */
    void deleteByCustomerId(Long customerId);
}
