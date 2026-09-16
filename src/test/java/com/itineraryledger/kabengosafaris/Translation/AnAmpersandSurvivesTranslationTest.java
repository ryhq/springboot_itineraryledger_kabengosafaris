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
 * Markup is not prose, and a translation engine cannot tell the difference.
 *
 * <p>Found in a real German quote, in print. Thymeleaf escapes on the way into the HTML, so
 * "All park, conservation &amp; crater-service fees" reaches the translator as
 * "conservation &amp;amp; crater-service fees" — and the engine obligingly translated the entity
 * too. The customer's PDF said:
 *
 * <pre>Alle Park, Erhaltung &amp; amp; Kraterservice Gebühren</pre>
 *
 * <p>Same shape as the number defect this service already guards against, and the same answer:
 * hide what must not change, translate the rest, put it back.
 */
class AnAmpersandSurvivesTranslationTest {

    @Test
    @DisplayName("an escaped ampersand is hidden from the engine and comes back whole")
    void theAmpersandThatReachedACustomer() {
        String html = "<li>All park, conservation &amp; crater-service fees</li>";

        Map<String, String> entities = new LinkedHashMap<>();
        String masked = TranslationService.maskEntities(html, entities);

        assertFalse(masked.contains("&amp;"),
            "the engine must never see an entity, or it translates it");
        assertEquals(1, entities.size());

        /* What a translator does to the masked text: rewrites the words, keeps the marker. */
        String translated = masked
            .replace("All park, conservation", "Alle Park, Erhaltung")
            .replace("crater-service fees", "Kraterservice Gebühren");

        assertEquals("<li>Alle Park, Erhaltung &amp; Kraterservice Gebühren</li>",
            TranslationService.restoreEntities(translated, entities));
    }

    @Test
    @DisplayName("every entity shape is hidden, including the ones carrying digits")
    void allEntityShapes() {
        String html = "A &amp; B &lt;C&gt; &quot;D&quot; &#39;E&#39; &nbsp; &#8226; &#x27;F&#x27;";

        Map<String, String> entities = new LinkedHashMap<>();
        String masked = TranslationService.maskEntities(html, entities);

        assertEquals(11, entities.size(), "every entity in that line must be hidden");
        assertFalse(masked.contains("&"), () -> "an entity survived masking: " + masked);
        assertEquals(html, TranslationService.restoreEntities(masked, entities));
    }

    @Test
    @DisplayName("entities are hidden before numbers, or a numeric entity is torn in half")
    void entitiesAreMaskedBeforeNumbers() {
        /*
         * The ordering trap. &#39; and &#160; carry digits, so masking numbers first would replace
         * the 39 with a number marker and leave "&#" and ";" behind as loose text for the engine
         * to translate into something that is no longer an entity at all.
         */
        String html = "Peter&#39;s trip, 5 nights, &#160; 2 guests";

        Map<String, String> entities = new LinkedHashMap<>();
        String withoutEntities = TranslationService.maskEntities(html, entities);

        Map<String, String> numbers = new LinkedHashMap<>();
        String masked = TranslationService.maskNumbers(withoutEntities, numbers);

        assertEquals(2, entities.size());
        assertEquals(2, numbers.size(), "only the real numbers, not the digits inside an entity");
        assertTrue(numbers.containsValue("5") && numbers.containsValue("2"));
        assertFalse(numbers.containsValue("39"), "39 belongs to &#39; and is not a number");
        assertFalse(numbers.containsValue("160"), "160 belongs to &#160; and is not a number");

        String restored = TranslationService.restoreEntities(
            TranslationService.restoreNumbers(html, masked, numbers), entities);
        assertEquals(html, restored);
    }

    @Test
    @DisplayName("a marker the engine loses leaves the entity alone rather than mangling it")
    void aLostMarkerIsNotAMangledEntity() {
        Map<String, String> entities = new LinkedHashMap<>();
        TranslationService.maskEntities("Tips &amp; gratuities", entities);

        /* The engine dropped the marker entirely. The words are still better than nothing. */
        assertEquals("Tipps und Trinkgelder",
            TranslationService.restoreEntities("Tipps und Trinkgelder", entities));
    }
}
