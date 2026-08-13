package com.itineraryledger.kabengosafaris.EmailEvent.Specifications;

import java.util.List;

import lombok.Data;

/**
 * Everything a caller can narrow the email-event list by.
 *
 * A fixed, smallish set — one row per thing the system sends — so this is less about
 * finding one than about the two states that matter and are otherwise invisible:
 *
 *   · An event switched off sends nothing at all. That is a legitimate choice and a
 *     silent one; nobody notices a receipt that never goes out.
 *   · An event with no enabled template has nothing to send even while switched on,
 *     which is the same outcome arrived at by accident rather than on purpose.
 */
@Data
public class EmailEventFilter {

    /** Free text across the name and the description. */
    private String keyword;
    /** what the older screens called it */
    private String search;

    private String name;
    private Boolean enabled;

    private List<String> statuses;

    /**
     * noTemplates — nothing to send, so the event is dead however it is configured
     * noSystemDefault — nothing to fall back to if somebody deletes the custom one
     */
    private List<String> qualities;

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
