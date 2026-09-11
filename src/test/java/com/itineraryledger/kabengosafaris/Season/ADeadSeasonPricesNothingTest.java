package com.itineraryledger.kabengosafaris.Season;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.MonthDay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Switching a season off has to mean it stops being chosen.
 *
 * <p>It did not. {@code containsDate} checked the PERIOD's own flag and never looked at the season
 * the period belongs to, so deactivating a season left every one of its periods still matching
 * dates. One Zanzibar property carries five overlapping seasons, three of them deactivated and
 * holding no rate rows at all. A resolver walking periods in arbitrary order landed on a dead one,
 * found no rate, and the night disappeared from the customer's quote: 1,695 on one, 1,830 on
 * another, with the property not even named for anybody to notice it was missing.
 */
class ADeadSeasonPricesNothingTest {

    private static final LocalDate CHRISTMAS_2026 = LocalDate.of(2026, 12, 25);

    private SeasonPeriod period(boolean seasonActive, boolean periodActive) {
        Season season = Season.builder()
            .name(seasonActive ? "STO 2026 - High Season" : "Festive Season")
            .isActive(seasonActive)
            .isGlobal(false)
            .build();
        return SeasonPeriod.builder()
            .season(season)
            .startDate(MonthDay.of(12, 20))
            .endDate(MonthDay.of(12, 31))
            .isActive(periodActive)
            .build();
    }

    @Test
    @DisplayName("a live period of a live season contains its dates")
    void theNormalCase() {
        assertTrue(period(true, true).containsDate(CHRISTMAS_2026));
    }

    @Test
    @DisplayName("a live period of a DEAD season contains nothing")
    void aDeadSeasonIsNotChosen() {
        assertFalse(period(false, true).containsDate(CHRISTMAS_2026),
            "this is the bug: the period was active, the season was not, and it still matched. "
            + "Three deactivated seasons with no rates were competing to price a night, and "
            + "whichever won deleted it.");
    }

    @Test
    @DisplayName("a dead period is still dead, whatever its season says")
    void aDeadPeriodIsNotChosen() {
        assertFalse(period(true, false).containsDate(CHRISTMAS_2026));
        assertFalse(period(false, false).containsDate(CHRISTMAS_2026));
    }

    @Test
    @DisplayName("a period with no season attached is judged on its own flag")
    void anOrphanPeriodStillWorks() {
        SeasonPeriod orphan = SeasonPeriod.builder()
            .startDate(MonthDay.of(12, 20))
            .endDate(MonthDay.of(12, 31))
            .isActive(true)
            .build();
        assertTrue(orphan.containsDate(CHRISTMAS_2026),
            "a global or unattached period must not be broken by the season check");
    }

    @Test
    @DisplayName("the year pin and the wrap-around still behave")
    void theExistingRulesSurvive() {
        Season live = Season.builder().name("High").isActive(true).isGlobal(false).build();

        SeasonPeriod pinnedTo2027 = SeasonPeriod.builder()
            .season(live).startDate(MonthDay.of(1, 4)).endDate(MonthDay.of(3, 15))
            .year(2027).isActive(true).build();
        assertTrue(pinnedTo2027.containsDate(LocalDate.of(2027, 2, 25)));
        assertFalse(pinnedTo2027.containsDate(LocalDate.of(2028, 2, 25)),
            "a year-pinned period is why every 2028 date falls through for the Zanzibar hotels");

        SeasonPeriod wraps = SeasonPeriod.builder()
            .season(live).startDate(MonthDay.of(12, 15)).endDate(MonthDay.of(1, 15))
            .isActive(true).build();
        assertTrue(wraps.containsDate(LocalDate.of(2026, 12, 25)));
        assertTrue(wraps.containsDate(LocalDate.of(2027, 1, 10)));
        assertFalse(wraps.containsDate(LocalDate.of(2027, 6, 1)));
    }
}
