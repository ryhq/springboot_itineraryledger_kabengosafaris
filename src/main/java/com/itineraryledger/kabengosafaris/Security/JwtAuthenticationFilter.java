package com.itineraryledger.kabengosafaris.Security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

@RequiredArgsConstructor // Generates a constructor with required arguments (final fields)
@Slf4j // Enables logging in this class
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Cookie name used for the short-lived "download bridge" — set by
     * BackupDownloadController.prepareDownload right before the frontend
     * does a top-level navigation to /api/backups/download/{filename},
     * because a {@code window.location} request cannot carry an
     * {@code Authorization} header. The cookie is HttpOnly, SameSite=Strict,
     * scoped to the download path, and expires within ~60s.
     */
    private static final String BACKUP_DOWNLOAD_COOKIE = "backup_dl_token";
    private static final String BACKUP_DOWNLOAD_PATH_PREFIX = "/api/backups/download/";

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip JWT filter for endpoints that handle their own token validation
        // and for static resources (e.g. email logo images)
        return path.startsWith("/api/auth/token/") || path.startsWith("/images/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                TokenType tokenType = tokenProvider.getTokenType(jwt);

                // Check if token type is valid for general endpoint access
                if (tokenType == null || tokenType != TokenType.ACCESS) {
                    log.warn("Attempt to access protected endpoint with invalid token type: {}", tokenType);
                    handleInvalidTokenType(response, tokenType);
                    return;
                }

                String username = tokenProvider.getUsernameFromToken(jwt);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set Spring Security authentication for user: {}", username);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private void handleInvalidTokenType(HttpServletResponse response, TokenType actualType) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String jsonResponse = objectMapper.writeValueAsString(
            ApiResponse.error(401,
                "Invalid token type. Expected ACCESS token, got " + (actualType != null ? actualType.getType() : "unknown"),
                "INVALID_TOKEN_TYPE"
            )
        );
        response.getWriter().write(jsonResponse);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // Primary path: Authorization: Bearer <jwt>. Used by every XHR / fetch.
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // Fallback for backup downloads only: read the JWT from a
        // short-lived, path-scoped, HttpOnly cookie. This lets us trigger
        // a real <a download> / window.location navigation (so Chrome shows
        // the file in its native download tray and supports pause/resume)
        // while keeping the JWT out of the URL and the access logs.
        String path = request.getServletPath();
        if (path != null && path.startsWith(BACKUP_DOWNLOAD_PATH_PREFIX)) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (BACKUP_DOWNLOAD_COOKIE.equals(cookie.getName())
                            && StringUtils.hasText(cookie.getValue())) {
                        return cookie.getValue();
                    }
                }
            }
        }
        return null;
    }
}
