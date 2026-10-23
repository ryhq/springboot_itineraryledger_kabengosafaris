package com.itineraryledger.kabengosafaris.ClientPortal.Controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalView;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalAccessService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalActivityService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalViewService;
import com.itineraryledger.kabengosafaris.Configurations.SecurityConfigurations;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
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
