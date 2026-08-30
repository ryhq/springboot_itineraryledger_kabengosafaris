package com.itineraryledger.kabengosafaris.DataTransfer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itineraryledger.kabengosafaris.DataTransfer.Services.BundleImportService;

import lombok.Data;

/**
 * The bundle format, and the reflective copier the whole export rests on.
 *
 * Both are the kind of thing that fails quietly. A copier that silently stops carrying a column
 * produces a bundle that imports cleanly and is missing a price; a zip reader that trusts entry
 * names writes files wherever the person who made the zip wanted.
 */
class BundleFormatTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /** Stands in for a rate: the kinds of value these entities actually hold. */
    @Data
    public static class Fixture {
        public enum Basis { PER_PERSON, PER_UNIT }

        private Long id = 42L;
        private String name = "High Season";
        private BigDecimal rackRate = new BigDecimal("1250.50");
        private Integer sleeps = 3;
        private Boolean isActive = true;
        private Basis basis = Basis.PER_PERSON;
        private MonthDay startDate = MonthDay.of(6, 1);
        private LocalDateTime createdAt = LocalDateTime.of(2020, 1, 1, 0, 0);
        /** a relation: must not travel, because a foreign key means nothing in another company */
        private Fixture parent;
        private java.util.List<String> children = java.util.List.of("a", "b");
    }

    @Test
    @DisplayName("every scalar column travels, and nothing else does")
    void scalarsRoundTrip() {
        Fixture source = new Fixture();
        source.setParent(new Fixture());

        ObjectNode node = Scalars.of(mapper, source);

        /* the identity and the timestamps belong to the row that gets written, not to the data */
        assertFalse(node.has("id"), "an id from another installation is meaningless here");
        assertFalse(node.has("createdAt"), "timestamps belong to the row being written");
        /* relations are resolved by name by the module that owns them */
        assertFalse(node.has("parent"), "a relation must not be serialised as a nested object");
        assertFalse(node.has("children"), "a collection is a relation too");

        assertEquals("High Season", node.path("name").asText());
        /*
         * compareTo, not equals: BigDecimal.equals compares SCALE as well as value, and JSON has no
         * concept of a trailing zero, so 1250.50 comes back as 1250.5. That is the same number, and
         * the column it is written to has its own scale — what would matter is a rounded VALUE, so
         * that is what is checked, here and on a figure with more decimals than money usually has.
         */
        assertEquals(0, new BigDecimal("1250.50").compareTo(node.path("rackRate").decimalValue()),
            "money must survive exactly — a rate rounded in transit is a wrong quote");
        assertEquals("PER_PERSON", node.path("basis").asText());
        assertEquals("--06-01", node.path("startDate").asText());

        Fixture target = new Fixture();
        target.setName("something else");
        target.setRackRate(null);
        Scalars.apply(mapper, node, target);

        assertEquals("High Season", target.getName());
        assertEquals(0, new BigDecimal("1250.50").compareTo(target.getRackRate()));
        assertEquals(Fixture.Basis.PER_PERSON, target.getBasis());
        assertEquals(MonthDay.of(6, 1), target.getStartDate());
        assertEquals(42L, target.getId(), "the target keeps its own identity");
    }

    @Test
    @DisplayName("a long decimal is not rounded on the way through")
    void precisionSurvives() {
        Fixture source = new Fixture();
        source.setRackRate(new BigDecimal("1234.567891"));

        Fixture target = new Fixture();
        Scalars.apply(mapper, Scalars.of(mapper, source), target);

        assertEquals(0, new BigDecimal("1234.567891").compareTo(target.getRackRate()),
            "a rate that loses digits in transit is a wrong quote in the target company");
    }

    @Test
    @DisplayName("a column the reader has never heard of is ignored, not fatal")
    void unknownColumnsAreTolerated() {
        ObjectNode node = mapper.createObjectNode();
        node.put("name", "Low Season");
        node.put("somethingAddedLater", "a column this build does not have");

        Fixture target = new Fixture();
        assertDoesNotThrow(() -> Scalars.apply(mapper, node, target),
            "a bundle from a newer installation must not be refused field by field — the manifest's "
                + "schema version is where a real incompatibility is caught");
        assertEquals("Low Season", target.getName());
    }

    private byte[] zip(String... namesAndBodies) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (int i = 0; i < namesAndBodies.length; i += 2) {
                zip.putNextEntry(new ZipEntry(namesAndBodies[i]));
                zip.write(namesAndBodies[i + 1].getBytes());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private BundleImportService reader() {
        return new BundleImportService(java.util.List.of(), mapper);
    }

    @Test
    @DisplayName("a zip with no manifest is refused, and says what it is not")
    void aFileThatIsNotABundle() throws Exception {
        byte[] notABundle = zip("holiday-photo.jpg", "not json");
        var thrown = assertThrows(IllegalArgumentException.class,
            () -> reader().read(new ByteArrayInputStream(notABundle)));
        assertTrue(thrown.getMessage().contains("manifest"), thrown.getMessage());
    }

    @Test
    @DisplayName("a bundle from a newer build is refused rather than half-read")
    void aNewerSchemaIsRefused() throws Exception {
        byte[] fromTheFuture = zip("manifest.json", "{\"schemaVersion\": 99}");
        var thrown = assertThrows(IllegalArgumentException.class,
            () -> reader().read(new ByteArrayInputStream(fromTheFuture)));
        assertTrue(thrown.getMessage().contains("newer version"), thrown.getMessage());
        /*
         * Refused rather than read on a best-effort basis: whatever the newer build added would be
         * dropped in silence, and a rate sheet that is quietly missing a dimension is worse than one
         * that would not load.
         */
    }

    @Test
    @DisplayName("a file path that climbs out of the bundle is refused")
    void zipSlipIsRefused() throws Exception {
        byte[] malicious = zip(
            "manifest.json", "{\"schemaVersion\": 1}",
            "files/../../../../tmp/escaped.txt", "anything");

        var thrown = assertThrows(IllegalArgumentException.class,
            () -> reader().read(new ByteArrayInputStream(malicious)));
        assertTrue(thrown.getMessage().contains("outside"), thrown.getMessage());
    }

    @Test
    @DisplayName("a bundle's data files are read into their modules")
    void dataIsReadByModuleName() throws Exception {
        byte[] bundle = zip(
            "manifest.json", "{\"schemaVersion\": 1, \"sourceCompany\": \"Somewhere\"}",
            "data/parks.json", "[{\"slug\": \"serengeti\"}]",
            "data/tariffs.json", "[{\"slug\": \"conservation-fee\"}]");

        var read = reader().read(new ByteArrayInputStream(bundle));
        assertEquals(2, read.getData().size());
        assertEquals("serengeti", read.getData().get("parks").get(0).path("slug").asText());
        assertEquals("Somewhere", read.getManifest().path("sourceCompany").asText());
    }
}
