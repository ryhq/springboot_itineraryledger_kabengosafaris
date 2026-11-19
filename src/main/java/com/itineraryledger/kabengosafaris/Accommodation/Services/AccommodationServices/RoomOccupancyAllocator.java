package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Who sleeps in which rooms on one night, so a per-person rate can bill people.
 *
 * <p>A lodge on <b>Per Person Sharing</b> charges once per guest per night. The estimators used to
 * bill {@code totalPax} against <b>every accommodation row of the night</b>, so a night written as
 * two rows billed the party twice:
 *
 * <pre>
 *   Kahawa House - Double Room   Per Person Sharing   qty=8  x 176 = 1408
 *   Kahawa House - Single Room   Per Person Sharing   qty=8  x 237 = 1896
 * </pre>
 *
 * <p>Sixteen people billed for a party of eight. It stayed invisible for as long as every
 * per-person night had exactly one row, and surfaced the moment a single room was added beside the
 * doubles - which is an ordinary thing to do, and had always been written in the party notes.
 * <b>Per Room</b> rates were never affected, which is why only some nights were wrong.
 *
 * <p>The rule: walk a night's rows in order and give each the people its rooms hold. {@code
 * roomCount} was already being read and then thrown away for per-person rates; this is what it is
 * for.
 *
 * <p><b>A night that cannot house the party bills only what it sleeps.</b> That is deliberate and
 * it was the operator's call: a trip was carrying two double rooms for eight guests in the central
 * Serengeti, and a rule that quietly billed all eight would have kept the price looking right while
 * four people had nowhere to sleep. A price that drops is a question somebody asks. {@link
 * Allocation#isShort()} says so explicitly, for the caller to report.
 *
 * <p>Pure arithmetic, no Spring, no database: the whole thing is testable as plain objects.
 */
public final class RoomOccupancyAllocator {

    private RoomOccupancyAllocator() {
    }

    /**
     * One row of a night: so many rooms of a type that sleeps so many each.
     *
     * @param rooms      how many rooms of this type, never below one
     * @param sleepsEach the room type's maximum occupancy, or null when nobody recorded it
     */
    public record RoomBlock(int rooms, Integer sleepsEach) {

        /** How many people this row can hold, or null when the room type's occupancy is unknown. */
        public Integer capacity() {
            if (sleepsEach == null || sleepsEach <= 0) return null;
            return Math.max(rooms, 0) * sleepsEach;
        }
    }

    /**
     * The answer: how many people each row carries, and whether the rooms covered the party.
     *
     * @param perRow          people allocated to each row, in the order the rows were given
     * @param party           how many were travelling
     * @param housed          how many the rooms could take
     * @param occupancyUnknown true when some row's room type had no occupancy recorded
     */
    public record Allocation(List<Integer> perRow, int party, int housed, boolean occupancyUnknown) {

        /** Nobody left without a bed. */
        public boolean isShort() {
            return housed < party;
        }

        /** How many of the party the rooms could not take. */
        public int unhoused() {
            return Math.max(party - housed, 0);
        }

        /** For the warning line: "rooms sleep 4 of 8". */
        public String shortfallMessage() {
            return "rooms sleep " + housed + " of " + party + (party == 1 ? " traveller" : " travellers");
        }
    }

    /**
     * Spread a party across one night's rows.
     *
     * <p>Rows are filled in the order given, each taking as many as its rooms hold and no more, so
     * the night bills the party once however many rows it is written as. A row the party has
     * already been exhausted on gets zero - surplus rooms are not billed to nobody.
     *
     * <p>A row whose occupancy nobody recorded takes everyone still unplaced. That keeps today's
     * figure on data we cannot read rather than silently billing less, and {@link
     * Allocation#occupancyUnknown()} lets the caller say why.
     *
     * @param blocks the night's rows, alternatives already removed by the caller
     * @param party  how many people are travelling
     */
    public static Allocation allocate(List<RoomBlock> blocks, int party) {
        List<Integer> perRow = new ArrayList<>();
        if (blocks == null || blocks.isEmpty()) {
            return new Allocation(perRow, party, 0, false);
        }

        int remaining = Math.max(party, 0);
        boolean unknown = false;

        for (RoomBlock block : blocks) {
            Integer capacity = block == null ? null : block.capacity();
            int taken;
            if (capacity == null) {
                /* Unknown occupancy: this row carries whoever is left rather than nobody. */
                unknown = true;
                taken = remaining;
            } else {
                taken = Math.min(capacity, remaining);
            }
            perRow.add(taken);
            remaining -= taken;
        }

        return new Allocation(perRow, Math.max(party, 0), Math.max(party, 0) - remaining, unknown);
    }
}
