package com.itineraryledger.kabengosafaris.BankAccount.Specifications;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Everything a caller can narrow the bank-account list by, in one object.
 *
 * A small list — an operator has a handful of accounts — so this is less about
 * finding one than about the counters being honest: which are live, which
 * currencies we can actually receive in, and whether anything is missing the
 * details a foreign transfer needs.
 */
@Data
public class BankAccountFilter {

    /** Free text across the code, the name, the bank and the account number. */
    private String keyword;
    /** what the old signature called it */
    private String search;

    private String currency;
    private List<String> currencies;

    private Boolean isActive;
    private List<String> statuses;

    private Boolean isDefault;

    /**
     * What is missing.
     *
     * noSwift — cannot receive an international transfer. noIban — the same for
     * Europe. Both are worth a card because a customer asked to pay and could not.
     */
    private List<String> qualities;

    public List<String> allCurrencies() {
        List<String> out = new ArrayList<>();
        if (currencies != null) currencies.stream().filter(c -> c != null && !c.isBlank()).forEach(out::add);
        if (currency != null && !currency.isBlank() && !out.contains(currency)) out.add(currency);
        return out;
    }

    /** The old `search` param and the house `keyword` mean the same thing. */
    public String effectiveKeyword() {
        if (keyword != null && !keyword.isBlank()) return keyword;
        return search;
    }

    public boolean wants(String quality) {
        return qualities != null && qualities.contains(quality);
    }
}
