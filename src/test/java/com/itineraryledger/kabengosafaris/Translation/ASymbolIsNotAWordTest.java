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
 * A symbol is not a word, and an engine asked to translate one will answer with a word.
 *
 * <p>Found in a real French quote, on its way to a customer in France. The English read:
 *
 * <pre>
 * #: QT-1040-0926-1
 * Discount (38.76%) off activity only        -$526.55
 * </pre>
 *
 * <p>The French came back:
 *
 * <pre>
 * - Oui QT-1040-0926-1
 * Remise (38.76Pourcentage hors activite seulement   - Oui$526.55
 * </pre>
 *
 * <p>Two separate faults. The percent sign was left outside the number mask, so the engine saw a
 * bare symbol and wrote it out as the word "Pourcentage" — and dropped the closing bracket with it.
 * And a cell holding nothing but "#:" was sent as prose, so the engine, having no answer, invented
 * one: "Oui". Yes.
 *
 * <p>The same shape as the number and ampersand defects this service already guards against, and
 * the same answer: a quantity travels with its symbol, and text with no letter in it is not text.
 */
class ASymbolIsNotAWordTest {

    @Test
    @DisplayName("a percent sign travels inside the number marker, not beside it")
    void percentIsMaskedWithItsNumber() {
        Map<String, String> numbers = new LinkedHashMap<>();
        String masked = TranslationService.maskNumbers("Discount (38.76%) off activity only", numbers);

        assertFalse(masked.contains("%"), "the engine must never see a bare percent sign: " + masked);
        assertFalse(masked.contains("38.76"), "the figure must be masked too: " + masked);
        assertTrue(numbers.containsValue("38.76%"), "the marker must carry the symbol: " + numbers);
    }

    @Test
    @DisplayName("a currency sign travels inside the number marker too")
    void currencyIsMaskedWithItsNumber() {
        Map<String, String> numbers = new LinkedHashMap<>();
        String masked = TranslationService.maskNumbers("Total $526.55 due", numbers);

        assertFalse(masked.contains("$"), "the engine must never see a bare currency sign: " + masked);
        assertTrue(numbers.containsValue("$526.55"), "the marker must carry the symbol: " + numbers);
    }

    @Test
    @DisplayName("the amount comes back exactly as it left")
    void theAmountSurvivesTheRoundTrip() {
        Map<String, String> numbers = new LinkedHashMap<>();
        String original = "Discount (38.76%) off activity only";
        String masked = TranslationService.maskNumbers(original, numbers);

        // Stand in for the engine: it translates the prose and leaves the marker alone.
        String translated = masked.replace("Discount", "Remise").replace("off activity only", "hors activite seulement");

        String restored = TranslationService.restoreNumbers(original, translated, numbers);

        assertTrue(restored.contains("38.76%"), "the rate must read 38.76%, not 38.76Pourcentage: " + restored);
        assertEquals("Remise (38.76%) hors activite seulement", restored);
    }

    @Test
    @DisplayName("a marker the engine ate gives the original back, not a wrong price")
    void alostMarkerFallsBackToTheOriginal() {
        Map<String, String> numbers = new LinkedHashMap<>();
        String original = "Discount (38.76%) off activity only";
        TranslationService.maskNumbers(original, numbers);

        String restored = TranslationService.restoreNumbers(original, "Remise hors activite seulement", numbers);

        assertEquals(original, restored, "a document whose purpose is figures may not lose one");
    }
}
