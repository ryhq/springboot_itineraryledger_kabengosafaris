package com.itineraryledger.kabengosafaris.BookingInquiry;

import com.itineraryledger.kabengosafaris.Attribution.Attribution;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.BookingInquiry;
import com.itineraryledger.kabengosafaris.BookingInquiry.Repository.BookingInquiryRepository;
import com.itineraryledger.kabengosafaris.BookingInquiry.Services.OfflineConversionExportService;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Telling Google which clicks became bookings, and nothing that is not true.
 *
 * <p>An offline conversion upload is the only way the ads account can learn that an enquiry
 * became money, because the booking happens weeks later in an office and no browser is there
 * to report it. It is also the easiest file in the business to get quietly wrong: Google
 * accepts a well-formed upload, matches nothing in it, and reports a successful import of zero
 * conversions. There is no error to notice.
 *
 * <p>Three ways that happens, each pinned here.
 */
class AClickIsOnlyWorthUploadingToItsOwnPlatformTest {

    @Test
    @DisplayName("only a Google click id is sent to Google")
    void anotherPlatformsClickIsNotUploaded() {
        /*
         * The arrival capture takes sixteen kinds of click id, because a visitor can arrive
         * from anywhere. Uploading a Microsoft or a Meta one to Google is not a mistake Google
         * catches - it matches nothing and says the import succeeded.
         */
        Map<String, Object> export = export(
            booking("INQ-0001", "gclid", "G-CLICK", convertedAt(), 9000),
            booking("INQ-0002", "msclkid", "MS-CLICK", convertedAt(), 9000),
            booking("INQ-0003", "fbclid", "FB-CLICK", convertedAt(), 9000),
            booking("INQ-0004", "gbraid", "GB-CLICK", convertedAt(), 9000));

        String csv = (String) export.get("csv");
        assertEquals(2, export.get("rowCount"), "only the gclid and the gbraid belong to Google");
        assertTrue(csv.contains("G-CLICK") && csv.contains("GB-CLICK"));
        assertFalse(csv.contains("MS-CLICK"), "a Microsoft click must not be uploaded to Google");
        assertFalse(csv.contains("FB-CLICK"), "a Meta click must not be uploaded to Google");

        /* And the two that were left out are named, with a reason somebody can act on. */
        List<?> skipped = (List<?>) export.get("skipped");
        assertEquals(2, skipped.size(), "a lead left out of the file has to be named");
        assertTrue(skipped.toString().contains("msclkid"),
            "the reason should say which platform it belongs to instead: " + skipped);
    }

    @Test
    @DisplayName("the conversion value is one currency, never a sum of two")
    void shillingsAreNotAddedToDollars() {
        /*
         * These trips are invoiced in dollars, and in shillings for the park fees of a guest
         * who is a resident. There is no exchange rate anywhere in this system, by decision -
         * adding the two would produce a number that exists in no account anywhere, and it
         * would be the number the ads account optimised towards.
         */
        BookingInquiry lead = booking("INQ-0005", "gclid", "G-CLICK", convertedAt(), 0);
        Invoice invoice = invoiceFor(lead.getCustomer(),
            price("USD", 25645.96), price("TZS", 222222.00));

        Map<String, Object> export = export(List.of(lead), List.of(invoice), "Safari booking", "USD");

        String csv = (String) export.get("csv");
        assertTrue(csv.contains("25645.96"), "the dollar total is the conversion value: " + csv);
        assertFalse(csv.contains("222222"), "the shillings are a different currency, not more money");
        assertFalse(csv.contains("247867"), "the two must never be added together");
        assertTrue(csv.trim().endsWith("USD"), "the row says which currency it is in: " + csv);
    }

