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
                .map(profile -> profile.getPortalUrl())
                .filter(url -> url != null && !url.isBlank())
                .map(url -> url.trim().replaceAll("/+$", ""))
                .orElse(null);
        } catch (Exception e) {
            log.warn("Could not read the portal address from the company record: {}", e.getMessage());
        }

        cached.set(new Cached(value, System.currentTimeMillis()));
        return value;
    }

    /** Called when the company record is written, so a change does not wait out the cache. */
    public void invalidate() {
        cached.set(null);
    }
}
