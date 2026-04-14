package com.itineraryledger.kabengosafaris.Initializers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import com.itineraryledger.kabengosafaris.Invoice.Repository.PaymentRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * One-time data migration initializer for the cross-currency payment feature.
 *
 * Backfills existing Payment rows that lack the new fields:
 *   - invoiceCurrency: set to the matching grand-total currency, or the invoice's first currency
 *   - exchangeRate: set to 1.000000 (existing payments are assumed same-currency)
 *   - baseAmount: set to amount (identity conversion)
 *
 * For legacy cross-currency payments (currency != any grandTotal currency), the
 * invoiceCurrency is set to the invoice's first grand-total currency and baseAmount = amount.
 * These will need manual exchange rate correction by the admin.
 *
 * Safe to run repeatedly — only touches rows where baseAmount IS NULL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentDataMigrationInitializer implements ApplicationRunner, Ordered {

    private final PaymentRepository paymentRepository;

    @Override
    public int getOrder() {
        return 999; // Run after all other initializers
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Payment> unmigrated = paymentRepository.findAll().stream()
            .filter(p -> p.getBaseAmount() == null)
            .collect(Collectors.toList());

        if (unmigrated.isEmpty()) {
            return;
        }

        log.info("PaymentDataMigration: backfilling {} payments with exchange rate fields", unmigrated.size());

        int sameCurrency = 0;
        int crossCurrency = 0;

        for (Payment payment : unmigrated) {
            Invoice invoice = payment.getInvoice();
            String paymentCurrency = payment.getCurrency();

            String invoiceCurrency = resolveInvoiceCurrencyForMigration(paymentCurrency, invoice);

            payment.setInvoiceCurrency(invoiceCurrency);
            payment.setExchangeRate(BigDecimal.ONE);
            payment.setBaseAmount(payment.getAmount());

            if (paymentCurrency.equalsIgnoreCase(invoiceCurrency)) {
                sameCurrency++;
            } else {
                crossCurrency++;
                log.warn("PaymentDataMigration: Payment #{} ({} {}) on invoice {} has cross-currency mismatch. " +
                    "Set invoiceCurrency={}, exchangeRate=1, baseAmount={}. " +
                    "Admin should update the exchange rate manually.",
                    payment.getId(), paymentCurrency, payment.getAmount(),
                    invoice.getInvoiceCode(), invoiceCurrency, payment.getAmount());
            }
        }

        paymentRepository.saveAll(unmigrated);

        log.info("PaymentDataMigration: completed. {} same-currency, {} cross-currency (need manual rate update)",
            sameCurrency, crossCurrency);
    }

    private String resolveInvoiceCurrencyForMigration(String paymentCurrency, Invoice invoice) {
        if (invoice == null || invoice.getGrandTotals() == null || invoice.getGrandTotals().isEmpty()) {
            return paymentCurrency;
        }

        Set<String> grandTotalCurrencies = invoice.getGrandTotals().stream()
            .map(Price::getCurrency)
            .collect(Collectors.toSet());

        // If payment currency matches a grand-total currency, use it
        if (grandTotalCurrencies.contains(paymentCurrency)) {
            return paymentCurrency;
        }

        // Otherwise, default to the first grand-total currency
        return invoice.getGrandTotals().get(0).getCurrency();
    }
}
