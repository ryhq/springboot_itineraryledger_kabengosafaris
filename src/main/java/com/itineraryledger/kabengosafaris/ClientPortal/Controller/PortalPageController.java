package com.itineraryledger.kabengosafaris.ClientPortal.Controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalView;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalAccessService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalActivityService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalViewService;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteDocumentRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteDocumentServices.QuoteDocumentStorageService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The pages a client actually opens.
 *
 * <p>Server rendered, on the API's own host, for three reasons that all point the same way. The
 * session cookie is then same origin, so there is no CORS to widen and no token sitting in
 * JavaScript for an injected script to read. There is no third application to build and deploy.
 * And the company's own branding, fonts and colours are already available here, because this is
 * where the PDFs are rendered from.
 *
 * <p><strong>Two tiers.</strong> The trip itself opens for anybody holding the link: a client
 * showing their family where they are going should not have to perform a verification, and there
 * is no money on that page. The quotation, the invoice, the documents and the device list need a
 * code, because that is where the figures and the personal details are.
 *
 * <p>Every method here answers the same question in the same way: resolve the link and the
 * cookie into an {@code Access}, and let that object decide. No page makes its own judgement, and
 * no page takes an id from the URL that could address another client's journey.
 */
@Controller
@RequestMapping("/portal")
@RequiredArgsConstructor
@Slf4j
public class PortalPageController {

    private final PortalAccessService access;
    private final PortalViewService views;
    private final PortalActivityService activity;
    private final QuoteDocumentRepository quoteDocuments;
    private final QuoteDocumentStorageService documentStorage;
    private final IdObfuscator idObfuscator;

    /** The trip. The one page that opens without a code. */
    @GetMapping("/{token}")
    public String trip(@PathVariable String token, HttpServletRequest request, Model model,
                       @RequestParam(required = false) String notice) {
        return render(token, request, model, "TRIP", notice, "portal/trip");
    }

    @GetMapping("/{token}/quote")
    public String quote(@PathVariable String token, HttpServletRequest request, Model model) {
        return renderPrivate(token, request, model, "QUOTE", "portal/quote");
    }

    @GetMapping("/{token}/invoice")
    public String invoice(@PathVariable String token, HttpServletRequest request, Model model) {
        return renderPrivate(token, request, model, "INVOICE", "portal/invoice");
    }

    @GetMapping("/{token}/documents")
    public String documents(@PathVariable String token, HttpServletRequest request, Model model) {
        return renderPrivate(token, request, model, "DOCUMENTS", "portal/documents");
    }

    /**
     * The devices that have been trusted, and the button that ends one.
     *
     * <p>A trusted browser is only safe because this page exists.
     */
    @GetMapping("/{token}/devices")
    public String devices(@PathVariable String token, HttpServletRequest request, Model model) {
        return renderPrivate(token, request, model, "DEVICES", "portal/devices");
    }

