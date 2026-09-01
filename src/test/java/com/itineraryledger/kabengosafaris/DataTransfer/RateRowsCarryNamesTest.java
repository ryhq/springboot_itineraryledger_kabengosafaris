package com.itineraryledger.kabengosafaris.DataTransfer;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate;

/**
 * A row carries its references as NAMES, and importing it must not choke on them.
 *
 * "High Season" is the only form of a season that means anything in another installation — an id
 * belongs to the database it came from. So every rate row has "season": "High Season" beside its
 * scalar columns, while the entity's season property is a Season.
 *
 * Applying that row asked Jackson to construct a Season out of a string. It failed, and because the
 * message printed an iterator's object address and dropped the cause, what a user saw was:
 *
 *     IllegalStateException: Could not apply java.util.LinkedHashMap$LinkedKeyIterator@458f3d5c
 *     to ParkTariffRate
 *
 * Parks, activities and accommodations all name their references this way, so no rate had ever
 * imported — on the first real bundle, all 2,664 of them.
 */
class RateRowsCarryNamesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /** A rate row as the exporter writes one: scalar columns, then the references by name. */
    private ObjectNode exportedRateRow() {
        ObjectNode row = mapper.createObjectNode();
        row.put("rackRate", "70.80");
        row.put("stoRate", "62.00");
        row.put("currency", "USD");
        row.put("notes", "per adult, per entry");
        row.put("isActive", true);
        row.put("tariff", "conservation-fee");
        row.put("season", "High Season");
        row.put("nationCategory", "Non-East African");
        row.put("ageCategory", "Adult");
        return row;
    }

    @Test
    @DisplayName("a rate row applies, references and all")
    void theRowApplies() {
        ParkTariffRate rate = new ParkTariffRate();
        assertDoesNotThrow(() -> Scalars.apply(mapper, exportedRateRow(), rate),
            "the four reference names must be ignored, not deserialised into entities");
    }

    @Test
    @DisplayName("the scalar columns still arrive")
    void theColumnsArrive() {
        ParkTariffRate rate = new ParkTariffRate();
        Scalars.apply(mapper, exportedRateRow(), rate);

        assertEquals(0, new BigDecimal("70.80").compareTo(rate.getRackRate()));
        assertEquals(0, new BigDecimal("62.00").compareTo(rate.getStoRate()));
        assertEquals("USD", rate.getCurrency());
        assertEquals("per adult, per entry", rate.getNotes());
        assertTrue(rate.getIsActive());
    }

    @Test
    @DisplayName("the references are left for the importer to resolve, not half-set")
    void referencesAreUntouched() {
        /*
         * The importer looks each name up in THIS installation and sets the association itself.
         * apply must leave those four alone rather than half-populate them from a string.
         */
        ParkTariffRate rate = new ParkTariffRate();
        Scalars.apply(mapper, exportedRateRow(), rate);

        assertNull(rate.getSeason());
        assertNull(rate.getNationCategory());
        assertNull(rate.getAgeCategory());
        assertNull(rate.getParkTariff());
    }

    @Test
    @DisplayName("when applying does fail, the message says what and where")
    void theMessageIsDiagnosable() {
        /*
         * The old message printed copy.fieldNames() — an Iterator — so it read like
         * "LinkedKeyIterator@458f3d5c", and the cause was dropped. A failed import told nobody
         * anything.
         */
        ObjectNode row = mapper.createObjectNode();
        row.put("rackRate", "not a number");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
            () -> Scalars.apply(mapper, row, new ParkTariffRate()));

        assertTrue(thrown.getMessage().contains("rackRate"),
            "the failing field should be named: " + thrown.getMessage());
        assertFalse(thrown.getMessage().contains("Iterator@"),
            "an object address is not a diagnosis: " + thrown.getMessage());
        assertNotNull(thrown.getCause(), "the cause carries the detail");
    }

    @Test
    @DisplayName("a column this installation has not got is still ignored, not refused")
    void unknownColumnsStayTolerated() {
        /*
         * The tolerance that lets a bundle from a newer installation import at all. Narrowing what
         * apply copies must not have narrowed this.
         */
        ObjectNode row = exportedRateRow();
        row.put("someColumnFromANewerVersion", "whatever");

        assertDoesNotThrow(() -> Scalars.apply(mapper, row, new ParkTariffRate()));
    }
}
