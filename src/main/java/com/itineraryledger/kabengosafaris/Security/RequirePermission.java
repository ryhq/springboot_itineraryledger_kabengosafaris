package com.itineraryledger.kabengosafaris.Security;

import java.lang.annotation.*;

/**
 * RequirePermission Annotation - Used for method-level access control
 * Fully database-driven: action codes are resolved at runtime from permission_action_types table.
 *
 * Example usage:
 * @RequirePermission(action = "create", resource = "Safari Package")
 * public void createPackage(Package pkg) { ... }
 *
 * @RequirePermission(permission = "create_booking")
 * public void createBooking(Booking booking) { ... }
 *
 * @RequirePermission(roles = {"ADMIN", "MANAGER"}, requireAllRoles = false)
 * public void deletePackage(Long id) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * Specific permission name to check
     * If this is provided, action and resource are ignored
     * Example: "create_booking", "view_dashboard"
     */
    String permission() default "";

    /**
     * Action code to check (database-driven, no longer enum)
     * Used together with resource parameter
     * Examples: "create", "read", "update", "delete", "execute", "submit"
     * Action codes are defined in permission_action_types table
     */
    String action() default "read";

    /**
     * Resource/Document type to check access for
     * Used together with action parameter
     */
    String resource() default "";

    /**
     * Alternative: role required (e.g., "ADMIN", "MANAGER")
     * If role() is provided, permission and action/resource are checked after role
     */
    String[] roles() default {};

    /**
     * Whether all roles are required (AND) or any role is sufficient (OR)
     * Only applies if roles() is not empty
     */
    boolean requireAllRoles() default false;

    /**
     * Description of why this permission is required
     */
    String description() default "";
}
