package com.itineraryledger.kabengosafaris.Testimony.Enums;

import lombok.Getter;

@Getter
public enum TestimonySource {
    WEBSITE("Website", "Review from company website"),
    GOOGLE("Google", "Google Maps/Business review"),
    TRIPADVISOR("TripAdvisor", "TripAdvisor review"),
    FACEBOOK("Facebook", "Facebook review"),
    EMAIL("Email", "Review received via email"),
    OTHER("Other", "Other review source");

    private final String displayName;
    private final String description;

    TestimonySource(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
