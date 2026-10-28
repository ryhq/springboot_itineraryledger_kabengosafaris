package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The build fails if anything a client's link can reach carries what their trip costs us.
 *
 * <p>Modelled on {@code NoClientDocumentPrintsOurCostTest}, which exists because an itinerary PDF
 * once reached a customer reading "Auto-linked alternative: MID_RANGE in Arusha, STO $90" — our
 * supplier price and our internal budget grading. The portal is the same hazard with a wider
 * mouth: a document is assembled deliberately, while a view model grows a field at a time, and
 * the entities it reads from carry our cost basis right beside the customer's price.
 *
 * <p><strong>The real danger is reuse, not carelessness.</strong> {@code FullQuoteDTO} and
 * {@code QuoteDayTreeDTO} are complete and correct and already written, and every one of them
 * carries {@code internalNotes}, {@code agentCommissionPercentage}, {@code marginUplift*} and
 * {@code isStoRate}. Handing one to the portal would take an afternoon off the work and publish
 * our margin to every customer, and nothing on the screen would look wrong. So the first scan
 * here is not about strings at all: it is that no client-facing portal file may so much as name
 * one of those types.
 *
 * <p>Note what is <em>not</em> forbidden: the word "cost" on its own. The portal recomputes the
 * client's own price through {@code QuoteCostEstimationService} every time they change something,
 * which is the whole point of letting them change it. Their price is theirs. Ours is not.
 */
class NoPortalResponseCarriesOurCostTest {

    private static final Path PORTAL =
        Path.of("src/main/java/com/itineraryledger/kabengosafaris/ClientPortal");
    private static final Path TEMPLATES = Path.of("src/main/resources/templates/portal");
    private static final Path SNAPSHOT = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Quote/DTOs/QuoteSnapshot.java");

    /**
     * The files a client can reach.
     *
     * <p>Everything under {@code ClientPortal} except the two that are the office's side of the
     * same feature, plus the snapshot, which is stored history and is served to the client as
     * their change list.
     *
     * <p>The exemption is by name and there are exactly two, because a long exemption list is
     * how a guard like this stops guarding anything.
     */
    private static boolean isTheOfficesSide(Path file) {
        String name = file.getFileName().toString();
        return name.equals("PortalAdminController.java") || name.equals("PortalAdminDTOs.java");
    }

    /** Types that are correct, complete, already written, and carry our margin. */
    private static final Map<Pattern, String> FORBIDDEN_TYPES = new LinkedHashMap<>();
    static {
        FORBIDDEN_TYPES.put(Pattern.compile("\\bFullQuoteDTO\\b"),
            "FullQuoteDTO carries internalNotes, agentCommissionPercentage, marginUplift* "
                + "and isStoRate");
        FORBIDDEN_TYPES.put(Pattern.compile("\\bQuoteDayTreeDTO\\b"),
            "QuoteDayTreeDTO carries the per-node operational notes");
        FORBIDDEN_TYPES.put(Pattern.compile("\\bQuoteItemDTO\\b"),
            "a quote item carries the line's own cost basis");
    }

    /** Values that are ours and not theirs, whatever they are called on the way through. */
    private static final Map<Pattern, String> FORBIDDEN_VALUES = new LinkedHashMap<>();
    static {
        FORBIDDEN_VALUES.put(Pattern.compile("internalNotes", Pattern.CASE_INSENSITIVE),
            "the internal note, which sits one word away from customerNotes on the same entity");
        FORBIDDEN_VALUES.put(Pattern.compile("agentCommission", Pattern.CASE_INSENSITIVE),
            "what we pay an agent");
        FORBIDDEN_VALUES.put(Pattern.compile("marginUplift", Pattern.CASE_INSENSITIVE),
            "our margin");
        FORBIDDEN_VALUES.put(Pattern.compile("\\bstoRate\\b|\\bisStoRate\\b|\\bstoPrice\\b",
            Pattern.CASE_INSENSITIVE),
            "the supplier rate this was priced from");
        FORBIDDEN_VALUES.put(Pattern.compile("getVendor\\b|\\bsupplierName\\b|\\bsupplierPrice\\b",
            Pattern.CASE_INSENSITIVE),
            "who we buy from, which is ours and is worth money to a competitor");
    }

    // ------------------------------------------------------------------ the scans

