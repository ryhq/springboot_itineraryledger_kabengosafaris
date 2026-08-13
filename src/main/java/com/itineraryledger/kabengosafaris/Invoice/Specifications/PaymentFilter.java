package com.itineraryledger.kabengosafaris.Invoice.Specifications;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;

import lombok.Data;

/**
 * Everything a caller can narrow the payments list by, in one object.
 *
 * The rows, the stat cards and the record walk are all built from this, so a card
 * cannot report a figure the table would contradict.
 */
@Data
public class PaymentFilter {

    /** Free text across the reference, the notes, the invoice code and the customer. */
    private String keyword;

    private String reference;

    private PaymentMethod paymentMethod;
    private List<PaymentMethod> paymentMethods;

    /** Obfuscated ids, as the list page sends them. */
    private String invoiceId;
    private String customerId;
    private String safariId;
    private String bankAccountId;

    /** What came in, as it was received — not what the invoice was written in. */
    private List<String> currencies;

    private LocalDate paidAfter;
    private LocalDate paidBefore;

    /**
     * Things worth checking.
     *
     * crossCurrency — received in a currency the invoice was not written in, so
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
