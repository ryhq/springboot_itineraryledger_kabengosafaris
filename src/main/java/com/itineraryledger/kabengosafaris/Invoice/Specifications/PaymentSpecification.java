package com.itineraryledger.kabengosafaris.Invoice.Specifications;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;

/**
 * Filters for money coming in, across every invoice.
 *
 * The per-invoice view answers "what has this customer paid". These answer the
 * questions asked of a bank statement instead: what came in this week, into which
 * account, and which of it needs checking.
 */
public class PaymentSpecification {

    public static Specification<Payment> byInvoiceId(Long invoiceId) {
        return (root, query, cb) -> invoiceId == null
            ? cb.conjunction()
            : cb.equal(root.get("invoice").get("id"), invoiceId);
    }

    public static Specification<Payment> byCustomerId(Long customerId) {
        return (root, query, cb) -> customerId == null
            ? cb.conjunction()
            : cb.equal(root.get("invoice").get("customer").get("id"), customerId);
    }

    public static Specification<Payment> bySafariId(Long safariId) {
        return (root, query, cb) -> safariId == null
            ? cb.conjunction()
            : cb.equal(root.get("invoice").get("safari").get("id"), safariId);
    }

    public static Specification<Payment> byBankAccountId(Long bankAccountId) {
        return (root, query, cb) -> bankAccountId == null
            ? cb.conjunction()
            : cb.equal(root.get("bankAccount").get("id"), bankAccountId);
    }

    public static Specification<Payment> byMethod(PaymentMethod method) {
        return (root, query, cb) -> method == null
            ? cb.conjunction()
            : cb.equal(root.get("paymentMethod"), method);
    }

    public static Specification<Payment> byMethods(List<PaymentMethod> methods) {
        return (root, query, cb) -> methods == null || methods.isEmpty()
            ? cb.conjunction()
            : root.get("paymentMethod").in(methods);
    }

    public static Specification<Payment> byCurrencies(List<String> currencies) {
        return (root, query, cb) -> currencies == null || currencies.isEmpty()
            ? cb.conjunction()
            : cb.upper(root.get("currency")).in(currencies.stream().map(String::toUpperCase).toList());
    }

    public static Specification<Payment> paidAfter(LocalDate date) {
        return (root, query, cb) -> date == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("paymentDate"), date);
    }

    public static Specification<Payment> paidBefore(LocalDate date) {
        return (root, query, cb) -> date == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("paymentDate"), date);
    }

    /** Received in the last N days, for the "what came in" cards. */
    public static Specification<Payment> receivedWithin(int days) {
        return (root, query, cb) -> cb.between(
            root.get("paymentDate"), LocalDate.now().minusDays(days), LocalDate.now());
    }

    /**
     * Received in a currency the invoice was not written in.
     *
     * Somebody applied an exchange rate by hand, and a wrong one quietly changes
     * what the customer still owes — so it is worth being able to list them.
     */
    public static Specification<Payment> crossCurrency() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("invoiceCurrency")),
            cb.notEqual(cb.upper(root.get("currency")), cb.upper(root.get("invoiceCurrency"))));
    }

    /** Nothing says where the money landed, so it cannot be reconciled. */
    public static Specification<Payment> noBankAccount() {
        return (root, query, cb) -> cb.isNull(root.get("bankAccount"));
    }

    public static Specification<Payment> byReference(String reference) {
        return (root, query, cb) -> reference == null || reference.isBlank()
            ? cb.conjunction()
            : cb.like(cb.lower(root.get("reference")), "%" + reference.toLowerCase().trim() + "%");
    }

    /**
     * The one search box: the reference, the notes, the invoice's code and the
     * customer's name — because a statement line is looked up by its reference and
     * a query from a customer by their name.
     */
    public static Specification<Payment> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            if (query != null) query.distinct(true);
            var invoice = root.join("invoice", jakarta.persistence.criteria.JoinType.LEFT);
            var customer = invoice.join("customer", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.or(
                cb.like(cb.lower(root.get("reference")), like),
                cb.like(cb.lower(root.get("notes")), like),
                cb.like(cb.lower(invoice.get("invoiceCode")), like),
                cb.like(cb.lower(invoice.get("title")), like),
                cb.like(cb.lower(customer.get("firstName")), like),
                cb.like(cb.lower(customer.get("lastName")), like),
                cb.like(cb.lower(customer.get("companyName")), like));
        };
    }

    public static Specification<Payment> createdAfter(java.time.LocalDateTime since) {
        return (root, query, cb) -> since == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }
}
