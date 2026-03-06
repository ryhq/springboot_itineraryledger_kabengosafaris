package com.itineraryledger.kabengosafaris.Park.Repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Park.Entities.ParkDocument;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkDocument.DocumentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ParkDocument entity.
 * Provides database operations for park document management.
 */
@Repository
public interface ParkDocumentRepository extends JpaRepository<ParkDocument, Long>, JpaSpecificationExecutor<ParkDocument> {

    /**
     * Find all documents for a park
     */
    @Query("SELECT doc FROM ParkDocument doc WHERE doc.park.id = :parkId ORDER BY doc.createdAt DESC")
    List<ParkDocument> findByParkIdOrderByCreatedAtDesc(@Param("parkId") Long parkId);

    /**
     * Find all active documents for a park
     */
    @Query("SELECT doc FROM ParkDocument doc WHERE doc.park.id = :parkId AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<ParkDocument> findActiveByParkId(@Param("parkId") Long parkId);

    /**
     * Find documents by park and type
     */
    @Query("SELECT doc FROM ParkDocument doc WHERE doc.park.id = :parkId AND doc.documentType = :documentType AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<ParkDocument> findByParkIdAndDocumentType(@Param("parkId") Long parkId, @Param("documentType") DocumentType documentType);

    /**
     * Find currently valid documents for a park
     */
    @Query("SELECT doc FROM ParkDocument doc WHERE doc.park.id = :parkId AND doc.isActive = true AND (doc.validFrom IS NULL OR doc.validFrom <= :date) AND (doc.validTo IS NULL OR doc.validTo >= :date) ORDER BY doc.createdAt DESC")
    List<ParkDocument> findCurrentlyValidByParkId(@Param("parkId") Long parkId, @Param("date") LocalDateTime date);

    /**
     * Find tariff documents for a park
     */
    @Query("SELECT doc FROM ParkDocument doc WHERE doc.park.id = :parkId AND (doc.documentType = 'TARIFF' OR doc.documentType = 'FEE_SCHEDULE') AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<ParkDocument> findTariffDocumentsByParkId(@Param("parkId") Long parkId);

    /**
     * Count documents for a park
     */
    @Query("SELECT COUNT(doc) FROM ParkDocument doc WHERE doc.park.id = :parkId")
    long countByParkId(@Param("parkId") Long parkId);

    /**
     * Count active documents for a park
     */
    @Query("SELECT COUNT(doc) FROM ParkDocument doc WHERE doc.park.id = :parkId AND doc.isActive = true")
    long countActiveByParkId(@Param("parkId") Long parkId);

    /**
     * Check if filename exists
     */
    boolean existsByFileName(String fileName);

    /**
     * Find document by filename
     */
    Optional<ParkDocument> findByFileName(String fileName);

    /**
     * Find documents with pagination
     */
    @Query("SELECT doc FROM ParkDocument doc WHERE doc.park.id = :parkId")
    Page<ParkDocument> findByParkIdPaginated(@Param("parkId") Long parkId, Pageable pageable);

    /**
     * Find all documents by type across all parks (for admin/reporting)
     */
    @Query("SELECT doc FROM ParkDocument doc WHERE doc.documentType = :documentType AND doc.isActive = true ORDER BY doc.createdAt DESC")
    List<ParkDocument> findAllByDocumentType(@Param("documentType") DocumentType documentType);

    /**
     * Find expired documents (validTo in the past)
     */
    @Query("SELECT doc FROM ParkDocument doc WHERE doc.isActive = true AND doc.validTo IS NOT NULL AND doc.validTo < :date ORDER BY doc.validTo ASC")
    List<ParkDocument> findExpiredDocuments(@Param("date") LocalDateTime date);

    /**
     * Find documents expiring soon (within given days)
     */
    @Query("SELECT doc FROM ParkDocument doc WHERE doc.isActive = true AND doc.validTo IS NOT NULL AND doc.validTo BETWEEN :startDate AND :endDate ORDER BY doc.validTo ASC")
    List<ParkDocument> findDocumentsExpiringSoon(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT doc.id FROM ParkDocument doc WHERE doc.id > :currentId ORDER BY doc.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT doc.id FROM ParkDocument doc WHERE doc.id < :currentId ORDER BY doc.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT doc.id FROM ParkDocument doc ORDER BY doc.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT doc.id FROM ParkDocument doc ORDER BY doc.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
