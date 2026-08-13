package com.itineraryledger.kabengosafaris.Permission.Specifications;

import java.util.ArrayList;
import java.util.List;

import com.itineraryledger.kabengosafaris.Permission.PermissionAction;

import lombok.Data;

/**
 * Everything a caller can narrow the permission catalogue by, in one object.
 *
 * Nearly five hundred rows, four fifths of them generated as action × entity, so this
 * list is never read top to bottom — it is filtered to the handful somebody is asking
 * about. The two dimensions that matter are which part of the system a permission
 * covers (entity) and what it lets you do with it (action).
 *
 * The third is the interesting one: which permissions no role grants at all. Those are
 * capabilities nobody in the company has, usually because a module was built and never
 * assigned to anybody — an endpoint that returns 403 to every person who tries it.
 */
@Data
public class PermissionFilter {

    /** Free text across the name, the entity and the description. */
    private String keyword;
    /** what the older screens called it */
    private String search;

    /** The old discrete params, still honoured. */
    private String name;
    private String entity;
    private PermissionAction action;
    private Boolean active;

    private List<String> entities;
    private List<PermissionAction> actions;
    private List<String> statuses;

    /**
     * noRoles — no role grants it, so nobody can do it
     * custom  — not one of the generated action × entity four; a named capability
     */
    private List<String> qualities;

    public List<String> allEntities() {
        List<String> out = new ArrayList<>();
        if (entities != null) entities.stream().filter(e -> e != null && !e.isBlank()).forEach(out::add);
        if (entity != null && !entity.isBlank() && !out.contains(entity)) out.add(entity);
        return out;
    }

    public List<PermissionAction> allActions() {
        List<PermissionAction> out = new ArrayList<>();
        if (actions != null) actions.stream().filter(a -> a != null).forEach(out::add);
        if (action != null && !out.contains(action)) out.add(action);
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
