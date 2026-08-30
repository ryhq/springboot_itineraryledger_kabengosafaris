package com.itineraryledger.kabengosafaris.Security.RateLimit;

import java.time.Duration;

/**
 * One limit: how many times a caller may hit a path before being asked to wait.
 *
 * `byEmail` is the half that matters most and is easiest to leave out. Limiting by IP stops one
 * machine hammering the API; it does nothing about a hundred machines asking this API to send mail
 * to the SAME person, which is the shape of an actual mail-bombing and is trivial to arrange. The
 * address being written to is therefore a key in its own right.
 */
public record RateLimitRule(
    String method,
    String path,
    int limit,
    Duration window,
    /** also count against the email address in the request, not just the caller */
    boolean byEmail,
    String what
) {
    public boolean matches(String requestMethod, String requestPath) {
        return method.equalsIgnoreCase(requestMethod) && requestPath.startsWith(path);
    }
}
