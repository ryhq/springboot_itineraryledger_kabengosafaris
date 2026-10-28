package com.itineraryledger.kabengosafaris.ClientPortal.Controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalOutcome;
import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalView;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalAccessService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalCatalogueService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalChangeLogService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalCommentService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalEditingService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalProposalService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalActivityService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalViewService;
import com.itineraryledger.kabengosafaris.Configurations.SecurityConfigurations;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Repository.QuoteDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteDocumentRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteDocumentServices.QuoteDocumentStorageService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The portal as JSON, for the pages that live in the panel's codebase.
 *
 * <p>The client portal is now a handful of React routes built from the same source as the
 * management app and served from its own hostname, so that it looks like something a company
 * would send a customer rather than like an internal tool. This is what those pages read.
 *
 * <p><strong>The session is a header, not a cookie.</strong> One company's panel and API are on
 * different registrable domains, so a SameSite cookie is simply never sent and the portal would
 * work for one company and silently not for the other. A header the browser does not attach by
 * itself is also what removes the need for a CSRF token: another origin can make the request,
 * but it cannot make the browser add {@code X-Portal-Session}.
 *
 * <p>Everything else is unchanged and deliberately shared with the server-rendered pages: the
 * same access resolution, the same view model with no field that can hold a cost, the same
 * activity log. Two front ends, one set of rules, because the second copy of a rule is the one
 * that is wrong.
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
@Slf4j
public class PortalApiController {

    private final PortalAccessService access;
    private final PortalViewService views;
    private final PortalChangeLogService changeLog;
    private final PortalProposalService proposals;
    private final PortalEditingService editing;
    private final PortalCatalogueService catalogue;
    private final QuoteDayAccommodationRepository dayAccommodations;
    private final PortalCommentService comments;
    private final PortalActivityService activity;
    private final QuoteDocumentRepository quoteDocuments;
    private final QuoteDocumentStorageService documentStorage;
    private final IdObfuscator idObfuscator;

    /**
     * Everything this caller may see of this journey.
     *
     * <p>One call rather than one per tab. The page is small, the parts are related, and a
     * client on a hotel connection should wait once.
     */
    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<?>> page(
        @PathVariable String token,
        @RequestHeader(value = SecurityConfigurations.PORTAL_SESSION_HEADER, required = false) String session,
        HttpServletRequest request
    ) {
        PortalAccessService.Access resolved = access.resolve(token, session);
        if (resolved.journey() == null || !resolved.canReadTrip()) {
            return gone();
        }

        PortalView.Page page = views.build(resolved);
        access.touch(resolved.device(), PortalPageController.clientAddress(request));
        activity.record(resolved.journey(), resolved.participant(), resolved.device(),
            "TRIP", null, PortalPageController.clientAddress(request),
            request.getHeader("User-Agent"));

        return ResponseEntity.ok(ApiResponse.success(200, "Trip retrieved", page));
    }

