package com.itineraryledger.kabengosafaris.Inclusion.Services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Inclusion.DTOs.DocumentInclusionDTO;
import com.itineraryledger.kabengosafaris.Inclusion.DTOs.SetDocumentInclusionsDTO;
import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionsSource;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.InvoiceInclusion.Entity.InvoiceInclusion;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteInclusion.Entity.QuoteInclusion;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariInclusion.Entity.SafariInclusion;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reading, editing and resetting what a quote, safari or invoice says its price covers.
 *
 * <p>Editable at every step, which is the point of the chain: five lines on the itinerary, two more
 * added while negotiating the quote, and the safari and invoice that follow carry seven. Editing
 * here never reaches back up — the itinerary still has five, and the next customer's quote starts
 * from five.
 *
 * <p>Reset is the other half of that. Without it an edit made by mistake two documents ago is
 * unrecoverable; with it, the parent's version is always one click away and the screen can say
 * honestly whether this document still matches its parent.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DocumentInclusionService {

    private final QuoteRepository quoteRepository;
    private final SafariRepository safariRepository;
    private final InvoiceRepository invoiceRepository;
    private final InclusionSnapshotService snapshot;
    private final InclusionAccuracyService accuracy;
    private final IdObfuscator idObfuscator;

    /* ================================================================== quote */

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getQuote(String obfuscatedId) {
        Quote quote = quote(obfuscatedId);
        if (quote == null) return notFound("Quote");

        List<DocumentInclusionDTO> rows = quote.getInclusionList().stream()
            .map(row -> DocumentInclusionDTO.builder()
                .id(idObfuscator.encodeId(row.getId()))
                .inclusionItemId(row.getInclusionItem() != null
                    ? idObfuscator.encodeId(row.getInclusionItem().getId()) : null)
                .label(row.getLabel())
                .category(row.getCategory())
                .claimAppliesTo(row.getClaimAppliesTo())
                .isIncluded(row.getIsIncluded())
                .sortOrder(row.getSortOrder())
                .build())
            .toList();

        ResponseEntity<ApiResponse<?>> response = ok(rows, quote.getInclusionsSource(),
            quote.getInclusionsSyncedAt(),
            quote.getItinerary() != null ? quote.getItinerary().getCode() : null, "itinerary");

        /*
         * Computed on the read the screen already makes, rather than behind an endpoint the panel
         * has to remember to call — a warning nobody fetches is a warning nobody sees. It reads
         * collections already loaded, so it costs nothing extra.
         *
         * Only on the quote. That is where the promise is negotiated and where somebody can still
         * change either half; on a safari the trip is sold and on an invoice the money is being
         * collected, so a warning there would be noise about a decision already taken.
         */
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody().getData();
        body.put("inclusionWarnings", accuracy.check(quote));
        return response;
    }

    @AuditLogAnnotation(action = "SET_QUOTE_INCLUSIONS",
        description = "Editing what a quote's price covers", entityType = "Quote")
    public ResponseEntity<ApiResponse<?>> setQuote(String obfuscatedId, SetDocumentInclusionsDTO dto) {
        Quote quote = quote(obfuscatedId);
        if (quote == null) return notFound("Quote");
        List<SetDocumentInclusionsDTO.Row> rows = validated(dto);
        if (rows == null) return badRows();

        quote.getInclusionList().clear();
        int order = 1;
        for (SetDocumentInclusionsDTO.Row row : rows) {
            quote.addInclusion(QuoteInclusion.builder()
                .label(row.getLabel().trim())
                .category(trim(row.getCategory()))
                .claimAppliesTo(trim(row.getClaimAppliesTo()))
                .isIncluded(!Boolean.FALSE.equals(row.getIsIncluded()))
                .sortOrder(order++)
                .build());
        }
        quote.setInclusionsSource(InclusionsSource.EDITED);
        quoteRepository.save(quote);
        return saved(order - 1, "quote");
    }

    /** Back to what the itinerary says, discarding what was changed here. */
    @AuditLogAnnotation(action = "RESET_QUOTE_INCLUSIONS",
        description = "Resetting a quote's inclusions to its itinerary", entityType = "Quote")
    public ResponseEntity<ApiResponse<?>> resetQuote(String obfuscatedId) {
        Quote quote = quote(obfuscatedId);
        if (quote == null) return notFound("Quote");
        if (quote.getItinerary() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "This quote has no itinerary to reset from", "NO_PARENT_TO_RESET_FROM"));
        }
        int copied = snapshot.itineraryToQuote(quote.getItinerary(), quote);
        quoteRepository.save(quote);
        return reset(copied, "itinerary " + quote.getItinerary().getCode());
    }

    /* ================================================================= safari */

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getSafari(String obfuscatedId) {
        Safari safari = safari(obfuscatedId);
        if (safari == null) return notFound("Safari");

        List<DocumentInclusionDTO> rows = safari.getInclusionList().stream()
            .map(row -> DocumentInclusionDTO.builder()
                .id(idObfuscator.encodeId(row.getId()))
                .inclusionItemId(row.getInclusionItem() != null
                    ? idObfuscator.encodeId(row.getInclusionItem().getId()) : null)
                .label(row.getLabel())
                .category(row.getCategory())
                .claimAppliesTo(row.getClaimAppliesTo())
                .isIncluded(row.getIsIncluded())
                .sortOrder(row.getSortOrder())
                .build())
            .toList();

        Quote parent = latestQuoteOf(safari);
        return ok(rows, safari.getInclusionsSource(), safari.getInclusionsSyncedAt(),
            parent != null ? parent.getQuoteCode()
                : (safari.getItinerary() != null ? safari.getItinerary().getCode() : null),
            parent != null ? "quote" : "itinerary");
    }

    @AuditLogAnnotation(action = "SET_SAFARI_INCLUSIONS",
        description = "Editing what a safari's price covers", entityType = "Safari")
    public ResponseEntity<ApiResponse<?>> setSafari(String obfuscatedId, SetDocumentInclusionsDTO dto) {
        Safari safari = safari(obfuscatedId);
        if (safari == null) return notFound("Safari");
        List<SetDocumentInclusionsDTO.Row> rows = validated(dto);
        if (rows == null) return badRows();

        safari.getInclusionList().clear();
        int order = 1;
        for (SetDocumentInclusionsDTO.Row row : rows) {
            safari.addInclusion(SafariInclusion.builder()
                .label(row.getLabel().trim())
                .category(trim(row.getCategory()))
                .claimAppliesTo(trim(row.getClaimAppliesTo()))
                .isIncluded(!Boolean.FALSE.equals(row.getIsIncluded()))
                .sortOrder(order++)
                .build());
        }
        safari.setInclusionsSource(InclusionsSource.EDITED);
        safariRepository.save(safari);
        return saved(order - 1, "safari");
    }

    /** Back to the quote the customer accepted, or the itinerary when there was no quote. */
    @AuditLogAnnotation(action = "RESET_SAFARI_INCLUSIONS",
        description = "Resetting a safari's inclusions to its quote", entityType = "Safari")
    public ResponseEntity<ApiResponse<?>> resetSafari(String obfuscatedId) {
        Safari safari = safari(obfuscatedId);
        if (safari == null) return notFound("Safari");

        Quote parent = latestQuoteOf(safari);
        if (parent != null && !parent.getInclusionList().isEmpty()) {
            int copied = snapshot.quoteToSafari(parent, safari);
            safariRepository.save(safari);
            return reset(copied, "quote " + parent.getQuoteCode());
        }
        if (safari.getItinerary() != null) {
            int copied = snapshot.itineraryToSafari(safari.getItinerary(), safari);
            safariRepository.save(safari);
            return reset(copied, "itinerary " + safari.getItinerary().getCode());
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(400,
            "This safari has neither a quote nor an itinerary to reset from",
            "NO_PARENT_TO_RESET_FROM"));
    }

    /* ================================================================ invoice */

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getInvoice(String obfuscatedId) {
        Invoice invoice = invoice(obfuscatedId);
        if (invoice == null) return notFound("Invoice");

        List<DocumentInclusionDTO> rows = invoice.getInclusionList().stream()
            .map(row -> DocumentInclusionDTO.builder()
                .id(idObfuscator.encodeId(row.getId()))
                .inclusionItemId(row.getInclusionItem() != null
                    ? idObfuscator.encodeId(row.getInclusionItem().getId()) : null)
                .label(row.getLabel())
                .category(row.getCategory())
                .claimAppliesTo(row.getClaimAppliesTo())
                .isIncluded(row.getIsIncluded())
                .sortOrder(row.getSortOrder())
                .build())
            .toList();

        return ok(rows, invoice.getInclusionsSource(), invoice.getInclusionsSyncedAt(),
            invoice.getSafari() != null ? invoice.getSafari().getCode() : null, "safari");
    }

    @AuditLogAnnotation(action = "SET_INVOICE_INCLUSIONS",
        description = "Editing what an invoice's price covers", entityType = "Invoice")
    public ResponseEntity<ApiResponse<?>> setInvoice(String obfuscatedId, SetDocumentInclusionsDTO dto) {
        Invoice invoice = invoice(obfuscatedId);
        if (invoice == null) return notFound("Invoice");
        List<SetDocumentInclusionsDTO.Row> rows = validated(dto);
        if (rows == null) return badRows();

        invoice.getInclusionList().clear();
        int order = 1;
        for (SetDocumentInclusionsDTO.Row row : rows) {
            invoice.addInclusion(InvoiceInclusion.builder()
                .label(row.getLabel().trim())
                .category(trim(row.getCategory()))
                .claimAppliesTo(trim(row.getClaimAppliesTo()))
                .isIncluded(!Boolean.FALSE.equals(row.getIsIncluded()))
                .sortOrder(order++)
                .build());
        }
        invoice.setInclusionsSource(InclusionsSource.EDITED);
        invoiceRepository.save(invoice);
        return saved(order - 1, "invoice");
    }

    /** Back to the safari being billed. */
    @AuditLogAnnotation(action = "RESET_INVOICE_INCLUSIONS",
        description = "Resetting an invoice's inclusions to its safari", entityType = "Invoice")
    public ResponseEntity<ApiResponse<?>> resetInvoice(String obfuscatedId) {
        Invoice invoice = invoice(obfuscatedId);
        if (invoice == null) return notFound("Invoice");
        if (invoice.getSafari() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "This invoice has no safari to reset from", "NO_PARENT_TO_RESET_FROM"));
        }
        Quote parent = latestQuoteOf(invoice.getSafari());
        int copied = snapshot.toInvoice(invoice.getSafari(), parent, invoice);
        invoiceRepository.save(invoice);
        return reset(copied, "safari " + invoice.getSafari().getCode());
    }

    /* ================================================================= shared */

    /**
     * The rows a caller sent, or null when any of them is unusable.
     *
     * <p>Validated whole before anything is written, so a bad row cannot leave a document holding
     * half a promise. An empty list is legitimate — it means this document says nothing about what
     * its price covers — and is not the same as never having been set.
     */
    private List<SetDocumentInclusionsDTO.Row> validated(SetDocumentInclusionsDTO dto) {
        if (dto == null || dto.getItems() == null) return null;
        List<SetDocumentInclusionsDTO.Row> rows = new ArrayList<>();
        for (SetDocumentInclusionsDTO.Row row : dto.getItems()) {
            if (row == null || row.getLabel() == null || row.getLabel().isBlank()) return null;
            rows.add(row);
        }
        return rows;
    }

    /**
     * The quote a safari most likely came from.
     *
     * <p><strong>A safari has no foreign key to its quote.</strong> The best available answer is
     * the latest quote for the same itinerary and the same customer, which is exactly the lookup
     * {@code InvoiceFromSafariGenerationService} already uses to inherit tax and discount scopes —
     * so at least the two agree about which quote a trip descends from.
     *
     * <p>It is a heuristic, and where it matters the screen says which document it would reset
     * from before doing it. A customer sent three versions of a quote gets the newest; if that is
     * not the one they accepted, the reset is visible and reversible rather than silent.
     */
    private Quote latestQuoteOf(Safari safari) {
        if (safari == null || safari.getItinerary() == null || safari.getCustomer() == null) {
            return null;
        }
        List<Quote> quotes = quoteRepository.findByItineraryAndCustomerOrdered(
            safari.getItinerary().getId(), safari.getCustomer().getId());
        return quotes.isEmpty() ? null : quotes.get(0);
    }

    private ResponseEntity<ApiResponse<?>> ok(
        List<DocumentInclusionDTO> rows,
        InclusionsSource source,
        LocalDateTime syncedAt,
        String parentCode,
        String parentKind
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("inclusions", rows);
        body.put("includedCount", rows.stream().filter(r -> Boolean.TRUE.equals(r.getIsIncluded())).count());
        body.put("excludedCount", rows.stream().filter(r -> Boolean.FALSE.equals(r.getIsIncluded())).count());
        body.put("inclusionsSource", source != null ? source.name() : null);
        body.put("inclusionsSourceLabel", source != null ? source.getDisplayName() : "Not recorded");
        body.put("inclusionsSyncedAt", syncedAt);
        body.put("parentCode", parentCode);
        body.put("parentKind", parentKind);
        body.put("canReset", parentCode != null);
        return ResponseEntity.ok(ApiResponse.success(200, "Inclusions retrieved successfully", body));
    }

    private ResponseEntity<ApiResponse<?>> saved(int count, String what) {
        Map<String, Object> body = new HashMap<>();
        body.put("savedCount", count);
        return ResponseEntity.ok(ApiResponse.success(200,
            "Saved — this " + what + " now states its own " + count + " line(s), "
            + "and no longer follows what it was copied from.", body));
    }

    private ResponseEntity<ApiResponse<?>> reset(int count, String parent) {
        Map<String, Object> body = new HashMap<>();
        body.put("copiedCount", count);
        return ResponseEntity.ok(ApiResponse.success(200,
            "Reset to " + parent + " — " + count + " line(s) copied, and anything edited here has "
            + "been discarded.", body));
    }

    private ResponseEntity<ApiResponse<?>> notFound(String what) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiResponse.error(404, what + " not found", what.toUpperCase() + "_NOT_FOUND"));
    }

    private ResponseEntity<ApiResponse<?>> badRows() {
        return ResponseEntity.badRequest().body(ApiResponse.error(400,
            "Every line needs wording. Send an empty list to say nothing at all.",
            "INCLUSION_LABEL_REQUIRED"));
    }

    private String trim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Quote quote(String id) {
        try {
            return quoteRepository.findById(idObfuscator.decodeId(id)).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Safari safari(String id) {
        try {
            return safariRepository.findById(idObfuscator.decodeId(id)).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Invoice invoice(String id) {
        try {
            return invoiceRepository.findById(idObfuscator.decodeId(id)).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
