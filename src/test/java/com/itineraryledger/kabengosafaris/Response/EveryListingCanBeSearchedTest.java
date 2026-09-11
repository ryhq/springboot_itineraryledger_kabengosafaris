package com.itineraryledger.kabengosafaris.Response;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A search box that does nothing is worse than no search box.
 *
 * <p>The PDF templates page had one. Typing anything -- a document name, a word, or nonsense --
 * returned all sixteen rows, because the module never declared a {@code keyword} parameter and
 * Spring quietly drops a query parameter no method asks for. It does not read as "this filter is
 * not supported". It reads as "sixteen matches", which is a wrong answer rather than a missing
 * feature.
 *
 * <p>The house rule (CLAUDE.md) is that the free-text parameter is {@code keyword}, and the panel
 * sends exactly that from every list page's search box. So every paged listing has to accept it.
 *
 * <p>{@link #WITHOUT_SEARCH_TODAY} is the backlog: endpoints that predate the rule. It is an
 * inventory, not permission -- the test fails if a new one appears AND if one is fixed without
 * being struck off, so the list can only ever get shorter.
 */
class EveryListingCanBeSearchedTest {

    private static final Path JAVA = Paths.get("src/main/java");

    /**
     * Paged listings that still ignore a keyword.
     *
     * <p>All of them serve the public website, which has no search box of its own -- it asks for a
     * park's images or a page of blog posts, never for a phrase. The panel's listings were worked
     * through and are gone from this list; if the website ever grows a search, these come with it.
     */
    private static final Set<String> WITHOUT_SEARCH_TODAY = new TreeSet<>(Set.of(
        "PublicController (\"/parks/{identifier}/images\")",
        "PublicController (\"/parks/{identifier}/activities\")",
        "PublicController (\"/parks/{identifier}/safaris\")",
        "PublicController (\"/activities/{identifier}/images\")",
        "PublicController (\"/activities/{identifier}/parks\")",
        "PublicController (\"/activities/{identifier}/safaris\")",
        "PublicController (\"/accommodations/{identifier}/images\")",
        "PublicController (\"/accommodations/{identifier}/safaris\")",
        "PublicController (\"/gallery\")",
        "PublicController (\"/testimonies\")",
        "PublicController (\"/blogs\")"
    ));

    /** A paged GET: it takes both page and size, so a person can be looking through a list. */
    private static final Pattern LIST_ENDPOINT = Pattern.compile(
        "@GetMapping([^\n]*)\n((?:\\s*@[A-Za-z][^\n]*\n)*)\\s*public\\s+[^(]*\\((.*?)\\)\\s*\\{",
        Pattern.DOTALL);

    /**
     * `@ModelAttribute QuoteFilter filter` carries its own params, and the annotation is spelled
     * fully qualified in places -- which is how this first read PDF documents and season periods
     * as broken when they were not.
     */
    private static final Pattern FILTER_OBJECT = Pattern.compile(
        "@(?:[\\w.]+\\.)?ModelAttribute\\s+([\\w.]+)\\s+\\w+");

    @Test
    @DisplayName("no new paged listing ships with a search box the API ignores")
    void everyPagedListingTakesAKeyword() throws IOException {
        Set<String> found = new LinkedHashSet<>(withoutKeyword());

        List<String> appeared = found.stream().filter(e -> !WITHOUT_SEARCH_TODAY.contains(e)).toList();
        List<String> fixed = WITHOUT_SEARCH_TODAY.stream().filter(e -> !found.contains(e)).toList();

        assertTrue(appeared.isEmpty(), () -> """
            %d paged listing(s) take page and size but no `keyword`, so the panel's search box
            sends a parameter nothing reads and the page answers with every row:

            %s

            Add `@RequestParam(required = false) String keyword` (or a field on the @ModelAttribute
            filter), pass it to the get service, and AND a specification that searches the columns
            the listing actually shows -- joining what the table displays, not only the name.
            """.formatted(appeared.size(), String.join("\n", appeared)));

        assertTrue(fixed.isEmpty(), () -> """
            %s now accept(s) a keyword. Strike them off WITHOUT_SEARCH_TODAY in this test: the
            list is an inventory of what is still broken, and one that lists working endpoints
            stops being read.
            """.formatted(String.join(", ", fixed)));
    }

    private static List<String> withoutKeyword() throws IOException {
        Map<String, String> filterSources = filterSources();
        List<String> offences = new ArrayList<>();

        for (Path file : controllers()) {
            String source = Files.readString(file);
            String controller = file.getFileName().toString().replace(".java", "");

            Matcher endpoint = LIST_ENDPOINT.matcher(source);
            while (endpoint.find()) {
                String params = endpoint.group(3);
                if (!params.contains("page") || !params.contains("size")) continue;

                StringBuilder searchable = new StringBuilder(params);
                Matcher filter = FILTER_OBJECT.matcher(params);
                while (filter.find()) {
                    String simple = filter.group(1).substring(filter.group(1).lastIndexOf('.') + 1);
                    searchable.append(filterSources.getOrDefault(simple, ""));
                }

                if (!searchable.toString().contains("keyword")) {
                    String mapping = endpoint.group(1).trim();
                    offences.add(controller + " " + (mapping.isEmpty() ? "(root)" : mapping));
                }
            }
        }
        return offences;
    }

    /** Every class that could be a filter bound with @ModelAttribute, by simple name. */
    private static Map<String, String> filterSources() throws IOException {
        Map<String, String> sources = new HashMap<>();
        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String name = file.getFileName().toString().replace(".java", "");
                if (name.endsWith("Filter") || name.endsWith("Criteria") || name.endsWith("Query")) {
                    sources.put(name, Files.readString(file));
                }
            }
        }
        return sources;
    }

    private static List<Path> controllers() throws IOException {
        try (Stream<Path> files = Files.walk(JAVA)) {
            return files.filter(f -> f.toString().endsWith("Controller.java")).sorted().toList();
        }
    }
}
