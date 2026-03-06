package com.itineraryledger.kabengosafaris.PdfDocument.Repository;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for PdfDocument entity.
 * PdfDocuments are built-in types that cannot be created/deleted via API.
 */
@Repository
public interface PdfDocumentRepository extends JpaRepository<PdfDocument, Long>, JpaSpecificationExecutor<PdfDocument> {

    /**
     * Find a PDF document by its unique name
     */
    Optional<PdfDocument> findByName(String name);

    /**
     * Check if a PDF document with the given name exists
     */
    boolean existsByName(String name);

    @Query("SELECT e.id FROM PdfDocument e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM PdfDocument e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM PdfDocument e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM PdfDocument e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
