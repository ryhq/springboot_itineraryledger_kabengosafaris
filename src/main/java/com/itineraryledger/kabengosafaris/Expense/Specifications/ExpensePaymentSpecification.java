package com.itineraryledger.kabengosafaris.Expense.Specifications;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Expense.Entity.ExpensePayment;
import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;

/**
 * Filters for money going out, across every bill.
 *
 * The per-bill view answers "what have we paid this supplier". These answer the
 * questions asked of a bank statement instead: what went out this week, from
 * which account, and which of it needs checking.
 */
public class ExpensePaymentSpecification {

    public static Specification<ExpensePayment> byExpenseId(Long expenseId) {
        return (root, query, cb) -> expenseId == null
            ? cb.conjunction()
            : cb.equal(root.get("expense").get("id"), expenseId);
    }

    public static Specification<ExpensePayment> byVendorId(Long vendorId) {
        return (root, query, cb) -> vendorId == null
            ? cb.conjunction()
            : cb.equal(root.get("expense").get("vendor").get("id"), vendorId);
    }

    public static Specification<ExpensePayment> bySafariId(Long safariId) {
        return (root, query, cb) -> safariId == null
            ? cb.conjunction()
            : cb.equal(root.get("expense").get("safari").get("id"), safariId);
    }

    public static Specification<ExpensePayment> byBankAccountId(Long bankAccountId) {
        return (root, query, cb) -> bankAccountId == null
            ? cb.conjunction()
            : cb.equal(root.get("bankAccount").get("id"), bankAccountId);
    }

    public static Specification<ExpensePayment> byMethod(PaymentMethod method) {
        return (root, query, cb) -> method == null
            ? cb.conjunction()
            : cb.equal(root.get("paymentMethod"), method);
    }

    public static Specification<ExpensePayment> byMethods(List<PaymentMethod> methods) {
        return (root, query, cb) -> methods == null || methods.isEmpty()
            ? cb.conjunction()
            : root.get("paymentMethod").in(methods);
    }

    public static Specification<ExpensePayment> byCurrencies(List<String> currencies) {
        return (root, query, cb) -> currencies == null || currencies.isEmpty()
            ? cb.conjunction()
            : cb.upper(root.get("currency")).in(currencies.stream().map(String::toUpperCase).toList());
    }

    public static Specification<ExpensePayment> paidAfter(LocalDate date) {
        return (root, query, cb) -> date == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("paymentDate"), date);
    }

    public static Specification<ExpensePayment> paidBefore(LocalDate date) {
        return (root, query, cb) -> date == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("paymentDate"), date);
    }

    /** Paid in the last N days, for the "what went out" cards. */
    public static Specification<ExpensePayment> paidWithin(int days) {
        return (root, query, cb) -> cb.between(
            root.get("paymentDate"), LocalDate.now().minusDays(days), LocalDate.now());
    }

    /**
     * Paid in a currency the bill was not written in.
     *
     * Somebody applied an exchange rate by hand, and a wrong one quietly changes
     * what we still owe the supplier — so it is worth being able to list them.
     */
    public static Specification<ExpensePayment> crossCurrency() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("expenseCurrency")),
            cb.notEqual(cb.upper(root.get("currency")), cb.upper(root.get("expenseCurrency"))));
    }

    /** Nothing says which account it left, so it cannot be reconciled. */
    public static Specification<ExpensePayment> noBankAccount() {
        return (root, query, cb) -> cb.isNull(root.get("bankAccount"));
    }

    public static Specification<ExpensePayment> byReference(String reference) {
        return (root, query, cb) -> reference == null || reference.isBlank()
            ? cb.conjunction()
            : cb.like(cb.lower(root.get("reference")), "%" + reference.toLowerCase().trim() + "%");
    }

    /**
     * The one search box: the reference, the notes, the bill's code and the
     * vendor's name — because a statement line is looked up by its reference and a
     * query from a supplier by their name.
     */
    public static Specification<ExpensePayment> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            if (query != null) query.distinct(true);
            var expense = root.join("expense", jakarta.persistence.criteria.JoinType.LEFT);
            var vendor = expense.join("vendor", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.or(
                cb.like(cb.lower(root.get("reference")), like),
                cb.like(cb.lower(root.get("notes")), like),
                cb.like(cb.lower(expense.get("expenseCode")), like),
                cb.like(cb.lower(expense.get("title")), like),
                cb.like(cb.lower(vendor.get("name")), like),
                cb.like(cb.lower(vendor.get("code")), like));
        };
    }

    public static Specification<ExpensePayment> createdAfter(java.time.LocalDateTime since) {
        return (root, query, cb) -> since == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }
}
