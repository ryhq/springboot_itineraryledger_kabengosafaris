package com.itineraryledger.kabengosafaris.Response;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A Specification that names a field the entity does not have compiles, then throws.
 *
 * <p>{@code root.get("originalFileName")} is a string. The compiler has no opinion on it, and
 * nothing complains until somebody types into a search box and the page answers 500. Accommodation
 * documents had exactly that, searching a field that entity never had; two more arrived the day
 * this was written, from copying a specification between two similar entities.
 *
 * <p>So the attribute names are checked against the entity's own fields here, at build time. It is
 * a source scan rather than a metamodel walk because the metamodel needs a database, and a check
 * that only runs where a database is is a check that does not run.
 */
class ASpecificationOnlyNamesFieldsThatExistTest {

    private static final Path JAVA = Paths.get("src/main/java");

    /** `Specification<Park>` — what this class filters. */
    private static final Pattern SUBJECT = Pattern.compile("Specification<(\\w+)>");

    /** `var park = root.join("park", …)` — an alias standing for another entity. */
    private static final Pattern JOIN = Pattern.compile(
        "(?:var|Join<[^>]*>)\\s+(\\w+)\\s*=\\s*root\\.join\\(\"(\\w+)\"");

    /** `root.get("name")`, `root.get("park").get("name")`, `park.get("name")`. */
    private static final Pattern ATTRIBUTE = Pattern.compile(
        "(\\broot|\\b[a-z]\\w*)\\.get\\(\"(\\w+)\"\\)(?:\\.get\\(\"(\\w+)\"\\))?");

    /** `private String name;` — including what it is, so a join can be followed. */
    private static final Pattern FIELD = Pattern.compile(
        "private\\s+([\\w<>,.\\[\\] ]+?)\\s+(\\w+)\\s*[;=]");

    @Test
    @DisplayName("every field a specification names exists on the entity it filters")
    void noSpecificationAsksForAFieldThatIsNotThere() throws IOException {
        Map<String, String> sources = sourcesBySimpleName();
        List<String> offences = new ArrayList<>();

        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path file : files.filter(f -> f.toString().endsWith("Specification.java")).sorted().toList()) {
                String source = Files.readString(file);

                Matcher subject = SUBJECT.matcher(source);
                if (!subject.find()) continue;
                Map<String, String> own = fieldsOf(subject.group(1), sources);
                if (own == null) continue;   // not an entity we can read; nothing to check against

                Map<String, String> aliases = new HashMap<>();
                Matcher join = JOIN.matcher(source);
                while (join.find()) {
                    String target = own.get(join.group(2));
                    if (target == null) {
                        offences.add(file.getFileName() + ": joins \"" + join.group(2)
                            + "\", which " + subject.group(1) + " does not have");
                    } else {
                        aliases.put(join.group(1), elementType(target));
                    }
                }

                Matcher attribute = ATTRIBUTE.matcher(source);
                while (attribute.find()) {
                    String owner = attribute.group(1);
                    String first = attribute.group(2);
                    String second = attribute.group(3);

                    if (owner.equals("root")) {
                        if (!own.containsKey(first)) {
                            offences.add(file.getFileName() + ": root.get(\"" + first + "\") — "
                                + subject.group(1) + " has no such field");
                            continue;
                        }
                        if (second != null) {
                            Map<String, String> nested = fieldsOf(elementType(own.get(first)), sources);
                            if (nested != null && !nested.containsKey(second)) {
                                offences.add(file.getFileName() + ": " + first + ".get(\"" + second
                                    + "\") — " + elementType(own.get(first)) + " has no such field");
                            }
                        }
                    } else if (aliases.containsKey(owner)) {
                        Map<String, String> joined = fieldsOf(aliases.get(owner), sources);
                        if (joined != null && !joined.containsKey(first)) {
                            offences.add(file.getFileName() + ": " + owner + ".get(\"" + first
                                + "\") — " + aliases.get(owner) + " has no such field");
                        }
                    }
                }
            }
        }

        assertTrue(offences.isEmpty(), () -> String.format("""
            %d specification(s) name a field that does not exist. Each one compiles and then throws
            the moment somebody filters by it, which reaches the user as a 500 on a page that was
            working a second earlier:

            %s

            Usually a specification copied between two entities that are nearly the same shape.
            """, offences.size(), String.join("\n", offences)));
    }

    /** Every type we can read, by simple name. Ambiguous names are skipped rather than guessed. */
    private static Map<String, String> sourcesBySimpleName() throws IOException {
        Map<String, String> sources = new LinkedHashMap<>();
        List<String> ambiguous = new ArrayList<>();
        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String name = file.getFileName().toString().replace(".java", "");
                if (sources.containsKey(name)) ambiguous.add(name);
                else sources.put(name, Files.readString(file));
            }
        }
        ambiguous.forEach(sources::remove);
        return sources;
    }

    private static Map<String, String> fieldsOf(String type, Map<String, String> sources) {
        String source = sources.get(type);
        if (source == null) return null;
        Map<String, String> fields = new LinkedHashMap<>();
        Matcher m = FIELD.matcher(source);
        while (m.find()) fields.put(m.group(2), m.group(1).trim());
        return fields.isEmpty() ? null : fields;
    }

    /** `List<ParkImage>` → `ParkImage`; anything else unchanged. */
    private static String elementType(String declared) {
        Matcher m = Pattern.compile("^(?:List|Set|Collection)<(.+)>$").matcher(declared);
        return m.matches() ? m.group(1) : declared;
    }
}
