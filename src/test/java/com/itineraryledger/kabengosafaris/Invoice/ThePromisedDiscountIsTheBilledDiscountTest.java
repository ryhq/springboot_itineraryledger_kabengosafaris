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
 * The other half of the scope, and the half that costs us rather than the client.
 *
 * <p>An invoice generated from a safari inherits the quote's discount PERCENTAGE. Without the scope
 * beside it, 10% promised on the parts we sell becomes 10% off everything when the money is
 * collected, and the extra 290.26 comes out of a park fee we merely pass on to the authority.
 *
 * <p>Same quote as {@code ADiscountComesOffOnlyWhatIsOursToDiscountTest}: beds 7,704.00, transport
 * 1,320.00, activities 2,000.00, park and crater fees 2,902.64.
 */
class ThePromisedDiscountIsTheBilledDiscountTest {

    private static final BigDecimal BEDS = new BigDecimal("7704.00");
    private static final BigDecimal TRANSPORT = new BigDecimal("1320.00");
    private static final BigDecimal ACTIVITIES = new BigDecimal("2000.00");
    private static final BigDecimal PARK_FEES = new BigDecimal("2902.64");
    private static final BigDecimal SUBTOTAL = new BigDecimal("13926.64");

    private static final String OURS = "ACCOMMODATION,ACTIVITY,TRANSPORT";

    private InvoiceTotalsCalculationService service;

    @BeforeEach
    void setUp() {
        InvoiceRepository invoices = mock(InvoiceRepository.class);
        InvoiceLineItemRepository lines = mock(InvoiceLineItemRepository.class);
        service = new InvoiceTotalsCalculationService(invoices, lines);

        when(lines.findByInvoiceIdAndIsActiveTrueOrderByDisplayOrderAsc(anyLong())).thenReturn(List.of(
            line(InvoiceItemType.ACCOMMODATION, BEDS),
            line(InvoiceItemType.TRANSPORT, TRANSPORT),
            line(InvoiceItemType.ACTIVITY, ACTIVITIES),
            line(InvoiceItemType.PARK_FEE, PARK_FEES)
        ));
    }

    @Test
    @DisplayName("an invoice carrying the quote's scope deducts exactly what was promised")
    void theScopeSurvivesInvoicing() {
        Invoice invoice = invoice(new BigDecimal("10"), OURS);

        service.recalculateTotals(invoice);

        assertEquals(new BigDecimal("1102.40"), usd(invoice.getDiscounts()),
            "the same 10% of 11,024.00 the quote showed");
        assertEquals(new BigDecimal("12824.24"), usd(invoice.getGrandTotals()));
    }

    @Test
    @DisplayName("without a scope it deducts as it always did")
    void noScopeIsTheOldBehaviour() {
        Invoice invoice = invoice(new BigDecimal("10"), null);

        service.recalculateTotals(invoice);

        assertEquals(new BigDecimal("1392.66"), usd(invoice.getDiscounts()));
        assertEquals(new BigDecimal("12533.98"), usd(invoice.getGrandTotals()));
    }

    @Test
    @DisplayName("290.26 is what dropping the scope at invoice time would have cost the company")
    void theSizeOfTheGap() {
        Invoice promised = invoice(new BigDecimal("10"), OURS);
        Invoice unscoped = invoice(new BigDecimal("10"), null);

        service.recalculateTotals(promised);
        service.recalculateTotals(unscoped);

        assertEquals(new BigDecimal("290.26"),
            usd(promised.getGrandTotals()).subtract(usd(unscoped.getGrandTotals())),
            "10% of the park fees, given away on a charge that is not ours to reduce");
    }

    @Test
    @DisplayName("the subtotal is still every line, discounted or not")
    void theSubtotalIsUntouched() {
        Invoice invoice = invoice(new BigDecimal("10"), OURS);

        service.recalculateTotals(invoice);

        assertEquals(SUBTOTAL, usd(invoice.getSubtotals()),
            "scoping the discount must never hide a charge from the client's own arithmetic");
    }

    @Test
    @DisplayName("the tax and the discount can be scoped to different lines on one invoice")
    void bothScopesAtOnce() {
        Invoice invoice = Invoice.builder()
            .id(1L)
            .taxPercentage(new BigDecimal("18"))
            .taxAppliesTo("ACCOMMODATION")
            .discountPercentage(new BigDecimal("10"))
            .discountAppliesTo(OURS)
            .build();

        service.recalculateTotals(invoice);

        assertEquals(new BigDecimal("1386.72"), usd(invoice.getTaxes()));
        assertEquals(new BigDecimal("1102.40"), usd(invoice.getDiscounts()));
        assertEquals(new BigDecimal("14210.96"), usd(invoice.getGrandTotals()));
    }

    @Test
    @DisplayName("a scope naming a category the invoice has none of deducts nothing")
    void nothingToDiscount() {
        Invoice invoice = invoice(new BigDecimal("10"), "VISA");

        service.recalculateTotals(invoice);

        assertTrue(invoice.getDiscounts().isEmpty() || usd(invoice.getDiscounts()).signum() == 0);
        assertEquals(SUBTOTAL, usd(invoice.getGrandTotals()));
    }

    private Invoice invoice(BigDecimal discountPercentage, String scope) {
        return Invoice.builder()
            .id(1L)
            .discountPercentage(discountPercentage)
            .discountAppliesTo(scope)
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
