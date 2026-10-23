package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.CompanyProfile.Repository.CompanyProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Where this company's client portal is served from.
 *
 * <p>Read from the company record rather than from a property, because it is needed in two
 * places and both of them are the company's business: the address printed in the emails, and
 * the origin a browser is allowed to call this API from. Put it in a server environment file
 * and changing it means root on two machines and a restart; put it here and it is a setting.
 *
 * <p>Cached, because the answer is consulted on <em>every</em> cross-origin preflight and a
 * database round trip per OPTIONS request is a tax on every page the portal draws. Thirty
 * seconds is short enough that somebody changing the hostname sees it work before they have
 * finished wondering whether it did.
 *
 * <p>A failed read returns null rather than throwing. The caller then falls back to this API's
 * own address, which is where the server-rendered pages are: a database hiccup must not take
 * the portal's CORS with it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalOriginService {

    private static final long CACHE_MILLIS = 30_000;

    private final CompanyProfileRepository companyProfiles;

    private final AtomicReference<Cached> cached = new AtomicReference<>(null);

    private record Cached(String value, long readAt) {}

    /** The configured portal origin, without a trailing slash, or null when none is set. */
    @Transactional(readOnly = true)
    public String get() {
        Cached now = cached.get();
        if (now != null && System.currentTimeMillis() - now.readAt() < CACHE_MILLIS) {
            return now.value();
        }

        String value = null;
        try {
            value = companyProfiles.findAll().stream()
                .findFirst()
                .map(profile -> {
                    String configured = clean(profile.getPortalUrl());
                    return configured != null ? configured : derivedFrom(profile.getWebsiteUrl());
                })
                .orElse(null);
        } catch (Exception e) {
            log.warn("Could not read the portal address from the company record: {}", e.getMessage());
        }

        cached.set(new Cached(value, System.currentTimeMillis()));
        return value;
    }

    /** An address with a scheme and no trailing slash, or null when there is nothing usable. */
    private String clean(String url) {
        if (url == null || url.isBlank()) return null;
        String trimmed = url.trim().replaceAll("/+$", "");
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * {@code mytrip.} in front of the company's own domain, when nobody has said otherwise.
     *
     * <p>The alternative fallback is this API's address, and a client who is sent
     * {@code api.<something>.com/portal/…} has been told, accurately, that they are looking at
     * our plumbing. Both companies already serve the portal at {@code mytrip.} their own domain,
     * so the guess is the answer in every case we have, and an explicit setting overrides it the
     * moment it is not.
     *
     * <p>The website field is not tidy -- it holds {@code www.kabengosafaris.com} for one company
     * and {@code jateloafricantravels.com/en} for the other -- so the scheme, any path and a
     * leading {@code www.} all come off before the subdomain goes on. A value that leaves nothing
     * recognisable behind returns null rather than a hostname nobody can reach.
     */
    private String derivedFrom(String websiteUrl) {
        String cleaned = clean(websiteUrl);
        if (cleaned == null) return null;

        String host = cleaned.replaceFirst("^[a-zA-Z][a-zA-Z0-9+.-]*://", "");
        host = host.split("[/?#]", 2)[0];
        host = host.replaceFirst("^www\\.", "");

        // A host worth building a link from has a dot in it and nothing a URL may not contain.
        if (!host.matches("[A-Za-z0-9.-]+") || !host.contains(".")) return null;

        return "https://mytrip." + host.toLowerCase();
    }

    /** Called when the company record is written, so a change does not wait out the cache. */
    public void invalidate() {
        cached.set(null);
    }
}
