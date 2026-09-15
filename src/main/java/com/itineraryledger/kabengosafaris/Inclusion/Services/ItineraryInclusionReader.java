package com.itineraryledger.kabengosafaris.Inclusion.Services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Entity.ItineraryInclusion;

import lombok.RequiredArgsConstructor;

/**
 * What an itinerary's price covers, resolved in one place.
 *
 * <p>This exists so that the fallback exists in exactly one place. Until the backfill has run —
 * and for anything it could not match, such as the three itineraries whose whole promise was typed
 * as a single 300-character paragraph — an itinerary's lines still live in the old
 * {@code inclusions} / {@code exclusions} TEXT columns. Every read goes through here, so an
 * itinerary with no rows yet still answers with what it has always said, and the migration is safe
 * by construction rather than by having been perfect.
 *
 * <p>The fallback is also what makes the whole change reversible: {@code DELETE FROM
 * itinerary_inclusions} restores the previous behaviour exactly, because nothing overwrote the
 * columns it reads.
 *
 * <p>Disabled catalogue items are filtered out of every live read. A line taken out of service
 * stops printing on new paperwork immediately, while documents already sent keep their own copy of
 * the wording and are untouched.
 */
@Service
@RequiredArgsConstructor
public class ItineraryInclusionReader {

    /** The lines this trip includes, in print order. */
    @Transactional(readOnly = true)
    public List<String> included(Itinerary itinerary) {
        return read(itinerary, true);
    }

    /** The lines this trip says it does NOT include, in print order. */
    @Transactional(readOnly = true)
    public List<String> excluded(Itinerary itinerary) {
        return read(itinerary, false);
    }

    /** True while this itinerary is still answering from the old text columns. */
    @Transactional(readOnly = true)
    public boolean isLegacy(Itinerary itinerary) {
        return itinerary != null
            && (itinerary.getInclusionList() == null || itinerary.getInclusionList().isEmpty());
    }

    private List<String> read(Itinerary itinerary, boolean wantIncluded) {
        if (itinerary == null) return List.of();

        List<ItineraryInclusion> rows = itinerary.getInclusionList();
        if (rows != null && !rows.isEmpty()) {
            List<String> lines = new ArrayList<>();
            for (ItineraryInclusion row : rows) {
                if (row.included() != wantIncluded) continue;
                var item = row.getInclusionItem();
                if (item == null || Boolean.FALSE.equals(item.getIsActive())) continue;
                String label = item.getLabel();
                if (label != null && !label.isBlank()) lines.add(label.trim());
            }
            return lines;
        }

        /* Nothing linked yet: answer from the column this replaced, exactly as before. */
        return splitLines(wantIncluded ? itinerary.getInclusions() : itinerary.getExclusions());
    }

    /**
     * One item per line, which is the contract the old columns were written under.
     *
     * <p>Kept byte-compatible with {@code PublicItineraryService.splitLines}, because the website
     * has been rendering the result of that method for months and a difference here would show up
     * as a changed bullet list on a page nobody edited.
     */
    public static List<String> splitLines(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\r?\\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .toList();
    }
}
