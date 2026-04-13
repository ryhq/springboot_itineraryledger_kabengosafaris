package com.itineraryledger.kabengosafaris.CreditNote.Specifications;

import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNote;
import com.itineraryledger.kabengosafaris.CreditNote.Enums.CreditNoteStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class CreditNoteSpecification {

    public static Specification<CreditNote> byCreditNoteCode(String code) {
        return (root, query, cb) -> {
            if (code == null || code.trim().isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("creditNoteCode")), "%" + code.toLowerCase().trim() + "%");
        };
    }

    public static Specification<CreditNote> byTitle(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<CreditNote> byStatus(CreditNoteStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<CreditNote> byInvoiceId(Long invoiceId) {
        return (root, query, cb) -> invoiceId == null ? cb.conjunction() : cb.equal(root.get("invoice").get("id"), invoiceId);
    }

    public static Specification<CreditNote> byCustomerId(Long customerId) {
        return (root, query, cb) -> customerId == null ? cb.conjunction() : cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<CreditNote> byIssueDateFrom(LocalDate from) {
        return (root, query, cb) -> from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("issueDate"), from);
    }

    public static Specification<CreditNote> byIssueDateTo(LocalDate to) {
        return (root, query, cb) -> to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("issueDate"), to);
    }

    public static Specification<CreditNote> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null ? cb.conjunction() : cb.equal(root.get("isActive"), isActive);
    }
}
