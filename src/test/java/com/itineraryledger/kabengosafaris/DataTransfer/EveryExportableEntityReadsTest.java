package com.itineraryledger.kabengosafaris.DataTransfer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * That the copier can actually read every entity the export walks.
 *
 * It reflects over an entity's properties and invokes each getter, so ONE getter that throws on the
 * data it meets takes the whole export down with a 500 and no clue which of eleven entity types was
 * responsible. That is exactly what happened on the first real export, and the message reaching the
 * screen said only "Could not build the bundle".
 *
 * A default-constructed instance is the harshest realistic case: every field null, which is what a
 * derived getter is most likely to trip over.
 */
class EveryExportableEntityReadsTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final List<Class<?>> EXPORTED = List.of(
        com.itineraryledger.kabengosafaris.Park.Park.class,
        com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate.class,
        com.itineraryledger.kabengosafaris.Tariff.Tariff.class,
        com.itineraryledger.kabengosafaris.Activity.Activity.class,
        com.itineraryledger.kabengosafaris.ActivityTariffRate.ActivityTariffRate.class,
        com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation.class,
        com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRate.class,
        com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType.class,
        com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard.class,
        com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType.class,
        com.itineraryledger.kabengosafaris.Season.Season.class,
        com.itineraryledger.kabengosafaris.Season.SeasonPeriod.class,
        com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory.class,
        com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory.class,
        com.itineraryledger.kabengosafaris.Park.Entities.ParkImage.class,
        com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage.class,
        com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage.class);

    @Test
    @DisplayName("every exported entity can be read with all its fields empty")
    void nothingThrowsOnAnEmptyRow() {
        List<String> broken = new ArrayList<>();

        for (Class<?> type : EXPORTED) {
            try {
                Object instance = type.getDeclaredConstructor().newInstance();
                Scalars.of(mapper, instance);
            } catch (NoSuchMethodException e) {
                broken.add(type.getSimpleName() + " has no no-arg constructor, so JPA cannot load it either");
            } catch (Exception e) {
                Throwable cause = e;
                while (cause.getCause() != null) cause = cause.getCause();
                StackTraceElement where = cause.getStackTrace().length > 0
                    ? cause.getStackTrace()[0] : null;
                broken.add(type.getSimpleName() + " -> " + cause.getClass().getSimpleName()
                    + ": " + cause.getMessage() + (where == null ? "" : "  at " + where));
            }
        }

        assertTrue(broken.isEmpty(),
            "The export reads every property of these, so one getter that throws is a 500 for the "
                + "whole bundle: " + broken);
    }
}
