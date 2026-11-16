package com.itineraryledger.kabengosafaris.Safari;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Who hears about a safari, and how many times.
 *
 * <p>Two defects, one cause. Every safari operations alert, readiness, payment gap, started,
 * completed and post-trip, was addressed from {@code BackupSettingsGetterServices}: the list on a
 * page about database dumps and retention. The only way to stop them was to empty that list, which
 * also switched off the backup failure warning. {@code NotificationSetting.Category.CLIENT_PORTAL}
 * was created to end exactly this arrangement for the portal alerts and says so in its own
 * comment; the safari ones were left behind.
 *
 * <p>And they repeated. The readiness check swept days 7 down to 3, then 2 and 1, re-sending the
 * same unresolved list on all seven mornings. The post-trip reminder fired on each of the seven
 * days after a trip ended, carrying a fixed task list it never checked. Worst of all, the payment
 * gap query was {@code startDate <= today} with no lower bound and nothing recording that it had
 * already spoken, and a safari in those states cannot leave them on its own: auto-start runs from
 * FULLY_PAID, auto-complete from IN_PROGRESS, auto-close from COMPLETED. One booking nobody marked
 * paid therefore produced a mail subject-lined CRITICAL every morning, indefinitely.
 *
 * <p>On Kabengo, 384 of 3,937 Resend events were bounces, 248 of them the nightly backup success
 * mail to one full mailbox, on the same sending identity that carries quotes and invoices to
 * clients. That is what a channel nobody can turn down costs.
 */
class AnAlertThatRepeatsForeverIsNotAnAlertTest {

    private static final Path PHASE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Safari/Services/"
            + "SafariPhaseAlertScheduler.java");
    private static final Path LIFECYCLE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Safari/Services/"
            + "SafariLifecycleScheduler.java");
    private static final Path REPOSITORY = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Safari/Repository/SafariRepository.java");
    private static final Path CATEGORY = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/NotificationSetting/"
            + "NotificationSetting.java");
    private static final Path GETTERS = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/NotificationSetting/"
            + "NotificationSettingGetterServices.java");

    @Test
    @DisplayName("safari alerts are not addressed from the backup settings")
    void theBackupListDoesNotDecideWhoHearsAboutATrip() throws IOException {
        for (Path scheduler : List.of(PHASE, LIFECYCLE)) {
            String body = Files.readString(scheduler);

            assertFalse(body.contains("BackupSettingsGetterServices"),
                scheduler.getFileName() + " still asks the backup settings who should hear about "
                    + "a safari. Nobody deciding that would look on a page about database dumps, "
                    + "and emptying the list to stop these alerts also silences backup failures.");
            assertTrue(body.contains("getSafariOperationsNotificationEmails()"),
                scheduler.getFileName() + " must address safari alerts from the SAFARI_OPERATIONS "
                    + "notification settings");
            assertTrue(body.contains("isSafariOperationsNotificationEnabled()"),
                scheduler.getFileName() + " must be switchable without emptying an address list");
        }
    }

    @Test
    @DisplayName("the payment gap is asked about on named days, never open-endedly")
    void thePaymentGapQueryHasALowerBound() throws IOException {
        String repository = Files.readString(REPOSITORY);

        assertFalse(repository.contains("s.startDate <= :today AND s.state IN :unpaidStates"),
            "an open-ended payment gap query reports the same safari every morning forever: it "
                + "can never leave those states, because auto-start runs from FULLY_PAID");
        assertTrue(repository.contains("s.startDate = :startDate AND s.state IN :unpaidStates"),
            "the payment gap must be asked about one start date at a time");

        String lifecycle = Files.readString(LIFECYCLE);
        assertTrue(lifecycle.contains("getSafariPaymentGapDays()"),
            "the days the payment gap speaks on must be configuration, not a window in the code");
    }

    @Test
    @DisplayName("readiness and the post-trip reminder speak on days, not across windows")
    void nothingSweepsAWindowAnyMore() throws IOException {
        String phase = Files.readString(PHASE);

        assertFalse(phase.contains("today.plusDays(3), today.plusDays(7)"),
            "sweeping days 7 to 3 re-sends the same unresolved list on five mornings");
        assertFalse(phase.contains("today.minusDays(7), today.minusDays(1)"),
            "the post-trip reminder carries a fixed task list it never checks, so the second copy "
                + "says nothing the first did not");

        assertTrue(phase.contains("getSafariReadinessLeadDays()"), "readiness days are configured");
        assertTrue(phase.contains("getSafariPostTripDays()"), "post-trip days are configured");
    }

    @Test
    @DisplayName("an empty day list silences that channel rather than restoring its default")
    void blankMeansSilent() throws IOException {
        String getters = Files.readString(GETTERS);

        int parser = getters.indexOf("private List<Integer> dayList(");
        assertTrue(parser >= 0, "the shared day-list parser is gone");

        String body = getters.substring(parser);
        assertTrue(body.contains("if (configured.trim().isEmpty()) return Collections.emptyList();"),
            "clearing the field is how an operator switches a channel off. Filling it back in with "
                + "the default, as the bill-due reminder deliberately does, would make that "
                + "impossible here.");
    }

    @Test
    @DisplayName("safari operations is a notification category of its own")
    void theCategoryExists() throws IOException {
        assertTrue(Files.readString(CATEGORY).contains("SAFARI_OPERATIONS("),
            "the alerts need a home beside the other things somebody outside has done");
    }
}
