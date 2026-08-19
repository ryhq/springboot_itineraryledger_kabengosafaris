package com.itineraryledger.kabengosafaris.Feature;

import java.util.List;

/**
 * The parts of this product a company may not have.
 *
 * One codebase serves several tour companies. Most of it is the same for all of them — customers,
 * quotes, safaris — but not all: a company with no vehicles of its own has no use for a fleet, one
 * that never issues credit notes should not be asked to learn what they are, and a company without a
 * website has nothing to fill the content pages with. Before this the only answers were "show
 * everybody everything" and "fork the codebase".
 *
 * A feature owns API paths. Switched off, those paths answer 404 — an absent module should look
 * absent, not forbidden — and the panel drops it from the sidebar, the command palette and the
 * breadcrumbs, because a nav entry that leads to a 404 is worse than no entry.
 *
 * Adding one: add the constant with the paths it owns, and FeatureCatalogueTest will tell you if a
 * path matches no controller (a flag that gates nothing) or if two features claim the same path.
 */
public enum Feature {

    FLEET(
        "fleet",
        "Fleet",
        "Vehicles, drivers, hires and rental clients. Off for a company that subcontracts its transport.",
        "OPERATIONS",
        List.of("/api/vehicles", "/api/drivers", "/api/vehicle-hires", "/api/rental-clients")),

    CREDIT_NOTES(
        "credit-notes",
        "Credit notes",
        "Refunds and write-offs against an invoice. Off where refunds are handled outside this system.",
        "SALES",
        List.of("/api/credit-notes")),

    AVAILABILITY_REQUESTS(
        "availability-requests",
        "Availability requests",
        "Letters asking accommodations to hold rooms, and the replies. Off where booking is done by phone.",
        "OPERATIONS",
        List.of("/api/availability-requests", "/api/safaris/*/availability-request*")),

    WEBSITE_CONTENT(
        "website-content",
        "Website content",
        "Heroes, blog, testimonies, FAQs and the newsletter — the pages a public site reads. "
            + "Off for a company whose website this system does not feed.",
        "CONTENT",
        List.of("/api/heroes", "/api/hero-images", "/api/blogs", "/api/blog-images",
            "/api/testimonies", "/api/testimony-images", "/api/faqs", "/api/newsletter-subscriptions")),

    TRANSLATION(
        "translation",
        "Translation",
        "Translating documents and email into a client's language. Needs a provider account, so it is "
            + "usually fixed per deployment rather than switched on here.",
        "SYSTEM",
        List.of("/api/translation", "/api/translation-accounts", "/api/translation-settings"));

    private final String key;
    private final String label;
    private final String description;
    /** which part of the panel it belongs to, for grouping the switches */
    private final String group;
    /** ant-style paths this feature owns; a `*` matches one segment */
    private final List<String> paths;

    Feature(String key, String label, String description, String group, List<String> paths) {
        this.key = key;
        this.label = label;
        this.description = description;
        this.group = group;
        this.paths = paths;
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public String getGroup() { return group; }
    public List<String> getPaths() { return paths; }

    /**
     * Everything is on unless somebody turns it off.
     *
     * The opposite default would mean a new feature silently disappears for every existing company on
     * the release that introduces it, and nobody would know to look for a switch they have never seen.
     */
    public boolean isEnabledByDefault() {
        return true;
    }

    /** The property that overrides the stored value: {@code app.features.fleet=false}. */
    public String getPropertyName() {
        return "app.features." + key;
    }

    public static Feature byKey(String key) {
        for (Feature feature : values()) {
            if (feature.key.equalsIgnoreCase(key)) return feature;
        }
        return null;
    }
}
