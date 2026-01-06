package com.itineraryledger.kabengosafaris.Permission;

import java.lang.annotation.*;

/**
 * RequirePermission Annotation - Used for method-level access control
 * Enum-based permission system using {ACTION}_{ENTITY} pattern
 *
 * Example usage:
 * @RequirePermission(action = PermissionAction.CREATE, entity = "USER")
 * public void createUser(UserDTO user) { ... }
 *
 * @RequirePermission(permission = "CREATE_BOOKING")
 * public void createBooking(Booking booking) { ... }
 *
 * @RequirePermission(roles = {"ADMIN", "MANAGER"}, requireAllRoles = false)
 * public void deletePackage(Long id) { ... }
 *
 * @RequirePermission(action = PermissionAction.UPDATE, entity = "ROLE", roles = {"ADMIN"})
 * public void updateRole(RoleDTO role) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * Specific permission name to check
     * If this is provided, action and entity are ignored
     * Format: {ACTION}_{ENTITY} (uppercase with underscores)
     * Examples: "CREATE_USER", "READ_ROLE", "UPDATE_EMAIL_ACCOUNT", "DELETE_BOOKING"
     */
    String permission() default "";

    /**
     * Action from PermissionAction enum
     * Used together with entity parameter
     * Examples: CREATE, READ, UPDATE, DELETE, EXECUTE, SUBMIT, AMEND, CANCEL, EXPORT, PRINT
     */
    PermissionAction action() default PermissionAction.READ;

    /**
     * Entity name to check access for
     * Used together with action parameter
     * Examples: "USER", "ROLE", "EMAIL_ACCOUNT", "BOOKING", "SAFARI_PACKAGE"
     */
    String entity() default "";

    /**
     * Alternative: role required (e.g., "ADMIN", "MANAGER")
     * If roles() is provided, permission and action/entity are checked after role
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
