package com.itineraryledger.kabengosafaris.Security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.Permission.PermissionActionService;

/**
 * PermissionCheckAspect - AOP aspect for @RequirePermission annotation
 * Checks user permissions before method execution
 *
 * This aspect intercepts methods annotated with @RequirePermission
 * and verifies that the current user has the required permissions.
 *
 * Database-driven: action codes are validated against permission_action_types table
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PermissionCheckAspect {

    private final PermissionActionService permissionActionService;

    /**
     * Before method execution, check if user has required permission
     */
    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Access denied: User not authenticated");
            throw new AccessDeniedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            log.warn("Access denied: Principal is not a User object");
            throw new AccessDeniedException("Invalid principal type");
        }

        User user = (User) principal;

        // First, check role if specified
        if (requirePermission.roles().length > 0) {
            boolean hasRole = checkRoleAccess(user, requirePermission.roles(), requirePermission.requireAllRoles());
            if (!hasRole) {
                log.warn("Access denied: User {} does not have required role", user.getEmail());
                throw new AccessDeniedException("User does not have required role");
            }
        }

        // Then check permission
        boolean hasPermission = false;

        // Check specific permission name if provided
        if (!requirePermission.permission().isEmpty()) {
            hasPermission = user.hasPermission(requirePermission.permission());
            if (!hasPermission) {
                log.warn("Access denied: User {} does not have permission '{}'",
                        user.getEmail(), requirePermission.permission());
                throw new AccessDeniedException("User does not have permission: " + requirePermission.permission());
            }
        } else if (!requirePermission.resource().isEmpty()) {
            // Check action + resource permission (database-driven action codes)
            String actionCode = requirePermission.action();

            // Validate action code exists in database (optional, for debugging)
            if (!permissionActionService.getActionByCode(actionCode).isPresent()) {
                log.warn("Invalid action code: {} in annotation for method: {}",
                        actionCode, joinPoint.getSignature().getName());
                throw new IllegalArgumentException("Invalid action code in @RequirePermission: " + actionCode);
            }

            hasPermission = user.hasPermission(actionCode, requirePermission.resource());
            if (!hasPermission) {
                log.warn("Access denied: User {} cannot '{}' on resource '{}'",
                        user.getEmail(), actionCode, requirePermission.resource());
                throw new AccessDeniedException("User cannot " + actionCode + " on " + requirePermission.resource());
            }
        }

        log.debug("Permission check passed for user: {} on method: {} using action code: {}",
                user.getEmail(), joinPoint.getSignature().getName(), requirePermission.action());
    }

    /**
     * Check if user has required roles
     *
     * @param user user to check
     * @param roles required roles
     * @param requireAll true if all roles required, false if any is sufficient
     * @return true if user has required roles
     */
    private boolean checkRoleAccess(User user, String[] roles, boolean requireAll) {
        if (roles.length == 0) {
            return true;
        }

        if (requireAll) {
            // User must have all specified roles
            for (String role : roles) {
                if (!user.hasRole(role)) {
                    return false;
                }
            }
            return true;
        } else {
            // User must have at least one of the specified roles
            for (String role : roles) {
                if (user.hasRole(role)) {
                    return true;
                }
            }
            return false;
        }
    }
}
