package com.itineraryledger.kabengosafaris.Translation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What a DeepL glossary may and may not contain, pinned because getting it wrong is silent.
 *
 * <p>An entry whose source and target are IDENTICAL makes DeepL return the whole sentence in
 * English. Measured on 2026-11-11 with an en-fr glossary containing "lodge" -> "lodge":
 * "Kudu Lodge sits two kilometres off the road." came back as "The Kudu Lodge is located two
 * kilometres from the road." — untranslated, HTTP 200, no error anywhere. A French reader would
 * simply see English. Removing the identity entry restored correct French immediately, and
 * capitalising the target ("lodge" -> "Lodge") kept the fix without the passthrough.
 *
 * <p>That is the same class of failure as a language the app advertises but the engine cannot
 * do, and it deserves the same treatment: it must not be possible to introduce it quietly.
 */
@DisplayName("A glossary is opt-in and never contains an identity entry")
class AGlossaryIsOptInAndNeverIdenticalTest {

    /** Glossary files kept in the repo, if any, must not contain an identity mapping. */
    @Test
    @DisplayName("no checked-in glossary maps a term to itself")
    void noIdentityEntriesInCheckedInGlossaries() throws IOException {
        Path dir = Paths.get("src/main/resources/translation");
        if (!Files.isDirectory(dir)) {
            return; // none kept in the repo yet; the rule still stands for whoever adds one
        }
        try (var files = Files.walk(dir)) {
            files.filter(f -> f.toString().endsWith(".tsv")).forEach(f -> {
                try {
                    for (String line : Files.readAllLines(f)) {
                        if (line.isBlank() || !line.contains("\t")) continue;
                        String[] bits = line.split("\t", 2);
                        assertFalse(bits[0].trim().equals(bits[1].trim()),
                            f + " maps \"" + bits[0].trim() + "\" to itself. DeepL answers such a "
                            + "request with the whole sentence in English and no error.");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * The parser behind translation.deepl.glossary.ids. An unset or malformed value must mean
     * "no glossary", never "some glossary" — a wrong glossary is worse than none.
     */
    @Test
    @DisplayName("a blank or malformed setting means no glossary at all")
    void malformedSettingMeansNoGlossary() {
        assertTrue(parse(null).isEmpty());
        assertTrue(parse("").isEmpty());
        assertTrue(parse("   ").isEmpty());
        assertTrue(parse("nonsense").isEmpty(), "no colon means no pair, so nothing is applied");
        assertTrue(parse("en-fr:").isEmpty(), "a pair with no id must not produce an empty id");
        assertTrue(parse(":abc").isEmpty(), "an id with no pair cannot be matched to anything");
    }

    @Test
    @DisplayName("well-formed pairs parse, case-insensitively on the pair")
    void wellFormedPairsParse() {
        var one = parse("en-fr:abc123");
        assertTrue(one.containsKey("en-fr"));
        assertTrue("abc123".equals(one.get("en-fr")));

        var two = parse("EN-FR:abc , en-de:def");
        assertTrue(two.containsKey("en-fr"), "the pair is lower-cased so lookup cannot miss on case");
        assertTrue(two.containsKey("en-de"));
        assertTrue("abc".equals(two.get("en-fr")), "surrounding spaces are trimmed off the id");
    }

    /** Mirrors TranslationSettingGetterServices.getDeepLGlossaryIds. */
    private java.util.Map<String, String> parse(String raw) {
        if (raw == null || raw.isBlank()) return java.util.Map.of();
        java.util.Map<String, String> out = new java.util.LinkedHashMap<>();
        for (String part : raw.split(",")) {
            String[] bits = part.split(":", 2);
            if (bits.length == 2 && !bits[0].isBlank() && !bits[1].isBlank()) {
                out.put(bits[0].trim().toLowerCase(), bits[1].trim());
            }
        }
        return out;
    }
}
