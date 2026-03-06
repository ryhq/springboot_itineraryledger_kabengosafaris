package com.itineraryledger.kabengosafaris.Safari.Repository;

import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SafariDocument entity operations.
 */
@Repository
public interface SafariDocumentRepository extends JpaRepository<SafariDocument, Long>, JpaSpecificationExecutor<SafariDocument> {

    List<SafariDocument> findBySafariIdOrderByCreatedAtDesc(Long safariId);

    List<SafariDocument> findBySafariIdAndIsActiveOrderByCreatedAtDesc(Long safariId, Boolean isActive);

    List<SafariDocument> findBySafariIdAndDocumentTypeOrderByCreatedAtDesc(Long safariId, DocumentType documentType);

    List<SafariDocument> findBySafariIdAndIsGeneratedOrderByCreatedAtDesc(Long safariId, Boolean isGenerated);

    Optional<SafariDocument> findByFileName(String fileName);

    @Query("SELECT sd FROM SafariDocument sd WHERE sd.safari.id = :safariId " +
           "AND sd.isActive = true " +
           "AND (sd.validFrom IS NULL OR sd.validFrom <= :date) " +
           "AND (sd.validTo IS NULL OR sd.validTo >= :date)")
    List<SafariDocument> findCurrentlyValidBySafariId(@Param("safariId") Long safariId, @Param("date") LocalDateTime date);

    @Query("SELECT sd FROM SafariDocument sd WHERE sd.safari.id = :safariId " +
           "AND sd.documentType IN :types " +
           "AND sd.isActive = true " +
           "ORDER BY sd.createdAt DESC")
    List<SafariDocument> findBySafariIdAndDocumentTypes(
        @Param("safariId") Long safariId,
        @Param("types") List<DocumentType> types
    );

    @Query("SELECT COUNT(sd) FROM SafariDocument sd WHERE sd.safari.id = :safariId")
    long countBySafariId(@Param("safariId") Long safariId);

    @Query("SELECT COUNT(sd) FROM SafariDocument sd WHERE sd.safari.id = :safariId AND sd.isActive = true")
    long countActiveBySafariId(@Param("safariId") Long safariId);

    @Query("SELECT COUNT(sd) FROM SafariDocument sd WHERE sd.safari.id = :safariId AND sd.isGenerated = true")
    long countGeneratedBySafariId(@Param("safariId") Long safariId);

    boolean existsBySafariIdAndFileName(Long safariId, String fileName);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT sd.id FROM SafariDocument sd WHERE sd.id > :currentId ORDER BY sd.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT sd.id FROM SafariDocument sd WHERE sd.id < :currentId ORDER BY sd.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT sd.id FROM SafariDocument sd ORDER BY sd.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT sd.id FROM SafariDocument sd ORDER BY sd.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
