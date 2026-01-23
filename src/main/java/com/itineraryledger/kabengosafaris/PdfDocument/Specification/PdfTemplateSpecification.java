package com.itineraryledger.kabengosafaris.PdfDocument.Specification;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfTemplate;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for PdfTemplate queries
 */
public class PdfTemplateSpecification {

    /**
     * Filter by PDF document ID
     */
    public static Specification<PdfTemplate> pdfDocumentId(Long pdfDocumentId) {
        return (root, query, cb) -> {
            if (pdfDocumentId == null) {
                return null;
            }
            return cb.equal(root.get("pdfDocument").get("id"), pdfDocumentId);
        };
    }

    /**
     * Filter by enabled status
     */
    public static Specification<PdfTemplate> enabled(Boolean enabled) {
        return (root, query, cb) -> {
            if (enabled == null) {
                return null;
            }
            return cb.equal(root.get("enabled"), enabled);
        };
    }

    /**
     * Filter by default status
     */
    public static Specification<PdfTemplate> isDefault(Boolean isDefault) {
        return (root, query, cb) -> {
            if (isDefault == null) {
                return null;
            }
            return cb.equal(root.get("isDefault"), isDefault);
        };
    }

    /**
     * Filter by system default status
     */
    public static Specification<PdfTemplate> isSystemDefault(Boolean isSystemDefault) {
        return (root, query, cb) -> {
            if (isSystemDefault == null) {
                return null;
            }
            return cb.equal(root.get("isSystemDefault"), isSystemDefault);
        };
    }

    /**
     * Filter by name (case-insensitive partial match)
     */
    public static Specification<PdfTemplate> nameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Filter by paper size
     */
    public static Specification<PdfTemplate> paperSize(String paperSize) {
        return (root, query, cb) -> {
            if (paperSize == null || paperSize.isBlank()) {
                return null;
            }
            return cb.equal(root.get("paperSize").as(String.class), paperSize);
        };
    }

    /**
     * Filter by orientation
     */
    public static Specification<PdfTemplate> orientation(String orientation) {
        return (root, query, cb) -> {
            if (orientation == null || orientation.isBlank()) {
                return null;
            }
            return cb.equal(root.get("orientation").as(String.class), orientation);
        };
    }
}
