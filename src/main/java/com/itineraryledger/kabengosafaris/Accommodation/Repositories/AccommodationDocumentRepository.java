package com.itineraryledger.kabengosafaris.Accommodation.Repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationDocument;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationDocument.DocumentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AccommodationDocument entity.
 * Provides database operations for accommodation document management.
 */
@Repository
public interface AccommodationDocumentRepository extends JpaRepository<AccommodationDocument, Long>, JpaSpecificationExecutor<AccommodationDocument> {

    /**
     * Find all documents for an accommodation
     */
    @Query("SELECT doc FROM AccommodationDocument doc WHERE doc.accommodation.id = :accommodationId ORDER BY doc.createdAt DESC")
    List<AccommodationDocument> findByAccommodationIdOrderByCreatedAtDesc(@Param("accommodationId") Long accommodationId);

    /**
     * Find all active documents for an accommodation
     */
    @Query("SELECT doc FROM AccommodationDocument doc WHERE doc.accommodation.id = :accommodationId AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<AccommodationDocument> findActiveByAccommodationId(@Param("accommodationId") Long accommodationId);

    /**
     * Find documents by accommodation and type
     */
    @Query("SELECT doc FROM AccommodationDocument doc WHERE doc.accommodation.id = :accommodationId AND doc.documentType = :documentType AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<AccommodationDocument> findByAccommodationIdAndDocumentType(@Param("accommodationId") Long accommodationId, @Param("documentType") DocumentType documentType);

    /**
     * Find currently valid documents for an accommodation
     */
    @Query("SELECT doc FROM AccommodationDocument doc WHERE doc.accommodation.id = :accommodationId AND doc.isActive = true AND (doc.validFrom IS NULL OR doc.validFrom <= :date) AND (doc.validTo IS NULL OR doc.validTo >= :date) ORDER BY doc.createdAt DESC")
    List<AccommodationDocument> findCurrentlyValidByAccommodationId(@Param("accommodationId") Long accommodationId, @Param("date") LocalDateTime date);

    /**
     * Find rate documents (STO_RATE or RACK_RATE) for an accommodation
     */
    @Query("SELECT doc FROM AccommodationDocument doc WHERE doc.accommodation.id = :accommodationId AND (doc.documentType = 'STO_RATE' OR doc.documentType = 'RACK_RATE') AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<AccommodationDocument> findRateDocumentsByAccommodationId(@Param("accommodationId") Long accommodationId);

    /**
     * Count documents for an accommodation
     */
    @Query("SELECT COUNT(doc) FROM AccommodationDocument doc WHERE doc.accommodation.id = :accommodationId")
    long countByAccommodationId(@Param("accommodationId") Long accommodationId);

    /**
     * Count active documents for an accommodation
     */
    @Query("SELECT COUNT(doc) FROM AccommodationDocument doc WHERE doc.accommodation.id = :accommodationId AND doc.isActive = true")
    long countActiveByAccommodationId(@Param("accommodationId") Long accommodationId);

    /**
     * Check if filename exists
     */
    boolean existsByFileName(String fileName);

    /**
     * Find document by filename
     */
    Optional<AccommodationDocument> findByFileName(String fileName);

    /**
     * Find documents with pagination
     */
    @Query("SELECT doc FROM AccommodationDocument doc WHERE doc.accommodation.id = :accommodationId")
    Page<AccommodationDocument> findByAccommodationIdPaginated(@Param("accommodationId") Long accommodationId, Pageable pageable);

    /**
     * Find all documents by type across all accommodations (for admin/reporting)
     */
    @Query("SELECT doc FROM AccommodationDocument doc WHERE doc.documentType = :documentType AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<AccommodationDocument> findAllByDocumentType(@Param("documentType") DocumentType documentType);

    /**
     * Find expired documents (validTo in the past)
     */
    @Query("SELECT doc FROM AccommodationDocument doc WHERE doc.isActive = true AND doc.validTo IS NOT NULL AND doc.validTo < :date ORDER BY doc.validTo ASC")
    List<AccommodationDocument> findExpiredDocuments(@Param("date") LocalDateTime date);

    /**
     * Find documents expiring soon (within given days)
     */
    @Query("SELECT doc FROM AccommodationDocument doc WHERE doc.isActive = true AND doc.validTo IS NOT NULL AND doc.validTo BETWEEN :startDate AND :endDate ORDER BY doc.validTo ASC")
    List<AccommodationDocument> findDocumentsExpiringSoon(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
