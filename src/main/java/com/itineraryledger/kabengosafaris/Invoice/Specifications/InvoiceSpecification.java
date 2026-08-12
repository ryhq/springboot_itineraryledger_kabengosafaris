package com.itineraryledger.kabengosafaris.Invoice.Specifications;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;

/**
 * JPA Specifications for Invoice filtering.
 */
public class InvoiceSpecification {

    // ========================
    // BASIC FILTERS
    // ========================

    public static Specification<Invoice> byInvoiceCode(String invoiceCode) {
        return (root, query, cb) -> {
            if (invoiceCode == null || invoiceCode.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("invoiceCode")), "%" + invoiceCode.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Invoice> byTitle(String title) {
        return (root, query, cb) -> {
            if (title == null || title.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Invoice> byStatus(InvoiceStatus status) {
        return (root, query, cb) -> status == null
            ? cb.conjunction()
            : cb.equal(root.get("status"), status);
    }

    public static Specification<Invoice> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    // ========================
    // DATE FILTERS
    // ========================

    public static Specification<Invoice> byIssueDateAfter(LocalDate issueDate) {
        return (root, query, cb) -> issueDate == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("issueDate"), issueDate);
    }

    public static Specification<Invoice> byIssueDateBefore(LocalDate issueDate) {
        return (root, query, cb) -> issueDate == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("issueDate"), issueDate);
    }

    public static Specification<Invoice> byDueDateAfter(LocalDate dueDate) {
        return (root, query, cb) -> dueDate == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("dueDate"), dueDate);
    }

    public static Specification<Invoice> byDueDateBefore(LocalDate dueDate) {
        return (root, query, cb) -> dueDate == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("dueDate"), dueDate);
    }

    public static Specification<Invoice> bySentAfter(LocalDate sentDate) {
        return (root, query, cb) -> sentDate == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("sentDate"), sentDate);
    }

