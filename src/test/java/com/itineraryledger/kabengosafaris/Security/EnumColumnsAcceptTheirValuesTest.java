package com.itineraryledger.kabengosafaris.Security;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 * That every Java enum value is one the database column will actually accept.
 *
 * MySQL stores several of these columns as `enum(...)`, so a value the COLUMN has never heard of is
 * rejected however valid it is in Java. Adding a constant is therefore a schema change, and forgetting
 * the migration does not fail the build, does not fail a unit test, and does not fail until the
 * application tries to write the value — which for anything seeded at boot means the application
 * never starts.
 *
 * That has now happened twice: asset_kind when the email logos were added, and category when the bill
 * reminders were. The second one took a failed deploy and a canary rollback to find, because the only
 * test that boots the Spring context is @Disabled for want of a real MySQL — so nothing here notices
 * that the application cannot start.
 *
 * This needs no database. It reads the migrations, applies them in order so the latest definition of
 * a column wins, and compares each against the Java enum that writes to it.
 */
class EnumColumnsAcceptTheirValuesTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

    /**
     * Which Java enum feeds which column.
     *
     * Written out rather than discovered. A column added without a line here is a column nobody
     * checked, and the point of the list is that adding one is a decision somebody makes on purpose.
     */
    private static final Map<String, Class<? extends Enum<?>>> WRITERS = new LinkedHashMap<>();
    static {
        WRITERS.put("notification_settings.category",
            com.itineraryledger.kabengosafaris.NotificationSetting.NotificationSetting.Category.class);
        WRITERS.put("company_assets.asset_kind",
            com.itineraryledger.kabengosafaris.CompanyProfile.Entity.CompanyAsset.AssetKind.class);
        WRITERS.put("expenses.status",
            com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus.class);
    }

    /** The last definition each `table.column` was given, migrations applied in order. */
    private Map<String, Set<String>> columnsAfterEveryMigration() throws IOException {
        Map<String, Set<String>> columns = new LinkedHashMap<>();

        List<Path> files;
        try (Stream<Path> found = Files.list(MIGRATIONS)) {
            files = found.filter(f -> f.toString().endsWith(".sql"))
                .sorted(java.util.Comparator.comparingInt(this::versionOf))
                .toList();
        }

        Pattern createTable = Pattern.compile("create table (\\w+) \\((.*?)\\) engine", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Pattern enumColumn = Pattern.compile("(\\w+) enum \\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);
        Pattern alter = Pattern.compile(
            "alter table\\s+(\\w+)\\s+modify column\\s+(\\w+)\\s+enum\\s*\\(([^)]*)\\)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        for (Path file : files) {
            String sql = Files.readString(file);

            Matcher tables = createTable.matcher(sql);
            while (tables.find()) {
                String table = tables.group(1);
                Matcher cols = enumColumn.matcher(tables.group(2));
                while (cols.find()) {
                    columns.put(table + "." + cols.group(1), valuesIn(cols.group(2)));
                }
            }

            Matcher altered = alter.matcher(sql);
            while (altered.find()) {
                columns.put(altered.group(1) + "." + altered.group(2), valuesIn(altered.group(3)));
            }
        }
        return columns;
    }

    private int versionOf(Path file) {
        Matcher m = Pattern.compile("^V(\\d+)__").matcher(file.getFileName().toString());
        return m.find() ? Integer.parseInt(m.group(1)) : Integer.MAX_VALUE;
    }

    private Set<String> valuesIn(String list) {
        Set<String> values = new TreeSet<>();
        Matcher m = Pattern.compile("'([^']*)'").matcher(list);
        while (m.find()) values.add(m.group(1));
        return values;
    }

    @Test
    @DisplayName("every enum constant is a value its column accepts")
    void noConstantTheDatabaseWouldRefuse() throws IOException {
        Map<String, Set<String>> columns = columnsAfterEveryMigration();
        List<String> problems = new ArrayList<>();

        for (Map.Entry<String, Class<? extends Enum<?>>> entry : WRITERS.entrySet()) {
            Set<String> accepted = columns.get(entry.getKey());
            if (accepted == null) {
                problems.add(entry.getKey() + " — no enum column by that name in any migration");
                continue;
            }
            for (Enum<?> constant : entry.getValue().getEnumConstants()) {
                if (!accepted.contains(constant.name())) {
                    problems.add(entry.getKey() + " would refuse " + constant.name()
                        + " (it accepts " + accepted + ") — add a migration widening the column");
                }
            }
        }

        assertTrue(problems.isEmpty(),
            "A Java enum value that the database column does not accept is rejected at write time. "
                + "For anything seeded at boot that means the application never starts: " + problems);
    }

    @Test
    @DisplayName("the mapping covers every enum column a migration has widened")
    void widenedColumnsAreAccountedFor() throws IOException {
        /*
         * A column somebody has already had to ALTER is a column whose Java enum grows — exactly the
         * ones worth watching. If one is widened and never listed above, this test says so rather
         * than quietly checking two of three.
         */
        List<String> widened = new ArrayList<>();
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".sql")).toList()) {
                Matcher m = Pattern.compile(
                    "alter table\\s+(\\w+)\\s+modify column\\s+(\\w+)\\s+enum",
                    Pattern.CASE_INSENSITIVE).matcher(Files.readString(file));
                while (m.find()) {
                    String key = m.group(1) + "." + m.group(2);
                    if (!WRITERS.containsKey(key) && !widened.contains(key)) widened.add(key);
                }
            }
        }

        assertTrue(widened.isEmpty(),
            "These enum columns have been widened by a migration but no Java enum is checked against "
                + "them. Add them to WRITERS: " + widened);
    }
}
