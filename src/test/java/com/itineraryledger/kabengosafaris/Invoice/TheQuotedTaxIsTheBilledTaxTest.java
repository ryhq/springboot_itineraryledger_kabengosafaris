package com.itineraryledger.kabengosafaris.Invoice;

import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceLineItem;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceItemType;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceLineItemRepository;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A scope that stops at the quote is not a scope.
 *
 * <p>An invoice generated from a safari inherits the quote's tax PERCENTAGE. Before the scope
 * travelled with it, a quote taxed 18% on the beds alone -- 1,281.60 on the VENOT family's
 * 13,177.74 -- became an invoice taxed 18% on everything, 2,371.99. The client agrees to one figure
 * and is billed one 1,090.39 higher, at the only point in the process where the difference is not a
 * conversation but a debt.
 */
class TheQuotedTaxIsTheBilledTaxTest {

    private static final BigDecimal BEDS = new BigDecimal("7120.00");
    private static final BigDecimal PARK_FEES = new BigDecimal("3412.24");
    private static final BigDecimal ACTIVITIES = new BigDecimal("2645.50");
    private static final BigDecimal SUBTOTAL = new BigDecimal("13177.74");

    private InvoiceRepository invoices;
    private InvoiceTotalsCalculationService service;

    @BeforeEach
    void setUp() {
        invoices = mock(InvoiceRepository.class);
        InvoiceLineItemRepository lines = mock(InvoiceLineItemRepository.class);
        service = new InvoiceTotalsCalculationService(invoices, lines);

        when(lines.findByInvoiceIdAndIsActiveTrueOrderByDisplayOrderAsc(anyLong())).thenReturn(List.of(
            line(InvoiceItemType.ACCOMMODATION, BEDS),
            line(InvoiceItemType.PARK_FEE, PARK_FEES),
            line(InvoiceItemType.ACTIVITY, ACTIVITIES)
        ));
    }

    @Test
    @DisplayName("an invoice carrying the quote's scope bills exactly what was quoted")
    void theScopeSurvivesInvoicing() {
        Invoice invoice = invoice(new BigDecimal("18"), "ACCOMMODATION");

        service.recalculateTotals(invoice);

        assertEquals(new BigDecimal("1281.60"), usd(invoice.getTaxes()),
            "the same 18% of 7,120.00 the quote showed");
        assertEquals(new BigDecimal("14459.34"), usd(invoice.getGrandTotals()));
    }

    @Test
    @DisplayName("without a scope it bills as it always did")
    void noScopeIsTheOldBehaviour() {
        Invoice invoice = invoice(new BigDecimal("18"), null);

        service.recalculateTotals(invoice);

        assertEquals(new BigDecimal("2371.99"), usd(invoice.getTaxes()));
        assertEquals(new BigDecimal("15549.73"), usd(invoice.getGrandTotals()));
    }

    @Test
    @DisplayName("1,090.39 is what dropping the scope at invoice time would have cost the client")
    void theSizeOfTheOldGap() {
        Invoice quoted = invoice(new BigDecimal("18"), "ACCOMMODATION");
        Invoice unscoped = invoice(new BigDecimal("18"), null);

        service.recalculateTotals(quoted);
        service.recalculateTotals(unscoped);

        assertEquals(new BigDecimal("1090.39"),
            usd(unscoped.getGrandTotals()).subtract(usd(quoted.getGrandTotals())));
    }

    @Test
    @DisplayName("the subtotal is still every line, taxed or not")
    void theSubtotalIsUntouched() {
        Invoice invoice = invoice(new BigDecimal("18"), "ACCOMMODATION");

        service.recalculateTotals(invoice);

        assertEquals(SUBTOTAL, usd(invoice.getSubtotals()),
            "scoping the tax must never hide a charge from the client's own arithmetic");
    }

    @Test
    @DisplayName("a scope naming a category the invoice has none of charges nothing")
    void nothingToTax() {
        Invoice invoice = invoice(new BigDecimal("18"), "TRANSPORT");

        service.recalculateTotals(invoice);

        assertTrue(invoice.getTaxes().isEmpty() || usd(invoice.getTaxes()).signum() == 0);
        assertEquals(SUBTOTAL, usd(invoice.getGrandTotals()));
    }

    private Invoice invoice(BigDecimal taxPercentage, String scope) {
        return Invoice.builder()
            .id(1L)
            .taxPercentage(taxPercentage)
            .taxAppliesTo(scope)
            .build();
    }

    private InvoiceLineItem line(InvoiceItemType type, BigDecimal total) {
        return InvoiceLineItem.builder()
            .itemType(type)
            .isActive(true)
            .prices(List.of(Price.builder()
                .currency("USD")
                .quantity(1)
                .unitPrice(total)
                .totalPrice(total)
                .build()))
            .build();
    }

    private BigDecimal usd(List<Price> prices) {
        return prices.stream()
            .filter(p -> "USD".equals(p.getCurrency()))
            .map(Price::getTotalPrice)
            .findFirst()
            .orElse(BigDecimal.ZERO);
    }
}
