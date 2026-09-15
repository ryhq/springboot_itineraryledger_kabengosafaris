package com.itineraryledger.kabengosafaris.Translation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicItineraryDTO;

/**
 * A customer-facing list of sentences has to be translated, or the page is half in one language.
 *
 * <p>This is written from a live defect. {@code PublicItineraryDTO.inclusions} and
 * {@code exclusions} were never annotated, while {@code description}, {@code highlights} and the
 * two locations beside them were — so a German visitor to a safari page read a German heading,
 * "Jede Kabengo-Safari beinhaltet", over eight bullets still in English. The machinery was never
 * the problem: {@code PublicTranslationService} has handled a {@code @Translatable List<String>}
 * since the blog's bullet blocks. Two annotations were simply missing.
 *
 * <p>So the rule is checked rather than remembered: every {@code String} and {@code List<String>}
 * on this DTO is customer-facing prose unless it is named below as a code, an id or a URL.
 */
class TheGermanPageSaysItInGermanTest {

    /**
     * The fields that are not prose.
     *
     * <p>A code is a code in every language; a slug and a URL must not be rewritten or the link
     * breaks; a locale name is the thing being selected, not something to translate.
     */
    private static final Set<String> NOT_PROSE = Set.of(
        "id", "code", "slug", "identifier", "reference",
        "primaryImageUrl", "imageUrl", "url", "link", "href",
        "locale", "language", "currency", "currencyCode",
        "status", "tripType", "budgetCategory"
    );

    @Test
    @DisplayName("every line of prose the public itinerary sends is translatable")
    void everyProseFieldOnThePublicItineraryIsTranslatable() {
        List<String> missing = new ArrayList<>();

        for (Field field : PublicItineraryDTO.class.getDeclaredFields()) {
            if (field.isSynthetic() || java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            if (NOT_PROSE.contains(field.getName())) continue;
            if (field.getName().toLowerCase().endsWith("url")
                || field.getName().toLowerCase().endsWith("id")) continue;

            boolean prose = field.getType() == String.class || isListOfString(field);
            if (!prose) continue;
            if (field.isAnnotationPresent(Translatable.class)) continue;

            missing.add(field.getType().getSimpleName() + " " + field.getName());
        }

        assertTrue(missing.isEmpty(), () -> String.format("""
            %d customer-facing field(s) on PublicItineraryDTO are not @Translatable, so they reach a
            German, French or Swahili visitor in English while the headings around them are
            translated:

              %s

            Add @Translatable, or add the field to NOT_PROSE with a reason if it is a code, an id or
            a URL that must never be rewritten.
            """, missing.size(), String.join("\n  ", missing)));
    }

    @Test
    @DisplayName("the two that caused this stay annotated")
    void theInclusionListsStayAnnotated() throws NoSuchFieldException {
        /*
         * Named explicitly as well as caught by the sweep above, because these two are the reason
         * the sweep exists and a future NOT_PROSE entry could quietly exempt them.
         */
        for (String name : List.of("inclusions", "exclusions")) {
            Field field = PublicItineraryDTO.class.getDeclaredField(name);
            assertTrue(field.isAnnotationPresent(Translatable.class),
                name + " must be @Translatable — a German page showed German headings over English "
                + "bullets for months because it was not");
            assertTrue(isListOfString(field), name + " should still be a List<String> on the wire");
        }
    }

    private boolean isListOfString(Field field) {
        if (!List.class.isAssignableFrom(field.getType())) return false;
        if (!(field.getGenericType() instanceof ParameterizedType parameterized)) return false;
        var args = parameterized.getActualTypeArguments();
        return args.length == 1 && args[0] == String.class;
    }
}
