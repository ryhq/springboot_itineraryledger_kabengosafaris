package com.itineraryledger.kabengosafaris.Hero.Specifications;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;

import lombok.Data;

/**
 * Everything a caller can narrow the hero list by, in one object.
 *
 * The rows, the stat cards and the record walk are all built from this, so a card cannot
 * report a figure the table would contradict, and prev/next cannot wander out of the set on
 * screen. Spring binds it from the query string with {@code @ModelAttribute}, so every
 * parameter the old signature took is still spelled the same on the wire — the v1 panel is
 * still live against this API.
 *
 * Note the enum is `heroPage`, not `page`: `page` is the pagination parameter, and a filter
 * field of that name would eat it.
 */
@Data
public class HeroFilter {

    /** Free text across title, subtitle, description and the button's own words. */
    private String keyword;

    private String title;

    private HeroPage heroPage;
    private List<HeroPage> heroPages;

    private String textAlignment;
    private List<String> textAlignments;

    private Boolean isActive;
    /** "active" / "inactive"; a contradictory pair cancels to no constraint. */
    private List<String> statuses;

    /**
     * Actionable gaps, each of which is also a card.
     *
     * A hero with no image renders as a coloured block on the live site; a button with no
     * link does nothing when a customer clicks it. Both are invisible from the list until
     * somebody is told, and both are worth fixing.
     */
    private List<String> qualities;

    private String createdById;
    private String updatedById;

    private LocalDateTime createdAfter;

    /** The pages asked for, however they were spelled. */
    public List<HeroPage> allPages() {
        List<HeroPage> out = new ArrayList<>();
        if (heroPages != null) heroPages.stream().filter(Objects::nonNull).forEach(out::add);
        if (heroPage != null && !out.contains(heroPage)) out.add(heroPage);
        return out;
    }

    public List<String> allAlignments() {
        List<String> out = new ArrayList<>();
        if (textAlignments != null)
            textAlignments.stream().filter(v -> v != null && !v.isBlank()).forEach(out::add);
        if (textAlignment != null && !textAlignment.isBlank() && !out.contains(textAlignment))
            out.add(textAlignment);
        return out;
    }

    /**
     * The single active/inactive answer, or null when the pair cancels.
     *
     * Asking for both is not a contradiction to refuse — it is somebody clearing a filter
     * by selecting everything, and the honest reading is no constraint at all.
     */
    public Boolean resolvedActive() {
        boolean wantsActive = statuses != null && statuses.contains("active");
        boolean wantsInactive = statuses != null && statuses.contains("inactive");
        if (wantsActive ^ wantsInactive) return wantsActive;
        return isActive;
    }

    public boolean wants(String quality) {
        return qualities != null && qualities.contains(quality);
    }
}
