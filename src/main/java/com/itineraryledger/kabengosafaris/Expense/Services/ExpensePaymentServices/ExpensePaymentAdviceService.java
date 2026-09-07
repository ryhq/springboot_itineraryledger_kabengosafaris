package com.itineraryledger.kabengosafaris.Expense.Services.ExpensePaymentServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpensePayment;
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
    private final ExpensePaymentAggregationService aggregation;
    private final EmailTemplateRenderer templateRenderer;
    private final EmailSendingService emailSendingService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "SEND_PAYMENT_ADVICE",
        entityType = "EXPENSE_PAYMENT",
        entityIdParamName = "paymentIdObfuscated",
        description = "Email a supplier a payment advice for money paid against their bill"
    )
    public ResponseEntity<ApiResponse<?>> send(
        String paymentIdObfuscated,
        List<String> toOverride,
        Long emailTemplateId
    ) {
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

            Set<String> recipients = new LinkedHashSet<>();
            if (toOverride != null) {
                for (String address : toOverride) {
                    if (address != null && !address.isBlank()) recipients.add(address.trim());
                }
            }
            if (recipients.isEmpty() && bill.getVendor() != null
                && bill.getVendor().getEmail() != null && !bill.getVendor().getEmail().isBlank()) {
                recipients.add(bill.getVendor().getEmail().trim());
            }
            if (recipients.isEmpty()) {
                /*
                 * Named rather than silent. The old shape of this mistake elsewhere in the system
                 * was a 200 that sent nothing, and nobody learns from a success.
                 */
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse.error(409,
                        bill.getVendor() == null
                            ? "This bill has no vendor, so there is nobody to advise. Set the "
                                + "vendor on the bill, or give an address to send to."
                            : "There is no email address on " + bill.getVendor().getName()
                                + ". Add one to the vendor, or give an address to send to.",
                        "NO_VENDOR_EMAIL"));
            }

            Map<String, String> variables = variablesFor(payment, bill);
            String subject = "Payment sent · " + variables.get("amountPaid")
                + " · " + variables.get("billCode");

            String html = emailTemplateId != null
                ? templateRenderer.renderTemplate(EVENT, emailTemplateId, variables)
                : templateRenderer.renderTemplate(EVENT, variables);

            List<String> sent = new ArrayList<>();
            List<String> failed = new ArrayList<>();
            for (String address : recipients) {
                try {
                    emailSendingService.sendHtmlEmail(address, subject, html);
                    sent.add(address);
                } catch (Exception e) {
                    /* one bad address must not cost the others their advice */
                    log.warn("Could not send the payment advice for {} to {}: {}",
                        bill.getExpenseCode(), address, e.getMessage());
                    failed.add(address);
                }
            }

            if (sent.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                    ApiResponse.error(502,
                        "The advice could not be sent to " + String.join(", ", failed),
                        "PAYMENT_ADVICE_SEND_FAILED"));
            }

            Map<String, Object> report = new HashMap<>();
            report.put("sentTo", sent);
            report.put("failed", failed);
            report.put("subject", subject);

            return ResponseEntity.ok(ApiResponse.success(200,
                failed.isEmpty()
                    ? "Payment advice sent to " + String.join(", ", sent)
                    : "Sent to " + String.join(", ", sent) + "; failed for "
                        + String.join(", ", failed),
                report));
        } catch (Exception e) {
            log.error("Error sending payment advice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to send the payment advice",
                    "PAYMENT_ADVICE_FAILED"));
        }
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
