package com.itineraryledger.kabengosafaris.Role.Specifications;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

/**
 * Everything a caller can narrow the role list by, in one object.
 *
 * A short list — a dozen roles, most of them shipped with the system — so the useful
 * dimensions are not "find one" but "which of these are actually doing anything":
 * which are switched off, which are ours rather than built in, which grant nothing,
 * and which nobody holds. The last two are how a role list rots: somebody adds
 * "Reservations Assistant", never assigns it, and it sits there looking meaningful.
 */
@Data
public class RoleFilter {

    /** Free text across the name, the display name and the description. */
    private String keyword;
    /** what the older role screens called it */
    private String search;

    /** The old discrete params, still honoured. */
    private String name;
    private String displayName;
    private Boolean active;
    private Boolean isSystemRole;

    private List<String> statuses;

    /**
     * built-in / custom.
     *
     * Worth its own dimension because the two behave differently: a system role cannot
     * be deleted and its permissions are topped up on every startup, so editing one is
     * a different act from editing a role somebody here wrote.
     */
    private List<String> kinds;

    /**
     * What is worth looking at.
     *
     * noPermissions — grants nothing; anybody holding it can sign in and do nothing
     * noUsers       — nobody holds it, so it is documentation rather than access
     */
    private List<String> qualities;

    private LocalDate createdAfter;
    private LocalDate createdBefore;

    /** The old `search` param and the house `keyword` mean the same thing. */
    public String effectiveKeyword() {
        if (keyword != null && !keyword.isBlank()) return keyword;
        return search;
    }

    public boolean wants(String quality) {
        return qualities != null && qualities.contains(quality);
    }

    public boolean hasStatus(String status) {
        return statuses != null && statuses.contains(status);
    }

    public boolean hasKind(String kind) {
        return kinds != null && kinds.contains(kind);
    }
}
