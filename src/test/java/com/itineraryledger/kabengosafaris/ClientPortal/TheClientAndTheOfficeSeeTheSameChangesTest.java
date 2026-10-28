package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalChangeLogService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalChangeLogService.Change;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteSnapshot;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteRevisionService;

/**
 * One list of changes, read by both sides.
 *
 * <p>The requirement behind the whole of change tracking is not that a change list exists; it is
 * that the client's and the office's are the same list. Two summaries of one conversation is how
 * a review ends with each side certain, and one of them wrong, about what was agreed — and the
 * disagreement surfaces at the worst possible moment, which is the invoice.
 *
 * <p>So two things are pinned here. The sentences, because a diff that reports a lodge swap as a
 * removal and an addition is technically correct and useless to read. And the wiring: the scan at
 * the bottom fails if either surface ever grows its own way of working out what changed.
 */
class TheClientAndTheOfficeSeeTheSameChangesTest {

    private PortalChangeLogService service() {
        return new PortalChangeLogService(mock(QuoteRevisionService.class));
    }

    // ------------------------------------------------------------------ builders

    private QuoteSnapshot snapshot(List<QuoteSnapshot.Day> days, BigDecimal total) {
        return snapshot(days, total, List.of(new QuoteSnapshot.Party("Non-Resident", "Adult", 2)));
    }

    private QuoteSnapshot snapshot(List<QuoteSnapshot.Day> days, BigDecimal total,
                                   List<QuoteSnapshot.Party> party) {
        return new QuoteSnapshot("QT-1046-0926-1", 1, party, days,
            total == null ? List.of()
                : List.of(new QuoteSnapshot.Money("USD", null, null, total)));
    }

    private QuoteSnapshot.Day dayWith(String lodge) {
        return dayWith(lodge, "Half Board", 1);
    }

    private QuoteSnapshot.Day dayWith(String lodge, String board, int rooms) {
        return new QuoteSnapshot.Day(3, "Serengeti", "Seronera",
            List.of(new QuoteSnapshot.Stay(lodge, "Double", "Standard", board, rooms, false)),
            List.of(), List.of(), List.of());
    }

    private List<String> sentences(List<Change> changes) {
        return changes.stream().map(Change::text).toList();
    }

    // ------------------------------------------------------------------ the sentences

    @Test
    @DisplayName("one lodge becoming another is one sentence, not a removal and an addition")
    void aSwapReadsAsASwap() {
        List<Change> changes = service().between(
            snapshot(List.of(dayWith("Kudu Lodge")), new BigDecimal("18313.40")),
            snapshot(List.of(dayWith("Farm of Dreams Lodge")), new BigDecimal("18313.40")));

        assertEquals(List.of("Day 3: Kudu Lodge became Farm of Dreams Lodge"), sentences(changes),
            "two lines saying half of one thing each is what a reader has to reassemble");
    }

    @Test
    @DisplayName("the price is reported with the change that caused it, and only the total")
    void thePriceFollowsTheChange() {
        List<Change> changes = service().between(
            snapshot(List.of(dayWith("Kudu Lodge")), new BigDecimal("18313.40")),
            snapshot(List.of(dayWith("Farm of Dreams Lodge")), new BigDecimal("19450.00")));

        assertEquals(List.of(
            "Day 3: Kudu Lodge became Farm of Dreams Lodge",
            "Total: USD 18,313.40 became USD 19,450.00"), sentences(changes));
    }

    @Test
    @DisplayName("a board basis or a room count moving is named, because it is what is charged")
    void theTermsOfTheStayAreReported() {
        List<Change> changes = service().between(
            snapshot(List.of(dayWith("Kudu Lodge", "Half Board", 1)), null),
            snapshot(List.of(dayWith("Kudu Lodge", "Full Board", 2)), null));

        assertEquals(List.of(
            "Day 3: Kudu Lodge, board Half Board became Full Board",
            "Day 3: Kudu Lodge, 1 room became 2 rooms"), sentences(changes));
    }

