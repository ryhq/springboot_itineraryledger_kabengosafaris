package com.itineraryledger.kabengosafaris.Text;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The character map exists in four repositories and has to say the same thing in all of them.
 *
 * <p>This API, the management panel and the two public websites each hold a copy of
 * {@code typography.json}, because none of them can import the others. That is the same
 * arrangement as {@code WebsiteCacheTags}, where a name had to exist in three places and a test
 * was the only thing holding them together. The failure mode is the same too, and quiet: somebody
 * adds a character here, the websites never hear about it, and the guard that was supposed to
 * catch it passes.
 *
 * <p>This pins the Java half. The frontend halves are pinned by each repository's own
 * {@code scripts/check-typography.mjs}, which reads its local copy and would fail on a file that
 * had drifted into a different shape.
 */
class TypographyMapIsOneDefinitionTest {

    private static final Path JSON = Path.of("src/main/resources/typography.json");

    private JsonNode read() throws IOException {
        return new ObjectMapper().readTree(Files.readString(JSON));
    }

    private Map<Character, String> charMap(JsonNode node) {
        Map<Character, String> map = new LinkedHashMap<>();
        node.fieldNames().forEachRemaining(key -> {
            assertEquals(1, key.length(),
                "every key is a single character, but the file has '" + key + "'");
            map.put(key.charAt(0), node.get(key).asText());
        });
        return map;
    }

    @Test
    @DisplayName("the Java map and the shipped file agree, character for character")
    void theTwoHalvesMatch() throws IOException {
        JsonNode root = read();

        assertEquals(charMap(root.get("fold")), Typography.FOLD,
            "Text/Typography.java and typography.json disagree about what folds. The file is what "
                + "the panel and both websites read, so a change made in only one of them is a "
                + "clean-up that works in the API and nowhere else.");

        assertEquals(charMap(root.get("foldInternal")), Typography.FOLD_INTERNAL,
            "the internal box-drawing map has drifted");

        assertEquals(charMap(root.get("manual")).keySet(), Typography.MANUAL.keySet(),
            "the characters needing a human decision have drifted. The guidance text may differ "
                + "in wording; the characters may not.");

        assertEquals(charMap(root.get("keep")).keySet(), Typography.KEEP.keySet(),
            "the keep list has drifted, and this is the half that loses meaning when it is wrong");
    }

    @Test
    @DisplayName("nothing is on two lists at once")
    void theListsDoNotOverlap() {
        for (Character c : Typography.FOLD.keySet()) {
            assertFalse(Typography.MANUAL.containsKey(c),
                "'" + c + "' is both folded automatically and flagged for a person. One of those "
                    + "is a lie to whoever reads the guard's output.");
            assertFalse(Typography.KEEP.containsKey(c),
                "'" + c + "' is both folded and kept");
        }
        for (Character c : Typography.MANUAL.keySet()) {
            assertFalse(Typography.KEEP.containsKey(c), "'" + c + "' is both flagged and kept");
        }
    }

    @Test
    @DisplayName("folding is settled after one pass")
    void noReplacementIntroducesAnotherKey() {
        for (Map.Entry<Character, String> entry : Typography.FOLD.entrySet()) {
            for (char c : entry.getValue().toCharArray()) {
                assertFalse(Typography.FOLD.containsKey(c),
                    "'" + entry.getKey() + "' becomes '" + entry.getValue() + "', which contains '"
                        + c + "', which folds again. Order would then matter and running the "
                        + "clean-up twice would not be the same as running it once.");
            }
        }
        String twice = Typography.fold(Typography.fold("A — B · C… “D”"));
        assertEquals("A - B | C... \"D\"", twice, "folding twice must equal folding once");
    }

    @Test
    @DisplayName("a letter with an accent is never touched, in any language")
    void diacriticsSurvive() {
        String names = "Sueños Luxury Camp, Château, München, François, "
            + "Größe, Ukraïne, Élysée";
        assertEquals(names, Typography.foldAll(names),
            "folding an accent is the one mistake here that changes a name rather than a style");

        String scripts = "日本語 中文 한국어 "
            + "العربية हिन्दी "
            + "Українська";
        assertEquals(scripts, Typography.foldAll(scripts),
            "the websites list language names natively and uk.json is entirely Cyrillic");

        for (char c : Typography.FOLD.keySet()) {
            assertFalse(Character.isLetter(c),
                "'" + c + "' is a letter and letters are content, not punctuation");
        }
        for (char c : Typography.FOLD_INTERNAL.keySet()) {
            assertFalse(Character.isLetter(c), "'" + c + "' is a letter");
        }
    }

    @Test
    @DisplayName("what the fold actually does to the strings we have")
    void realExamples() {
        assertEquals("Kabengo Safaris - ACCOMMODATION PLAN",
            Typography.fold("Kabengo Safaris — ACCOMMODATION PLAN"));
        assertEquals("-", Typography.fold("—"),
            "the standalone placeholder becomes a hyphen, not an empty cell");
        assertEquals("info@x.com | +255 000 000 000 | www.x.com",
            Typography.fold("info@x.com · +255 000 000 000 · www.x.com"),
            "the spacing comes from the text, so the footer keeps its gaps");
        assertEquals("1-4 Feb", Typography.fold("1–4 Feb"));
        assertEquals("Select...", Typography.fold("Select…"));
        assertEquals("don't", Typography.fold("don’t"));
        assertEquals("4x4", Typography.fold("4×4"));
        assertEquals("a b", Typography.fold("a b"), "a non-breaking space is still a space");
    }

    @Test
    @DisplayName("null and empty pass straight through")
    void nothingIsInvented() {
        assertEquals(null, Typography.fold(null),
            "a null description was never written and must not become an empty string");
        assertEquals("", Typography.fold(""));
        assertEquals("already clean", Typography.fold("already clean"));
    }

    @Test
    @DisplayName("the file says why, not only what")
    void theReasoningShips() throws IOException {
        JsonNode root = read();
        assertTrue(root.has("_"), "the explanation block is gone");
        assertTrue(root.has("keepRules"), "the keep rules are the half that cannot be a list of "
            + "characters, and they are gone");

        String rules = root.get("keepRules").toString();
        for (String mustMention : new TreeSet<>(java.util.List.of(
            "diacritic", "Cyrillic", "fr.json", "HtmlToText"))) {
            assertTrue(rules.contains(mustMention),
                "the keep rules no longer mention " + mustMention + ", which is one of the four "
                    + "ways this clean-up can destroy something");
        }
    }
}
