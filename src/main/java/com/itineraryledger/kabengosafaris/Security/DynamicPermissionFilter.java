package com.itineraryledger.kabengosafaris.Security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.itineraryledger.kabengosafaris.Permission.EndpointPermissionService;
import com.itineraryledger.kabengosafaris.User.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DynamicPermissionFilter - Enforces database-driven endpoint permissions
 *
 * This filter intercepts all HTTP requests and checks if the user has
 * permission to access the endpoint based on endpoint_permissions table.
 *
 * Features:
 * - No @RequirePermission annotations needed on controllers
 * - All permissions configured in database
 * - Supports exact path and regex pattern matching
 * - Supports public endpoints (no auth required)
 * - Supports both permission-name and action+resource permission models
 * - Logs all access decisions for audit trail
 *
 * Permission Lookup Flow:
 * 1. Request comes in (HTTP method + path)
 * 2. Filter looks up endpoint_permissions table (exact match first)
 * 3. If exact match: use that permission mapping
 * 4. If not found: try pattern matching (regex)
 * 5. If not found: allow by default (configurable)
 * 6. Get current user from security context
 * 7. Check if user has required permission/role
 * 8. Allow or deny with 403 Forbidden
 */
@RequiredArgsConstructor
@Slf4j
public class DynamicPermissionFilter extends OncePerRequestFilter {

    private final EndpointPermissionService endpointPermissionService;

    /**
     * Endpoints that should always be allowed (bypass this filter)
     * Typically auth and swagger endpoints
     */
    private static final String[] PUBLIC_PATHS = {
        "/api/auth/",
        "/swagger-ui/",
        "/v3/api-docs/",
        "/actuator/",
        "/health"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Check if this is a public path that should bypass dynamic permission check
        if (isPublicPath(path)) {
            log.debug("Public path, allowing: {} {}", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Checking dynamic permissions for: {} {}", method, path);

        // Get current user (may be null if not authenticated)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = extractUser(auth);

        // Check if user can access this endpoint
        boolean canAccess = endpointPermissionService.canUserAccessEndpoint(user, method, path);

        if (!canAccess) {
            log.warn("Access denied for {} {} by user: {}", method, path,
                user != null ? user.getUsername() : "ANONYMOUS");
            sendForbiddenResponse(response, user, path);
            return;
        }

        log.debug("Access allowed for: {} {} by user: {}", method, path,
            user != null ? user.getUsername() : "ANONYMOUS");

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Check if path is in the public paths list
     * These paths bypass the dynamic permission check
     *
     * @param path request path
     * @return true if path is public
     */
    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract User from Authentication
     * Returns null if not authenticated or principal is not a User
     *
     * @param auth Spring Authentication
     * @return User or null
     */
    private User extractUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }

        return null;
    }

    /**
     * Send 403 Forbidden response with detailed message
     *
     * @param response HTTP response
     * @param user user who was denied (may be null)
     * @param path requested path
     * @throws IOException if response write fails
     */
    private void sendForbiddenResponse(HttpServletResponse response, User user, String path)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        String message;
        if (user == null) {
            message = String.format(
                "{\"error\": \"Forbidden\", \"message\": \"Authentication required to access %s\"}",
                path
            );
        } else {
            message = String.format(
                "{\"error\": \"Forbidden\", \"message\": \"User %s does not have permission to access %s\"}",
                user.getUsername(), path
            );
        }

        response.getWriter().write(message);
        response.getWriter().flush();
    }

    /**
     * Filter name for debugging
     */
    @Override
    public String getFilterName() {
        return "DynamicPermissionFilter";
    }
}
