package com.itineraryledger.kabengosafaris.User.Specifications;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Everything a caller can narrow the user list by, in one object.
 *
 * The questions this list is actually asked are about access, not about people:
 * who can log in, who is locked out and needs helping this morning, who has no
 * role and therefore cannot do anything, and who has been invited but never
 * arrived. Each of those is a filter here and a counter on the list, because a
 * figure nobody can click through to is only half an answer.
 */
@Data
public class UserFilter {

    /** Free text across name, username, email and phone. */
    private String keyword;
    /** what the older role screens called it */
    private String search;

    /** Narrow to holders of one role, by obfuscated id. */
    private String roleId;
    /** …or several, OR'd together. */
    private List<String> roleIds;

    /** Role names, for links that arrive knowing "ADMIN" rather than an id. */
    private List<String> roleNames;

    private Boolean enabled;
    private Boolean accountLocked;
    private Boolean mfaEnabled;

    /**
     * The status dimension, as the list presents it.
     *
     * active / inactive is enabled; locked is the lockout flag, which is separate:
     * a locked account is still enabled, it just cannot get in right now.
     */
    private List<String> statuses;

    /**
     * What needs attention.
     *
     * noRoles     — can log in and do nothing; almost always an unfinished setup
     * neverSignedIn — invited, never activated; the invite may have been lost
     * mfaOff      — no second factor on an account that can see customer data
     * passwordExpired — will be refused at the next login
     * failedAttempts  — somebody is getting the password wrong, or guessing it
     */
    private List<String> qualities;

    private LocalDate createdAfter;
    private LocalDate createdBefore;

    public List<String> allRoleIds() {
        List<String> out = new ArrayList<>();
        if (roleIds != null) roleIds.stream().filter(r -> r != null && !r.isBlank()).forEach(out::add);
        if (roleId != null && !roleId.isBlank() && !out.contains(roleId)) out.add(roleId);
        return out;
    }

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
}
