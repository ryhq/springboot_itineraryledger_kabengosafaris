package com.itineraryledger.kabengosafaris.Feature;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * That a feature switch actually switches something.
 *
 * The failure this guards against is quiet: a path typed one character wrong, or a module renamed, and
 * the flag still appears in the panel, still saves, still reads "off" — while every endpoint it was
 * meant to close stays open. Nobody finds out until a company sees a module it does not have.
 */
class FeatureCatalogueTest {

    private static final Path JAVA = Paths.get("src/main/java/com/itineraryledger/kabengosafaris");

    @Test
    @DisplayName("every path a feature claims belongs to a real controller")
    void noFeatureGatesNothing() throws IOException {
        Set<String> mapped = mappedPaths();
        List<String> dead = new ArrayList<>();

        for (Feature feature : Feature.values()) {
            for (String path : feature.getPaths()) {
                /* a wildcard path is checked on its fixed prefix: /api/safaris/x/y -> /api/safaris */
                String fixed = path.contains("*") ? path.substring(0, path.indexOf("/*")) : path;
                boolean exists = mapped.stream().anyMatch(m -> m.equals(fixed)
                    || m.startsWith(fixed + "/") || fixed.startsWith(m + "/"));
                if (!exists) dead.add(feature.getKey() + "  ->  " + path);
            }
        }

        assertTrue(dead.isEmpty(), () -> """
            %d feature path(s) match no controller, so the switch would close nothing:

            %s

            If a module was renamed, the flag has to be renamed with it — otherwise the switch reads
            as off while every endpoint stays open.
            """.formatted(dead.size(), String.join("\n", dead)));
    }

    @Test
    @DisplayName("no two features claim the same path")
    void noOverlap() {
        Map<String, String> owners = new HashMap<>();
        List<String> clashes = new ArrayList<>();

        for (Feature feature : Feature.values()) {
            for (String path : feature.getPaths()) {
                String existing = owners.putIfAbsent(path, feature.getKey());
                if (existing != null) {
                    clashes.add(path + " claimed by both " + existing + " and " + feature.getKey());
                }
            }
        }

        assertTrue(clashes.isEmpty(), () -> "two features cannot own one path — whichever is checked "
            + "first would decide:\n" + String.join("\n", clashes));
    }

    @Test
    @DisplayName("the gate matches a feature's own paths and nothing else")
    void matchingIsExact() {
        FeatureService service = service(new MockEnvironment());

        assertEquals(Feature.FLEET, service.featureFor("/api/vehicles"));
        assertEquals(Feature.FLEET, service.featureFor("/api/vehicles/6vVWmOe"));
        assertEquals(Feature.FLEET, service.featureFor("/api/drivers/6vVWmOe/deactivate"));
        assertEquals(Feature.CREDIT_NOTES, service.featureFor("/api/credit-notes/6vVWmOe/line-items"));
        assertEquals(Feature.AVAILABILITY_REQUESTS, service.featureFor("/api/availability-requests"));
        assertEquals(Feature.AVAILABILITY_REQUESTS,
            service.featureFor("/api/safaris/6vVWmOe/availability-requests"),
            "the nested route is the one somebody forgets");
        assertEquals(Feature.AVAILABILITY_REQUESTS,
            service.featureFor("/api/safaris/6vVWmOe/availability-request-coverage"));
        assertEquals(Feature.WEBSITE_CONTENT, service.featureFor("/api/blogs/some-slug"));
        assertEquals(Feature.TRANSLATION, service.featureFor("/api/translation/detect"));

        /* the rest of the product must never be caught by a feature's net */
        for (String untouched : List.of("/api/customers", "/api/quotes", "/api/invoices", "/api/safaris",
            "/api/safaris/6vVWmOe", "/api/itineraries", "/api/accommodations", "/api/parks", "/api/users",
            "/api/company", "/api/dashboard", "/api/expenses", "/api/vendors", "/api/bank-accounts")) {
            assertNull(service.featureFor(untouched), untouched + " is not part of any feature");
        }
    }

    @Test
    @DisplayName("a property beats the stored row, and says so")
    void propertyWins() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("app.features.translation", "false");
        FeatureService service = service(environment);

        assertFalse(service.isEnabled(Feature.TRANSLATION), "the deployment said no");
        assertTrue(service.isEnabled(Feature.FLEET), "everything else keeps its default");

        Map<String, Object> translation = service.view().stream()
            .filter(row -> "translation".equals(row.get("key"))).findFirst().orElseThrow();
        assertEquals(Boolean.TRUE, translation.get("fixedByDeployment"),
            "the panel has to be able to say the switch is not its to flip");
    }

    @Test
    @DisplayName("a database that cannot answer leaves the product intact")
    void brokenDatabaseDefaultsOn() {
        FeatureSettingRepository broken = mock(FeatureSettingRepository.class);
        when(broken.findAll()).thenThrow(new RuntimeException("no connection"));

        FeatureService service = new FeatureService(broken, new MockEnvironment());

        for (Feature feature : Feature.values()) {
            assertTrue(service.isEnabled(feature), feature.getKey() + " must stay on when the read fails");
        }
    }

    // ------------------------------------------------------------------ helpers

    private FeatureService service(MockEnvironment environment) {
        FeatureSettingRepository repository = mock(FeatureSettingRepository.class);
        when(repository.findAll()).thenReturn(List.of());
        return new FeatureService(repository, environment);
    }

    /**
     * Every path a controller answers, read from the source.
     *
     * Asking Spring would need a running context and a database, which CI does not have; a regex over
     * the controllers finds the same mappings in a second.
     */
    private Set<String> mappedPaths() throws IOException {
        Pattern classLevel = Pattern.compile("@RequestMapping\\(\"(/api/[a-z0-9-]+)\"\\)");
        Pattern methodLevel = Pattern.compile("@(?:Get|Post|Put|Patch|Delete)Mapping\\(\"(/api/[a-z0-9-]+)");
        Set<String> paths = new TreeSet<>();

        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path file : files.filter(f -> f.toString().endsWith("Controller.java")).toList()) {
                String source = Files.readString(file);
                for (Pattern pattern : List.of(classLevel, methodLevel)) {
                    Matcher m = pattern.matcher(source);
                    while (m.find()) paths.add(m.group(1));
                }
            }
        }

        assertFalse(paths.isEmpty(), "found no controller mappings — this guard would check nothing");
        return paths;
    }
}
