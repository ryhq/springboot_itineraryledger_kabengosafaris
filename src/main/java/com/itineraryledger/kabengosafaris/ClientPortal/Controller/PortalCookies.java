package com.itineraryledger.kabengosafaris.ClientPortal.Controller;

import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * The portal session cookie, written in one place.
 *
 * <p>Every attribute here is load bearing, which is why this is a class and not four string
 * literals scattered through a controller.
 *
 * <p><strong>HttpOnly</strong>, so an injected script cannot read the session even if one ever
 * gets onto a page. <strong>Secure</strong>, because the whole thing travels over the public
 * internet to a laptop in a hotel. <strong>SameSite=Lax</strong>, which is what stands in for
 * CSRF tokens here: a form posted from another origin does not carry the cookie, so nobody can
 * make a client accept a quotation by getting them to open a page.
 *
 * <p>Path is {@code /} rather than {@code /portal}, because the pages live under {@code /portal}
 * and the form posts under {@code /api/portal}, and a cookie is only sent to paths beneath its
 * own. The portal filter chain is the only thing that reads it.
 */
public final class PortalCookies {

    public static final String NAME = "portal_session";

    private PortalCookies() {}

    /** The value a browser sent back, or null. */
    public static String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    /**
     * @param days how long the browser should keep it, or 0 for a session cookie that dies when
     *             the browser closes. Zero is what "this is not my device" writes, and it is the
     *             reason the choice is offered at all.
     */
    public static String issue(String value, int days) {
        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(NAME, value)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/");
        if (days > 0) cookie.maxAge(java.time.Duration.ofDays(days));
        return cookie.build().toString();
    }

    /** Ending a session: the same cookie, emptied and expired. */
    public static String clear() {
        return ResponseCookie.from(NAME, "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build()
            .toString();
    }
}
