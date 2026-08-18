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

    /**
     * Several statuses at once — OR inside the dimension, as every other list here does.
     *
     * "Confirmed or sent" is one question (what is out with a customer and not yet used); the single
     * `status` param could only ever ask half of it.
     */
    public static Specification<CreditNote> statusIn(java.util.List<CreditNoteStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) return null;
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    /**
     * Free text over the four things somebody remembers: the code, the title, the customer, and the
     * invoice it credits. Joined, so `query.distinct(true)` — otherwise a note joins twice and
     * appears twice.
     */
    public static Specification<CreditNote> keyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        String like = "%" + keyword.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            if (query != null) query.distinct(true);
            var customer = root.join("customer", jakarta.persistence.criteria.JoinType.LEFT);
            var invoice = root.join("invoice", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.or(
                cb.like(cb.lower(root.get("creditNoteCode")), like),
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(cb.coalesce(root.get("reason"), "")), like),
                cb.like(cb.lower(cb.coalesce(customer.get("firstName"), "")), like),
                cb.like(cb.lower(cb.coalesce(customer.get("lastName"), "")), like),
                cb.like(cb.lower(cb.coalesce(customer.get("companyName"), "")), like),
                cb.like(cb.lower(cb.coalesce(invoice.get("invoiceCode"), "")), like));
        };
    }

    /**
     * Stages, rather than states: "outstanding" is CONFIRMED or SENT, and the office asks about it
     * as one thing.
     *
     * It exists as its own dimension because a stat card is a filter here, and a card whose value
     * was the comma-joined pair could be applied but never be seen as applied — the active check
     * splits the URL value on commas and would never match it back.
     */
    public static Specification<CreditNote> statusGroupIn(java.util.List<String> groups) {
        if (groups == null || groups.isEmpty()) return null;
        java.util.Set<CreditNoteStatus> wanted = new java.util.LinkedHashSet<>();
        for (String group : groups) {
            if (group == null) continue;
            switch (group.trim().toLowerCase()) {
                case "draft" -> wanted.add(CreditNoteStatus.DRAFT);
                case "outstanding" -> {
                    wanted.add(CreditNoteStatus.CONFIRMED);
                    wanted.add(CreditNoteStatus.SENT);
                }
                case "settled" -> wanted.add(CreditNoteStatus.CONSUMED);
                default -> { /* an unknown group narrows nothing rather than emptying the list */ }
            }
        }
        if (wanted.isEmpty()) return null;
        return (root, query, cb) -> root.get("status").in(wanted);
    }

    /** Confirmed or sent and not yet consumed — money the customer is still owed. */
    public static Specification<CreditNote> outstanding() {
        return (root, query, cb) -> root.get("status").in(
            java.util.List.of(CreditNoteStatus.CONFIRMED, CreditNoteStatus.SENT));
    }

    /**
     * Issued on or after a date — the same column and the same comparison the `issueDateFrom`
     * filter uses.
     *
     * It read `createdAt` first, which made the "issued lately" card count one set while its own
     * filter showed another. A card is a filter here, so the two cannot be allowed to disagree.
     */
    public static Specification<CreditNote> issuedFrom(LocalDate from) {
        if (from == null) return null;
        return byIssueDateFrom(from);
    }
}
