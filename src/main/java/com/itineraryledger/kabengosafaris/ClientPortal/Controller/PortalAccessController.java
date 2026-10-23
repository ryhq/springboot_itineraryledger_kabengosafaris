package com.itineraryledger.kabengosafaris.ClientPortal.Controller;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalAccessService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Proving who you are, and ending a session.
 *
 * <p>Under {@code /api/portal} rather than with the pages, for a prosaic reason that matters: the
 * rate limiter matches on a path prefix, and a rule cannot be written for
 * {@code /portal/{token}/access/request} because the token sits in the middle. Asking for a code
 * sends mail to an address somebody else owns, so it has to be countable.
 *
 * <p>Form posts and redirects rather than JSON, because these are submitted by a plain HTML form
 * on a server-rendered page. Redirecting after the post is also what stops a refresh re-sending
 * a code.
 */
@Controller
@RequestMapping("/api/portal")
@RequiredArgsConstructor
@Slf4j
public class PortalAccessController {

    private final PortalAccessService access;
    private final IdObfuscator idObfuscator;

    /**
     * Send a code to an address, if that address is on this booking.
     *
     * <p>The redirect is identical either way. A page that says "we have sent a code" for a known
     * address and something else for an unknown one is a tool for discovering who our customers
     * are, one address at a time.
     */
    @PostMapping(value = "/access/request", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String requestCode(@RequestParam String token,
                              @RequestParam String email,
                              HttpServletRequest request) {
        PortalAccessService.Access resolved = access.resolve(token, null);
        if (resolved.journey() == null || !resolved.canReadTrip()) {
            return "redirect:/portal/" + safe(token);
        }

        access.requestCode(resolved.journey(), email, PortalPageController.clientAddress(request));
        return "redirect:/portal/" + safe(token) + "/verify?sent=1";
    }

    /**
     * Check the code and, if it is right, trust this browser.
     *
     * <p>{@code remember} false is the shared-computer case: the session still works, and it dies
     * when the browser closes rather than sitting on a hotel machine for a month.
     */
    @PostMapping(value = "/access/verify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String verify(@RequestParam String token,
                         @RequestParam String email,
                         @RequestParam String code,
                         @RequestParam(required = false) String remember,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        PortalAccessService.Access resolved = access.resolve(token, null);
        if (resolved.journey() == null || !resolved.canReadTrip()) {
            return "redirect:/portal/" + safe(token);
        }

        boolean rememberDevice = remember != null;
        PortalAccessService.Verification result = access.verify(
            resolved.journey(), email, code,
            PortalPageController.clientAddress(request),
            request.getHeader("User-Agent"),
            rememberDevice);

        if (!result.accepted()) {
            /* One message for every refusal. The reason is in the log, not on the page. */
            log.info("Portal verification refused on journey {}: {}",
                resolved.journey().getId(), result.reason());
            return "redirect:/portal/" + safe(token) + "/verify?failed=1";
        }

        response.addHeader(HttpHeaders.SET_COOKIE,
            PortalCookies.issue(result.deviceToken(), rememberDevice ? 30 : 0));
        return "redirect:/portal/" + safe(token);
    }

    /** The client ending one of their own sessions from the devices page. */
    @PostMapping(value = "/devices/{deviceId}/revoke", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String revokeDevice(@PathVariable String deviceId,
                               @RequestParam String token,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        PortalAccessService.Access resolved = access.resolve(token, PortalCookies.read(request));
        if (!resolved.canReadPrivate()) {
            return "redirect:/portal/" + safe(token) + "/verify";
        }

        Long id = decode(deviceId, resolved);
        boolean endingThisOne = resolved.device() != null
            && resolved.device().getId().equals(id);

        access.revokeDevice(resolved.journey(), id);

        if (endingThisOne) {
            /* They ended the session they are sitting in. Take the cookie with it. */
            response.addHeader(HttpHeaders.SET_COOKIE, PortalCookies.clear());
            return "redirect:/portal/" + safe(token);
        }
        return "redirect:/portal/" + safe(token) + "/devices";
    }

    /** Sign out of this browser without ending anything else. */
    @PostMapping(value = "/access/sign-out", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String signOut(@RequestParam String token,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        PortalAccessService.Access resolved = access.resolve(token, PortalCookies.read(request));
        if (resolved.device() != null) {
            access.revokeDevice(resolved.journey(), resolved.device().getId());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, PortalCookies.clear());
        return "redirect:/portal/" + safe(token);
    }

    /**
     * The device id, decoded, and still only ever revoked if it belongs to this journey.
     *
     * <p>The ownership check is in {@code PortalAccessService.revokeDevice}, not here: a caller
     * that decodes an id is not a caller that has established it is theirs, and putting the two
     * in one place is how the second gets forgotten.
     *
     * <p>Returns an impossible id when it does not decode, so the revoke finds nothing. Throwing
     * would turn a mistyped URL into a stack trace on a client's screen.
     */
    private Long decode(String deviceId, PortalAccessService.Access resolved) {
        try {
            Long id = idObfuscator.decodeId(deviceId);
            return id == null ? -1L : id;
        } catch (Exception e) {
            log.info("Unreadable device id on journey {}", resolved.journey().getId());
            return -1L;
        }
    }

    /** A token goes into a redirect, so it is checked rather than trusted. */
    private String safe(String token) {
        if (token == null) return "";
        return token.replaceAll("[^A-Za-z0-9_-]", "");
    }
}
