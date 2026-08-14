package com.itineraryledger.kabengosafaris.Newsletter.Specifications;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.itineraryledger.kabengosafaris.Newsletter.Entity.SubscriptionStatus;

import lombok.Data;

/**
 * Everything a caller can narrow the subscriber list by, in one object.
 *
 * The rows, the stat cards and the record walk are all built from this, so a card cannot
 * report a figure the table would contradict, and prev/next cannot wander out of the set on
 * screen. Spring binds it with {@code @ModelAttribute}, so every parameter the old
 * signature took is still spelled the same on the wire — the v1 panel is still live against
 * this API.
 */
@Data
public class NewsletterFilter {

    /** Free text across email and name. */
    private String keyword;

    private String email;
    private String name;

    private SubscriptionStatus status;
    private List<SubscriptionStatus> statuses;

    private String source;
    private List<String> sources;

    /**
     * Actionable gaps, each of which is also a card.
     *
     * An address that bounced is a list you are paying to mail into a void; a subscriber
     * with no matching customer is somebody the sales side does not know about.
     */
    private List<String> qualities;

    private LocalDateTime subscribedAfter;
    private LocalDateTime subscribedBefore;

    public List<SubscriptionStatus> allStatuses() {
        List<SubscriptionStatus> out = new ArrayList<>();
        if (statuses != null) statuses.stream().filter(Objects::nonNull).forEach(out::add);
        if (status != null && !out.contains(status)) out.add(status);
        return out;
    }

    public List<String> allSources() {
        List<String> out = new ArrayList<>();
        if (sources != null) sources.stream().filter(v -> v != null && !v.isBlank()).forEach(out::add);
        if (source != null && !source.isBlank() && !out.contains(source)) out.add(source);
        return out;
    }

    public boolean wants(String quality) {
        return qualities != null && qualities.contains(quality);
    }
}
