package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services;

import java.util.List;

import lombok.Data;

/**
 * Everything a caller can narrow the signature list by, in one object.
 *
 * The rows, the stat cards and the record walk are all built from this, so a card cannot
 * report a figure the table would contradict, and prev/next cannot wander out of the set on
 * screen.
 */
@Data
public class EmailAccountSignatureFilter {

    /** Free text across the name and description — never the body, which is a whole file. */
    private String keyword;
    private String search;

    /** Narrows to one mailbox, which is how the account's own tab uses this list. */
    private String emailAccountId;

    private String name;

    private Boolean enabled;
    /** "enabled" / "disabled"; a contradictory pair cancels to no constraint. */
    private List<String> statuses;

    private Boolean isDefault;

    /**
     * "shipped" / "custom" — whether this came with the system or somebody wrote it.
     *
     * Worth a filter because the two behave differently: a shipped signature can always be
     * restored and can never be deleted.
     */
    private List<String> kinds;

    public String effectiveKeyword() {
        if (keyword != null && !keyword.isBlank()) return keyword;
        return search;
    }

    public Boolean resolvedEnabled() {
        boolean yes = statuses != null && statuses.contains("enabled");
        boolean no = statuses != null && statuses.contains("disabled");
        if (yes ^ no) return yes;
        return enabled;
    }

    public boolean wantsKind(String kind) {
        return kinds != null && kinds.contains(kind);
    }
}
