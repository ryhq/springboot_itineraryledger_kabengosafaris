package com.itineraryledger.kabengosafaris.Storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where does a company's file actually go?
 *
 * <p>Expense documents answered that with a default nobody had written down. The key was in no
 * properties file at all, so it fell back to the {@code @Value} default baked into the service --
 * a release-relative {@code ./data/expense-documents/} that resolves against the working directory
 * rather than the data root, and the first bill document anyone attached failed with "Failed to
 * initialize expense document storage directory": no path, no cause. Five more families were in
 * the same state.
 *
 * <p>So the rule is: a storage path is declared in application.properties, and it hangs off
 * {@code ${app.data.dir}} -- the one root ISOLATION.md gives each company and the readiness check
 * writes a probe file into on every boot. A deploy cannot go green with a data directory the
 * service cannot write, which is exactly the failure this catches.
 */
class StoragePathsAreDeclaredTest {

    private static final Path MAIN_JAVA = Paths.get("src/main/java");
    private static final Path CANONICAL = Paths.get("src/main/resources/application.properties");
    private static final Path PRODUCTION = Paths.get("src/main/resources/application-production.properties");

    /** {@code ${some.key.storage.path:default}} — the placeholder form the storage services use. */
    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\$\\{([a-zA-Z0-9._-]+\\.storage(?:\\.base)?\\.path)[:}]");

    /**
     * Families whose files live in the release-relative tree ({@code /opt/<company>/data}) instead
     * of {@code ${app.data.dir}}, because they were built that way and have live files in it --
     * Kabengo serves a blog cover from there. Moving them is a file move on two droplets, not a
     * properties edit, so the pin stays until that happens and this list records it.
     *
     * <p>A NEW family must not be added here. It has no legacy tree to strand, so it belongs in
     * the data root, and pinning it is how expense documents broke.
     */
    private static final Set<String> LEGACY_PINS = new LinkedHashSet<>(List.of(
            "email.signature.storage.path",
            "email.template.storage.path",
            "email.storage.base.path",
            "pdf.template.storage.path",
            "accommodation.image.storage.path",
            "accommodation.document.storage.path",
            "park.image.storage.path",
            "park.document.storage.path",
            "activity.image.storage.path",
            "activity.document.storage.path",
            "park-activity.image.storage.path",
            "park-activity.document.storage.path",
            "itinerary.document.storage.path",
            "quote.document.storage.path",
            "safari.document.storage.path",
            "blog.image.storage.path",
            "hero.image.storage.path",
            "testimony.image.storage.path",
            "itinerary.image.storage.path",
            "invoice.document.storage.path",
            /* the backup archive has its own root and its own env var, and a restore reads it */
            "backup.storage.path"
    ));

    @Test
    @DisplayName("every storage path a service reads is declared in application.properties")
    void everyStoragePathIsDeclared() throws IOException {
        Properties canonical = load(CANONICAL);
        Map<String, String> undeclared = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> used : keysUsedInCode().entrySet()) {
            if (!canonical.containsKey(used.getKey())) {
                undeclared.put(used.getKey(), String.join(", ", used.getValue()));
            }
        }

        assertTrue(undeclared.isEmpty(),
                "these storage paths exist only as @Value defaults, so nothing says where a "
                        + "company's files are and the first upload finds out the hard way. Declare "
                        + "each in application.properties as ${app.data.dir}/<folder>/:\n"
                        + render(undeclared));
    }

    @Test
    @DisplayName("declared storage paths hang off ${app.data.dir}")
    void declaredPathsUseTheDataRoot() throws IOException {
        Properties canonical = load(CANONICAL);
        Map<String, String> wrong = new LinkedHashMap<>();

        for (String key : canonical.stringPropertyNames()) {
            if (!key.matches(".*\\.storage(\\.base)?\\.path")) continue;
            String value = canonical.getProperty(key);
            if (!value.contains("${app.data.dir}")) wrong.put(key, value);
        }

        assertTrue(wrong.isEmpty(),
                "a storage path that does not resolve against ${app.data.dir} is outside the root "
                        + "ISOLATION.md gives the company and outside what readiness proves writable:\n"
                        + render(wrong));
    }

    @Test
    @DisplayName("the production profile pins only the families that already have files there")
    void productionPinsOnlyLegacyFamilies() throws IOException {
        Properties production = load(PRODUCTION);
        List<String> unexpected = new ArrayList<>();

        for (String key : production.stringPropertyNames()) {
            if (!key.matches(".*\\.storage(\\.base)?\\.path")) continue;
            if (!LEGACY_PINS.contains(key)) unexpected.add(key + "=" + production.getProperty(key));
        }

        assertTrue(unexpected.isEmpty(),
                "this pins a storage path to a release-relative ./data in production, which is not "
                        + "the data root and not what the readiness probe tests. A new family has no "
                        + "files to strand, so it does not need pinning:\n  "
                        + String.join("\n  ", unexpected));
    }

    @Test
    @DisplayName("expense documents live in the data root, in every profile")
    void expenseDocumentsResolveToTheDataRoot() throws IOException {
        assertEquals("${app.data.dir}/expense-documents/",
                load(CANONICAL).getProperty("expense.document.storage.path"),
                "the bill document upload failed because this key was missing entirely");

        assertTrue(load(PRODUCTION).getProperty("expense.document.storage.path") == null,
                "pinning this in production puts bill documents back in the working directory, "
                        + "which is where 'Failed to initialize expense document storage directory' "
                        + "came from");
    }

    /** key -> the files that read it, so a failure names something to open. */
    private Map<String, List<String>> keysUsedInCode() throws IOException {
        Map<String, List<String>> found = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = PLACEHOLDER.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (m.find()) {
                    found.computeIfAbsent(m.group(1), k -> new ArrayList<>())
                            .add(file.getFileName().toString());
                }
            }
        }
        return found;
    }

    private Properties load(Path path) throws IOException {
        Properties properties = new Properties();
        try (var in = Files.newInputStream(path)) {
            properties.load(in);
        }
        return properties;
    }

    private String render(Map<String, String> entries) {
        return entries.entrySet().stream()
                .map(e -> "  " + e.getKey() + "  (" + e.getValue() + ")")
                .reduce((a, b) -> a + "\n" + b).orElse("");
    }
}
