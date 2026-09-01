package com.itineraryledger.kabengosafaris.DataTransfer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * That importing a rate sheet does not ask the database about every row.
 *
 * Each rate used to cost three round trips — "is this one already here?", "does the park-tariff
 * link exist?", then the insert — across three modules. For a bundle of 5,394 rates that is roughly
 * sixteen thousand statements, and a check of one took 274 seconds: longer than the gateway holds
 * the request open, so the report could never reach the person who asked for it. The rows were
 * never the cost; the conversation about them was.
 *
 * Each module now reads what an owner already has ONCE, into a map, and keeps it current as rows
 * are written.
 */
class RatesDoNotQueryPerRowTest {

    private String sourceOf(String module) throws IOException {
        return Files.readString(Path.of("src/main/java/com/itineraryledger/kabengosafaris/"
            + "DataTransfer/Modules/" + module + ".java"));
    }

    @Test
    @DisplayName("every rate importer looks its owner's rates up in a map")
    void ratesComeFromAnIndex() throws IOException {
        for (String module : new String[] {
            "ParkTransfer", "ActivityTransfer", "AccommodationTransfer"}) {
            String source = sourceOf(module);
            assertTrue(source.contains("ratesAlreadyHere("),
                module + " should read its owner's existing rates once, not per row");
            assertTrue(source.contains("context.cached("),
                module + "'s index must live on the run's cache, or the second rate re-reads it");
        }
    }

    @Test
    @DisplayName("the per-row finders are gone from the rate loops")
    void noFinderPerRow() throws IOException {
        /*
         * Named exactly, because these are the three that cost the 274 seconds. A new one would
         * not be caught here — the map is the pattern to copy — but a revert would be.
         */
        assertFalse(sourceOf("ParkTransfer")
                .contains("findByParkIdAndTariffIdAndSeasonIdAndNationCategoryIdAndAgeCategoryId"),
            "ParkTransfer is asking per rate again");
        assertFalse(sourceOf("ActivityTransfer").contains("rates.findTheOne("),
            "ActivityTransfer is asking per rate again");
        assertFalse(sourceOf("AccommodationTransfer")
                .contains("findByAccommodationIdAndSeasonIdAndRoomTypeIdAndRoomStandardIdAndBoardTypeId"),
            "AccommodationTransfer is asking per rate again");
    }

    @Test
    @DisplayName("the park-tariff link is resolved once per tariff, not once per rate")
    void theLinkIsCached() throws IOException {
        String source = sourceOf("ParkTransfer");
        int lookup = source.indexOf("parkTariffs.findByParkIdAndTariffId");
        assertTrue(lookup > 0, "the lookup has moved");

        String before = source.substring(Math.max(0, lookup - 200), lookup);
        assertTrue(before.contains("context.cached("),
            "every rate in a park names the same handful of tariffs; the link must be cached");
    }

    @Test
    @DisplayName("a written rate joins the index, so a bundle naming one twice still sees the first")
    void writesUpdateTheIndex() throws IOException {
        for (String module : new String[] {
            "ParkTransfer", "ActivityTransfer", "AccommodationTransfer"}) {
            assertTrue(sourceOf(module).contains(".put(rateKey, rate)"),
                module + " must add what it writes to the index, or a duplicate row in one bundle "
                    + "is inserted twice instead of being seen as already here");
        }
    }
}
