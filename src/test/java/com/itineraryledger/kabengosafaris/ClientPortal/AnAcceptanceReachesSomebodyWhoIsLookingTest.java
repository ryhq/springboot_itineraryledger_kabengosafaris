package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.NotificationSetting.NotificationSetting;

/**
 * The alert that must not be missed was being sent to the backup page.
 *
 * <p>Pressing Accept in the portal deliberately does not move the quotation's own status: that
 * path is audited and the audit row needs a real person. The email is therefore the only thing
 * that tells anybody a customer has said yes, and it was going to
 * {@code backup.notification.emails} — a setting that lives on a screen about backup scheduling,
 * scope, storage and retention.
 *
 * <p>Nobody deciding who should hear that a customer accepted a quotation would look there.
 * Twice, while writing the test instructions for this very feature, nobody did: once at Settings
 * then Company, once at Settings then Notification, which is the page it should have been on all
 * along. If the author of the feature cannot find the switch, the operator has no chance.
 *
 * <p>So the portal has its own entry beside the newsletter, booking-inquiry and contact-us lists,
 * which are the other three "somebody outside has done something and one of us needs to know".
 * This file holds it there.
 */
class AnAcceptanceReachesSomebodyWhoIsLookingTest {

    private static final Path NOTIFIER = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/ClientPortal/Services/PortalNotifier.java");
    private static final Path GETTERS = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/NotificationSetting/NotificationSettingGetterServices.java");
    private static final Path INITIALIZER = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/Initializers/NotificationSettingsInitializer.java");

    @Test
    @DisplayName("the portal's alerts are a notification setting, not a backup setting")
    void theOfficeAlertsComeFromTheNotificationSettings() throws IOException {
        String source = Files.readString(NOTIFIER);

        assertTrue(source.contains("getClientPortalNotificationEmails()"),
            "the addresses must come from the page an operator would actually open");
        assertFalse(source.toLowerCase(Locale.ROOT).contains("backupsettings"),
            "a customer's acceptance has no business being configured on the backup screen: "
                + "that is where this started and it is why nobody could find the switch");
    }

    @Test
    @DisplayName("it sits beside the other three alerts about somebody outside doing something")
    void itLivesWithItsRelatives() {
        List<String> categories = Arrays.stream(NotificationSetting.Category.values())
            .map(Enum::name).toList();

        assertTrue(categories.contains("CLIENT_PORTAL"));
        assertTrue(categories.containsAll(List.of("NEWSLETTER", "BOOKING_INQUIRY", "CONTACT_US")),
            "the company it keeps is the point: those three are the same kind of event");
    }

    @Test
    @DisplayName("both halves of the setting exist, or the tab is a switch that does nothing")
    void theSettingIsSeeded() throws IOException {
        String source = Files.readString(INITIALIZER);

        assertTrue(source.contains("\"notification.client_portal.enabled\""));
        assertTrue(source.contains("\"notification.client_portal.emails\""));
        assertTrue(source.contains("NotificationSetting.Category.CLIENT_PORTAL"),
            "without the category the rows exist and the page never shows them");
    }

    /**
     * An unconfigured installation still tells somebody.
     *
     * <p>The same fallback the bill-due reminders use, and for the same stated reason with more
     * at stake: an alert nobody receives is indistinguishable from an alert that was never
     * built, and this is the one that decides whether a customer who has agreed is noticed.
     */
    @Test
    @DisplayName("an empty list falls back to contact-us rather than to nobody")
    void silenceIsNotTheDefault() throws IOException {
        String source = Files.readString(GETTERS);
        int method = source.indexOf("public List<String> getClientPortalNotificationEmails()");
        assertTrue(method > 0, "the getter has been renamed");

        String body = source.substring(method, source.indexOf("\n    }", method));
        assertTrue(body.contains("getContactMessageNotificationEmails()"),
            "a brand-new installation would otherwise send a customer's acceptance nowhere, and "
                + "look exactly like one that is working");
    }

    @Test
    @DisplayName("switched off is said in the log, not confused with nobody being configured")
    void offAndEmptyAreDifferentSentences() throws IOException {
        String source = Files.readString(NOTIFIER);
        int method = source.indexOf("private void toTheOffice(");
        String body = source.substring(method, method + 1600);

        assertTrue(body.contains("isClientPortalNotificationEnabled()"),
            "a switch on the page that nothing reads is worse than no switch");
        assertTrue(body.contains("Settings > Notification"),
            "the warning is read by whoever has to fix it, so it should say where to go");
    }

    @Test
    @DisplayName("the backup list no longer claims to carry anything about customers")
    void theBackupListStaysOutOfIt() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/Initializers/BackupSettingsInitializer.java"));
        int at = source.indexOf("\"backup.notification.emails\",");
        String declaration = source.substring(at, at + 900);

        assertFalse(declaration.contains("client portal sends"),
            "pointing the portal at the backup list was the first fix and the wrong one");
        assertEquals(1, countOf(declaration, "Backups only"),
            "say what it is for, once and plainly");
    }

    private int countOf(String haystack, String needle) {
        int count = 0;
        int at = haystack.indexOf(needle);
        while (at >= 0) {
            count++;
            at = haystack.indexOf(needle, at + needle.length());
        }
        return count;
    }
}