    /**
     * Send a code, if that address is on this booking.
     *
     * <p>The answer is the same either way. A response that distinguishes a known address from
     * an unknown one is a way of discovering who our customers are, one address at a time, and
     * it is the kind of difference that is invisible in a screenshot and obvious to a script.
     */
    @PostMapping(value = "/access/request", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<?>> requestCode(@RequestBody Map<String, String> body,
                                                      HttpServletRequest request) {
        String token = body.get("token");
        PortalAccessService.Access resolved = access.resolve(token, null);
        if (resolved.journey() != null && resolved.canReadTrip()) {
            access.requestCode(resolved.journey(), body.get("email"),
                PortalPageController.clientAddress(request));
        }
        return ResponseEntity.ok(ApiResponse.success(200,
            "If that address is on this booking, the code is on its way.", null));
    }

    /**
     * Check the code and hand back a session.
     *
     * <p>The token goes in the body because the page has to keep it; there is nowhere else for
     * it to go when the two hostnames are different sites. It is the client's own session and
     * can read one journey, so the thing it is worth to an attacker is bounded by that.
     */
    @PostMapping(value = "/access/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<?>> verify(@RequestBody Map<String, Object> body,
                                                 HttpServletRequest request) {
        String token = String.valueOf(body.getOrDefault("token", ""));
        PortalAccessService.Access resolved = access.resolve(token, null);
        if (resolved.journey() == null || !resolved.canReadTrip()) {
            return gone();
        }

        boolean remember = !Boolean.FALSE.equals(body.get("remember"));
        PortalAccessService.Verification result = access.verify(
            resolved.journey(),
            String.valueOf(body.getOrDefault("email", "")),
            String.valueOf(body.getOrDefault("code", "")),
            PortalPageController.clientAddress(request),
            request.getHeader("User-Agent"),
            remember);

        if (!result.accepted()) {
            /*
             * One message for every refusal: wrong code, expired code, exhausted attempts, an
             * address that is not on the booking. The reason is in the log for us, not on the
             * page for whoever is typing.
             */
            log.info("Portal verification refused on journey {}: {}",
                resolved.journey().getId(), result.reason());
            return ResponseEntity.status(401).body(ApiResponse.error(401,
                "That code was not right, or it has expired. Ask for another one.",
                "PORTAL_CODE_REFUSED"));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("session", result.deviceToken());
        return ResponseEntity.ok(ApiResponse.success(200, "Verified", payload));
    }

    /**
     * Every round of this quotation, and what moved in each.
     *
     * <p>The verified tier, because a change list is mostly about money. The same service answers
     * the office's Portal tab, so the two sides are reading one list rather than two summaries
     * that can disagree about what was agreed.
     */
    @GetMapping("/{token}/changes")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> changes(
        @PathVariable String token,
        @RequestHeader(value = SecurityConfigurations.PORTAL_SESSION_HEADER, required = false) String session,
        HttpServletRequest request
    ) {
        PortalAccessService.Access resolved = access.resolve(token, session);
        if (resolved.journey() == null || !resolved.canReadTrip()) {
            return gone();
        }
        if (!resolved.canReadPrivate()) {
            return ResponseEntity.status(401).body(ApiResponse.error(401,
                "Please enter a code first", "PORTAL_NOT_VERIFIED"));
        }

        activity.record(resolved.journey(), resolved.participant(), resolved.device(),
            "CHANGES", null, PortalPageController.clientAddress(request),
            request.getHeader("User-Agent"));

        return ResponseEntity.ok(ApiResponse.success(200, "Changes retrieved",
            changeLog.forQuote(resolved.journey().getQuote())));
    }

    /**
     * "Please look at this", "yes", and "no".
     *
     * <p>One endpoint for the three, because they are one decision with three answers and three
     * endpoints would be three places to forget the same four checks. The action is in the body
     * rather than the path so that an unknown one is a 400 from this method, not a 404 from the
     * router that reads like a broken link.
     *
     * <p>Submitting does not lock the client out. That is the property the whole loop is built
     * on and it is stated again here because this is where somebody would quietly remove it.
     */
    @PostMapping(value = "/{token}/proposal", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<?>> proposal(
        @PathVariable String token,
        @RequestBody Map<String, String> body,
        @RequestHeader(value = SecurityConfigurations.PORTAL_SESSION_HEADER, required = false) String session
    ) {
        PortalAccessService.Access resolved = access.resolve(token, session);
        if (resolved.journey() == null || !resolved.canReadTrip()) {
            return gone();
        }
        if (!resolved.canReadPrivate()) {
            return ResponseEntity.status(401).body(ApiResponse.error(401,
                "Please enter a code first", "PORTAL_NOT_VERIFIED"));
        }
        /*
         * Only whether they are the lead, and nothing else.
         *
         * canAct() would be the obvious call and it is the wrong one here: it also folds in
         * whether the journey is frozen or closed, so a frozen booking would tell the lead
         * traveller that they are not the lead traveller. Every other reason belongs to the
         * state machine, which has a sentence for each of them.
         *
         * A reader may look at everything the lead can look at and decide none of it. Somebody
         * invited to read a honeymoon itinerary must not be able to accept it.
         */
        if (resolved.participant() == null || !resolved.participant().mayAct()) {
            return ResponseEntity.status(403).body(ApiResponse.error(403,
                "Only the lead traveller on this booking can do that.", "PORTAL_NOT_THE_LEAD"));
        }

        String action = String.valueOf(body.getOrDefault("action", "")).toUpperCase();
        PortalOutcome outcome = switch (action) {
            case "SUBMIT" -> proposals.submit(resolved.journey(), resolved.participant());
            case "AGREE" -> proposals.agree(resolved.journey(), resolved.participant());
            case "DECLINE" -> proposals.decline(resolved.journey(), resolved.participant(),
                body.get("reason"));
            default -> null;
        };

        if (outcome == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "That is not something you can do here.", "PORTAL_UNKNOWN_ACTION"));
        }
        if (!outcome.done()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, outcome.message(), outcome.code()));
        }
        return ResponseEntity.ok(ApiResponse.success(200, outcome.message(), null));
    }

    /**
     * The four things a client may change about their own quotation.
     *
     * <p>One endpoint, for the reason the proposal endpoint is one: they are four answers to
     * one question and four endpoints would be four places to forget the same checks. Every
     * one of them is refused unless the quotation may change, the client's level reaches that
     * far, and the row named is on this booking.
     *
     * <p>The id is in the body and is decoded here, which is safe in a way a stored id is not:
     * it was minted by the page the client is looking at, in this run of the application. It is
     * still only a request. Whether the row is theirs is proved in the service, by walking back
     * from the row to the quotation on their journey.
     */
    @PostMapping(value = "/{token}/edit", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<ApiResponse<?>> edit(
        @PathVariable String token,
        @RequestBody Map<String, Object> body,
        @RequestHeader(value = SecurityConfigurations.PORTAL_SESSION_HEADER, required = false) String session
    ) {
        PortalAccessService.Access resolved = access.resolve(token, session);
        if (resolved.journey() == null || !resolved.canReadTrip()) {
            return gone();
        }
        if (!resolved.canReadPrivate()) {
            return ResponseEntity.status(401).body(ApiResponse.error(401,
                "Please enter a code first", "PORTAL_NOT_VERIFIED"));
        }
        if (resolved.participant() == null || !resolved.participant().mayAct()) {
            return ResponseEntity.status(403).body(ApiResponse.error(403,
                "Only the lead traveller on this booking can change it.", "PORTAL_NOT_THE_LEAD"));
        }

        Long rowId;
        try {
            rowId = idObfuscator.decodeId(String.valueOf(body.get("id")));
        } catch (Exception e) {
            /* an id we cannot read and an id that is not theirs get the same answer */
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "That is not on this booking.", "NOT_ON_THIS_BOOKING"));
        }

        String action = String.valueOf(body.getOrDefault("action", "")).toUpperCase();
        PortalOutcome outcome = switch (action) {
            case "STAY" -> editing.chooseStay(resolved.journey(), resolved.participant(), rowId);
            case "ROOMS" -> editing.setRooms(resolved.journey(), resolved.participant(),
                rowId, asInteger(body.get("value")));
            case "ACTIVITY" -> editing.setActivityIncluded(resolved.journey(),
                resolved.participant(), rowId, Boolean.TRUE.equals(body.get("value")));
            case "PARTY" -> editing.setPartySize(resolved.journey(), resolved.participant(),
                rowId, asInteger(body.get("value")));
            case "CATALOGUE" -> editing.chooseFromCatalogue(resolved.journey(),
                resolved.participant(), rowId, decodeOrNull(body.get("option")));
            default -> null;
        };

        if (outcome == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "That is not something you can change here.", "PORTAL_UNKNOWN_ACTION"));
        }
        if (!outcome.done()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, outcome.message(), outcome.code()));
        }
        return ResponseEntity.ok(ApiResponse.success(200, outcome.message(), null));
    }

    /**
     * What else they could sleep in on one night, at editing level CHOOSE_CATALOGUE.
     *
     * <p>Asked for one row at a time rather than for the whole trip, because the answer depends
     * on where that night already is and what we hold a price for on that date. A whole-trip
     * version would be fourteen region searches to answer a question about one of them.
     *
     * <p>The list is empty, not refused, when the level does not reach this far. The page does
     * not offer the control in that case, and an empty list is the honest answer to a request
     * that should not have been made rather than an error to handle twice.
     */
    @GetMapping("/{token}/catalogue")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> catalogue(
        @PathVariable String token,
        @RequestParam("row") String rowId,
        @RequestHeader(value = SecurityConfigurations.PORTAL_SESSION_HEADER, required = false) String session
    ) {
        PortalAccessService.Access resolved = access.resolve(token, session);
        if (resolved.journey() == null || !resolved.canReadTrip()) return gone();
        if (!resolved.canReadPrivate()) {
            return ResponseEntity.status(401).body(ApiResponse.error(401,
                "Please enter a code first", "PORTAL_NOT_VERIFIED"));
        }
        if (editing.whyNotEditable(resolved.journey()) != null) {
            return ResponseEntity.ok(ApiResponse.success(200, "Nothing to choose from", List.of()));
        }

        Long id = decodeOrNull(rowId);
        QuoteDayAccommodation row = id == null ? null
            : dayAccommodations.findById(id).orElse(null);

        /*
         * The row has to be on THIS booking, the same rule as every edit. A catalogue for
         * somebody else's night would leak which region they are travelling in.
         */
        if (row == null || row.getQuoteDay() == null || row.getQuoteDay().getQuote() == null
                || resolved.journey().getQuote() == null
                || !row.getQuoteDay().getQuote().getId().equals(resolved.journey().getQuote().getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "That is not on this booking.", "NOT_ON_THIS_BOOKING"));
        }

        return ResponseEntity.ok(ApiResponse.success(200, "Options retrieved",
            catalogue.optionsFor(resolved.journey().getQuote(), row)));
    }

    /** An id we cannot read and an id that is not theirs get the same answer further in. */
    private Long decodeOrNull(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try {
            return idObfuscator.decodeId(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    /** JSON hands back a Number or a String depending on who wrote the caller. */
    private Integer asInteger(Object value) {
        if (value instanceof Number number) return number.intValue();
        try {
            return value == null ? null : Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * What has been said about this quotation, part by part.
     *
     * <p>Only what is visible to them, filtered in the query rather than here. The same thread
     * carries the office talking among themselves, and a filter applied in a controller is a
     * filter somebody refactors out of one of two call sites.
     */
    @GetMapping("/{token}/comments")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> comments(
        @PathVariable String token,
        @RequestHeader(value = SecurityConfigurations.PORTAL_SESSION_HEADER, required = false) String session
    ) {
        PortalAccessService.Access resolved = access.resolve(token, session);
        if (resolved.journey() == null || !resolved.canReadTrip()) return gone();
        if (!resolved.canReadPrivate()) {
            return ResponseEntity.status(401).body(ApiResponse.error(401,
                "Please enter a code first", "PORTAL_NOT_VERIFIED"));
        }
        return ResponseEntity.ok(ApiResponse.success(200, "Comments retrieved",
            comments.forClient(resolved.journey().getQuote())));
    }

    /**
     * Say something about one part of the trip.
     *
     * <p>Deliberately NOT gated on who holds the pen. Editing is about not overwriting each
     * other's work; a question is never a conflict. A client who can see their quotation and is
     * told they may not ask about it has been handed a document with a closed door beside it,
     * which is the position the portal exists to get them out of.
     *
     * <p>Any verified participant may comment, including an invited reader. They may not decide
     * anything, but somebody invited onto a honeymoon is exactly the person who knows the
     * flight gets in at midnight.
     */
    @PostMapping(value = "/{token}/comments", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<ApiResponse<?>> comment(
        @PathVariable String token,
        @RequestBody Map<String, Object> body,
        @RequestHeader(value = SecurityConfigurations.PORTAL_SESSION_HEADER, required = false) String session
    ) {
        PortalAccessService.Access resolved = access.resolve(token, session);
        if (resolved.journey() == null || !resolved.canReadTrip()) return gone();
        if (!resolved.canReadPrivate()) {
            return ResponseEntity.status(401).body(ApiResponse.error(401,
                "Please enter a code first", "PORTAL_NOT_VERIFIED"));
        }

        Long targetId = null;
        Object raw = body.get("targetId");
        if (raw != null && !String.valueOf(raw).isBlank()) {
            try {
                targetId = idObfuscator.decodeId(String.valueOf(raw));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "That is not on this booking.", "NOT_ON_THIS_BOOKING"));
            }
        }

        PortalOutcome outcome = comments.addFromClient(
            resolved.journey(), resolved.participant(),
            body.get("anchor") == null ? null : String.valueOf(body.get("anchor")),
            asInteger(body.get("dayNumber")),
            targetId,
            body.get("body") == null ? null : String.valueOf(body.get("body")));

        if (!outcome.done()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, outcome.message(), outcome.code()));
        }
        return ResponseEntity.ok(ApiResponse.success(200, outcome.message(), null));
    }

    /** End this browser's session, leaving every other device alone. */
    @PostMapping(value = "/access/sign-out", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<?>> signOut(
        @RequestBody Map<String, String> body,
        @RequestHeader(value = SecurityConfigurations.PORTAL_SESSION_HEADER, required = false) String session
    ) {
        PortalAccessService.Access resolved = access.resolve(body.get("token"), session);
        if (resolved.device() != null) {
            access.revokeDevice(resolved.journey(), resolved.device().getId());
        }
        return ResponseEntity.ok(ApiResponse.success(200, "Signed out", null));
    }

    /** The client ending one of their own sessions from the devices list. */
    @PostMapping(value = "/devices/{deviceId}/revoke", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<?>> revokeDevice(
        @PathVariable String deviceId,
        @RequestBody Map<String, String> body,
        @RequestHeader(value = SecurityConfigurations.PORTAL_SESSION_HEADER, required = false) String session
    ) {
        PortalAccessService.Access resolved = access.resolve(body.get("token"), session);
        if (!resolved.canReadPrivate()) {
            return ResponseEntity.status(401).body(ApiResponse.error(401,
                "Please enter a code first", "PORTAL_NOT_VERIFIED"));
        }

        Long id;
        try {
            id = idObfuscator.decodeId(deviceId);
        } catch (Exception e) {
            id = -1L;
        }
        /* ownership is checked inside, not here: a caller that decodes an id has not shown it is theirs */
        boolean done = access.revokeDevice(resolved.journey(), id);
        return done
            ? ResponseEntity.ok(ApiResponse.success(200, "Signed out", null))
            : ResponseEntity.status(404).body(ApiResponse.error(404,
                "That device is not on this booking", "DEVICE_NOT_FOUND"));
    }

    /**
     * One document's bytes, streamed by us.
     *
     * <p>Client paperwork is private media and none of those modules is {@code permitAll}: a URL
     * is not a credential, and that one holds passports. This does not change it. The session is
     * checked, the document is checked to belong to <em>this</em> journey, and only then is the
     * file read.
     *
     * <p>The second check is the one that matters. An id in a URL is a request, not a claim.
     */
    @GetMapping("/{token}/documents/{documentId}/file")
    public ResponseEntity<byte[]> document(
        @PathVariable String token,
        @PathVariable String documentId,
        @RequestHeader(value = SecurityConfigurations.PORTAL_SESSION_HEADER, required = false) String session,
        HttpServletRequest request
    ) {
        PortalAccessService.Access resolved = access.resolve(token, session);
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
            /* 404 for all of them, including "it exists but is not yours" */
            return ResponseEntity.status(404).build();
        }

        byte[] bytes = documentStorage.readDocumentBytes(document.getFileName());
        if (bytes == null || bytes.length == 0) return ResponseEntity.status(404).build();

        activity.record(resolved.journey(), resolved.participant(), resolved.device(),
            "DOCUMENT", document.getTitle(), PortalPageController.clientAddress(request),
            request.getHeader("User-Agent"));

        MediaType type = document.getFileType() == null
            ? MediaType.APPLICATION_OCTET_STREAM
            : MediaType.parseMediaType(document.getFileType());

        return ResponseEntity.ok()
            .contentType(type)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + safeFileName(document) + "\"")
            .body(bytes);
    }

    private String safeFileName(QuoteDocument document) {
        String name = document.getOriginalFileName() != null
            ? document.getOriginalFileName()
            : document.getFileName();
        if (name == null) return "document.pdf";
        return name.replaceAll("[\"\\r\\n\\\\]", "_");
    }

    /** Mistyped, withdrawn, expired, never existed: one answer, so none of them is discoverable. */
    private ResponseEntity<ApiResponse<?>> gone() {
        return ResponseEntity.status(404).body(ApiResponse.error(404,
            "This link is not available.", "PORTAL_LINK_UNAVAILABLE"));
    }
}
