package com.itineraryledger.kabengosafaris.ParkActivity.Repositories;

import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityDocument;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityDocument.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ParkActivityDocument entity operations.
 * Queries use composite key (parkId + activityId) to identify park-activity relationships.
 */
@Repository
public interface ParkActivityDocumentRepository extends JpaRepository<ParkActivityDocument, Long>, JpaSpecificationExecutor<ParkActivityDocument> {

    /**
     * Find all documents for a specific park-activity relationship
     */
    @Query("SELECT pad FROM ParkActivityDocument pad " +
           "WHERE pad.parkActivity.park.id = :parkId " +
           "AND pad.parkActivity.activity.id = :activityId " +
           "ORDER BY pad.createdAt DESC")
    List<ParkActivityDocument> findByParkActivityOrderByCreatedAtDesc(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId
    );

    /**
     * Find active documents for a specific park-activity relationship
     */
    @Query("SELECT pad FROM ParkActivityDocument pad " +
           "WHERE pad.parkActivity.park.id = :parkId " +
           "AND pad.parkActivity.activity.id = :activityId " +
           "AND pad.isActive = :isActive " +
           "ORDER BY pad.createdAt DESC")
    List<ParkActivityDocument> findByParkActivityAndIsActiveOrderByCreatedAtDesc(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId,
        @Param("isActive") Boolean isActive
    );

    /**
     * Find documents by type for a specific park-activity relationship
     */
    @Query("SELECT pad FROM ParkActivityDocument pad " +
           "WHERE pad.parkActivity.park.id = :parkId " +
           "AND pad.parkActivity.activity.id = :activityId " +
           "AND pad.documentType = :documentType " +
           "ORDER BY pad.createdAt DESC")
    List<ParkActivityDocument> findByParkActivityAndDocumentTypeOrderByCreatedAtDesc(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId,
        @Param("documentType") DocumentType documentType
    );

    /**
     * Find all documents for a specific park
     */
    @Query("SELECT pad FROM ParkActivityDocument pad " +
           "WHERE pad.parkActivity.park.id = :parkId " +
           "ORDER BY pad.createdAt DESC")
    List<ParkActivityDocument> findByParkIdOrderByCreatedAtDesc(@Param("parkId") Long parkId);

    /**
     * Find all documents for a specific activity (across all parks)
     */
    @Query("SELECT pad FROM ParkActivityDocument pad " +
           "WHERE pad.parkActivity.activity.id = :activityId " +
           "ORDER BY pad.createdAt DESC")
    List<ParkActivityDocument> findByActivityIdOrderByCreatedAtDesc(@Param("activityId") Long activityId);

    /**
     * Find document by filename
     */
    Optional<ParkActivityDocument> findByFileName(String fileName);

    /**
     * Find currently valid documents for a park-activity relationship
     */
    @Query("SELECT pad FROM ParkActivityDocument pad " +
           "WHERE pad.parkActivity.park.id = :parkId " +
           "AND pad.parkActivity.activity.id = :activityId " +
           "AND pad.isActive = true " +
           "AND (pad.validFrom IS NULL OR pad.validFrom <= :date) " +
           "AND (pad.validTo IS NULL OR pad.validTo >= :date)")
    List<ParkActivityDocument> findCurrentlyValidByParkActivity(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId,
        @Param("date") LocalDateTime date
    );

    /**
     * Find documents by multiple types for a park-activity relationship
     */
    @Query("SELECT pad FROM ParkActivityDocument pad " +
           "WHERE pad.parkActivity.park.id = :parkId " +
           "AND pad.parkActivity.activity.id = :activityId " +
           "AND pad.documentType IN :types " +
           "AND pad.isActive = true " +
           "ORDER BY pad.createdAt DESC")
    List<ParkActivityDocument> findByParkActivityAndDocumentTypes(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId,
        @Param("types") List<DocumentType> types
    );

    /**
     * Count documents for a park-activity relationship
     */
    @Query("SELECT COUNT(pad) FROM ParkActivityDocument pad " +
           "WHERE pad.parkActivity.park.id = :parkId " +
           "AND pad.parkActivity.activity.id = :activityId")
    long countByParkActivity(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId
    );

    /**
     * Count active documents for a park-activity relationship
     */
    @Query("SELECT COUNT(pad) FROM ParkActivityDocument pad " +
           "WHERE pad.parkActivity.park.id = :parkId " +
           "AND pad.parkActivity.activity.id = :activityId " +
           "AND pad.isActive = true")
    long countActiveByParkActivity(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId
    );

    /**
     * Check if a document with filename exists for a park-activity relationship
     */
    @Query("SELECT COUNT(pad) > 0 FROM ParkActivityDocument pad " +
           "WHERE pad.parkActivity.park.id = :parkId " +
           "AND pad.parkActivity.activity.id = :activityId " +
           "AND pad.fileName = :fileName")
    boolean existsByParkActivityAndFileName(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId,
        @Param("fileName") String fileName
    );
}