    /**
     * The bytes of one document, streamed by us rather than linked to.
     *
     * <p>Client paperwork is private media: the document modules hold passports and invoices, and
     * a URL is not a credential, which is why none of them is {@code permitAll}. This endpoint
     * does not change that. It checks the session, checks the document belongs to <em>this</em>
     * journey, and then reads the file itself.
     *
     * <p>That second check is the one that matters. An id in a URL is a request, not a claim:
     * without comparing it to the journey in the session, a client with one valid link could
     * walk the document table by changing a number.
     */
    @GetMapping("/{token}/documents/{documentId}/file")
    public ResponseEntity<byte[]> document(@PathVariable String token,
                                           @PathVariable String documentId,
                                           HttpServletRequest request) {
        PortalAccessService.Access resolved = resolve(token, request);
        if (!resolved.canReadPrivate()) return ResponseEntity.status(404).build();

        QuoteDocument document;
        try {
            document = quoteDocuments.findById(idObfuscator.decodeId(documentId)).orElse(null);
        } catch (Exception e) {
            return ResponseEntity.status(404).build();
        }

        if (document == null
                || !Boolean.TRUE.equals(document.getIsActive())
                || document.getQuote() == null
                || resolved.journey().getQuote() == null
                || !document.getQuote().getId().equals(resolved.journey().getQuote().getId())) {
            /*
             * 404 for every one of those, including "it exists but is not yours". Distinguishing
             * them tells a caller which ids are real.
             */
            return ResponseEntity.status(404).build();
        }

        byte[] bytes = documentStorage.readDocumentBytes(document.getFileName());
        if (bytes == null || bytes.length == 0) return ResponseEntity.status(404).build();

        activity.record(resolved.journey(), resolved.participant(), resolved.device(),
            "DOCUMENT", document.getTitle(), clientAddress(request),
            request.getHeader("User-Agent"));

        MediaType type = document.getFileType() == null
            ? MediaType.APPLICATION_OCTET_STREAM
            : MediaType.parseMediaType(document.getFileType());

        return ResponseEntity.ok()
            .contentType(type)
            /* inline: a client opening their own quotation expects to read it, not to save it */
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + safeFileName(document) + "\"")
            .body(bytes);
    }

    /** A filename from a record still goes into a header, so it is stripped rather than trusted. */
    private String safeFileName(QuoteDocument document) {
        String name = document.getOriginalFileName() != null
            ? document.getOriginalFileName()
            : document.getFileName();
        if (name == null) return "document.pdf";
        return name.replaceAll("[\"\\r\\n\\\\]", "_");
    }

    /** What we collect and how long we keep it. Open, because consent you must verify to read is not consent. */
    @GetMapping("/{token}/privacy")
    public String privacy(@PathVariable String token, HttpServletRequest request, Model model) {
        return render(token, request, model, "PRIVACY", null, "portal/privacy");
    }

    /** Where the code is typed. Deliberately says nothing about who the booking belongs to. */
    @GetMapping("/{token}/verify")
    public String verify(@PathVariable String token, HttpServletRequest request, Model model,
                         @RequestParam(required = false) String sent,
                         @RequestParam(required = false) String failed) {
        PortalAccessService.Access resolved = resolve(token, request);
        if (resolved.journey() == null || !resolved.canReadTrip()) return gone(model);

        model.addAttribute("page", views.build(resolved));
        model.addAttribute("codeSent", sent != null);
        model.addAttribute("failed", failed != null);
        activity.record(resolved.journey(), resolved.participant(), resolved.device(),
            "VERIFY_PAGE", null, clientAddress(request), request.getHeader("User-Agent"));
        return "portal/verify";
    }

    // ------------------------------------------------------------------ internals

    private String render(String token, HttpServletRequest request, Model model,
                          String section, String notice, String view) {
        PortalAccessService.Access resolved = resolve(token, request);
        if (resolved.journey() == null || !resolved.canReadTrip()) return gone(model);

        PortalView.Page page = views.build(resolved);
        model.addAttribute("page", page);
        model.addAttribute("notice", notice);
        touchAndRecord(resolved, request, section);
        return view;
    }

    /**
     * The same, but nothing is assembled unless they have verified.
     *
     * <p>An unverified caller is sent to the code page rather than refused, because they are
     * almost always the right person who has simply not proved it yet.
     */
    private String renderPrivate(String token, HttpServletRequest request, Model model,
                                 String section, String view) {
        PortalAccessService.Access resolved = resolve(token, request);
        if (resolved.journey() == null || !resolved.canReadTrip()) return gone(model);
        if (!resolved.canReadPrivate()) {
            return "redirect:/portal/" + resolved.journey().getToken() + "/verify";
        }

        model.addAttribute("page", views.build(resolved));
        touchAndRecord(resolved, request, section);
        return view;
    }

    private void touchAndRecord(PortalAccessService.Access resolved, HttpServletRequest request,
                                String section) {
        access.touch(resolved.device(), clientAddress(request));
        activity.record(resolved.journey(), resolved.participant(), resolved.device(),
            section, null, clientAddress(request), request.getHeader("User-Agent"));
    }

    private PortalAccessService.Access resolve(String token, HttpServletRequest request) {
        return access.resolve(token, PortalCookies.read(request));
    }

    /**
     * One page for every reason a link does not work: wrong, withdrawn, expired, never existed.
     *
     * <p>Saying which would turn the portal into a way of discovering whether a token is real.
     */
    private String gone(Model model) {
        model.addAttribute("companyName", "");
        return "portal/gone";
    }

    /** The same rule the rate limiter uses: the end of the forwarded chain, not the start. */
    static String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            return hops[hops.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
