package com.itineraryledger.kabengosafaris.Feature;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A switched-off feature's endpoints do not exist.
 *
 * 404 rather than 403, deliberately: 403 says "this exists and you may not have it", which invites
 * somebody to go looking for the permission. For a company that does not have a fleet at all, the
 * honest answer is that there is no fleet here.
 *
 * The panel hides a disabled module from the sidebar, the palette and the breadcrumbs — but a
 * bookmark, an old link or a stale tab will still ask, and the frontend is not a security boundary.
 * This is where the answer is actually enforced.
 *
 * `/api/public/**` is never gated: a public website reading published content is not the module being
 * switched off, and gating it would take a company's site down along with its content pages.
 */
@RequiredArgsConstructor
@Slf4j
public class FeatureGateFilter extends OncePerRequestFilter {

    private final FeatureService featureService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/api/public/") || path.startsWith("/api/features")) {
            chain.doFilter(request, response);
            return;
        }

        Feature feature = featureService.featureFor(path);
        if (feature != null && !featureService.isEnabled(feature)) {
            log.debug("Feature '{}' is off — answering 404 for {} {}", feature.getKey(), request.getMethod(), path);
            respondNotFound(response, feature);
            return;
        }

        chain.doFilter(request, response);
    }

    /** The house error shape, so a client parses this like any other 404. */
    private void respondNotFound(HttpServletResponse response, Feature feature) throws IOException {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
            {"success":false,"statusCode":404,"message":"%s is not enabled for this company.",\
            "errorCode":"FEATURE_NOT_ENABLED"}"""
            .formatted(feature.getLabel()));
    }
}
