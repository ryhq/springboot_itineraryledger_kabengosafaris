package com.itineraryledger.kabengosafaris.Expense.Specifications;

import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseCategory;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

public class ExpenseSpecification {

    private ExpenseSpecification() {}

    public static Specification<Expense> byExpenseCode(String code) {
        return (root, query, cb) -> {
            if (code == null || code.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("expenseCode")), "%" + code.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Expense> byTitle(String title) {
        return (root, query, cb) -> {
            if (title == null || title.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Expense> byStatus(ExpenseStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Expense> byStatusIn(List<ExpenseStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) return cb.conjunction();
            return root.get("status").in(statuses);
        };
    }

    public static Specification<Expense> byVendorId(Long vendorId) {
        return (root, query, cb) -> vendorId == null ? cb.conjunction() : cb.equal(root.get("vendor").get("id"), vendorId);
    }

    public static Specification<Expense> bySafariId(Long safariId) {
        return (root, query, cb) -> safariId == null ? cb.conjunction() : cb.equal(root.get("safari").get("id"), safariId);
    }

    public static Specification<Expense> bySafariIsNull(Boolean operationalOnly) {
        return (root, query, cb) -> {
            if (operationalOnly == null) return cb.conjunction();
            return operationalOnly ? cb.isNull(root.get("safari")) : cb.isNotNull(root.get("safari"));
        };
    }

    public static Specification<Expense> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null ? cb.conjunction() : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<Expense> byCreatedById(Long createdById) {
        return (root, query, cb) -> createdById == null ? cb.conjunction() : cb.equal(root.get("createdBy").get("id"), createdById);
    }

    public static Specification<Expense> byExpenseDateAfter(LocalDate date) {
        return (root, query, cb) -> date == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("expenseDate"), date);
    }

    public static Specification<Expense> byExpenseDateBefore(LocalDate date) {
        return (root, query, cb) -> date == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("expenseDate"), date);
    }

    public static Specification<Expense> byDueDateAfter(LocalDate date) {
        return (root, query, cb) -> date == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("dueDate"), date);
    }

    public static Specification<Expense> byDueDateBefore(LocalDate date) {
        return (root, query, cb) -> date == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("dueDate"), date);
    }

    public static Specification<Expense> byOverdue() {
        return (root, query, cb) -> {
            LocalDate today = LocalDate.now();
            Predicate datePast = cb.lessThan(root.get("dueDate"), today);
            Predicate notFinal = cb.not(root.get("status").in(ExpenseStatus.PAID, ExpenseStatus.CANCELLED));
            return cb.and(datePast, cb.isNotNull(root.get("dueDate")), notFinal);
        };
    }

    public static Specification<Expense> byUnpaid() {
        return (root, query, cb) ->
            root.get("status").in(ExpenseStatus.DRAFT, ExpenseStatus.RECORDED, ExpenseStatus.PARTIALLY_PAID);
    }

    public static Specification<Expense> byReferenceNumber(String reference) {
        return (root, query, cb) -> {
            if (reference == null || reference.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("referenceNumber")), "%" + reference.toLowerCase().trim() + "%");
        };
    }

    /**
     * Any of these categories, asked of the LINE ITEMS.
     *
     * Category is a property of a line, not of a bill: one lodge invoice can
     * carry a room charge and a park fee. So this means "has at least one line of
     * that kind", and it joins — hence distinct, or a two-line bill would be
     * returned twice.
     */
    public static Specification<Expense> byCategories(java.util.List<ExpenseCategory> categories) {
        return (root, query, cb) -> {
            if (categories == null || categories.isEmpty()) return cb.conjunction();
            if (query != null) query.distinct(true);
            var lines = root.join("lineItems", jakarta.persistence.criteria.JoinType.LEFT);
            return lines.get("category").in(categories);
        };
    }

    public static Specification<Expense> byStatuses(java.util.List<ExpenseStatus> statuses) {
        return (root, query, cb) -> statuses == null || statuses.isEmpty()
            ? cb.conjunction()
            : root.get("status").in(statuses);
    }

    /** Falls due within the window and is not settled — the pay-this-week list. */
    public static Specification<Expense> dueWithin(int days) {
        return (root, query, cb) -> {
            LocalDate today = LocalDate.now();
            return cb.and(
                cb.isNotNull(root.get("dueDate")),
                cb.between(root.get("dueDate"), today, today.plusDays(days)),
                cb.not(root.get("status").in(ExpenseStatus.PAID, ExpenseStatus.CANCELLED)));
        };
    }

    /**
     * Free text over the code, the title, the reference and the vendor's name.
     *
     * The vendor is joined because that is how a bill is looked up — by who sent
     * it. LEFT join: a bill with no vendor yet must still be findable.
     */
    public static Specification<Expense> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            if (query != null) query.distinct(true);
            var vendor = root.join("vendor", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.or(
                cb.like(cb.lower(root.get("expenseCode")), like),
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("referenceNumber")), like),
                cb.like(cb.lower(vendor.get("name")), like),
                cb.like(cb.lower(vendor.get("code")), like));
        };
    }

    public static Specification<Expense> createdAfter(java.time.LocalDateTime since) {
        return (root, query, cb) -> since == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }
}
