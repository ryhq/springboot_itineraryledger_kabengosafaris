package com.itineraryledger.kabengosafaris.Itinerary.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ItineraryDocument entity operations.
 */
@Repository
public interface ItineraryDocumentRepository extends JpaRepository<ItineraryDocument, Long>, JpaSpecificationExecutor<ItineraryDocument> {

    List<ItineraryDocument> findByItineraryIdOrderByCreatedAtDesc(Long itineraryId);

    List<ItineraryDocument> findByItineraryIdAndIsActiveOrderByCreatedAtDesc(Long itineraryId, Boolean isActive);

    List<ItineraryDocument> findByItineraryIdAndDocumentTypeOrderByCreatedAtDesc(Long itineraryId, DocumentType documentType);

    List<ItineraryDocument> findByItineraryIdAndIsGeneratedOrderByCreatedAtDesc(Long itineraryId, Boolean isGenerated);

    Optional<ItineraryDocument> findByFileName(String fileName);

    @Query("SELECT id FROM ItineraryDocument id WHERE id.itinerary.id = :itineraryId " +
           "AND id.isActive = true " +
           "AND (id.validFrom IS NULL OR id.validFrom <= :date) " +
           "AND (id.validTo IS NULL OR id.validTo >= :date)")
    List<ItineraryDocument> findCurrentlyValidByItineraryId(@Param("itineraryId") Long itineraryId, @Param("date") LocalDateTime date);

    @Query("SELECT id FROM ItineraryDocument id WHERE id.itinerary.id = :itineraryId " +
           "AND id.documentType IN :types " +
           "AND id.isActive = true " +
           "ORDER BY id.createdAt DESC")
    List<ItineraryDocument> findByItineraryIdAndDocumentTypes(
        @Param("itineraryId") Long itineraryId,
        @Param("types") List<DocumentType> types
    );

    @Query("SELECT COUNT(id) FROM ItineraryDocument id WHERE id.itinerary.id = :itineraryId")
    long countByItineraryId(@Param("itineraryId") Long itineraryId);

    @Query("SELECT COUNT(id) FROM ItineraryDocument id WHERE id.itinerary.id = :itineraryId AND id.isActive = true")
    long countActiveByItineraryId(@Param("itineraryId") Long itineraryId);

    @Query("SELECT COUNT(id) FROM ItineraryDocument id WHERE id.itinerary.id = :itineraryId AND id.isGenerated = true")
    long countGeneratedByItineraryId(@Param("itineraryId") Long itineraryId);

    boolean existsByItineraryIdAndFileName(Long itineraryId, String fileName);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT d.id FROM ItineraryDocument d WHERE d.id > :currentId ORDER BY d.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT d.id FROM ItineraryDocument d WHERE d.id < :currentId ORDER BY d.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT d.id FROM ItineraryDocument d ORDER BY d.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT d.id FROM ItineraryDocument d ORDER BY d.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
