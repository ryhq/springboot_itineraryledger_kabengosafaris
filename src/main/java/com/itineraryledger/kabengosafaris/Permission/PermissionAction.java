package com.itineraryledger.kabengosafaris.Permission;

/**
 * PermissionAction Enum - Defines the types of actions that can be controlled
 */
public enum PermissionAction {
    /**
     * CREATE - Ability to create new records
     */
    CREATE("create", "Create new records"),

    /**
     * READ - Ability to view/read records
     */
    READ("read", "View and read records"),

    /**
     * UPDATE - Ability to modify existing records
     */
    UPDATE("update", "Edit and update records"),

    /**
     * DELETE - Ability to remove records
     */
    DELETE("delete", "Delete records"),

    /**
     * EXECUTE - Ability to execute actions/workflows
     */
    EXECUTE("execute", "Execute actions and workflows"),

    /**
     * SUBMIT - Ability to submit documents (workflow approval)
     */
    SUBMIT("submit", "Submit documents for approval"),

    /**
     * AMEND - Ability to amend submitted documents
     */
    AMEND("amend", "Amend submitted documents"),

    /**
     * CANCEL - Ability to cancel documents
     */
    CANCEL("cancel", "Cancel submitted documents"),

    /**
     * EXPORT - Ability to export data
     */
    EXPORT("export", "Export data to external formats"),

    /**
     * PRINT - Ability to print documents
     */
    PRINT("print", "Print documents");

    private final String code;
    private final String description;

    PermissionAction(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return code + " (" + description + ")";
    }

}
