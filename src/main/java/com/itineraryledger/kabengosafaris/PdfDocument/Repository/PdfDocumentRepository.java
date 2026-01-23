package com.itineraryledger.kabengosafaris.PdfDocument.Repository;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
