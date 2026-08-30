package com.itineraryledger.kabengosafaris.DataTransfer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

/**
 * What an import did, record by record.
 *
 * The same shape the deletes use, for the same reason: a 200 that quietly did nothing is the worst
 * possible answer. Somebody moving a rate sheet between companies needs to know that 340 rates
 * landed, 12 were left alone because they already existed, and 3 could not be placed because the
 * target has no lodge by that name — and WHICH three.
 *
 * `unresolved` is deliberately separate from `skipped`. Skipping is a decision the import made on
 * purpose; an unresolved reference is a hole in the data, and it is the only part of the report that
 * usually means somebody has to do something.
 */
@Getter
public class TransferReport {

    /** One module's outcome. */
    @Getter
    public static class ModuleOutcome {
        private final String module;
        private int created;
        private int updated;
        private final List<Map<String, String>> skipped = new ArrayList<>();
        private final List<Map<String, String>> unresolved = new ArrayList<>();

        ModuleOutcome(String module) {
            this.module = module;
        }

        public void created() {
            created++;
        }

        public void updated() {
            updated++;
        }

        /** Deliberately left alone — it already exists and the mode says not to touch it. */
        public void skip(String key, String reason) {
            skipped.add(entry(key, reason));
        }

        /**
         * Could not be placed, because something it points at is not in this company.
         *
         * Always names what was missing rather than saying "invalid": the whole value of this line
         * is that somebody can go and create the thing, or correct a spelling, and run it again.
         */
        public void unresolved(String key, String missing) {
            unresolved.add(entry(key, missing));
        }

        private Map<String, String> entry(String key, String reason) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("key", key);
            row.put("reason", reason);
            return row;
        }

        public int total() {
            return created + updated + skipped.size() + unresolved.size();
        }
    }

    private final Map<String, ModuleOutcome> modules = new LinkedHashMap<>();

    public ModuleOutcome forModule(String module) {
        return modules.computeIfAbsent(module, ModuleOutcome::new);
    }

    /** Everything that landed, across every module. */
    public int totalWritten() {
        return modules.values().stream().mapToInt(m -> m.getCreated() + m.getUpdated()).sum();
    }

    public int totalUnresolved() {
        return modules.values().stream().mapToInt(m -> m.getUnresolved().size()).sum();
    }

    /** The wire shape: one row per module, in the order they ran. */
    public List<Map<String, Object>> asRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ModuleOutcome outcome : modules.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("module", outcome.getModule());
            row.put("created", outcome.getCreated());
            row.put("updated", outcome.getUpdated());
            row.put("skipped", outcome.getSkipped());
            row.put("unresolved", outcome.getUnresolved());
            rows.add(row);
        }
        return rows;
    }
}
