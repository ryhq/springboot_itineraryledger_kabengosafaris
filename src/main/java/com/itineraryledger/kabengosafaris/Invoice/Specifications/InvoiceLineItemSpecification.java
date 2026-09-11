package com.itineraryledger.kabengosafaris.Invoice.Specifications;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceLineItem;

/**
 * JPA Specifications for InvoiceLineItem filtering.
 */
public class InvoiceLineItemSpecification {

    // ========================
    // BASIC FILTERS
    // ========================

    public static Specification<InvoiceLineItem> byInvoiceId(Long invoiceId) {
        return (root, query, cb) -> invoiceId == null
            ? cb.conjunction()
            : cb.equal(root.get("invoice").get("id"), invoiceId);
    }

    public static Specification<InvoiceLineItem> byItemName(String itemName) {
        return (root, query, cb) -> {
            if (itemName == null || itemName.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("itemName")), "%" + itemName.toLowerCase().trim() + "%");
        };
    }

    public static Specification<InvoiceLineItem> byDescription(String description) {
        return (root, query, cb) -> {
            if (description == null || description.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase().trim() + "%");
        };
    }

    public static Specification<InvoiceLineItem> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    /**
     * Search by keyword across item name and description
     */
    public static Specification<InvoiceLineItem> searchByKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("itemName")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    /**
     * The free-text search, over what the line items listing shows.
     *
     * <p>The tab has a search box and this module had no keyword parameter, so the box sent one
     * nothing read and every line came back.
     */
    public static Specification<InvoiceLineItem> matchesKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String like = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("itemName")), like),
                cb.like(cb.lower(root.get("description")), like)
            );
        };
    }

}
