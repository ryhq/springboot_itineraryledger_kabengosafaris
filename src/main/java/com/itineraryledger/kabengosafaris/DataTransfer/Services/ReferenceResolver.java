package com.itineraryledger.kabengosafaris.DataTransfer.Services;

import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.DataTransfer.TransferContext;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Repositories.PaxAgeCategoryRepository;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonRepository;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import com.itineraryledger.kabengosafaris.Tariff.Repositories.TariffRepository;

import lombok.RequiredArgsConstructor;

/**
 * Turning a name in a bundle into a row in this company.
 *
 * The single most important piece of an import, and the one place a fault is invisible. Every id in
 * a bundle is worthless here — they are per-installation and rotate on restart — so names are all
 * there is, and a name that fails to match does not throw. It produces null, and the caller reports
 * a rate it could not place, by name. The alternative is worse in both directions: throwing abandons
 * 2,599 good rates over one bad one, and creating the missing thing silently invents a park.
 *
 * Every lookup is case-insensitive and cached for the run. A bundle of park rates names the same six
 * seasons thousands of times, and asking the database each time turns a ten-second import into
 * several minutes of identical queries.
 */
@Service
@RequiredArgsConstructor
public class ReferenceResolver {

    private final TariffRepository tariffs;
    private final com.itineraryledger.kabengosafaris.Activity.ActivityRepository activities;
    private final SeasonRepository seasons;
    private final PaxNationCategoryRepository nations;
    private final PaxAgeCategoryRepository ages;
    private final ParkRepository parks;

    public Tariff tariff(TransferContext context, String slug) {
        if (blank(slug)) return null;
        return context.cached("tariff", slug, key -> tariffs.findBySlug(key).orElse(null));
    }

    /**
     * A company-wide season by name.
     *
     * Global only. A lodge's own "High Season" is a different row with different dates, and letting
     * a park rate bind to one would price a park against a single lodge's calendar.
     */
    public Season globalSeason(TransferContext context, String name) {
        if (blank(name)) return null;
        return context.cached("season:global", name,
            key -> seasons.findByIsGlobalTrueAndNameIgnoreCase(key).orElse(null));
    }

    /** A season belonging to one accommodation, falling back to a company-wide one of that name. */
    public Season seasonFor(TransferContext context, Long accommodationId, String name) {
        if (blank(name) || accommodationId == null) return globalSeason(context, name);
        Season own = context.cached("season:" + accommodationId, name,
            key -> seasons.findByAccommodationIdAndNameIgnoreCase(accommodationId, key).orElse(null));
        return own != null ? own : globalSeason(context, name);
    }

    /** An activity by slug. Slugs travel; names get retyped. */
    public com.itineraryledger.kabengosafaris.Activity.Activity activity(
            TransferContext context, String slug) {
        if (slug == null || slug.isBlank()) return null;
        return context.cached("activity", slug, key -> activities.findBySlug(key).orElse(null));
    }

    public PaxNationCategory nation(TransferContext context, String name) {
        if (blank(name)) return null;
        return context.cached("nation", name, key -> nations.findByNameIgnoreCase(key).orElse(null));
    }

    public PaxAgeCategory age(TransferContext context, String name) {
        if (blank(name)) return null;
        return context.cached("age", name, key -> ages.findByNameIgnoreCase(key).orElse(null));
    }

    /** By slug, then by name — an activity rate may name a park that was created by hand elsewhere. */
    public Park park(TransferContext context, String slugOrName) {
        if (blank(slugOrName)) return null;
        return context.cached("park", slugOrName, key -> parks.findBySlug(key)
            .or(() -> parks.findByName(key))
            .orElse(null));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank() || "null".equals(value);
    }
}
