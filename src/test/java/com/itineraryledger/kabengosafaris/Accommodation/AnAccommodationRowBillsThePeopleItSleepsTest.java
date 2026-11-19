package com.itineraryledger.kabengosafaris.Accommodation;

import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices.RoomOccupancyAllocator;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices.RoomOccupancyAllocator.Allocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A night bills the party once, however many rows it is written as.
 *
 * <p>The estimators used to charge the whole party against every accommodation row, so writing a
 * night as "three doubles and a single" billed eight people twice - USD 3,304 for a night that
 * costs 1,293. Found on 2026-11-19 on a real quotation, where adding the single room the party
 * notes had always called for nearly doubled the quotation.
 */
class AnAccommodationRowBillsThePeopleItSleepsTest {

    private static RoomOccupancyAllocator.RoomBlock rooms(int count, Integer sleepsEach) {
        return new RoomOccupancyAllocator.RoomBlock(count, sleepsEach);
    }

    @Test
    @DisplayName("two rows never bill the party twice")
    void twoRowsBillThePartyOnce() {
        // Kahawa House: three doubles and a single, for a party of eight
        Allocation a = RoomOccupancyAllocator.allocate(
            List.of(rooms(3, 2), rooms(1, 1)), 8);

        assertEquals(List.of(6, 1), a.perRow(), "six in the doubles, one in the single");
        assertEquals(7, a.housed());
        assertEquals(7, a.perRow().stream().mapToInt(Integer::intValue).sum(),
            "the night bills seven people, not sixteen");
    }

    @Test
    @DisplayName("a night whose rooms hold everyone reports no shortfall")
    void aFullNightIsNotShort() {
        // Karatu: two double/twin, one triple, one single - exactly eight
        Allocation a = RoomOccupancyAllocator.allocate(
            List.of(rooms(2, 2), rooms(1, 3), rooms(1, 1)), 8);

        assertEquals(List.of(4, 3, 1), a.perRow());
        assertEquals(8, a.housed());
        assertFalse(a.isShort(), "every traveller has a bed");
        assertEquals(0, a.unhoused());
    }

    @Test
    @DisplayName("a night short of beds bills what it sleeps and says so")
    void aShortNightBillsItsBedsAndReports() {
        // The central Serengeti as it was found: two doubles for eight guests
        Allocation a = RoomOccupancyAllocator.allocate(List.of(rooms(2, 2)), 8);

        assertEquals(List.of(4), a.perRow(), "four beds bill four people, not eight");
        assertTrue(a.isShort());
        assertEquals(4, a.unhoused(), "four of the party have nowhere to sleep");
        assertEquals("rooms sleep 4 of 8 travellers", a.shortfallMessage());
    }

    @Test
    @DisplayName("surplus rooms are billed to nobody")
    void surplusRoomsBillNobody() {
        // four doubles booked for a party of five
        Allocation a = RoomOccupancyAllocator.allocate(
            List.of(rooms(2, 2), rooms(2, 2)), 5);

        assertEquals(List.of(4, 1), a.perRow(), "the party runs out part way through the second row");
        assertEquals(5, a.housed());
        assertFalse(a.isShort());
    }

    @Test
    @DisplayName("a row past the end of the party bills zero rather than the party again")
    void rowsPastThePartyBillZero() {
        Allocation a = RoomOccupancyAllocator.allocate(
            List.of(rooms(4, 2), rooms(1, 1)), 8);

        assertEquals(List.of(8, 0), a.perRow());
        assertEquals(8, a.housed());
    }

    @Test
    @DisplayName("an unrecorded occupancy takes whoever is left rather than under-billing")
    void unknownOccupancyAbsorbsTheRemainder() {
        Allocation a = RoomOccupancyAllocator.allocate(
            List.of(rooms(1, 2), rooms(1, null)), 8);

        assertEquals(List.of(2, 6), a.perRow(),
            "the row we cannot read carries the rest, so the night still bills eight");
        assertEquals(8, a.housed());
        assertFalse(a.isShort());
        assertTrue(a.occupancyUnknown(), "and the caller is told why");
    }

    @Test
    @DisplayName("a night with no rooms houses nobody")
    void noRoomsHousesNobody() {
        Allocation a = RoomOccupancyAllocator.allocate(List.of(), 8);
        assertEquals(0, a.housed());
        assertTrue(a.isShort());
        assertEquals(8, a.unhoused());
    }

    @Test
    @DisplayName("a party of nobody bills nobody")
    void noPartyBillsNobody() {
        Allocation a = RoomOccupancyAllocator.allocate(List.of(rooms(3, 2)), 0);
        assertEquals(List.of(0), a.perRow());
        assertEquals(0, a.housed());
        assertFalse(a.isShort());
    }
}
