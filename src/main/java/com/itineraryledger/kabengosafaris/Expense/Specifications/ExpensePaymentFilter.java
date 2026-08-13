package com.itineraryledger.kabengosafaris.Expense.Specifications;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;

import lombok.Data;

/**
 * Everything a caller can narrow the outgoing-payments list by, in one object.
 *
 * The rows, the stat cards and the record walk are all built from this, so a card
 * cannot report a figure the table would contradict.
 */
@Data
public class ExpensePaymentFilter {

    /** Free text across the reference, the notes, the bill code and the vendor. */
    private String keyword;

    private String reference;

    private PaymentMethod paymentMethod;
    private List<PaymentMethod> paymentMethods;

    /** Obfuscated ids, as the list page sends them. */
    private String expenseId;
    private String vendorId;
    private String safariId;
    private String bankAccountId;

    /** What came in, as it was received — not what the bill was written in. */
    private List<String> currencies;

    private LocalDate paidAfter;
    private LocalDate paidBefore;

    /**
     * Things worth checking.
     *
     * crossCurrency — paid in a currency the bill was not written in, so
     * somebody applied a rate that is worth a second look. noBankAccount — nothing
     * says where the money landed, which makes it unreconcilable against a
     * statement.
     */
    private List<String> qualities;

    public List<PaymentMethod> allMethods() {
        List<PaymentMethod> out = new ArrayList<>();
        if (paymentMethods != null) paymentMethods.stream().filter(Objects::nonNull).forEach(out::add);
        if (paymentMethod != null && !out.contains(paymentMethod)) out.add(paymentMethod);
        return out;
    }

    public boolean wants(String quality) {
        return qualities != null && qualities.contains(quality);
    }
}
