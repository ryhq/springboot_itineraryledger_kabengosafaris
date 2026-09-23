package com.itineraryledger.kabengosafaris.WebsiteCache;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.itineraryledger.kabengosafaris.CompanyProfile.Entity.CompanyProfile;
import com.itineraryledger.kabengosafaris.CompanyProfile.Repository.CompanyProfileRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Tells this company's website to stop showing something it was told earlier.
 *
 * <p>The website renders from this API and then keeps the answer — five minutes for anything that
 * says a trip is for sale, an hour for the rest. Without this, an edit made in the panel is simply
 * not visible until that timer runs out, and the only honest thing the office can say to a client
 * is "give it an hour".
 *
 * <h2>Why the API dials the website, and not the panel</h2>
 *
 * The panel is ONE application serving both companies; which one it is showing depends on who
 * signed in. If it held the website addresses and the secrets, it would have to choose between them
 * on every call, and the day it chooses wrong is the day one company's edit empties the other
 * company's site. This API is not one application serving two companies: each installation is its
 * own process with its own database and its own company row, so there is exactly one website it
 * could possibly mean. Choosing wrong stops being unlikely and becomes impossible.
 *
 * <h2>Why it never fails a save</h2>
 *
 * Every call here is best-effort. The edit is committed before anything is dialled, and a website
 * that is down, mid-deploy or misconfigured must not be able to make somebody's work bounce. What
 * a failure costs is the stale page the visitor would have seen anyway until the timer expired —
 * so the failure is recorded on the company row and shown in Settings, not thrown.
 */
@Service
@Slf4j
public class WebsiteCacheService {

    /** The path the website serves its revalidation endpoint on. Both repositories agree on it. */
    public static final String REVALIDATE_PATH = "/api/revalidate";

    /** The header the secret travels in. Not Authorization: this is not a user, it is a doorbell. */
    public static final String SECRET_HEADER = "X-Revalidate-Secret";

    private final CompanyProfileRepository profileRepository;
    private final RestTemplate restTemplate;