    @Test
    @DisplayName("no portal response is built from a type that carries our margin")
    void thePortalDoesNotReuseAStaffDTO() throws IOException {
        List<String> offences = new ArrayList<>();

        for (Path file : clientFacingJava()) {
            scan(file, FORBIDDEN_TYPES, offences);
        }

        assertTrue(offences.isEmpty(), () -> """
            A client-facing portal file names a type that carries our cost basis:

            %s

            Write the portal's own record. It will take an afternoon longer and it is the only
            version of this that cannot leak by accident: a field added to the staff DTO next
            year would otherwise appear on the customer's page with nobody deciding that it
            should.
            """.formatted(String.join("\n", offences)));
    }

    @Test
    @DisplayName("no portal source reads a value that is ours rather than the customer's")
    void thePortalDoesNotReadOurCost() throws IOException {
        List<String> offences = new ArrayList<>();

        for (Path file : clientFacingJava()) {
            scan(file, FORBIDDEN_VALUES, offences);
        }
        scan(SNAPSHOT, FORBIDDEN_VALUES, offences);

        assertTrue(offences.isEmpty(), () -> """
            A client-facing portal file reaches a value that is ours and not the customer's:

            %s

            Their own price is fine and is recomputed for them on every change. What they paid,
            what it is worth, what is included: all theirs. What we paid for it is not.
            """.formatted(String.join("\n", offences)));
    }

    @Test
    @DisplayName("no portal template prints our cost, our margin or our grading")
    void noPortalTemplatePrintsOurCost() throws IOException {
        Map<Pattern, String> rules = new LinkedHashMap<>(FORBIDDEN_VALUES);
        rules.put(Pattern.compile("\\bSTO\\b"),
            "the word STO — our pricing basis is not the customer's vocabulary");
        rules.put(Pattern.compile("\\b(MID_RANGE|MIDRANGE|ULTRA_LUXURY)\\b"),
            "an internal budget grading");
        rules.put(Pattern.compile("\\$\\{[^}]*\\.internalNotes\\b"),
            "an operational note");

        List<String> offences = new ArrayList<>();
        for (Path file : templates()) {
            scan(file, rules, offences);
        }

        assertTrue(offences.isEmpty(),
            () -> "A page a customer opens could print our cost:\n\n" + String.join("\n", offences));
    }

    /**
     * The snapshot stores no identifier either, which is a different rule for a related reason.
     *
     * <p>Pinned in {@code TheClientAndTheOfficeSeeTheSameChangesTest} because that is where the
     * reason lives. Named here so that somebody reading this file for "what may a snapshot
     * contain" is sent to the other half of the answer rather than concluding there is only one.
     */
    @Test
    @DisplayName("the snapshot's rules are in two files, and this one says where the other is")
    void theOtherHalfOfTheSnapshotRule() throws IOException {
        String other = Files.readString(Path.of("src/test/java/com/itineraryledger/"
            + "kabengosafaris/ClientPortal/TheClientAndTheOfficeSeeTheSameChangesTest.java"));
        assertTrue(other.contains("encodeId"),
            "the no-identifiers rule has moved or gone; this cross-reference is now a dead end");
    }

    // ------------------------------------------------------------------ the plumbing

    private void scan(Path file, Map<Pattern, String> rules, List<String> offences)
            throws IOException {
        String[] lines = withoutComments(Files.readString(file)).split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            for (Map.Entry<Pattern, String> rule : rules.entrySet()) {
                Matcher m = rule.getKey().matcher(lines[i]);
                if (m.find()) {
                    offences.add("%s:%d  '%s'  ->  %s"
                        .formatted(file.getFileName(), i + 1, m.group().trim(), rule.getValue()));
                    break;
                }
            }
        }
    }

    /**
     * Comments are blanked rather than stripped.
     *
     * <p>A comment explaining why a field is left out necessarily names the field, so a scan
     * that read them would report the explanation as the leak. Blanking keeps the line numbers
     * in a failure pointing at the right place.
     */
    private static String withoutComments(String source) {
        String blanked = blank(source, Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL));
        blanked = blank(blanked, Pattern.compile("<!--.*?-->", Pattern.DOTALL));
        return blank(blanked, Pattern.compile("//[^\n]*"));
    }

    private static String blank(String source, Pattern pattern) {
        Matcher m = pattern.matcher(source);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out,
                Matcher.quoteReplacement(m.group().replaceAll("[^\n]", " ")));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static List<Path> clientFacingJava() throws IOException {
        try (Stream<Path> files = Files.walk(PORTAL)) {
            return files.filter(f -> f.toString().endsWith(".java"))
                .filter(f -> !isTheOfficesSide(f))
                .sorted().toList();
        }
    }

    private static List<Path> templates() throws IOException {
        try (Stream<Path> files = Files.list(TEMPLATES)) {
            return files.filter(f -> f.toString().endsWith(".html")).sorted().toList();
        }
    }
}
