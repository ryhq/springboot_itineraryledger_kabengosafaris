package com.itineraryledger.kabengosafaris.Security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A column has to be the type the mapping will ask for, not the type a person would pick.
 *
 * <p>{@code ddl-auto=validate} compares every column against what Hibernate derives from the
 * Java field and refuses to build the SessionFactory on a mismatch. There is no warning and no
 * degraded mode: the application does not start. It failed exactly that way on a deploy, and
 * the mismatch was a single word -
 *
 * <pre>
 *   Schema-validation: wrong column type encountered in column [draft_token] in table
 *   [booking_inquiries]; found [char (Types#CHAR)], but expecting [varchar(36)]
 * </pre>
 *
 * <p>because the migration said {@code CHAR(36)} where the field was a {@code String}. A second
 * column in the same statement said {@code SMALLINT} for an {@code Integer}. Both are types a
 * careful hand reaches for in MySQL and neither is a type the mapping produces.
 *
 * <p>This is not fussiness on Hibernate's part either: CHAR pads with trailing spaces where
 * VARCHAR does not, so a token written through one and read through the other stops matching.
 *
 * <p>The migrations are replayed in order and the <b>final</b> type of each column is checked,
 * so a type corrected by a later migration is correct here too - which is the only way a fix
 * can be shipped, since Flyway checksums what it has already applied and an edited migration
 * stops that installation deploying at all.
 */
class AColumnIsTheTypeTheMappingExpectsTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

    /** `ADD COLUMN x TYPE`, `MODIFY COLUMN x TYPE`, and a bare column line inside CREATE TABLE. */
    private static final Pattern COLUMN = Pattern.compile(
        "(?:(?:ADD|MODIFY|CHANGE)\\s+COLUMN\\s+"
            /* or an indented line inside CREATE TABLE - but not one that opens a clause, or
             * `ADD COLUMN x` is read as a column called "add" of type "COLUMN". */
            + "|^[ \\t]{2,}(?!ADD\\b|MODIFY\\b|CHANGE\\b|DROP\\b|ALTER\\b|RENAME\\b))"
            + "`?(\\w+)`?\\s+"
            + "([A-Za-z]+(?:\\(\\s*\\d+\\s*(?:,\\s*\\d+\\s*)?\\))?)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern TABLE = Pattern.compile(
        "(?:CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?|ALTER\\s+TABLE)\\s+`?(\\w+)`?",
        Pattern.CASE_INSENSITIVE);

    @Test
    @DisplayName("no column ends up a type the mapping will refuse")
    void everyColumnSurvivesValidation() throws IOException {
        Map<String, String> finalType = replayMigrations();
        List<String> offenders = new ArrayList<>();

        for (Map.Entry<String, String> entry : finalType.entrySet()) {
            String type = entry.getValue().toUpperCase().replace(" ", "");

            /* A String is varchar, whatever its length. CHAR also pads, which breaks lookups. */
            if (type.startsWith("CHAR(") || type.equals("CHAR")) {
                offenders.add(entry.getKey() + " is " + entry.getValue()
                    + " - a String field maps to varchar, so validation will refuse this");
            }
            /* An Integer is int. SMALLINT and MEDIUMINT are narrower and are not what it asks for. */
            if (type.startsWith("SMALLINT") || type.startsWith("MEDIUMINT")) {
                offenders.add(entry.getKey() + " is " + entry.getValue()
                    + " - an Integer field maps to int, however small the values are in practice");
            }
            /* TINYINT(1) is how MySQL spells boolean, and is correct. Anything wider is not. */
            if (type.startsWith("TINYINT") && !type.startsWith("TINYINT(1)")) {
                offenders.add(entry.getKey() + " is " + entry.getValue()
                    + " - only TINYINT(1) is a boolean; a number wants int");
            }
        }

        assertTrue(offenders.isEmpty(),
            "These columns will stop the application starting under ddl-auto=validate:\n  "
                + String.join("\n  ", offenders));
    }

    @Test
    @DisplayName("a type corrected by a later migration counts as corrected")
    void theReplayIsWhatIsChecked() throws IOException {
        Map<String, String> finalType = replayMigrations();

        /*
         * The case that produced this test. V39 added draft_token as CHAR(36) and last_step as
         * SMALLINT; V40 corrects both. V39 is deliberately NOT edited - it has already run on
         * one of the two installations, and Flyway checksums what it has applied.
         */
        assertTrue(finalType.containsKey("booking_inquiries.draft_token"),
            "draft_token is gone - has the progressive-capture work been reverted?");
        assertFalse(finalType.get("booking_inquiries.draft_token").toUpperCase().startsWith("CHAR("),
            "draft_token is still CHAR, so the correcting migration is missing");
        assertTrue(finalType.get("booking_inquiries.draft_token").toUpperCase().startsWith("VARCHAR"),
            "draft_token must be varchar to match a String field, found "
                + finalType.get("booking_inquiries.draft_token"));
        assertTrue(finalType.get("booking_inquiries.last_step").equalsIgnoreCase("INT"),
            "last_step must be int to match an Integer field, found "
                + finalType.get("booking_inquiries.last_step"));
    }

    /** Every column declaration in version order, last write winning, keyed `table.column`. */
    private static Map<String, String> replayMigrations() throws IOException {
        Map<String, String> columns = new LinkedHashMap<>();

        List<Path> files;
        try (Stream<Path> walk = Files.list(MIGRATIONS)) {
            files = walk.filter(p -> p.getFileName().toString().matches("V\\d+__.*\\.sql"))
                .sorted(Comparator.comparingInt(AColumnIsTheTypeTheMappingExpectsTest::versionOf))
                .toList();
        }
        assertFalse(files.isEmpty(), "no migrations found - has the directory moved?");

        for (Path file : files) {
            String body = Files.readString(file);
            /* Comments carry example SQL explaining past mistakes; reading them as schema would
             * flag the very thing the comment is describing. */
            body = body.replaceAll("(?m)^\\s*--.*$", "");

            String table = null;
            for (String statement : body.split(";")) {
                Matcher tableMatch = TABLE.matcher(statement);
                if (tableMatch.find()) table = tableMatch.group(1).toLowerCase();
                if (table == null) continue;

                Matcher column = COLUMN.matcher(statement);
                while (column.find()) {
                    String name = column.group(1).toLowerCase();
                    if (RESERVED.contains(name)) continue;
                    columns.put(table + "." + name, column.group(2));
                }
            }
        }
        return columns;
    }

    /** Words that start a line inside CREATE TABLE but are not columns. */
    private static final List<String> RESERVED = List.of(
        "primary", "unique", "key", "index", "constraint", "foreign", "fulltext", "check");

    private static int versionOf(Path file) {
        Matcher m = Pattern.compile("^V(\\d+)__").matcher(file.getFileName().toString());
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
}