    /**
     * Short on purpose. A revalidation call is a courtesy shouted after a commit that already
     * happened; nothing waits on it and nothing is retried, so a website that has stopped
     * answering must cost seconds, not minutes.
     */
    public WebsiteCacheService(CompanyProfileRepository profileRepository,
                               RestTemplateBuilder builder) {
        this.profileRepository = profileRepository;
        this.restTemplate = builder
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Value("${app.website.cache.enabled:true}")
    private boolean enabled;

    /** What happened, for the page that asked. */
    public record Outcome(boolean ok, String detail, String target, Set<String> tags) {}

    /**
     * Clear these labels, and say what happened.
     *
     * <p>Used by the button in Settings, where somebody is waiting for an answer.
     */
    public Outcome clear(Set<String> tags) {
        CompanyProfile profile = profileRepository.findSingleton().orElse(null);
        if (profile == null) return record(null, false, "There is no company profile to read a website from", tags);

        if (!enabled) {
            return record(profile, false, "Cache clearing is switched off for this deployment "
                + "(app.website.cache.enabled=false)", tags);
        }
        if (profile.getWebsiteUrl() == null || profile.getWebsiteUrl().isBlank()) {
            return record(profile, false, "No website address is recorded, so there is nothing to call", tags);
        }
        if (profile.getWebsiteCacheSecret() == null || profile.getWebsiteCacheSecret().isBlank()) {
            return record(profile, false, "No website key is recorded — the website will refuse an "
                + "unsigned request, which is the point of the key", tags);
        }

        String url = profile.getWebsiteUrl() + REVALIDATE_PATH;
        String secret;
        try {
            secret = EncryptionUtil.decrypt(profile.getWebsiteCacheSecret());
        } catch (RuntimeException e) {
            return record(profile, false, "The stored website key could not be read back. Set it "
                + "again in Settings, and set the same value on the website", tags);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(SECRET_HEADER, secret);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tags", tags);

        try {
            ResponseEntity<String> res = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            boolean ok = res.getStatusCode().is2xxSuccessful();
            String detail = ok
                ? "Cleared " + WebsiteCacheTags.describe(tags)
                : "The website answered " + res.getStatusCode().value();
            return record(profile, ok, detail, tags);
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized
                 | org.springframework.web.client.HttpClientErrorException.Forbidden e) {
            /*
             * The one failure worth naming precisely. Everything else is "the site is down"; this
             * one is "the two halves of the key no longer match", which is a different fix and
             * otherwise takes an afternoon to work out.
             */
            return record(profile, false, "The website rejected the key. The value stored here and "
                + "the one in the website's environment are not the same", tags);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            return record(profile, false, "The website has no " + REVALIDATE_PATH + " — it is "
                + "running a version from before cache clearing existed", tags);
        } catch (RuntimeException e) {
            return record(profile, false, "Could not reach " + url + ": " + rootMessage(e), tags);
        }
    }

    /**
     * Clear these labels without anybody waiting.
     *
     * <p>This is what a save triggers. It runs after the transaction has committed and on another
     * thread, so the person who pressed Save is never held up by, and never sees an error from, a
     * website that has nothing to do with whether their edit was stored.
     */
    @Async
    public void clearQuietly(Set<String> tags) {
        try {
            if (!worthCalling(tags)) return;
            Outcome outcome = clear(tags);
            if (outcome.ok()) {
                log.debug("Website cache cleared: {}", outcome.detail());
            } else {
                log.warn("Website cache NOT cleared ({}): {}",
                    WebsiteCacheTags.describe(tags), outcome.detail());
            }
        } catch (RuntimeException e) {
            log.warn("Website cache call failed outright: {}", e.toString());
        }
    }

    /**
     * How long an automatic clear covers, so a burst of saves is one call and not twenty.
     *
     * <p>The panel's record pages save ONE FIELD PER REQUEST — that is what makes click-to-edit
     * safe from two people overwriting each other — so correcting a lodge's name, region and
     * description is three saves in about ten seconds. Each would otherwise be its own call telling
     * the website the same thing.
     *
     * <p>Twenty seconds, not two minutes: the window has to be short enough that somebody who
     * changes a price and then reloads the site sees the new one. It only ever suppresses a
     * DUPLICATE of a call already made, so the worst case is that the website was told slightly
     * earlier than the last keystroke, and the next save after the window tells it again.
     */
    private static final long COALESCE_MILLIS = 20_000;

    private final java.util.concurrent.ConcurrentHashMap<String, Long> lastCleared =
        new java.util.concurrent.ConcurrentHashMap<>();

    /** False when this exact set was already cleared moments ago. Manual clears never ask. */
    private boolean worthCalling(Set<String> tags) {
        String key = String.join(",", new java.util.TreeSet<>(tags));
        long now = System.currentTimeMillis();
        Long previous = lastCleared.get(key);
        if (previous != null && now - previous < COALESCE_MILLIS) {
            log.debug("Skipping a repeat clear of {} — one went out {}ms ago",
                key, now - previous);
            return false;
        }
        lastCleared.put(key, now);
        // Unbounded growth is not possible in practice (ten tags), but a stale entry is dead weight.
        if (lastCleared.size() > 64) lastCleared.entrySet()
            .removeIf(e -> now - e.getValue() > COALESCE_MILLIS * 10);
        return true;
    }

    /** Whether the website answers and accepts the key, without throwing any page away. */
    public Outcome test() {
        CompanyProfile profile = profileRepository.findSingleton().orElse(null);
        if (profile == null || !profile.websiteCacheConfigured()) {
            return new Outcome(false, "A website address and a key are both needed before this can "
                + "be tested", profile == null ? null : profile.getWebsiteUrl(), Set.of());
        }
        String url = profile.getWebsiteUrl() + REVALIDATE_PATH;
        HttpHeaders headers = new HttpHeaders();
        try {
            headers.set(SECRET_HEADER, EncryptionUtil.decrypt(profile.getWebsiteCacheSecret()));
        } catch (RuntimeException e) {
            return new Outcome(false, "The stored key could not be read back. Set it again.", url, Set.of());
        }
        try {
            ResponseEntity<String> res = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return new Outcome(res.getStatusCode().is2xxSuccessful(),
                res.getStatusCode().is2xxSuccessful()
                    ? "The website answered and accepted the key"
                    : "The website answered " + res.getStatusCode().value(),
                url, Set.of());
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized
                 | org.springframework.web.client.HttpClientErrorException.Forbidden e) {
            return new Outcome(false, "The website rejected the key", url, Set.of());
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            return new Outcome(false, "The website has no " + REVALIDATE_PATH
                + " — it is running a version from before cache clearing existed", url, Set.of());
        } catch (RuntimeException e) {
            return new Outcome(false, "Could not reach " + url + ": " + rootMessage(e), url, Set.of());
        }
    }

    /**
     * Write down what was asked for and whether anybody answered.
     *
     * <p>A three-column update rather than a save: see the repository for why. It is also wrapped
     * in its own try — a cache call that worked must not be reported as failed because the note
     * about it could not be written.
     */
    private Outcome record(CompanyProfile profile, boolean ok, String detail, Set<String> tags) {
        if (profile != null && profile.getId() != null) {
            try {
                profileRepository.recordCacheCall(profile.getId(), LocalDateTime.now(), ok,
                    detail.length() > 500 ? detail.substring(0, 500) : detail);
            } catch (RuntimeException e) {
                log.warn("Could not store the website cache outcome: {}", e.toString());
            }
        }
        return new Outcome(ok, detail, profile == null ? null : profile.getWebsiteUrl(), tags);
    }

    /** The innermost cause's message: "Connection refused" beats four lines of wrapper classes. */
    private String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        String m = t.getMessage();
        return m == null || m.isBlank() ? t.getClass().getSimpleName() : m;
    }
}
