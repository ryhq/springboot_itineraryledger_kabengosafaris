package com.itineraryledger.kabengosafaris.AuditLog;

import java.lang.annotation.*;

@Target(ElementType.METHOD) // Can be applied to methods
@Retention(RetentionPolicy.RUNTIME) // Retained at runtime for reflection
@Documented // Included in Javadoc
public @interface AuditLogAnnotation {

    /**
     * The action being performed (e.g., "CREATE_ROLE", "UPDATE_USER", "DELETE_PERMISSION")
     */
    String action();

    /**
     * The type of entity being acted upon (e.g., "Role", "User", "Permission")
     */
    String entityType();

    /**
     * The parameter name or index that contains the entity ID
     * Use null if entity ID is not needed
     */
    String entityIdParamName() default "";

    /**
     * Description of what this method does
     */
    String description() default "";

}
