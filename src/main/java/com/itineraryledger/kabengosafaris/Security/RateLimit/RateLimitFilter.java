package com.itineraryledger.kabengosafaris.Security.RateLimit;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A ceiling on the public endpoints that send mail or write records.
 *
 * Every rule here guards something a stranger can reach with no account: registering, asking for a
 * password reset, asking for an activation link again, and the three forms the websites post. Each
 * of those causes an email to leave this company's domain, so without a limit they are a way to have
 * this installation deliver unwanted mail to anybody — at the company's own sending reputation and
 * quota, from the company's own address.
 *
 * Deliberately NOT applied to the authenticated API. Somebody signed in and holding a permission is
 * a known person doing their job, and a colleague importing three bundles in a row should not be
 * told to come back later.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    /**
     * How far to trust X-Forwarded-For.
     *
     * The header is written by whoever is in front, and by anybody who feels like sending one — so
     * taking the FIRST entry means a caller can invent a new identity per request and the limit
     * counts nothing. This installation sits behind exactly one proxy, so the last entry is the
     * address that proxy actually saw. Set to 0 when nothing is in front and the socket is the truth.
     */
    @Value("${app.rate-limit.proxy-hops:1}")
    private int proxyHops;

    private static final List<RateLimitRule> RULES = List.of(
        /* each of these sends an email to an address the caller chose */
        /*
         * byEmail is false here and true for the other two, which looks inconsistent and is not:
         * this endpoint carries its email in a JSON body, and a filter that reads the body consumes
         * the stream the controller needs. The per-recipient limit for registration lives in
         * RegistrationHandler, where the address is already parsed.
         */
        new RateLimitRule("POST", "/api/auth/register", 5, Duration.ofHours(1), false,
            "account registrations"),
        new RateLimitRule("POST", "/api/auth/forgot-password", 5, Duration.ofHours(1), true,
            "password reset requests"),
        new RateLimitRule("POST", "/api/auth/resend-account-activation", 5, Duration.ofHours(1), true,
            "activation emails"),
        /*
         * Login is here for a different reason: the account lockout counts failures per ACCOUNT, so
         * somebody trying one password against a thousand usernames never trips it.
         */
        new RateLimitRule("POST", "/api/auth/login", 30, Duration.ofMinutes(15), false,
            "sign-in attempts"),
        /* the website's own forms — each one notifies somebody by mail */
        new RateLimitRule("POST", "/api/public/booking-inquiries", 10, Duration.ofHours(1), false,
            "booking inquiries"),
        new RateLimitRule("POST", "/api/public/contact", 10, Duration.ofHours(1), false,
            "contact messages"),
        new RateLimitRule("POST", "/api/public/newsletter/subscribe", 10, Duration.ofHours(1), false,
            "newsletter sign-ups"),
        new RateLimitRule("POST", "/api/public/testimonies", 5, Duration.ofHours(1), false,
            "testimonials"));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        RateLimitRule rule = RULES.stream()
            .filter(r -> r.matches(request.getMethod(), path))
            .findFirst().orElse(null);

        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        String caller = clientAddress(request);
        long waitFor = limiter.check("ip:" + rule.path() + ":" + caller, rule.limit(), rule.window());

        /*
         * And against the address being written to, where there is one. Limiting only by caller
         * leaves the interesting attack untouched: many machines, one victim's inbox.
         */
        if (waitFor == 0 && rule.byEmail()) {
            String email = request.getParameter("email");
            if (email != null && !email.isBlank()) {
                waitFor = limiter.check("email:" + rule.path() + ":" + email.trim().toLowerCase(),
                    Math.max(3, rule.limit() / 2), rule.window());
            }
        }

        if (waitFor > 0) {
            log.warn("Rate limit reached for {} on {} — {} more seconds", caller, path, waitFor);
            refuse(response, rule, waitFor);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * The address to count against.
     *
     * Taken from the END of X-Forwarded-For rather than the beginning. A caller can send whatever
     * header they like, so the leading entries are their claim; the trailing one was appended by the
     * proxy this request genuinely came through. Reading the first would let anybody mint a fresh
     * identity per request and make the whole filter decorative.
     */
    private String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (proxyHops > 0 && forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            int index = Math.max(0, hops.length - proxyHops);
            if (index < hops.length) return hops[index].trim();
        }
        return request.getRemoteAddr();
    }

    private void refuse(HttpServletResponse response, RateLimitRule rule, long waitFor)
            throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(waitFor));

        long minutes = Math.max(1, waitFor / 60);
        /*
         * Says what was limited and for how long. "Too many requests" leaves somebody who filled in
         * a contact form twice wondering whether the site is broken.
         */
        String body = String.format(
            "{\"success\":false,\"statusCode\":429,\"message\":\"Too many %s from here. "
                + "Try again in about %d minute%s.\",\"errorCode\":\"RATE_LIMITED\"}",
            rule.what(), minutes, minutes == 1 ? "" : "s");
        response.getWriter().write(body);
    }
}
