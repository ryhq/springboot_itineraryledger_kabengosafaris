package com.itineraryledger.kabengosafaris.Response;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * How an amount of money is written, everywhere, once.
 *
 * <p>This existed six times, in three dialects that disagreed. An invoice PDF formatted with
 * {@code NumberFormat.getCurrencyInstance} and printed <b>$2,700.00</b>; the receipt for that same
 * invoice used a code prefix and printed <b>USD 2,700.00</b>; the client portal used a third
 * spelling of the same thing. A client who received all three got three descriptions of one debt.
 *
 * <p><b>The code in front, never the symbol.</b> A symbol is ambiguous in a business that charges
 * in dollars and shillings: {@code $} is claimed by a dozen currencies and Java renders TZS as
 * {@code TSh}, which a German client has no reason to recognise. The ISO code is unambiguous to
 * everyone and survives a PDF font that lacks a glyph. This follows the portal's spelling, which
 * already said why: "Grouped thousands, two decimals, the code in front. No locale guessing."
 *
 * <p><b>A missing currency is not USD.</b> The old symbol dialect fell back to
 * {@code code + " " + amount} whenever {@code Currency.getInstance} threw, so an unrecognised code
 * silently switched dialect halfway down a document. Here there is one dialect, and an amount with
 * no currency comes back with no currency rather than wearing a default — a number whose currency
 * nobody recorded must not be presented as though somebody had.
 */
public final class MoneyFormat {

    private MoneyFormat() {
    }

    /**
     * {@code "USD 2,700.00"}, or null when there is no amount to write.
     *
     * <p>Null in means null out, because the templates distinguish "no figure" from "zero" and a
     * zero would read as a settled balance.
     */
    public static String format(String currency, BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        String figure = String.format("%,.2f", amount.setScale(2, RoundingMode.HALF_UP));
        return currency == null || currency.isBlank()
            ? figure
            : currency.toUpperCase() + " " + figure;
    }

    /**
     * The same, with the arguments the other way round.
     *
     * <p>Both orders were in use and both read naturally at the call site, so both are kept rather
     * than renaming thirty callers and risking a silent swap of two String-and-BigDecimal
     * arguments that the compiler would not catch.
     */
    public static String of(BigDecimal amount, String currency) {
        return format(currency, amount);
    }
}