    @Test
    @DisplayName("the party is reported per kind of traveller, not as one number")
    void thePartyIsReportedPerKind() {
        List<Change> changes = service().between(
            snapshot(List.of(), null, List.of(new QuoteSnapshot.Party("Non-Resident", "Adult", 2))),
            snapshot(List.of(), null, List.of(
                new QuoteSnapshot.Party("Non-Resident", "Adult", 2),
                new QuoteSnapshot.Party("Non-Resident", "Child", 1))));

        assertEquals(List.of("Non-Resident, Child: 1 added"), sentences(changes),
            "a child and an adult are not priced alike, so 2 became 3 is not the change");
    }

    @Test
    @DisplayName("an alternative we offered is context, not a change the client made")
    void theMenuIsNotTheChange() {
        QuoteSnapshot.Day before = new QuoteSnapshot.Day(3, "Serengeti", "Seronera", List.of(
            new QuoteSnapshot.Stay("Kudu Lodge", "Double", "Standard", "Half Board", 1, false),
            new QuoteSnapshot.Stay("Farm of Dreams Lodge", "Double", "Standard", "Half Board", 1, true)),
            List.of(), List.of(), List.of());

        QuoteSnapshot.Day after = new QuoteSnapshot.Day(3, "Serengeti", "Seronera", List.of(
            new QuoteSnapshot.Stay("Farm of Dreams Lodge", "Double", "Standard", "Half Board", 1, false),
            new QuoteSnapshot.Stay("Kudu Lodge", "Double", "Standard", "Half Board", 1, true)),
            List.of(), List.of(), List.of());

        assertEquals(List.of("Day 3: Kudu Lodge became Farm of Dreams Lodge"),
            sentences(service().between(snapshot(List.of(before), null),
                snapshot(List.of(after), null))),
            "reporting the mirrored swap in the menu says the same thing twice, backwards");
    }

    @Test
    @DisplayName("nothing changing says nothing, rather than listing the whole trip")
    void anUnchangedQuotationIsSilent() {
        QuoteSnapshot same = snapshot(List.of(dayWith("Kudu Lodge")), new BigDecimal("18313.40"));
        assertTrue(service().between(same, same).isEmpty());
    }

    @Test
    @DisplayName("the first round has no before, and is not reported as forty additions")
    void thereIsNothingToSayAboutTheFirstRound() {
        assertTrue(service().between(
            null, snapshot(List.of(dayWith("Kudu Lodge")), new BigDecimal("18313.40"))).isEmpty(),
            "a trip is not a change; it is the thing the changes are to");
    }

    // ------------------------------------------------------------------ the snapshot

    @Test
    @DisplayName("a stored snapshot holds no obfuscated id, because the salt moves under it")
    void theSnapshotOutlivesARestart() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/itineraryledger/kabengosafaris/Quote/DTOs/QuoteSnapshot.java"));

        int body = source.indexOf("public record QuoteSnapshot");
        assertFalse(source.substring(body).contains("encodeId"),
            "this installation mints a fresh Hashids salt on every restart unless one is "
                + "configured, so an id written into a snapshot today decodes to a different "
                + "record after the next deploy. A snapshot is history and has to outlive one.");
    }

    // ------------------------------------------------------------------ the wiring

    /**
     * Neither side works out for itself what changed.
     *
     * <p>A list of two controllers would rot. Reading the tree is the check that still holds when
     * somebody adds a third surface, and the failure message names the file.
     */
    @Test
    @DisplayName("the client's list and the office's come from one service")
    void oneServiceFeedsBothSurfaces() throws IOException {
        Path portal = Path.of("src/main/java/com/itineraryledger/kabengosafaris/ClientPortal");

        String client = Files.readString(portal.resolve("Controller/PortalApiController.java"));
        String office = Files.readString(portal.resolve("Controller/PortalAdminController.java"));

        assertTrue(client.contains("changeLog.forQuote("),
            "the client's change list must come from PortalChangeLogService");
        assertTrue(office.contains("changeLog.forQuote("),
            "the office's change list must come from the same service, or the two can disagree");

        List<String> offenders = new java.util.ArrayList<>();
        try (var files = Files.walk(Path.of("src/main/java/com/itineraryledger/kabengosafaris"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String name = file.getFileName().toString();
                if (name.equals("PortalChangeLogService.java")) continue;
                if (Files.readString(file).contains("class ChangeLog")) offenders.add(name);
            }
        }
        assertTrue(offenders.isEmpty(), "a second change log has appeared: " + offenders);
    }
}
