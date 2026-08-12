package com.itineraryledger.kabengosafaris.Vendor.Specifications;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.itineraryledger.kabengosafaris.Vendor.Enums.VendorType;

import lombok.Data;

/**
 * Everything a caller can narrow the vendor list by, in one object.
 *
 * The rows, the stat cards and the record walk are all built from this, so a card
 * cannot report a figure the table would contradict, and prev/next cannot wander
 * out of the set on screen. Spring binds it from the query string with
 * {@code @ModelAttribute}, so every parameter the old signature took is still
 * spelled the same on the wire.
 */
@Data
public class VendorFilter {

    /** Free text across name, code, contact person, email and phone. */
    private String keyword;

    private String name;
    private String code;

    private VendorType type;
    private List<VendorType> types;

    private String city;
    private List<String> cities;
    private String country;
    private List<String> countries;

    /** What we normally settle in — TZS, USD, EUR. */
    private List<String> currencies;

    private Boolean isActive;
    /** "active" / "inactive"; a contradictory pair cancels to no constraint. */
    private List<String> statuses;

    /**
     * Actionable gaps, the same idea as the customer list's data-quality cards.
     *
     * A vendor with no email cannot be sent a purchase order; one with no phone
     * cannot be chased when a booking has to change today. Both are worth a card
     * because both are worth fixing.
     */
    private List<String> qualities;

    private LocalDateTime createdAfter;

    /** The types asked for, however they were spelled. */
    public List<VendorType> allTypes() {
        List<VendorType> out = new ArrayList<>();
        if (types != null) types.stream().filter(Objects::nonNull).forEach(out::add);
        if (type != null && !out.contains(type)) out.add(type);
        return out;
    }

    public List<String> allCities() {
        return merge(cities, city);
    }

    public List<String> allCountries() {
        return merge(countries, country);
    }

    public boolean wants(String quality) {
        return qualities != null && qualities.contains(quality);
    }

    private List<String> merge(List<String> many, String one) {
        List<String> out = new ArrayList<>();
        if (many != null) many.stream().filter(v -> v != null && !v.isBlank()).forEach(out::add);
        if (one != null && !one.isBlank() && !out.contains(one)) out.add(one);
        return out;
    }
}
