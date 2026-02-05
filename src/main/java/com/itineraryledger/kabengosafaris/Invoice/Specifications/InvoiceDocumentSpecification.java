package com.itineraryledger.kabengosafaris.Invoice.Specifications;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument.DocumentType;

/**
 * JPA Specifications for InvoiceDocument filtering.
 */
public class InvoiceDocumentSpecification {

    // ========================
    // DOCUMENT SPECIFICATIONS
    // ========================

    public static Specification<InvoiceDocument> byInvoiceId(Long invoiceId) {
        return (root, query, cb) -> invoiceId == null
            ? cb.conjunction()
            : cb.equal(root.get("invoice").get("id"), invoiceId);
    }

    public static Specification<InvoiceDocument> byDocumentType(DocumentType documentType) {
        return (root, query, cb) -> documentType == null
            ? cb.conjunction()
            : cb.equal(root.get("documentType"), documentType);
    }

    public static Specification<InvoiceDocument> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<InvoiceDocument> byIsGenerated(Boolean isGenerated) {
        return (root, query, cb) -> isGenerated == null
            ? cb.conjunction()
            : cb.equal(root.get("isGenerated"), isGenerated);
    }

    public static Specification<InvoiceDocument> byTitleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<InvoiceDocument> byVersion(String version) {
        return (root, query, cb) -> {
            if (version == null || version.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("version")), "%" + version.toLowerCase().trim() + "%");
        };
    }

    public static Specification<InvoiceDocument> byCurrentlyValid(LocalDateTime date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            return cb.and(
                cb.equal(root.get("isActive"), true),
                cb.or(
                    cb.isNull(root.get("validFrom")),
                    cb.lessThanOrEqualTo(root.get("validFrom"), date)
                ),
                cb.or(
                    cb.isNull(root.get("validTo")),
                    cb.greaterThanOrEqualTo(root.get("validTo"), date)
                )
            );
        };
    }

    // ========================
    // DOCUMENT TYPE GROUPS
    // ========================

    public static Specification<InvoiceDocument> byPrimaryDocuments() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.INVOICE_PDF),
            cb.equal(root.get("documentType"), DocumentType.TAX_INVOICE),
            cb.equal(root.get("documentType"), DocumentType.CONTRACT)
        );
    }

    public static Specification<InvoiceDocument> byPaymentDocuments() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.PAYMENT_RECEIPT),
            cb.equal(root.get("documentType"), DocumentType.PAYMENT_SCHEDULE),
            cb.equal(root.get("documentType"), DocumentType.PAYMENT_CONFIRMATION),
            cb.equal(root.get("documentType"), DocumentType.REFUND_RECEIPT),
            cb.equal(root.get("documentType"), DocumentType.CREDIT_NOTE),
            cb.equal(root.get("documentType"), DocumentType.DEBIT_NOTE)
        );
    }

    public static Specification<InvoiceDocument> byVoucherDocuments() {
        return (root, query, cb) -> cb.or(
            cb.equal(root.get("documentType"), DocumentType.ACCOMMODATION_VOUCHER),
            cb.equal(root.get("documentType"), DocumentType.ACTIVITY_VOUCHER),
            cb.equal(root.get("documentType"), DocumentType.TRANSPORT_VOUCHER)
        );
    }

    // ========================
    // INVOICE FILTERS
    // ========================

    public static Specification<InvoiceDocument> byInvoiceCode(String invoiceCode) {
        return (root, query, cb) -> {
            if (invoiceCode == null || invoiceCode.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("invoice").get("invoiceCode")), "%" + invoiceCode.toLowerCase().trim() + "%");
        };
    }

    public static Specification<InvoiceDocument> byCustomerId(Long customerId) {
        return (root, query, cb) -> customerId == null
            ? cb.conjunction()
            : cb.equal(root.get("invoice").get("customer").get("id"), customerId);
    }
}
