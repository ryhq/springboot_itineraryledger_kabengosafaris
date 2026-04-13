package com.itineraryledger.kabengosafaris.Safari.Services;

import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SafariPaymentScheduler - Auto-transitions safaris from PENDING_PAYMENT → FULLY_PAID
 * when all linked invoices are settled (status = PAID).
 *
 * Runs every hour to detect invoice payments promptly.
 *
 * Logic:
 * 1. Find all safaris in PENDING_PAYMENT state
 * 2. For each, check if there are any linked invoices
 * 3. If all invoices are PAID (none unpaid), transition to FULLY_PAID
 * 4. If no invoices exist, skip (manual payment flow applies)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SafariPaymentScheduler {

    private final SafariRepository safariRepository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Check invoice payment status hourly and auto-transition if fully settled
     */
    @Scheduled(cron = "${safari.payment.schedule.cron:0 0 * * * ?}")
    @Transactional
    public void checkInvoicePayments() {
        List<Safari> pendingPayment = safariRepository.findByState(SafariState.PENDING_PAYMENT);

        if (pendingPayment.isEmpty()) {
            return;
        }

        log.debug("Checking invoice payments for {} safari(s) in PENDING_PAYMENT state", pendingPayment.size());

        int transitioned = 0;
        for (Safari safari : pendingPayment) {
            if (isFullyPaidByInvoices(safari)) {
                safari.changeState(SafariState.FULLY_PAID, "Auto-transitioned — all linked invoices are fully paid");
                safariRepository.save(safari);
                transitioned++;
                log.info("AUTO-PAID: {} ({}) — all invoices settled — transitioned to FULLY_PAID",
                    safari.getCode(), safari.getName());
            }
        }

        if (transitioned > 0) {
            log.info("SafariPaymentScheduler: {} safari(s) auto-transitioned to FULLY_PAID", transitioned);
        }
    }

    /**
     * Check if all invoices for a safari are fully paid.
     * Returns false if:
     * - Safari has no invoices (manual payment flow — don't auto-transition)
     * - Any invoice is still unpaid (DRAFT, SENT, VIEWED, PARTIALLY_PAID, OVERDUE)
     */
    private boolean isFullyPaidByInvoices(Safari safari) {
        List<Invoice> allInvoices = invoiceRepository.findBySafariId(safari.getId());

        // No invoices linked — can't auto-determine payment status
        if (allInvoices.isEmpty()) {
            return false;
        }

        // Check if any invoices are still unpaid
        List<Invoice> unpaidInvoices = invoiceRepository.findUnpaidBySafariId(safari.getId());

        if (!unpaidInvoices.isEmpty()) {
            log.debug("Safari {} has {} unpaid invoice(s) out of {} total",
                safari.getCode(), unpaidInvoices.size(), allInvoices.size());
            return false;
        }

        // All invoices are in paid/settled state
        log.debug("Safari {} — all {} invoice(s) are settled", safari.getCode(), allInvoices.size());
        return true;
    }
}
