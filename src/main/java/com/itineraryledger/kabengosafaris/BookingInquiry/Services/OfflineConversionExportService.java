package com.itineraryledger.kabengosafaris.BookingInquiry.Services;

import com.itineraryledger.kabengosafaris.Attribution.Attribution;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.BookingInquiry;
import com.itineraryledger.kabengosafaris.BookingInquiry.Repository.BookingInquiryRepository;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Telling Google which clicks turned into bookings.
 *
 * <p>The tag can report an enquiry the moment it is sent, because the visitor is still on the
 * page. It cannot report a booking: that happens weeks later, in the office, when somebody
 * accepts a quotation. So the ads account can see that a campaign produces enquiries and has no
 * idea whether any of them became money - which is the difference between "47 clicks" and "one
 * booking at USD 900 of spend", and the only number that decides whether advertising pays.
 *
 * <p>The click id has been stored against every lead since the attribution work and has never
 * once been read back out. This reads it out.
 *
 * <p><b>Two rules this will not break.</b> Currencies are never summed - there is no exchange
 * rate anywhere in this system, by decision - so a booking invoiced in shillings as well as
 * dollars contributes its dollars and says so, rather than inventing a total that exists
 * nowhere. And a lead that cannot be exported is <b>named with a reason</b> rather than
 * quietly dropped: an export that silently halves itself is worse than one that refuses.
 */
@Service
@Slf4j
public class OfflineConversionExportService {

    /**
     * The click ids Google will accept back.
     *
     * <p>A lead can carry a Microsoft or a Meta click id - the capture takes sixteen kinds -
     * and uploading one of those to Google is not a mistake it will catch. It will match
     * nothing and quietly report a zero import.
     */
    private static final Set<String> GOOGLE_CLICK_IDS = Set.of("gclid", "gbraid", "wbraid");

    /** Google rejects a conversion whose click is older than this. */
    private static final int CLICK_AGE_LIMIT_DAYS = 90;

    /** `2026-11-26 14:30:00+03:00` - the form that carries its own offset, so no header is needed. */
    private static final DateTimeFormatter GOOGLE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssxxx");

    private final BookingInquiryRepository inquiryRepository;
    private final InvoiceRepository invoiceRepository;

    public OfflineConversionExportService(BookingInquiryRepository inquiryRepository,
                                          InvoiceRepository invoiceRepository) {
        this.inquiryRepository = inquiryRepository;
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Build the upload, and the list of everything that could not go in it.
     *
     * @param conversionName must match the conversion action's name in Google Ads <b>exactly</b>,
     *                       or the import is accepted and matches nothing
     * @param currency       which currency's total is the conversion value
     */
    @Transactional(readOnly = true)
    public Map<String, Object> build(String conversionName, String currency) {
        String action = conversionName == null || conversionName.isBlank()
            ? "Safari booking" : conversionName.trim();
        String money = currency == null || currency.isBlank() ? "USD" : currency.trim().toUpperCase();

        List<String[]> rows = new ArrayList<>();
        List<Map<String, String>> skipped = new ArrayList<>();

        /* Read once and index, rather than re-reading every invoice for every lead. */
        Map<Long, BigDecimal> bookedByCustomer = totalsByCustomer(money);

        for (BookingInquiry inquiry : inquiryRepository.findAll()) {
            /* Not a booking yet. Not a failure, and not worth naming - most leads are here. */
            if (inquiry.getConvertedAt() == null) continue;

            Attribution arrival = inquiry.getAttribution();
            if (arrival == null || arrival.getClickId() == null || arrival.getClickId().isBlank()) {
                skip(skipped, inquiry, "No click id recorded, so Google has nothing to match it to");
                continue;
            }
            String kind = arrival.getClickIdType() == null
                ? "" : arrival.getClickIdType().toLowerCase();
            if (!GOOGLE_CLICK_IDS.contains(kind)) {
                skip(skipped, inquiry, "Arrived through " + (kind.isBlank() ? "an unnamed source" : kind)
                    + ", which is not a Google click - upload it to that platform instead");
                continue;
            }

            BigDecimal value = inquiry.getCustomer() == null
                ? null : bookedByCustomer.get(inquiry.getCustomer().getId());
            if (value == null) {
                skip(skipped, inquiry, "No invoice in " + money
                    + " for this customer, so there is no conversion value to report");
                continue;
            }

            LocalDateTime converted = inquiry.getConvertedAt();
            if (tooOld(arrival, converted)) {
                skip(skipped, inquiry, "The click is more than " + CLICK_AGE_LIMIT_DAYS
                    + " days older than the booking, so Google will reject it");
                continue;
            }

            rows.add(new String[]{
                arrival.getClickId(),
                action,
                converted.atZone(ZoneId.systemDefault()).format(GOOGLE_TIME),
                value.toPlainString(),
                money,
            });
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("csv", toCsv(rows));
        result.put("rowCount", rows.size());
        result.put("conversionName", action);
        result.put("currency", money);
        result.put("skipped", skipped);
        log.info("Offline conversion export: {} row(s), {} skipped", rows.size(), skipped.size());
        return result;
    }

    /**
     * What the booking was worth, in one currency and one only.
     *
     * <p>Summing the dollars and the shillings would produce a number that exists in no
     * account anywhere. The shillings on these trips are one guest's park fees, not a second
     * price for the trip, so the dollar total is the conversion value and the rest is noted
     * nowhere because Google has no column for it.
     */
    private Map<Long, BigDecimal> totalsByCustomer(String currency) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        for (Invoice invoice : invoiceRepository.findAll()) {
            if (invoice.getCustomer() == null) continue;
            for (Price price : invoice.getGrandTotals()) {
                if (currency.equalsIgnoreCase(price.getCurrency()) && price.getTotalPrice() != null) {
                    totals.merge(invoice.getCustomer().getId(), price.getTotalPrice(), BigDecimal::add);
                }
            }
        }
        return totals;
    }

    private boolean tooOld(Attribution arrival, LocalDateTime converted) {
        LocalDateTime clicked = arrival.getFirstSeenAt();
        if (clicked == null) return false;   // unknown, so let Google be the judge
        return clicked.plusDays(CLICK_AGE_LIMIT_DAYS).isBefore(converted);
    }

    private void skip(List<Map<String, String>> skipped, BookingInquiry inquiry, String reason) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("code", inquiry.getCode());
        entry.put("name", inquiry.getDisplayName());
        entry.put("reason", reason);
        skipped.add(entry);
    }

    /**
     * Google's own column names, spelled exactly as the importer expects them.
     *
     * <p>No {@code Parameters:TimeZone} line: every timestamp carries its own offset, which
     * cannot be contradicted by a header somebody forgets to change when the server moves.
     */
    private String toCsv(List<String[]> rows) {
        StringBuilder csv = new StringBuilder(
            "Google Click ID,Conversion Name,Conversion Time,Conversion Value,Conversion Currency\n");
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) csv.append(',');
                csv.append(quote(row[i]));
            }
            csv.append('\n');
        }
        return csv.toString();
    }

    /** A conversion name is free text and can hold a comma, which would otherwise shift a column. */
    private String quote(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
