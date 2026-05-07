package com.itineraryledger.kabengosafaris.Expense.Specifications;

import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
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
}
