package com.itineraryledger.kabengosafaris.WebsiteCache;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Settings → Company → Website: the button that says "no, now".
 *
 * <p>Under {@code /api/company} because the website address and its key are fields on the company
 * record, and because that is the permission a person editing them already has. There is no
 * separate permission entity for this: somebody who may change the company's address may also tell
 * the company's website to refresh, and inventing a second name to keep in step with the catalogue
 * would be a new way to 403 everybody for no gain.
 */
@RestController
@RequestMapping("/api/company/website-cache")
@RequiredArgsConstructor
@Slf4j
public class WebsiteCacheController {

    private final WebsiteCacheService cacheService;

    /** What a person may ask to clear, so the page never has to hardcode the list. */
    @GetMapping("/tags")
    @PreAuthorize("hasAnyAuthority('PERM_READ_COMPANY_PROFILE', 'PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> tags() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tags", WebsiteCacheTags.CATALOGUE);
        return ResponseEntity.ok(ApiResponse.success(200, "Website cache labels", payload));
    }

    /**
     * Throw pages away.
     *
     * <p>An empty or absent tag list means the whole site, which is what the plain button does. A
     * name the website would not recognise is refused rather than accepted and dropped: "cleared"
     * for a label nothing carries is the worst possible answer, because the page then looks current
     * and is not.
     */
    @PostMapping("/clear")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> clear(@RequestBody(required = false) ClearRequest body) {
        Set<String> tags;
        try {
            tags = WebsiteCacheTags.validate(body == null ? null : body.getTags());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, e.getMessage(), "INVALID_CACHE_TAG"));
        }

        log.info("Clearing website cache: {}", WebsiteCacheTags.describe(tags));
        WebsiteCacheService.Outcome outcome = cacheService.clear(tags);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ok", outcome.ok());
        payload.put("detail", outcome.detail());
        payload.put("target", outcome.target());
        payload.put("tags", outcome.tags());

        /*
         * 200 either way, with ok=false inside. This is a report about a third party, not a verdict
         * on the request: the caller asked a legitimate question and got a true answer, and a 502
         * here would have the panel's error handling swallow the one sentence that says what to fix.
         */
        return ResponseEntity.ok(ApiResponse.success(200,
            outcome.ok() ? "Website cache cleared" : "Website cache was not cleared", payload));
    }

    /** Does the website answer, and does it accept the key? Throws nothing away. */
    @PostMapping("/test")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> test() {
        WebsiteCacheService.Outcome outcome = cacheService.test();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ok", outcome.ok());
        payload.put("detail", outcome.detail());
        payload.put("target", outcome.target());
        return ResponseEntity.ok(ApiResponse.success(200,
            outcome.ok() ? "The website is reachable" : "The website could not be reached", payload));
    }

    @Data
    public static class ClearRequest {
        /** Absent or empty means the whole site. */
        private List<String> tags;
    }
}
