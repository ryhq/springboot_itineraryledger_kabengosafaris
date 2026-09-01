package com.itineraryledger.kabengosafaris.DataTransfer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What is allowed to leave the building.
 *
 * An export feature is a hole in a wall that the rest of this system spends a lot of effort
 * maintaining: separate processes, separate databases, separate credentials, an allow-list per
 * company for CORS. All of that is about a company's data staying in its own company, and one
 * carelessly added module here would walk a customer list out through the front door with the
 * blessing of a permission called "export".
 *
 * So the exportable surface is an allow-list, checked here rather than trusted to review. Adding a
 * module is meant to require reading this and deciding, which is exactly the friction wanted.
 */
class DataTransferSurfaceTest {

    private static final Path MODULES =
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/DataTransfer/Modules");

    /**
     * Reference and inventory data only.
     *
     * Every one of these describes a thing the company SELLS — a park, an activity, a lodge, what
     * they cost, and the small vocabularies those prices are written in. None of them describes a
     * person, an agreement, or money that changed hands.
     */
    private static final Set<String> MAY_BE_EXPORTED = Set.of(
        "TariffTransfer", "PaxCategoryTransfer", "SeasonTransfer",
        "ParkTransfer", "ActivityTransfer", "AccommodationTransfer",
        /*
         * Which activities a park offers. A decision about the product, not about a person, and
         * the link an itinerary needs before a crater descent can be put on a day at all.
         */
        "ParkActivityTransfer",
        /*
         * Itineraries and their days.
         *
         * Read the line carefully before adding anything like this again. An itinerary is a
         * PRODUCT assembled from inventory: parks, lodges, activities and the order they are
         * visited in. It names no customer, carries no price, and belongs to nobody. A SAFARI is
         * the same shape with a customer and dates attached, and that is the thing that must never
         * appear in this list. The distinction is one word in a package name and the whole reason
         * this test exists.
         */
        "ItineraryTransfer");

    /** Never, whatever anybody names the class. */
    private static final List<String> FORBIDDEN = List.of(
        "Customer", "Quote", "Invoice", "Payment", "Safari", "CreditNote", "Expense",
        "User", "Role", "Permission", "EmailAccount", "EmailMessage", "AuditLog", "Backup",
        "BookingInquiry", "ContactMessage", "Newsletter", "CompanyProfile");

    @Test
    @DisplayName("only reference and inventory modules are exportable")
    void theSurfaceIsAnAllowList() throws IOException {
        List<String> unexpected = new ArrayList<>();

        try (Stream<Path> files = Files.walk(MODULES)) {
            for (Path file : files.filter(f -> f.toString().endsWith("Transfer.java")).toList()) {
                String name = file.getFileName().toString().replace(".java", "");
                if (!MAY_BE_EXPORTED.contains(name)) unexpected.add(name);
            }
        }

        assertTrue(unexpected.isEmpty(),
            "A new exportable module appeared: " + unexpected + ". If it carries anything belonging "
                + "to a customer it must not exist; if it is reference data, add it to "
                + "MAY_BE_EXPORTED and say why here.");
    }

    @Test
    @DisplayName("no exportable module reads a customer-owned repository")
    void nothingReachesCustomerData() throws IOException {
        List<String> offenders = new ArrayList<>();

        try (Stream<Path> files = Files.walk(MODULES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                for (String forbidden : FORBIDDEN) {
                    /*
                     * The import line is the tell. A module cannot read what it has not imported, and
                     * a name in a comment is not a data path — hence matching the import, not the file.
                     */
                    if (source.contains("import com.itineraryledger.kabengosafaris." + forbidden + ".")
                        || source.contains("." + forbidden + "Repository;")) {
                        offenders.add(file.getFileName() + " reaches " + forbidden);
                    }
                }
            }
        }

        assertTrue(offenders.isEmpty(),
            "An export module must not be able to read data belonging to a customer: " + offenders);
    }

    @Test
    @DisplayName("every module declares what it depends on, or its rates arrive unplaceable")
    void dependenciesAreDeclared() throws IOException {
        String parks = Files.readString(MODULES.resolve("ParkTransfer.java"));
        String activities = Files.readString(MODULES.resolve("ActivityTransfer.java"));
        String accommodations = Files.readString(MODULES.resolve("AccommodationTransfer.java"));

        /* a park rate is (park, tariff, season, residency, age) — four of them live elsewhere */
        for (String needed : new String[] { "tariffs", "pax-categories", "seasons" }) {
            assertTrue(parks.contains("\"" + needed + "\""),
                "parks must carry " + needed + ", or every rate is unplaceable");
        }
        /* an activity rate may be priced for a particular park, so parks go first */
        assertTrue(activities.contains("\"parks\""),
            "activities must carry parks: a rate may be priced for one, and it is read after them");
        /* a lodge's room vocabulary is inside the lodge; only the season can be company-wide */
        assertTrue(accommodations.contains("\"seasons\""),
            "accommodations must carry seasons");
    }
}
