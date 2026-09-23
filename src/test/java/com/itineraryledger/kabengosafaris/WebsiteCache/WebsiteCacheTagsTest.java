package com.itineraryledger.kabengosafaris.WebsiteCache;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * That the labels this API clears are labels the website actually puts on something.
 *
 * <p>This feature spans three repositories that cannot import each other: the tag catalogue here,
 * CACHE_TAGS in each website's server-api.ts, and KNOWN_TAGS in each website's revalidate route. A
 * name that exists in only one of them clears nothing and says it succeeded, which is the worst
 * answer a cache can give — the page then looks current and is not. These tests pin the half that
 * lives in Java; the websites' own halves are pinned by their type checkers.
 */
class WebsiteCacheTagsTest {

    @Test
    @DisplayName("every tag a write can produce is one a caller may also ask for by hand")
    void everyPathTagIsInTheCatalogue() {
        List<String> paths = List.of(
            "/api/itineraries", "/api/parks", "/api/activities", "/api/accommodations",
            "/api/heroes", "/api/testimonies", "/api/blogs", "/api/faqs", "/api/company",
            "/api/park-activities", "/api/park-images", "/api/hero-images");
        for (String path : paths) {
            Set<String> tags = WebsiteCacheTags.forPath(path);
            assertFalse(tags.isEmpty(), path + " is public content but maps to no tag");
            for (String tag : tags) {
                assertTrue(WebsiteCacheTags.CATALOGUE.contains(tag),
                    path + " produces \"" + tag + "\", which the catalogue does not list — the "
                        + "website would be asked to clear a label nothing wears");
            }
        }
    }

    @Test
    @DisplayName("the longest matching prefix wins, so park-activities is not read as parks")
    void longestPrefixWins() {
        assertEquals(Set.of(WebsiteCacheTags.ACTIVITIES, WebsiteCacheTags.PARKS),
            WebsiteCacheTags.forPath("/api/park-activities/abc123"));
        assertEquals(Set.of(WebsiteCacheTags.PARKS, WebsiteCacheTags.SAFARIS),
            WebsiteCacheTags.forPath("/api/parks/abc123"));
    }

    @Test
    @DisplayName("a prefix only matches a whole segment")
    void prefixDoesNotMatchHalfASegment() {
        // "/api/park" must not be satisfied by "/api/parks", and vice versa.
        assertTrue(WebsiteCacheTags.forPath("/api/parkings").isEmpty());
        assertTrue(WebsiteCacheTags.forPath("/api/companies").isEmpty());
    }

    @Test
    @DisplayName("a private resource clears nothing")
    void privateResourcesClearNothing() {
        for (String path : List.of("/api/quotes", "/api/invoices/abc", "/api/users",
                                   "/api/customers/abc/notes", "/api/safaris", "/api/payments")) {
            assertTrue(WebsiteCacheTags.forPath(path).isEmpty(),
                path + " is not on the public website, so clearing anything for it is waste");
        }
    }

    @Test
    @DisplayName("an unknown label is refused rather than accepted and dropped")
    void unknownTagIsRefused() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> WebsiteCacheTags.validate(List.of("safaris", "lodges")));
        assertTrue(e.getMessage().contains("lodges"));
    }

    @Test
    @DisplayName("nothing asked for means everything, and everything absorbs the rest")
    void emptyMeansAllAndAllAbsorbs() {
        assertEquals(Set.of(WebsiteCacheTags.ALL), WebsiteCacheTags.validate(null));
        assertEquals(Set.of(WebsiteCacheTags.ALL), WebsiteCacheTags.validate(List.of()));
        assertEquals(Set.of(WebsiteCacheTags.ALL),
            WebsiteCacheTags.validate(List.of("parks", "site", "blog")));
    }

    @Test
    @DisplayName("the catalogue is what the panel offers, so it must not carry duplicates")
    void catalogueIsDistinct() {
        assertEquals(WebsiteCacheTags.CATALOGUE.size(),
            Set.copyOf(WebsiteCacheTags.CATALOGUE).size());
    }
}
