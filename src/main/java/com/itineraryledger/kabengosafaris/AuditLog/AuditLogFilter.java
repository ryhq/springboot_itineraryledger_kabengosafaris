package com.itineraryledger.kabengosafaris.AuditLog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Everything a caller can narrow the audit log by, in one object.
 *
 * The rows, the stat cards and the record walk are all built from this, so a card cannot
 * report a figure the table would contradict, and prev/next cannot wander out of the set on
 * screen. Spring binds it with {@code @ModelAttribute}, so every parameter the old signature
 * took is still spelled the same on the wire.
 *
 * This is the one list where the time window is the primary filter rather than a nicety: an
 * audit log is read as "what happened between Tuesday and now", and without a range every
 * question starts by paging through today.
 */
@Data
public class AuditLogFilter {

    /** Free text across who, what, which record and the description. */
    private String keyword;

    private String name;
    private String username;
    private String userId;

    private String action;
    private List<String> actions;

    private String entityType;
    private List<String> entityTypes;

    private String entityId;

    private String status;
    private List<String> statuses;

    private String ipAddress;
    private String userAgent;
    private String description;
    private String errorMessage;

    /**
     * Worth looking at, each of which is also a card.
     *
     * A failure is an action somebody attempted and did not get; a deletion is the one kind
     * of success that cannot be undone. Both are invisible among thousands of ordinary rows.
     */
    private List<String> qualities;

    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;

    public List<String> allActions() {
        return merge(actions, action);
    }

    public List<String> allEntityTypes() {
        return merge(entityTypes, entityType);
    }

    public List<String> allStatuses() {
        return merge(statuses, status);
    }

    public boolean wants(String quality) {
        return qualities != null && qualities.contains(quality);
    }

    private List<String> merge(List<String> many, String one) {
        List<String> out = new ArrayList<>();
        if (many != null) many.stream().filter(v -> v != null && !v.isBlank()).forEach(out::add);
        if (one != null && !one.isBlank() && !out.contains(one)) out.add(one);
        return out;
    }
}
