package com.itineraryledger.kabengosafaris.Inclusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.GlobalEnums.LineCategoryScope;
import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionItem;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;

/**
 * An inclusion line claims something checkable, or it claims nothing — and null means nothing.
 *
 * <p>This is the one place in the feature where a correct-looking one-liner produces a wrong answer
 * on every row. {@link LineCategoryScope} is shared with {@code taxAppliesTo} and
 * {@code discountAppliesTo}, where an unset scope means "the whole quote" — so
 * {@code covers(null, anything)} answers <strong>true</strong> and {@code parse(null, …)} returns
 * every category. Here the meaning is inverted: an unset scope means this sentence says nothing a
 * machine can verify. Read it the shared way and "Drinking water on game drives" accuses every
 * quote ever written of contradicting itself about park fees, insurance and visas at once.
 *
 * <p>The second half pins the two seeds whose scope is deliberately null, because both are the kind
 * of "obvious" tightening a later reader would add, and both would fire on every standard quote.
 */
class AnInclusionClaimsOnlyWhatCanBeCheckedTest {

    private static final Path SEEDER = Paths.get(
        "src/main/java/com/itineraryledger/kabengosafaris/Initializers/InclusionItemInitializer.java");

    @Test
    @DisplayName("the shared scope helper reads null as everything, which is why we never pass it null")
    void theSharedHelperReadsNullTheOtherWayRound() {
        /*
         * Not a complaint about LineCategoryScope — it is right for tax and discount. This asserts
         * the behaviour so that anybody changing it learns that this module depends on it.
         */
        assertTrue(LineCategoryScope.covers(null, QuoteItemType.PARK_FEE),
            "LineCategoryScope treats an unset scope as every category; the inclusion guard must "
            + "test for null BEFORE it calls covers()");
        assertTrue(LineCategoryScope.covers("", QuoteItemType.ACCOMMODATION));
    }

    @Test
    @DisplayName("an item with no scope makes no claim, so nothing can check it")
    void noScopeMeansNoClaim() {
        InclusionItem water = InclusionItem.builder()
            .label("Drinking water on game drives")
            .claimAppliesTo(null)
            .build();
        InclusionItem blank = InclusionItem.builder()
            .label("Government taxes & levies")
            .claimAppliesTo("   ")
            .build();
        InclusionItem parkFees = InclusionItem.builder()
            .label("All park, conservation & crater-service fees")
            .claimAppliesTo("PARK_FEE")
            .build();

        assertFalse(water.makesACheckableClaim(), "no scope must mean no claim");
        assertFalse(blank.makesACheckableClaim(), "a blank scope must mean no claim");
        assertTrue(parkFees.makesACheckableClaim());
    }

    @Test
    @DisplayName("the two seeds that would fire on every quote keep their null scope")
    void theDeliberateNullsStayNull() {
        /*
         * Travel insurance scoped to INSURANCE would contradict the flying-doctors cover we DO
         * include, and optional activities scoped to ACTIVITY would contradict every game drive.
         * Both look like obvious tightenings and both are wrong; a guard that fires on every quote
         * is a guard somebody switches off.
         */
        assertEquals(null, scopeOfSeed("Travel & medical insurance"),
            "scoping travel insurance to INSURANCE contradicts the flying-doctors cover we include");
        assertEquals(null, scopeOfSeed("Optional activities (e.g. balloon safari, cultural visits)"),
            "scoping optional activities to ACTIVITY contradicts every included game drive");

        /* And the one that is deliberately NOT the obvious category. */
        assertEquals("VISA", scopeOfSeed("International flights & visas"),
            "VISA, not TRANSPORT — an internal hopper is a TRANSPORT line and is routinely included");
    }

    @Test
    @DisplayName("every seed's scope names only real line categories")
    void everySeedScopeIsReal() {
        List<String> offences = new ArrayList<>();
        for (String[] seed : seeds()) {
            String scope = seed[1];
            if (scope == null) continue;
            for (String name : scope.split(",")) {
                try {
                    QuoteItemType.valueOf(name.trim());
                } catch (IllegalArgumentException e) {
                    offences.add(seed[0] + " claims \"" + name.trim() + "\", which is not a QuoteItemType");
                }
            }
        }
        assertTrue(offences.isEmpty(), () ->
            "A scope naming a category that does not exist is silently dropped by "
            + "LineCategoryScope.parse, and a scope of ONLY unknown names widens to everything:\n"
            + String.join("\n", offences));
    }

    /* ---- reading the seed table out of the initializer, so the test pins the shipped values ---- */

    private static final Pattern SEED = Pattern.compile(
        "new Seed\\(\"((?:[^\"\\\\]|\\\\.)*)\"\\s*,\\s*\"[^\"]*\"\\s*,\\s*(?:true|false)\\s*,\\s*(null|\"[^\"]*\")\\s*\\)");

    private String scopeOfSeed(String label) {
        for (String[] seed : seeds()) {
            if (seed[0].equals(label)) return seed[1];
        }
        throw new AssertionError("No seed called \"" + label + "\" — was it renamed? "
            + "The backfill matches existing itineraries on this exact text.");
    }

    /** Each entry: {label, scope-or-null}. */
    private List<String[]> seeds() {
        try {
            String source = Files.readString(SEEDER);
            Matcher m = SEED.matcher(source);
            List<String[]> found = new ArrayList<>();
            while (m.find()) {
                String scope = m.group(2);
                found.add(new String[] {
                    m.group(1),
                    "null".equals(scope) ? null : scope.substring(1, scope.length() - 1)
                });
            }
            assertEquals(14, found.size(),
                "Expected the fourteen standard lines 58 itineraries already carried");
            return found;
        } catch (IOException e) {
            throw new AssertionError("Could not read " + SEEDER, e);
        }
    }
}
