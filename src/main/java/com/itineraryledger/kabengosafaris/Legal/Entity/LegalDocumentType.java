package com.itineraryledger.kabengosafaris.Legal.Entity;

/** The notices a company publishes. */
public enum LegalDocumentType {

    PRIVACY_POLICY("Privacy policy", "privacy-policy"),
    TERMS("Terms and conditions", "terms");

    private final String displayName;
    /** What it is called in a URL, on the websites and on the public endpoint. */
    private final String slug;

    LegalDocumentType(String displayName, String slug) {
        this.displayName = displayName;
        this.slug = slug;
    }

    public String getDisplayName() { return displayName; }
    public String getSlug() { return slug; }

    public static LegalDocumentType bySlug(String slug) {
        if (slug == null) return null;
        for (LegalDocumentType type : values()) {
            if (type.slug.equalsIgnoreCase(slug) || type.name().equalsIgnoreCase(slug)) return type;
        }
        return null;
    }
}
