package com.itineraryledger.kabengosafaris.WebsiteCache;

import java.io.IOException;
import java.util.Set;

import org.springframework.web.filter.OncePerRequestFilter;

import com.itineraryledger.kabengosafaris.CompanyProfile.Repository.CompanyProfileRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Saving public content clears the website, without anybody having to remember to.
 *
 * <h2>Why a filter and not a hook in each service</h2>
 *
 * The alternative is a line in every write service that touches public content — about forty of
 * them today, and one more every time a resource is added. The line that gets forgotten is the one
 * that makes somebody say "the website is wrong again", months later, about a module nobody
 * connects to this feature. A filter is keyed on the URL space instead, which is the same thing the
 * panel's resource registry is keyed on: a new public resource is reachable at a path, and a path
 * is all this needs.
 *
 * <h2>What it deliberately does not catch</h2>
 *
 * Anything that changes content without an HTTP request: the data importers, the initializers, a
 * hand-run SQL statement. Those are operator actions with an operator present, and the operator has
 * the button in Settings. Pretending to cover them would mean hooking Hibernate, which is a much
 * larger promise to keep correct than this feature is worth.
 *
 * <p>It also fires on the METHOD and the STATUS, never on the payload: a PUT that the API rejected
 * changed nothing, and a GET never does.
 */
@RequiredArgsConstructor
@Slf4j
public class WebsiteCacheFilter extends OncePerRequestFilter {

    private final WebsiteCacheService cacheService;
    private final CompanyProfileRepository profileRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        chain.doFilter(request, response);

        try {
            afterResponse(request, response);
        } catch (RuntimeException e) {
            // A cache hook must never be the reason a completed request looks like it failed.
            log.warn("Website cache trigger failed for {} {}: {}",
                request.getMethod(), request.getRequestURI(), e.toString());
        }
    }

    private void afterResponse(HttpServletRequest request, HttpServletResponse response) {
        if (!isWrite(request.getMethod())) return;
        if (response.getStatus() < 200 || response.getStatus() >= 300) return;

        Set<String> tags = WebsiteCacheTags.forPath(request.getRequestURI());
        if (tags.isEmpty()) return;

        /*
         * Read the switch before spending a thread on it. Most installations that have not filled
         * in a website address will hit this on every single save, so the cheap check belongs here
         * rather than inside the async call.
         */
        var profile = profileRepository.findSingleton().orElse(null);
        if (profile == null || !Boolean.TRUE.equals(profile.getWebsiteCacheAuto())) return;
        if (!profile.websiteCacheConfigured()) return;

        log.debug("{} {} changed public content — clearing {}",
            request.getMethod(), request.getRequestURI(), WebsiteCacheTags.describe(tags));
        cacheService.clearQuietly(tags);
    }

    private boolean isWrite(String method) {
        return "POST".equals(method) || "PUT".equals(method)
            || "PATCH".equals(method) || "DELETE".equals(method);
    }
}
