package com.itineraryledger.kabengosafaris.EmailEvent.Specifications;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Everything a caller can narrow the template list by, across every event.
 *
 * The nested list under an event answers "what can this email send". This answers the
 * questions that span events and could not be asked before: which wording did somebody
 * change, which templates are switched off, and which events are relying on a custom
 * template with no original left to fall back to.
 */
@Data
public class EmailTemplateFilter {

    /** Free text across the name, the description and the file name. */
    private String keyword;
    /** what the older screens called it */
    private String search;

    private String name;

    /** Narrow to one event, by obfuscated id — what the event's own tab sends. */
    private String eventId;
    /** …or several. */
    private List<String> eventIds;

    private Boolean enabled;
    private Boolean isDefault;
    private Boolean isSystemDefault;

    private List<String> statuses;

    /**
     * kinds — the three roles a template can hold:
     *   sent      — the one this event actually uses
     *   original  — shipped with the system; editable and restorable, never deletable
     *   custom    — written here
     */
    private List<String> kinds;

    public List<String> allEventIds() {
        List<String> out = new ArrayList<>();
        if (eventIds != null) eventIds.stream().filter(e -> e != null && !e.isBlank()).forEach(out::add);
        if (eventId != null && !eventId.isBlank() && !out.contains(eventId)) out.add(eventId);
        return out;
    }

    /** The old `search` param and the house `keyword` mean the same thing. */
    public String effectiveKeyword() {
        if (keyword != null && !keyword.isBlank()) return keyword;
        return search;
    }

    public boolean hasStatus(String status) {
        return statuses != null && statuses.contains(status);
    }

    public boolean hasKind(String kind) {
        return kinds != null && kinds.contains(kind);
    }
}
