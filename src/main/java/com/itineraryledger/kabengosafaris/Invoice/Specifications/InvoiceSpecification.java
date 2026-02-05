package com.itineraryledger.kabengosafaris.Invoice.Specifications;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentStatus;

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

    public static Specification<Invoice> byPaymentStatus(PaymentStatus paymentStatus) {
        return (root, query, cb) -> paymentStatus == null
            ? cb.conjunction()
            : cb.equal(root.get("paymentStatus"), paymentStatus);
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

    public static Specification<Invoice> byOverdue() {
        return (root, query, cb) -> {
            LocalDate today = LocalDate.now();
            return cb.and(
                cb.lessThan(root.get("dueDate"), today),
                cb.notEqual(root.get("paymentStatus"), PaymentStatus.PAID),
                cb.notEqual(root.get("paymentStatus"), PaymentStatus.REFUNDED)
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
            .value(InvoiceStatus.VIEWED)
            .value(InvoiceStatus.OVERDUE);
    }

    public static Specification<Invoice> byPaidStatuses() {
        return (root, query, cb) -> cb.in(root.get("status"))
            .value(InvoiceStatus.PAID)
            .value(InvoiceStatus.PARTIALLY_PAID);
    }

    public static Specification<Invoice> byClosedStatuses() {
        return (root, query, cb) -> cb.in(root.get("status"))
            .value(InvoiceStatus.CANCELLED)
            .value(InvoiceStatus.REFUNDED);
    }
}
