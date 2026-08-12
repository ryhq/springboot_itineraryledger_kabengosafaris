package com.itineraryledger.kabengosafaris.Expense.Specifications;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseDocument;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseDocument.DocumentType;

/**
 * Filters for the expense-document list, matching the other document modules.
 *
 * The Type facet is multi-value because the two halves of the paper trail are
 * asked about together — "the invoices they sent" is four types, "what we sent
 * back once paid" is five — and a singular enum param cannot answer either.
 */
public class ExpenseDocumentSpecification {

    public static Specification<ExpenseDocument> byExpenseId(Long expenseId) {
        return (root, query, cb) -> expenseId == null
            ? cb.conjunction()
            : cb.equal(root.get("expense").get("id"), expenseId);
    }

    public static Specification<ExpenseDocument> byVendorId(Long vendorId) {
        return (root, query, cb) -> vendorId == null
            ? cb.conjunction()
            : cb.equal(root.get("expense").get("vendor").get("id"), vendorId);
    }

    public static Specification<ExpenseDocument> bySafariId(Long safariId) {
        return (root, query, cb) -> safariId == null
            ? cb.conjunction()
            : cb.equal(root.get("expense").get("safari").get("id"), safariId);
    }

    public static Specification<ExpenseDocument> byDocumentType(DocumentType type) {
        return (root, query, cb) -> type == null
            ? cb.conjunction()
            : cb.equal(root.get("documentType"), type);
    }

    public static Specification<ExpenseDocument> byDocumentTypes(List<DocumentType> types) {
        return (root, query, cb) -> types == null || types.isEmpty()
            ? cb.conjunction()
            : root.get("documentType").in(types);
    }

    public static Specification<ExpenseDocument> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    /**
     * Proof of a payment, rather than what the supplier asked for.
     *
     * The link to a payment is exactly what makes a document proof OF something,
     * so it is worth filtering by: "which of these bills has a slip behind it".
     */
    public static Specification<ExpenseDocument> byIsProofOfPayment(Boolean proof) {
        return (root, query, cb) -> proof == null
            ? cb.conjunction()
            : Boolean.TRUE.equals(proof)
                ? cb.isNotNull(root.get("expensePayment"))
                : cb.isNull(root.get("expensePayment"));
    }

    public static Specification<ExpenseDocument> byTitleContains(String title) {
        return (root, query, cb) -> title == null || title.isBlank()
            ? cb.conjunction()
            : cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
    }

    public static Specification<ExpenseDocument> byVersion(String version) {
        return (root, query, cb) -> version == null || version.isBlank()
            ? cb.conjunction()
            : cb.equal(cb.lower(root.get("version")), version.toLowerCase().trim());
    }

    /* Validity counters, each one clickable as a filter. */

    public static Specification<ExpenseDocument> expired() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("validTo")),
            cb.lessThan(root.get("validTo"), LocalDateTime.now()));
    }

    public static Specification<ExpenseDocument> expiringWithin(int days) {
        return (root, query, cb) -> {
            LocalDateTime now = LocalDateTime.now();
            return cb.and(
                cb.isNotNull(root.get("validTo")),
                cb.greaterThanOrEqualTo(root.get("validTo"), now),
                cb.lessThanOrEqualTo(root.get("validTo"), now.plusDays(days)));
        };
    }

    public static Specification<ExpenseDocument> noExpiry() {
        return (root, query, cb) -> cb.isNull(root.get("validTo"));
    }

    public static Specification<ExpenseDocument> byCurrentlyValid(LocalDateTime at) {
        return (root, query, cb) -> cb.and(
            cb.or(cb.isNull(root.get("validFrom")), cb.lessThanOrEqualTo(root.get("validFrom"), at)),
            cb.or(cb.isNull(root.get("validTo")), cb.greaterThanOrEqualTo(root.get("validTo"), at)));
    }

    public static Specification<ExpenseDocument> createdAfter(LocalDateTime since) {
        return (root, query, cb) -> since == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }

    /**
     * The one search box: the title, the notes, either filename, their document
     * number, and the bill's own code — because "the slip for EXP-000104" is how
     * somebody looks for it.
     */
    public static Specification<ExpenseDocument> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            if (query != null) query.distinct(true);
            var expense = root.join("expense", jakarta.persistence.criteria.JoinType.LEFT);
            var vendor = expense.join("vendor", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("description")), like),
                cb.like(cb.lower(root.get("fileName")), like),
                cb.like(cb.lower(root.get("originalFileName")), like),
                cb.like(cb.lower(root.get("documentNumber")), like),
                cb.like(cb.lower(expense.get("expenseCode")), like),
                cb.like(cb.lower(vendor.get("name")), like));
        };
    }
}