    @Test
    @DisplayName("the header is spelled the way the importer reads it")
    void googlesOwnColumnNames() {
        String csv = (String) export(booking("INQ-0006", "gclid", "G", convertedAt(), 100))
            .get("csv");

        assertTrue(csv.startsWith(
                "Google Click ID,Conversion Name,Conversion Time,Conversion Value,Conversion Currency"),
            "Google matches its columns by name, so one wrong word imports nothing: " + csv);

        /*
         * Every timestamp carries its own offset, so there is no Parameters:TimeZone line to
         * contradict it - a header nobody updates when the server moves is a silent error.
         */
        assertFalse(csv.contains("Parameters:TimeZone"),
            "the times are offset-qualified, so a timezone header would be a second source of truth");
        assertTrue(csv.lines().skip(1).findFirst().orElse("").matches(
                ".*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}.*"),
            "the conversion time must carry its offset: " + csv);
    }

    @Test
    @DisplayName("a lead with no booking, or no click, is left out and said so")
    void nothingIsDroppedSilently() {
        BookingInquiry neverBooked = booking("INQ-0007", "gclid", "G-CLICK", null, 9000);
        BookingInquiry noClick = booking("INQ-0008", null, null, convertedAt(), 9000);

        Map<String, Object> export = export(neverBooked, noClick);

        assertEquals(0, export.get("rowCount"));
        List<?> skipped = (List<?>) export.get("skipped");
        /*
         * A lead that never became a booking is not a failure and is not named - most leads
         * are that. One that DID become a booking and still cannot be uploaded is named,
         * because that is money the ads account will never learn about.
         */
        assertEquals(1, skipped.size(), "only the booking that cannot be uploaded is worth naming");
        assertTrue(skipped.toString().contains("INQ-0008"), skipped.toString());
    }

    /* ------------------------------------------------------------------ fixtures */

    private static LocalDateTime convertedAt() {
        return LocalDateTime.now().minusDays(10);
    }

    private static Price price(String currency, double amount) {
        Price p = new Price();
        p.setCurrency(currency);
        p.setTotalPrice(BigDecimal.valueOf(amount));
        return p;
    }

    private static long nextCustomerId = 1;

    private static BookingInquiry booking(String code, String clickIdType, String clickId,
                                          LocalDateTime converted, double usd) {
        BookingInquiry inquiry = new BookingInquiry();
        inquiry.setCode(code);
        inquiry.setFirstName("Test");
        inquiry.setConvertedAt(converted);

        Customer customer = new Customer();
        customer.setId(nextCustomerId++);
        inquiry.setCustomer(customer);

        if (clickId != null) {
            Attribution arrival = new Attribution();
            arrival.setClickId(clickId);
            arrival.setClickIdType(clickIdType);
            inquiry.setAttribution(arrival);
        }
        BOOKED_VALUE.put(inquiry, usd);
        return inquiry;
    }

    /**
     * What each fixture lead was invoiced, kept beside the lead rather than on it.
     *
     * <p>A test-only setter on a production entity is a field somebody later reads in earnest.
     */
    private static final Map<BookingInquiry, Double> BOOKED_VALUE = new java.util.IdentityHashMap<>();

    private static Invoice invoiceFor(Customer customer, Price... prices) {
        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);
        invoice.setGrandTotals(new ArrayList<>(List.of(prices)));
        return invoice;
    }

    private static Map<String, Object> export(BookingInquiry... leads) {
        List<Invoice> invoices = new ArrayList<>();
        for (BookingInquiry lead : leads) {
            double usd = BOOKED_VALUE.getOrDefault(lead, 0.0);
            if (usd > 0) invoices.add(invoiceFor(lead.getCustomer(), price("USD", usd)));
        }
        return export(List.of(leads), invoices, "Safari booking", "USD");
    }

    private static Map<String, Object> export(List<BookingInquiry> leads, List<Invoice> invoices,
                                              String action, String currency) {
        BookingInquiryRepository inquiries = mock(BookingInquiryRepository.class);
        InvoiceRepository invoiceRepo = mock(InvoiceRepository.class);
        when(inquiries.findAll()).thenReturn(leads);
        when(invoiceRepo.findAll()).thenReturn(invoices);
        return new OfflineConversionExportService(inquiries, invoiceRepo).build(action, currency);
    }
}