    public static Specification<Invoice> bySentBefore(LocalDate sentDate) {
        return (root, query, cb) -> sentDate == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("sentDate"), sentDate);
    }

    /**
     * Filter invoices that are overdue
     * An invoice is overdue if it's past the due date and not yet PAID or REFUNDED
     */
    public static Specification<Invoice> byOverdue() {
        return (root, query, cb) -> {
            LocalDate today = LocalDate.now();
            return cb.and(
                cb.lessThan(root.get("dueDate"), today),
                cb.notEqual(root.get("status"), InvoiceStatus.PAID),
                cb.notEqual(root.get("status"), InvoiceStatus.CANCELLED)
            );
        };
    }

    // ========================
    // RELATIONSHIP FILTERS
    // ========================

    public static Specification<Invoice> byCustomerId(Long customerId) {
        return (root, query, cb) -> customerId == null
            ? cb.conjunction()
            : cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Invoice> bySafariId(Long safariId) {
        return (root, query, cb) -> safariId == null
            ? cb.conjunction()
            : cb.equal(root.get("safari").get("id"), safariId);
    }

    public static Specification<Invoice> byCreatedById(Long createdById) {
        return (root, query, cb) -> createdById == null
            ? cb.conjunction()
            : cb.equal(root.get("createdBy").get("id"), createdById);
    }

    public static Specification<Invoice> byUpdatedById(Long updatedById) {
        return (root, query, cb) -> updatedById == null
            ? cb.conjunction()
            : cb.equal(root.get("updatedBy").get("id"), updatedById);
    }

    // ========================
    // STATUS GROUP FILTERS
    // ========================

    public static Specification<Invoice> byDraftStatus() {
        return (root, query, cb) -> cb.equal(root.get("status"), InvoiceStatus.DRAFT);
    }

    public static Specification<Invoice> byUnpaidStatuses() {
        return (root, query, cb) -> cb.in(root.get("status"))
            .value(InvoiceStatus.SENT)
            .value(InvoiceStatus.PARTIALLY_PAID)
            .value(InvoiceStatus.OVERDUE);
    }

    public static Specification<Invoice> byPaidStatuses() {
        return (root, query, cb) -> cb.in(root.get("status"))
            .value(InvoiceStatus.PAID)
            .value(InvoiceStatus.PARTIALLY_PAID);
    }

    public static Specification<Invoice> byClosedStatuses() {
        return (root, query, cb) -> cb.equal(root.get("status"), InvoiceStatus.CANCELLED);
    }

    /** One of several statuses — OR inside the dimension, as the facets expect. */
    public static Specification<Invoice> byStatuses(List<InvoiceStatus> statuses) {
        return (root, query, cb) -> statuses == null || statuses.isEmpty()
            ? cb.conjunction()
            : root.get("status").in(statuses);
    }

    /** The named group, or nothing constrained if the name means nothing. */
    public static Specification<Invoice> byStatusGroup(String group) {
        if (group == null) return (root, query, cb) -> cb.conjunction();
        return switch (group.toLowerCase()) {
            case "draft" -> byDraftStatus();
            case "unpaid" -> byUnpaidStatuses();
            case "paid" -> (root, query, cb) -> cb.equal(root.get("status"), InvoiceStatus.PAID);
            case "closed" -> byClosedStatuses();
            default -> (root, query, cb) -> cb.conjunction();
        };
    }

    /** Any of the named groups. */
    public static Specification<Invoice> byStatusGroups(List<String> groups) {
        if (groups == null || groups.isEmpty()) return (root, query, cb) -> cb.conjunction();
        Specification<Invoice> any = null;
        for (String group : groups) {
            Specification<Invoice> one = byStatusGroup(group);
            any = any == null ? one : any.or(one);
        }
        return any;
    }

    /**
     * Free text over the code, the title, and the names on either side of it.
     *
     * The customer and the safari are joined because that is how the office looks
     * an invoice up — by whose it is and which trip it was for, rarely by its
     * code. LEFT joins: an invoice with no safari must still be findable.
     */
    public static Specification<Invoice> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            if (query != null) query.distinct(true);
            var customer = root.join("customer", jakarta.persistence.criteria.JoinType.LEFT);
            var safari = root.join("safari", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.or(
                cb.like(cb.lower(root.get("invoiceCode")), like),
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(customer.get("firstName")), like),
                cb.like(cb.lower(customer.get("lastName")), like),
                cb.like(cb.lower(customer.get("companyName")), like),
                cb.like(cb.lower(customer.get("code")), like),
                cb.like(cb.lower(safari.get("name")), like),
                cb.like(cb.lower(safari.get("code")), like)
            );
        };
    }

    /** A second invoice for a trip that changed after it was billed. */
    public static Specification<Invoice> bySupplement(Boolean isSupplement) {
        return (root, query, cb) -> isSupplement == null
            ? cb.conjunction()
            : cb.equal(root.get("isSupplement"), isSupplement);
    }

    /** Falls due within the window and is not settled — the chase list. */
    public static Specification<Invoice> dueWithin(int days) {
        return (root, query, cb) -> {
            LocalDate today = LocalDate.now();
            return cb.and(
                cb.between(root.get("dueDate"), today, today.plusDays(days)),
                cb.notEqual(root.get("status"), InvoiceStatus.PAID),
                cb.notEqual(root.get("status"), InvoiceStatus.CANCELLED)
            );
        };
    }

    /** Prepared and never sent. Nothing is owed until somebody is asked. */
    public static Specification<Invoice> unsent() {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("status"), InvoiceStatus.DRAFT),
            cb.isNull(root.get("sentDate"))
        );
    }

    /** Money still outstanding: sent, part paid or overdue. */
    public static Specification<Invoice> unpaid() {
        return byUnpaidStatuses();
    }

    public static Specification<Invoice> createdAfter(java.time.LocalDateTime when) {
        return (root, query, cb) -> when == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), when);
    }
}
