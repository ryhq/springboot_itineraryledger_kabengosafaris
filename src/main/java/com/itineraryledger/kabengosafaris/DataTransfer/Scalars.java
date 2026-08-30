package com.itineraryledger.kabengosafaris.DataTransfer;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * An entity's own columns, without its relations, in both directions.
 *
 * Written reflectively rather than as a field list per entity, because a field list is a promise to
 * remember. Park alone has twenty-odd descriptive columns and they change; the version of this that
 * enumerated them would export a park correctly today and drop `bestTimeToVisit` the week somebody
 * adds `bestTimeToAvoid` next to it. Nothing would fail — the column would just quietly stop
 * travelling, and only turn up as a blank field in another company months later.
 *
 * Only SIMPLE values are copied. Anything that is another entity or a collection is a relation, and
 * relations are resolved by natural key by the module that owns them — a foreign key means nothing
 * in the company being imported into.
 */
@Slf4j
public final class Scalars {

    private Scalars() {}

    /** Never travels: identity is per-installation, and timestamps belong to the row that is written. */
    private static final Set<String> NEVER = Set.of("id", "createdAt", "updatedAt", "class");

    private static final Set<Class<?>> SIMPLE = Set.of(
        String.class, Boolean.class, boolean.class, Integer.class, int.class,
        Long.class, long.class, Double.class, double.class, Float.class, float.class,
        BigDecimal.class, LocalDate.class, LocalDateTime.class, LocalTime.class, MonthDay.class);

    private static boolean isSimple(Class<?> type) {
        return SIMPLE.contains(type) || type.isEnum();
    }

    /**
     * The entity's scalar columns as JSON.
     *
     * @param exclude property names to leave out on top of the standard ones — for a column that is
     *                local to this installation and would be nonsense elsewhere
     */
    public static ObjectNode of(ObjectMapper mapper, Object entity, String... exclude) {
        Set<String> skip = new java.util.HashSet<>(NEVER);
        skip.addAll(Set.of(exclude));

        Map<String, Object> values = new LinkedHashMap<>();
        try {
            BeanInfo info = Introspector.getBeanInfo(entity.getClass(), Object.class);
            for (PropertyDescriptor property : info.getPropertyDescriptors()) {
                Method getter = property.getReadMethod();
                if (getter == null || skip.contains(property.getName())) continue;
                if (!isSimple(property.getPropertyType())) continue;

                /*
                 * A column is something that can be written back. Anything with a getter and no
                 * setter is DERIVED — computed from the real columns — and treating it as data was
                 * a fault twice over: it put values in the bundle that no import could ever restore,
                 * and it invoked code that has every right to fail.
                 *
                 * Which it did, on a live export. PaxAgeCategory.getAgeRangeDisplay() reads maxAge
                 * to print "6–11 years", and an adult category is legitimately "12 and over" with no
                 * maximum — so exporting a company's guest categories threw a NullPointerException
                 * on correct data. SeasonPeriod.getDurationDays() the same, on a period with no
                 * start. The whole bundle came back as a 500 saying nothing.
                 */
                if (property.getWriteMethod() == null) continue;
                Object value = getter.invoke(entity);
                if (value != null) values.put(property.getName(), value);
            }
        } catch (Exception e) {
            /* a bundle missing a column is a data problem; a bundle that failed to build is worse */
            log.error("Could not read the columns of {}", entity.getClass().getSimpleName(), e);
            throw new IllegalStateException("Could not read " + entity.getClass().getSimpleName(), e);
        }
        return mapper.valueToTree(values);
    }

    /**
     * Copy scalar columns from JSON onto an entity, leaving everything else alone.
     *
     * Unknown fields are ignored rather than refused: a bundle from a newer installation may name a
     * column this one has not got yet, and refusing the whole import over it would make every upgrade
     * a flag day. The manifest's schema version is where an actual incompatibility is caught.
     */
    public static void apply(ObjectMapper mapper, JsonNode node, Object entity, String... exclude) {
        if (node == null || !node.isObject()) return;
        ObjectNode copy = ((ObjectNode) node).deepCopy();
        copy.remove(NEVER);
        for (String name : exclude) copy.remove(name);

        try {
            /*
             * Unknown fields ignored, and it has to be said explicitly: the default is to throw, so
             * the version of this without the next line refused an entire bundle because a newer
             * installation had added one column. Caught by a test that asserted the tolerance this
             * comment claimed — the claim was wrong, not the test.
             */
            mapper.copy()
                .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readerForUpdating(entity)
                .readValue(copy);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Could not apply " + copy.fieldNames() + " to " + entity.getClass().getSimpleName(), e);
        }
    }
}
