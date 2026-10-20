package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Inclusion.Services.InclusionSnapshotService;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.FullInvoiceShareDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.FullInvoiceShareDTO.PayerLineDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceShare;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceShareRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds the model behind one payer's payment request.
 *
 * <p>The amounts are computed here rather than read off a column, the same way the invoice's own
 * balance is, so a request can never ask for money that has already arrived.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceShareFullGetService {

    private final InvoiceShareRepository shareRepository;
    private final InvoicePaymentAggregationService aggregationService;
    private final InclusionSnapshotService inclusionSnapshot;
    private final IdObfuscator idObfuscator;

    @Transactional(readOnly = true)
    public FullInvoiceShareDTO fetch(String obfuscatedShareId) {
        InvoiceShare share;
        try {
            share = shareRepository.findById(idObfuscator.decodeId(obfuscatedShareId)).orElse(null);
        } catch (Exception e) {
            return null;
        }
        if (share == null) {
            return null;
        }

        Invoice invoice = share.getInvoice();
        String currency = share.getCurrency();

        BigDecimal paid = aggregationService.computeSharePaid(share);
        BigDecimal balance = aggregationService.computeShareBalance(share);
        String status = aggregationService.computeShareStatus(share);

        BigDecimal invoiceTotal = invoice == null || invoice.getGrandTotals() == null
            ? null
            : invoice.getGrandTotals().stream()
                .filter(total -> currency != null && currency.equalsIgnoreCase(total.getCurrency()))
                .map(Price::getTotalPrice)
                .findFirst()
                .orElse(null);

        List<PayerLineDTO> payers = invoice == null ? List.of()
            : shareRepository.findByInvoiceIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(invoice.getId())
                .stream()
                .map(other -> PayerLineDTO.builder()
                    .payerName(other.resolvedPayerName())
                    .amount(other.getAmount())
                    .currency(other.getCurrency())
                    .formattedAmount(format(other.getCurrency(), other.getAmount()))
                    .status(aggregationService.computeShareStatus(other))
                    .isThisPayer(other.getId().equals(share.getId()))
                    .build())
                .toList();

        return FullInvoiceShareDTO.builder()
            .id(idObfuscator.encodeId(share.getId()))
            .payerName(share.resolvedPayerName())
            .payerEmail(share.resolvedPayerEmail())
            .amount(share.getAmount())
            .currency(currency)
            .formattedAmount(format(currency, share.getAmount()))
            .amountPaid(paid)
            .formattedAmountPaid(format(currency, paid))
            .balance(balance)
            .formattedBalance(format(currency, balance))
            .status(status)
            .statusDisplayName(switch (status) {
                case "PAID" -> "Paid in full";
                case "PARTIALLY_PAID" -> "Part paid";
                default -> "Awaiting payment";
            })
            .shareReference(share.getShareReference())
            .notes(share.getNotes())
            .invoiceCode(invoice != null ? invoice.getInvoiceCode() : null)
            .invoiceTitle(invoice != null ? invoice.getTitle() : null)
            .issueDate(invoice != null ? invoice.getIssueDate() : null)
            .dueDate(invoice != null ? invoice.getDueDate() : null)
            .paymentTerms(invoice != null ? invoice.getPaymentTerms() : null)
            .billedToName(invoice != null && invoice.getCustomer() != null
                ? invoice.getCustomer().getDisplayName() : null)
            .safariName(invoice != null && invoice.getSafari() != null
                ? invoice.getSafari().getName() : null)
            .invoiceTotal(invoiceTotal)
            .formattedInvoiceTotal(format(currency, invoiceTotal))
            /* The same statement of price the invoice carries, because this reader may never
             * have seen the invoice. */
            .inclusions(invoice != null ? inclusionSnapshot.linesOf(invoice) : null)
            .payers(payers)
            .payerCount(payers.size())
            .generatedAt(LocalDateTime.now())
            .build();
    }

    private static String format(String currencyCode, BigDecimal amount) {
        if (amount == null || currencyCode == null) {
            return null;
        }
        try {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
            formatter.setCurrency(Currency.getInstance(currencyCode));
            return formatter.format(amount);
        } catch (Exception e) {
            return currencyCode + " " + amount;
        }
    }
}
