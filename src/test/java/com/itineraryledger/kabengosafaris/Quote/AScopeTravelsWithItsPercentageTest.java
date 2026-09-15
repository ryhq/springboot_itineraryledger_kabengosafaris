package com.itineraryledger.kabengosafaris.Quote;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Wherever the tax's scope is carried, the discount's must be carried too.
 *
 * <p>A real quote showed "Discount 935.55" beside the words "off every line". The figure was
 * right, scoped to accommodation and activities exactly as asked. The sentence was wrong, because
 * the full-quote mapper set taxAppliesTo and never set discountAppliesTo, so the label fell back
 * to its "no scope means everything" default. That contradiction was one PDF away from a customer.
 *
 * <p>The two fields were added together and are read together by the documents, so a mapper that
 * knows about one and not the other is always a mistake. Cheap to check, and the failure it
 * catches is the kind nobody spots in review: the number is right, only the explanation lies.
 */
class AScopeTravelsWithItsPercentageTest {

    private static final Path JAVA = Paths.get("src/main/java");

    private static final Pattern CARRIES_TAX_SCOPE = Pattern.compile(
        "(?:set|\\.)TaxAppliesTo\\s*\\(|\\.taxAppliesTo\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern CARRIES_DISCOUNT_SCOPE = Pattern.compile(
        "(?:set|\\.)DiscountAppliesTo\\s*\\(|\\.discountAppliesTo\\s*\\(", Pattern.CASE_INSENSITIVE);

    @Test
    @DisplayName("anything that carries the tax scope carries the discount scope too")
    void neitherScopeTravelsAlone() throws IOException {
        List<String> offences = new ArrayList<>();

        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).sorted().toList()) {
                String source = Files.readString(file);
                Matcher tax = CARRIES_TAX_SCOPE.matcher(source);
                if (!tax.find()) continue;
                /* the DTOs and the entity declare the fields; it is the MAPPERS that must pair them */
                if (file.toString().contains("/DTOs/") || file.toString().contains("/Entity/")) continue;
                if (!CARRIES_DISCOUNT_SCOPE.matcher(source).find()) {
                    offences.add(file.getFileName().toString());
                }
            }
        }

        assertTrue(offences.isEmpty(), () -> String.format("""
            %s carries the tax scope but not the discount scope.

            The document prints both beside their percentages. Carrying one and not the other is
            how a quote came to show a correctly scoped discount under the words "off every line":
            the figure right, the sentence wrong, and only the customer to notice.
            """, String.join(", ", offences)));
    }
}
