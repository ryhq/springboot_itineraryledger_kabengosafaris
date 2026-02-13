package com.itineraryledger.kabengosafaris.Hero.Enums;

import lombok.Getter;

/**
 * HeroPage Enum - Defines pages/locations where hero sections can be displayed
 *
 * Each page can have multiple hero sections for carousel/slider functionality
 */
@Getter
public enum HeroPage {
    HOME("Home", "Main homepage"),
    ABOUT("About", "About us page"),
    SERVICES("Services", "Services overview page"),
    SAFARIS("Safaris", "Safari packages page"),
    DESTINATIONS("Destinations", "Destinations overview page"),
    ACCOMMODATIONS("Accommodations", "Accommodations listing page"),
    GALLERY("Gallery", "Photo gallery page"),
    CONTACT("Contact", "Contact us page"),
    BLOG("Blog", "Blog/news page"),
    FAQ("FAQ", "Frequently asked questions page"),
    TESTIMONIALS("Testimonials", "Customer testimonials page"),
    BOOKING("Booking", "Booking page"),
    OTHER("Other", "Other pages");

    private final String displayName;
    private final String description;

    HeroPage(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
