package com.itineraryledger.kabengosafaris.Expense.Services.ExpensePaymentServices;

import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseDocument;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpensePayment;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseDocumentRepository;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpensePaymentRepository;
import com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices.ExpensePaymentAggregationService;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Telling a supplier we have paid them.
 *
 * The trade calls this a payment or remittance advice, and a lodge's accounts desk needs four
 * things from it: the amount, the date, OUR transfer reference and THEIR own invoice number. With
 * those they can match the money to a reservation; without them a payment sits unallocated and
 * somebody chases a bill that is already settled.
 *
 * <p>Sent by hand from the payment, never fired by a state change. Correcting a mistyped reference
 * would otherwise email the supplier a second time about money that only moved once, and a supplier
 * who receives two advices for one transfer has to ring somebody to find out which is real.
 *
 * <p>What is still owing is stated either way, and it comes from the same
 * {@link ExpensePaymentAggregationService#computeBalances} the bill's own screen reads. A part
 * payment that reads as a settlement is how a supplier stops chasing a balance they are owed, and
 * a second opinion about the arithmetic is how the two figures come to disagree.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpensePaymentAdviceService {

    private static final String EVENT = "SEND_PAYMENT_ADVICE";
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private final ExpensePaymentRepository payments;
    private final ExpenseDocumentRepository documents;
    private final ExpensePaymentAggregationService aggregation;
    private final EmailTemplateRenderer templateRenderer;
    private final IdObfuscator idObfuscator;

    /**
     * The letter, ready for the composer. Nothing is sent from here.
     *
     * The office presses Send in the mailbox, which is where a sent message belongs: it lands in
     * Sent, it can be found again, and a supplier's reply threads under it. An advice fired from a
     * drawer left no trace anybody could search, and the person sending it never saw what went.
     *
     * <p>Attachments are OFFERED, not attached. The payment's own proof — the bank slip — is what a
     * supplier usually wants, so it leads; the bill's other documents follow. Whoever is sending
     * decides, because a bill can carry paperwork that is ours rather than theirs.
     */
    public ResponseEntity<ApiResponse<?>> letter(String paymentIdObfuscated) {
        try {
            Long paymentId = idObfuscator.decodeId(paymentIdObfuscated);
            if (paymentId == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid payment id", "INVALID_ID"));
            }

            ExpensePayment payment = payments.findById(paymentId).orElse(null);
            if (payment == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Payment not found", "EXPENSE_PAYMENT_NOT_FOUND"));
            }

            Expense bill = payment.getExpense();
            if (bill == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse.error(409,
                        "This payment is not attached to a bill", "EXPENSE_PAYMENT_ORPHANED"));
            }

            Map<String, String> variables = variablesFor(payment, bill);
            String html = templateRenderer.renderTemplate(EVENT, variables);

            Set<String> to = new LinkedHashSet<>();
            if (bill.getVendor() != null && bill.getVendor().getEmail() != null
                && !bill.getVendor().getEmail().isBlank()) {
                to.add(bill.getVendor().getEmail().trim());
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("to", new ArrayList<>(to));
            body.put("cc", List.of());
            body.put("subject", "Payment sent · " + variables.get("amountPaid")
                + " · " + variables.get("billCode"));
            body.put("html", html);
            body.put("vendorName", variables.get("vendorName"));
            body.put("attachments", attachmentsFor(payment, bill));

            return ResponseEntity.ok(ApiResponse.success(200, "Payment advice ready", body));
        } catch (Exception e) {
            log.error("Error preparing payment advice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Could not prepare the payment advice",
                    "PAYMENT_ADVICE_FAILED"));
        }
    }

    /**
     * What could be attached, the payment's own proof first.
     *
     * {@code suggested} is the slip for THIS payment: a supplier asking "show me the transfer"
     * wants that one file, and offering the bill's whole folder as though it were all relevant is
     * how somebody accidentally sends a supplier our internal paperwork.
     */
    private List<Map<String, Object>> attachmentsFor(ExpensePayment payment, Expense bill) {
        List<Map<String, Object>> out = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();

        for (ExpenseDocument doc : documents.findByExpensePaymentIdOrderByCreatedAtDesc(payment.getId())) {
            if (seen.add(doc.getId())) out.add(describe(doc, true));
        }
        for (ExpenseDocument doc : documents.findByExpenseIdOrderByCreatedAtDesc(bill.getId())) {
            if (doc.getExpensePayment() != null) continue;
            if (seen.add(doc.getId())) out.add(describe(doc, false));
        }
        return out;
    }

    private Map<String, Object> describe(ExpenseDocument doc, boolean suggested) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", idObfuscator.encodeId(doc.getId()));
        row.put("title", text(doc.getTitle(), text(doc.getOriginalFileName(), "Document")));
        row.put("fileName", text(doc.getOriginalFileName(), text(doc.getFileName(), "")));
        row.put("fileType", text(doc.getFileType(), ""));
        row.put("fileSize", doc.getFileSize());
        row.put("documentType", doc.getDocumentType() == null ? null : doc.getDocumentType().name());
        row.put("suggested", suggested);
        row.put("source", suggested ? "Proof of this payment" : "Filed on the bill");
        return row;
    }

    /** Everything the template names, and nothing it does not. */
    private Map<String, String> variablesFor(ExpensePayment payment, Expense bill) {
        Map<String, String> variables = new HashMap<>();

        String currency = payment.getExpenseCurrency() != null && !payment.getExpenseCurrency().isBlank()
            ? payment.getExpenseCurrency()
            : text(payment.getCurrency(), "USD");

        BigDecimal owing = balanceIn(bill, currency);
        boolean settled = owing.compareTo(BigDecimal.ZERO) <= 0;

        variables.put("vendorName", bill.getVendor() != null ? text(bill.getVendor().getName(), "—") : "—");
        variables.put("settlement", settled ? "Paid in full" : "Part payment");
        variables.put("amountPaid", money(payment.getAmount(), text(payment.getCurrency(), currency)));
        variables.put("paidOn", payment.getPaymentDate() == null ? "—" : payment.getPaymentDate().toString());
        variables.put("method", payment.getPaymentMethod() == null ? "—"
            : readable(payment.getPaymentMethod().name()));
        variables.put("reference", text(payment.getReference(), ""));
        /* the account's NAME only: a supplier reconciling does not need our account number */
        variables.put("paidFrom", payment.getBankAccount() != null
            ? text(payment.getBankAccount().getAccountName(), "") : "");
        variables.put("billCode", text(bill.getExpenseCode(), "—"));
        variables.put("billTitle", text(bill.getTitle(), "—"));
        variables.put("billDescription", text(bill.getDescription(), ""));
        variables.put("vendorReference", text(bill.getReferenceNumber(), ""));
        variables.put("billTotal", money(totalIn(bill, currency), currency));
        variables.put("balanceRemaining", money(settled ? BigDecimal.ZERO : owing, currency));
        variables.put("isSettled", settled ? "yes" : "no");
        variables.put("safariName", bill.getSafari() != null ? text(bill.getSafari().getName(), "") : "");
        variables.put("safariCode", bill.getSafari() != null ? text(bill.getSafari().getCode(), "") : "");
        variables.put("notes", text(payment.getNotes(), ""));

        return variables;
    }

    /** What is left owing in one currency, read from the bill's own balances. */
    private BigDecimal balanceIn(Expense bill, String currency) {
        for (Price price : orEmpty(aggregation.computeBalances(bill))) {
            if (currency.equalsIgnoreCase(price.getCurrency()) && price.getTotalPrice() != null) {
                return price.getTotalPrice();
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal totalIn(Expense bill, String currency) {
        for (Price price : orEmpty(bill.getGrandTotals())) {
            if (currency.equalsIgnoreCase(price.getCurrency()) && price.getTotalPrice() != null) {
                return price.getTotalPrice();
            }
        }
        return BigDecimal.ZERO;
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static String money(BigDecimal amount, String currency) {
        if (amount == null) return "—";
        return (currency == null || currency.isBlank() ? "" : currency + " ") + MONEY.format(amount);
    }

    /** BANK_TRANSFER reads as "Bank transfer" to somebody outside this office. */
    private static String readable(String enumName) {
        String spaced = enumName.replace('_', ' ').toLowerCase();
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

    private static String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
