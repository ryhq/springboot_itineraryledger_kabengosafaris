package com.itineraryledger.kabengosafaris.WebsiteCache;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The names this API and the public website agree to call things.
 *
 * <p>The website labels every answer it caches with one or more of these, and clearing a label
 * throws away every page built from an answer wearing it. So this list is a contract between two
 * repositories: a name added here means nothing until the website tags a fetch with it.
 *
 * <h2>Why these are collections and not records</h2>
 *
 * The obvious design is a tag per record — {@code park:7xKq2} — so editing one park leaves the
 * other nineteen alone. It does not work here, for a reason specific to this API: <b>ids are
 * obfuscated and the obfuscation rotates on every restart</b>. The id this process would put in a
 * tag today is not the id the website cached the page under yesterday, so a per-record tag would
 * silently stop matching after each deploy — the exact failure that is invisible until a client
 * sees a stale price.
 *
 * <p>Clearing all parks because one changed costs a re-render of about twenty pages, once, at the
 * moment somebody pressed Save. That is a price worth paying for a scheme that cannot drift.
 */
public final class WebsiteCacheTags {

    /** Every cached answer carries this as well, so one call can empty the whole site. */
    public static final String ALL = "site";

    public static final String SAFARIS = "safaris";
    public static final String PARKS = "parks";
    public static final String ACCOMMODATIONS = "accommodations";
    public static final String ACTIVITIES = "activities";
    public static final String TESTIMONIES = "testimonies";
    public static final String HEROES = "heroes";
    public static final String BLOG = "blog";
    public static final String FAQS = "faqs";
    /** The company's own name, logo, colours and contact details, which sit in the site's layout. */
    public static final String BRAND = "brand";

    /** Every tag a caller may ask for, in the order the Settings page should offer them. */
    public static final List<String> CATALOGUE = List.of(
        ALL, SAFARIS, PARKS, ACCOMMODATIONS, ACTIVITIES, TESTIMONIES, HEROES, BLOG, FAQS, BRAND);

    /**
     * Which admin endpoints change which public pages.
     *
     * <p>Keyed on the URL space rather than on the entity, because the URL space is what the panel
     * actually calls and what a future resource descriptor will register. A path with no entry here
     * changes nothing a visitor can see — a quote, an invoice, a user — and clears nothing.
     *
     * <p>Longest prefix wins, so {@code /api/park-activities} is not read as {@code /api/park}.
     */
    private static final Map<String, List<String>> BY_PATH = Map.ofEntries(
        Map.entry("/api/itineraries", List.of(SAFARIS)),
        Map.entry("/api/itinerary-images", List.of(SAFARIS)),
        Map.entry("/api/inclusion-items", List.of(SAFARIS)),

        Map.entry("/api/parks", List.of(PARKS, SAFARIS)),
        Map.entry("/api/park-images", List.of(PARKS)),
        Map.entry("/api/park-tariffs", List.of(PARKS)),
        Map.entry("/api/park-tariff-rates", List.of(PARKS)),

        Map.entry("/api/activities", List.of(ACTIVITIES, SAFARIS)),
        Map.entry("/api/activity-images", List.of(ACTIVITIES)),
        Map.entry("/api/activity-tariff-rates", List.of(ACTIVITIES)),
        // A park activity is shown on both the park page and the activity page.
        Map.entry("/api/park-activities", List.of(ACTIVITIES, PARKS)),
        Map.entry("/api/park-activity-images", List.of(ACTIVITIES, PARKS)),

        Map.entry("/api/accommodations", List.of(ACCOMMODATIONS, SAFARIS)),
        Map.entry("/api/accommodation-images", List.of(ACCOMMODATIONS)),
        Map.entry("/api/accommodation-room-types", List.of(ACCOMMODATIONS)),
        Map.entry("/api/accommodation-room-standards", List.of(ACCOMMODATIONS)),
        Map.entry("/api/accommodation-board-types", List.of(ACCOMMODATIONS)),

        Map.entry("/api/heroes", List.of(HEROES)),
        Map.entry("/api/hero-images", List.of(HEROES)),
        Map.entry("/api/testimonies", List.of(TESTIMONIES)),
        Map.entry("/api/testimony-images", List.of(TESTIMONIES)),
        Map.entry("/api/blogs", List.of(BLOG)),
        Map.entry("/api/blog-images", List.of(BLOG)),
        Map.entry("/api/faqs", List.of(FAQS)),

        // The company record is the site's header, footer and every logo on it.
        Map.entry("/api/company", List.of(BRAND))
    );

    /** The tags a write to this path should clear, or empty when a visitor could not tell. */
    public static Set<String> forPath(String path) {
        if (path == null) return Set.of();
        String best = null;
        for (String prefix : BY_PATH.keySet()) {
            if (!path.equals(prefix) && !path.startsWith(prefix + "/")) continue;
            if (best == null || prefix.length() > best.length()) best = prefix;
        }
        return best == null ? Set.of() : new LinkedHashSet<>(BY_PATH.get(best));
    }

    /** Refuses a name the website would not recognise, rather than clearing nothing in silence. */
    public static Set<String> validate(List<String> requested) {
        if (requested == null || requested.isEmpty()) return Set.of(ALL);
        Set<String> out = new LinkedHashSet<>();
        for (String t : requested) {
            String tag = t == null ? "" : t.trim().toLowerCase();
            if (!CATALOGUE.contains(tag)) {
                throw new IllegalArgumentException(
                    "\"" + t + "\" is not something the website labels. Known labels: "
                        + String.join(", ", CATALOGUE));
            }
            out.add(tag);
        }
        // Asking for everything makes the rest of the list redundant.
        return out.contains(ALL) ? Set.of(ALL) : out;
    }

    /** The words a person should see, not the words the machines exchange. */
    public static String describe(Set<String> tags) {
        if (tags.contains(ALL)) return "the whole site";
        return String.join(", ", Arrays.stream(tags.toArray(String[]::new)).sorted().toList());
    }

    private WebsiteCacheTags() {}
}
