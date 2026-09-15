package com.itineraryledger.kabengosafaris.Translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;

/**
 * A translation may change the words. It may never change the money.
 *
 * <p>A German quote for a family of four came back reading "Steuersatz ()18.00 Uhr% nur in der
 * unterkunft". The engine had read the tax rate 18.00 as a clock time and written "18.00 Uhr",
 * half past six in the evening. The same document turned the grand total 14,203.49 into 14.203.49.
 *
 * <p>Comparing the digits before and after does not catch the first one, because 18.00 and
 * "18.00 Uhr" hold identical digits; the inserted WORD is the damage. So the engine is never shown
 * a number at all: figures are replaced by markers, translated around, and put back.
 */
class ATranslationNeverChangesAPriceTest {

    @Test
    @DisplayName("the engine is handed no digits to misread")
    void nothingNumericIsSent() {
        Map<String, String> numbers = new LinkedHashMap<>();
        String masked = TranslationService.maskNumbers(
            "Tax (18.00%) on accommodation only, total 14,203.49", numbers);

        assertFalse(masked.matches(".*\\d.*"), "a digit survived into the request: " + masked);
        assertTrue(numbers.containsValue("18.00"), numbers.toString());
        assertTrue(numbers.containsValue("14,203.49"), numbers.toString());
    }

    @Test
    @DisplayName("18.00 cannot come back as half past six, because it was never sent as a number")
    void theTaxRateIsNotATimeOfDay() {
        Map<String, String> numbers = new LinkedHashMap<>();
        String english = "Tax (18.00%) on accommodation only";
        String masked = TranslationService.maskNumbers(english, numbers);

        // what a faithful engine does with markers it cannot interpret
        String germanWithMarkers = masked.replace("Tax", "Steuer")
            .replace("on accommodation only", "nur auf Unterkunft");
        String out = TranslationService.restoreNumbers(english, germanWithMarkers, numbers);

        assertEquals("Steuer (18.00%) nur auf Unterkunft", out);
    }

    @Test
    @DisplayName("an engine that loses a number gets nothing: the original stands")
    void aSwallowedNumberFallsBack() {
        Map<String, String> numbers = new LinkedHashMap<>();
        String english = "GRAND TOTAL $14,203.49";
        TranslationService.maskNumbers(english, numbers);

        assertEquals(english,
            TranslationService.restoreNumbers(english, "GESAMTSUMME $", numbers),
            "a total that lost its figure must never reach a customer");
    }

    @Test
    @DisplayName("separators cannot drift, because the engine never touched them")
    void theSeparatorsCannotMove() {
        Map<String, String> numbers = new LinkedHashMap<>();
        String english = "Total 2,453.10";
        String masked = TranslationService.maskNumbers(english, numbers);

        assertEquals("Gesamt 2,453.10",
            TranslationService.restoreNumbers(english, masked.replace("Total", "Gesamt"), numbers));
    }

    @Test
    @DisplayName("prose with no figures passes through untouched")
    void proseIsLeftAlone() {
        Map<String, String> numbers = new LinkedHashMap<>();
        String english = "Your safari begins at dawn";

        assertEquals(english, TranslationService.maskNumbers(english, numbers));
        assertTrue(numbers.isEmpty());
        assertEquals("Ihre Safari beginnt im Morgengrauen",
            TranslationService.restoreNumbers(english, "Ihre Safari beginnt im Morgengrauen", numbers));
    }

    @Test
    @DisplayName("several figures in one sentence each come back in their own place")
    void manyNumbersInOneLine() {
        Map<String, String> numbers = new LinkedHashMap<>();
        String english = "3 x 663.00 and 1 x 464.10";
        String masked = TranslationService.maskNumbers(english, numbers);

        assertEquals("3 x 663.00 und 1 x 464.10",
            TranslationService.restoreNumbers(english, masked.replace("and", "und"), numbers));
    }
}
