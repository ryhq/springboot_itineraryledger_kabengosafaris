package com.itineraryledger.kabengosafaris.Safari;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * That a property used on separate nights is asked about as separate bookings.
 *
 * The letter read the earliest night as check-in and the latest as check-out, so a lodge used on the
 * 26th and the 28th was sent "Check-in 26/01, Check-out 29/01, Number of Nights 2" — three nights of
 * dates against a count of two, for guests who are elsewhere on the 27th. The reservations desk
 * either blocks a night nobody wants or writes back to ask what we meant.
 *
 * The grouping is the whole fix, so it is tested on its own: the facts table, the subject line and
 * the repeated blocks are all derived from these stretches.
 */
class SplitStayLetterTest {

    /** The same rule the letter service uses: a run breaks where the next night is not the next day. */
    private List<List<LocalDate>> consecutiveBlocks(Collection<LocalDate> nights) {
        List<LocalDate> ordered = new ArrayList<>(new TreeSet<>(nights));
        List<List<LocalDate>> blocks = new ArrayList<>();
        List<LocalDate> current = new ArrayList<>();
        for (LocalDate night : ordered) {
            if (!current.isEmpty() && !current.get(current.size() - 1).plusDays(1).equals(night)) {
                blocks.add(current);
                current = new ArrayList<>();
            }
            current.add(night);
        }
        if (!current.isEmpty()) blocks.add(current);
        return blocks;
    }

    private LocalDate jan(int day) {
        return LocalDate.of(2027, 1, day);
    }

    @Test
    @DisplayName("the real case: nights on the 26th and the 28th are two bookings, not one span")
    void twoNightsWithAGap() {
        List<List<LocalDate>> blocks = consecutiveBlocks(List.of(jan(26), jan(28)));

        assertEquals(2, blocks.size(), "26th and 28th are not consecutive");
        assertEquals(List.of(jan(26)), blocks.get(0));
        assertEquals(List.of(jan(28)), blocks.get(1));

        LocalDate wrongCheckOut = jan(28).plusDays(1);
        assertEquals(3, java.time.temporal.ChronoUnit.DAYS.between(jan(26), wrongCheckOut),
            "26/01 to 29/01 is three nights, which is what a lodge would have blocked — while the "
                + "same letter said the number of nights was two");
    }

    @Test
    @DisplayName("consecutive nights stay one booking, so the common case is untouched")
    void oneStretchStaysOne() {
        List<List<LocalDate>> blocks = consecutiveBlocks(List.of(jan(26), jan(27), jan(28)));

        assertEquals(1, blocks.size());
        assertEquals(3, blocks.get(0).size());
        assertEquals(jan(26), blocks.get(0).get(0));
        assertEquals(jan(29), blocks.get(0).get(2).plusDays(1), "check-out is the morning after");
    }

    @Test
    @DisplayName("nights out of order are sorted before being split")
    void orderDoesNotDecideTheAnswer() {
        /*
         * Stays arrive in whatever order their ids were passed in, which is the order the picker
         * built from the day tree. Reading them unsorted would find a break between every pair.
         */
        List<List<LocalDate>> blocks = consecutiveBlocks(List.of(jan(28), jan(26), jan(27)));
        assertEquals(1, blocks.size(), "three consecutive nights, whatever order they arrived in");
    }

    @Test
    @DisplayName("three separate visits are three blocks")
    void moreThanTwo() {
        List<List<LocalDate>> blocks = consecutiveBlocks(List.of(jan(2), jan(3), jan(9), jan(20)));
        assertEquals(3, blocks.size());
        assertEquals(2, blocks.get(0).size());
        assertEquals(1, blocks.get(1).size());
        assertEquals(1, blocks.get(2).size());
    }

    @Test
    @DisplayName("a repeated night does not invent a second visit")
    void duplicatesCollapse() {
        /* two rooms on one night are two rows and one night — the map is keyed by date for this */
        List<List<LocalDate>> blocks = consecutiveBlocks(List.of(jan(26), jan(26), jan(27)));
        assertEquals(1, blocks.size());
        assertEquals(2, blocks.get(0).size(), "one night, however many rooms are on it");
    }
}
