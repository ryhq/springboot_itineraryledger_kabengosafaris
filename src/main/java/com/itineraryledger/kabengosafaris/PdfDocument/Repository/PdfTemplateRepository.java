package com.itineraryledger.kabengosafaris.PdfDocument.Repository;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PdfTemplate entity.
 */
@Repository
public interface PdfTemplateRepository extends JpaRepository<PdfTemplate, Long>, JpaSpecificationExecutor<PdfTemplate> {

    /**
     * Find all templates for a specific PDF document type
     */
    List<PdfTemplate> findByPdfDocumentId(Long pdfDocumentId);

    /**
     * Find a template by document ID and name
     */
    Optional<PdfTemplate> findByPdfDocumentIdAndName(Long pdfDocumentId, String name);

    /**
     * Find the default enabled template for a document type
     */
    Optional<PdfTemplate> findByPdfDocumentIdAndIsDefaultAndEnabled(Long pdfDocumentId, Boolean isDefault, Boolean enabled);

    /**
     * Check if a template with the given name exists for a document type
     */
    boolean existsByPdfDocumentIdAndName(Long pdfDocumentId, String name);

    /**
     * Check if a template with the given filename exists for a document type
     */
    boolean existsByPdfDocumentIdAndFileName(Long pdfDocumentId, String fileName);

    /**
     * Count templates for a document type
     */
    long countByPdfDocumentId(Long pdfDocumentId);

    /**
     * Check if a system default template exists for a document type
     */
    @Query("SELECT COUNT(t) > 0 FROM PdfTemplate t WHERE t.pdfDocument.id = :pdfDocumentId AND t.isSystemDefault = true")
    boolean hasSystemDefaultTemplate(@Param("pdfDocumentId") Long pdfDocumentId);

    /**
     * Find all enabled templates for a document type
     */
    List<PdfTemplate> findByPdfDocumentIdAndEnabled(Long pdfDocumentId, Boolean enabled);

    /**
     * Find system default template for a document type
     */
    Optional<PdfTemplate> findByPdfDocumentIdAndIsSystemDefault(Long pdfDocumentId, Boolean isSystemDefault);
}
