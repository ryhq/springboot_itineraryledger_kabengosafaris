package com.itineraryledger.kabengosafaris.Safari.Services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoicePaymentAggregationService;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariBillingDTO;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * What a safari has been invoiced and what has been paid — asked of the
 * invoices, never remembered by the safari.
 *
 * The safari used to hold this as two states of its own (PENDING_PAYMENT,
 * FULLY_PAID), set by a switch somebody ticked. Nothing joined that switch to
 * the money: settle the invoice properly and the safari still said "awaiting
 * payment"; tick the switch and it said "paid" with no payment recorded
 * anywhere. Two records held the same fact and neither could correct the other.
 *
 * <p>So the invoice is the truth about money, and this reads it. A figure here
 * cannot disagree with the invoice it came from, because it IS the invoice.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SafariBillingService {

    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentAggregationService paymentAggregation;
    private final IdObfuscator idObfuscator;

    /** Nothing invoiced yet is a real answer, not a missing one. */
    @Transactional(readOnly = true)
    public SafariBillingDTO forSafari(Long safariId) {
        List<Invoice> invoices = invoiceRepository.findBySafariId(safariId).stream()
            .filter(i -> i.getStatus() != InvoiceStatus.CANCELLED)
            .toList();

        if (invoices.isEmpty()) {
            return SafariBillingDTO.builder()
                .invoiceCount(0)
                .paymentStatus("NOT_INVOICED")
                .paymentStatusDisplayName("Not invoiced")
                .invoiced(new ArrayList<>())
                .paid(new ArrayList<>())
                .balance(new ArrayList<>())
                .build();
        }

        Map<String, BigDecimal> invoiced = new LinkedHashMap<>();
        Map<String, BigDecimal> paid = new LinkedHashMap<>();

        for (Invoice invoice : invoices) {
            for (Price total : invoice.getGrandTotals() != null ? invoice.getGrandTotals() : List.<Price>of()) {
                add(invoiced, total.getCurrency(), total.getTotalPrice());
            }
            for (Price payment : paymentAggregation.computeAmountsPaid(invoice)) {
                add(paid, payment.getCurrency(), payment.getTotalPrice());
            }
        }

        List<Price> balance = new ArrayList<>();
        boolean anyOutstanding = false;
        boolean anyPaid = false;
        for (Map.Entry<String, BigDecimal> entry : invoiced.entrySet()) {
            BigDecimal settled = paid.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal due = entry.getValue().subtract(settled);
            if (due.signum() > 0) anyOutstanding = true;
            if (settled.signum() > 0) anyPaid = true;
            balance.add(price(entry.getKey(), due));
        }

        // overdue is the invoice's word, and it outranks a mere balance
        boolean overdue = invoices.stream().anyMatch(i -> i.getStatus() == InvoiceStatus.OVERDUE);

        String status = !anyOutstanding ? "PAID"
            : overdue ? "OVERDUE"
            : anyPaid ? "PARTIALLY_PAID"
            : "AWAITING_PAYMENT";

        String display = switch (status) {
            case "PAID" -> "Paid in full";
            case "OVERDUE" -> "Overdue";
            case "PARTIALLY_PAID" -> "Part paid";
            default -> "Awaiting payment";
        };

        Invoice latest = invoices.get(invoices.size() - 1);

        return SafariBillingDTO.builder()
            .invoiceCount(invoices.size())
            .latestInvoiceId(idObfuscator.encodeId(latest.getId()))
            .latestInvoiceCode(latest.getInvoiceCode())
            .latestInvoiceStatus(latest.getStatus() != null ? latest.getStatus().name() : null)
            .dueDate(latest.getDueDate())
            .paymentStatus(status)
            .paymentStatusDisplayName(display)
            .invoiced(toPrices(invoiced))
            .paid(toPrices(paid))
            .balance(balance)
            .invoices(invoices.stream().map(this::line).toList())
            .build();
    }

    /** One invoice, stated in its own right so the page can say which is which. */
    private SafariBillingDTO.InvoiceLine line(Invoice invoice) {
        List<Price> total = invoice.getGrandTotals() != null
            ? invoice.getGrandTotals() : List.<Price>of();
        List<Price> settled = paymentAggregation.computeAmountsPaid(invoice);

        Map<String, BigDecimal> owing = new LinkedHashMap<>();
        for (Price price : total) add(owing, price.getCurrency(), price.getTotalPrice());
        for (Price price : settled) {
            if (price.getCurrency() == null) continue;
            owing.merge(price.getCurrency(),
                price.getTotalPrice() != null ? price.getTotalPrice().negate() : BigDecimal.ZERO,
                BigDecimal::add);
        }

        return SafariBillingDTO.InvoiceLine.builder()
            .id(idObfuscator.encodeId(invoice.getId()))
            .invoiceCode(invoice.getInvoiceCode())
            .title(invoice.getTitle())
            .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
            .statusDisplayName(invoice.getStatus() != null ? invoice.getStatus().getDisplayName() : null)
            .issueDate(invoice.getIssueDate())
            .dueDate(invoice.getDueDate())
            .isSupplement(Boolean.TRUE.equals(invoice.getIsSupplement()))
            .supplementReason(invoice.getSupplementReason())
            .total(new ArrayList<>(total))
            .paid(new ArrayList<>(settled))
            .balance(toPrices(owing))
            .build();
    }

    private void add(Map<String, BigDecimal> into, String currency, BigDecimal amount) {
        if (currency == null) return;
        into.merge(currency, amount != null ? amount : BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Price> toPrices(Map<String, BigDecimal> amounts) {
        List<Price> out = new ArrayList<>();
        amounts.forEach((currency, amount) -> out.add(price(currency, amount)));
        return out;
    }

    private Price price(String currency, BigDecimal amount) {
        return Price.builder()
            .currency(currency)
            .quantity(1)
            .unitPrice(amount)
            .totalPrice(amount)
            .build();
    }
}
