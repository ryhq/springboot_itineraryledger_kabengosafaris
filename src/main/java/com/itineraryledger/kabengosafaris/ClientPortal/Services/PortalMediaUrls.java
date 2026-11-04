package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Where a portal page points when it wants a photograph.
 *
 * <p>One copy, shared. This was a private method on {@code PortalViewService} until the catalogue
 * needed the same URLs, and a second copy of a rule about addresses is the kind that goes stale
 * quietly: the day photographs would keep working while the lodge pictures in the catalogue
 * silently 404, and nothing anywhere would say why.
 *
 * <p>Only PUBLISHED image modules belong here — park, accommodation, itinerary and the rest of
 * the family the public website already renders in a bare {@code <img>}. Client documents are the
 * opposite and are streamed through the API with the session; there is no method on this class
 * that would build a URL to one.
 */
@Service
public class PortalMediaUrls {

    /**
     * This API's own public address.
     *
     * <p>The portal is served from a different hostname, so a relative path would resolve against
     * that one and every photograph would be a broken box.
     */
    @Value("${app.base.url:}")
    private String apiBaseUrl;

    /**
     * An absolute URL to a published image, or null when there is nothing to point at.
     *
     * <p>By file name rather than by id: an obfuscated id is re-salted on every restart, so a URL
     * built from one stops resolving after the next deploy, and a page that was fine yesterday
     * fills with broken images.
     */
    public String image(String module, String fileName) {
        if (fileName == null || apiBaseUrl == null || apiBaseUrl.isBlank()) return null;
        return apiBaseUrl.replaceAll("/+$", "") + "/api/" + module + "/file/" + fileName;
    }
}
