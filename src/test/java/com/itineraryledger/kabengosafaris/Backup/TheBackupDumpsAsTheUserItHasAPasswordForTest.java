package com.itineraryledger.kabengosafaris.Backup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A credential is a pair, and this one was assembled from two different places.
 *
 * <p>{@code BackupService} injects the password straight from
 * {@code spring.datasource.password} and takes the username from a row in the database. Those
 * two can disagree, and they did: the stored row said {@code root}, seeded from an old hardcoded
 * default before the properties file named a user, and never revisited because
 * {@code createOrUpdateSetting} skips keys that already exist. So every nightly run handed
 * mysqldump the application user's password alongside the username {@code root}.
 *
 * <p>That cannot authenticate under any configuration. On these installs it surfaced as MySQL
 * error 1698 -- root authenticates by unix socket, so a password connection is refused outright
 * -- but a plain wrong-password rejection would have been just as fatal. Two companies ran with
 * no database backup for over two months.
 *
 * <p>What is pinned here is the rule, not the incident: the username the backup dumps as has to
 * default to the user whose password it is about to send.
 */
class TheBackupDumpsAsTheUserItHasAPasswordForTest {

    private static final Path INITIALIZER = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Initializers/BackupSettingsInitializer.java");
    private static final Path SERVICE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Backup/Services/BackupService.java");
    private static final Path PROPERTIES = Path.of("src/main/resources/application.properties");

    @Test
    @DisplayName("the backup username defaults to the datasource user, never to a bare root")
    void theUsernameDefaultsToTheDatasourceUser() throws Exception {
        String source = Files.readString(INITIALIZER);

        assertFalse(source.contains("${backup.database.username:root}"),
            "defaulting to root pairs a username with a password that is not its own; it cannot "
            + "authenticate and the failure is silent until somebody needs the backup");
        assertTrue(source.contains("${backup.database.username:${spring.datasource.username:root}}"),
            "the username must fall back to the user whose password BackupService sends");
    }

    @Test
    @DisplayName("the password still comes from the datasource, so the pair stays together")
    void thePasswordIsTheDatasourcePassword() throws Exception {
        assertTrue(Files.readString(SERVICE).contains("${spring.datasource.password:}"),
            "if this ever becomes a separately configurable secret, the username default above "
            + "stops being the right one and both have to move together");
    }

    @Test
    @DisplayName("a stored username that cannot authenticate is repaired, not left to fail nightly")
    void theStaleRootRowIsRepaired() throws Exception {
        String source = Files.readString(INITIALIZER);

        assertTrue(source.contains("repairUnusableDatabaseUsername"),
            "createOrUpdateSetting only creates, so a bad value seeded once survives every "
            + "restart and every release; it has to be corrected in place");
        assertTrue(source.contains("!databaseUsername.equals(setting.getSettingValue())"),
            "the repair must compare the stored name against the resolved one rather than "
            + "against a single known-bad literal. It first matched only \"root\", and missed "
            + "Jatelo entirely, where the seeded name was \"kabengosafaris\" and just as unusable");
    }

    @Test
    @DisplayName("the shipped properties take the backup username from the datasource, not a second variable")
    void thePropertiesAgree() throws Exception {
        String properties = Files.readString(PROPERTIES);

        assertTrue(properties.contains("backup.database.username=${BACKUP_DB_USERNAME:${spring.datasource.username}}"),
            "the username must be derived from the datasource property itself. Keyed off "
            + "DB_USERNAME it was right only on an install that happened to use that variable: "
            + "Jatelo sets SPRING_DATASOURCE_USERNAME, so DB_USERNAME was unset and the backup "
            + "seeded the hardcoded fallback, a user that exists on the other company");
        assertFalse(properties.contains("backup.database.username=${DB_USERNAME:"),
            "a second variable for one half of a credential is how the two halves drifted apart");
    }
}
