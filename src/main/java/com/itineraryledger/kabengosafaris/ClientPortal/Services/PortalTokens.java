package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

/**
 * The secrets this module hands out, and how they are stored.
 *
 * <p>In one place because every one of them is a decision that is easy to get subtly wrong and
 * impossible to notice afterwards: a token short enough to guess, a code generated from
 * {@code Math.random}, a comparison that leaks its answer in how long it takes.
 *
 * <p>Three rules hold throughout.
 *
 * <p><strong>Nothing here is an obfuscated id.</strong> Hashids encode the primary key; they are
 * not secrets, they are sequential, and this application mints a fresh salt on every restart, so
 * an id is both guessable and unstable. Journey tokens are their own random secret, which is the
 * same recipe the newsletter confirmation link has used since double opt-in shipped.
 *
 * <p><strong>Bearer secrets are stored hashed.</strong> The device cookie and the verification
 * code are both things that grant access, so a leaked copy of those tables must not be a leaked
 * set of sessions. The journey token is the exception and is stored as issued: it grants nothing
 * on its own and the row has to be findable by it.
 *
 * <p><strong>Comparison is constant time.</strong> A verification code compared with
 * {@code equals} tells an attacker how much of it was right, one character at a time.
 */
@Component
public class PortalTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Six digits. A million, which is nothing without limits and plenty with them. */
    private static final int CODE_DIGITS = 6;

    /**
     * A journey or device secret: 32 random bytes, url-safe Base64, 43 characters.
     *
     * <p>Url-safe because it goes in a link, unpadded because a trailing {@code =} survives
     * nothing that touches a URL.
     */
    public String newSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * A verification code a person can read over the phone and type without resenting it.
     *
     * <p>Fixed width, leading zeros kept, so "042913" is not quietly shortened to five digits by
     * anything that treats it as a number.
     */
    public String newCode() {
        int bound = (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", RANDOM.nextInt(bound));
    }

    /** SHA-256, hex. What goes in the column. */
    public String hash(String value) {
        if (value == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(out.length * 2);
            for (byte b : out) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            /*
             * SHA-256 is required of every JVM, so this cannot happen. Throwing rather than
             * returning null matters: a null hash would compare equal to a null column and let
             * somebody in.
             */
            throw new IllegalStateException("SHA-256 is unavailable, which should be impossible", e);
        }
    }

    /**
     * Whether a supplied secret matches a stored hash, without saying how nearly.
     *
     * <p>{@code MessageDigest.isEqual} is the constant-time comparison in the standard library.
     */
    public boolean matches(String supplied, String storedHash) {
        if (supplied == null || storedHash == null) return false;
        return MessageDigest.isEqual(
            hash(supplied).getBytes(StandardCharsets.UTF_8),
            storedHash.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Something a person recognises their own browser by.
     *
     * <p>Deliberately coarse. The point is "is that the laptop or the phone", not identification:
     * a full user agent string on a page is noise, and a fingerprint is a thing we would then be
     * holding about somebody.
     */
    public String deviceLabel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown device";

        String ua = userAgent.toLowerCase();
        String browser =
            ua.contains("edg/") ? "Edge"
            : ua.contains("opr/") || ua.contains("opera") ? "Opera"
            : ua.contains("firefox") ? "Firefox"
            : ua.contains("chrome") || ua.contains("crios") ? "Chrome"
            : ua.contains("safari") ? "Safari"
            : "Browser";

        String platform =
            ua.contains("iphone") ? "iPhone"
            : ua.contains("ipad") ? "iPad"
            : ua.contains("android") ? "Android"
            : ua.contains("windows") ? "Windows"
            : ua.contains("mac os") || ua.contains("macintosh") ? "Mac"
            : ua.contains("linux") ? "Linux"
            : null;

        return platform == null ? browser : browser + " on " + platform;
    }
}
