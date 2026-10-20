package com.itineraryledger.kabengosafaris.Backup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Backup.Services.BackupService;

/**
 * A backup with no database in it is a copy of the templates.
 *
 * <p>{@code performBackup} used to call {@code result.setSuccess(true)} unconditionally, after
 * the database and file halves had each already recorded whether they worked. So mysqldump could
 * be refused outright, log {@code ✗ Database backup failed}, and the scheduler would log
 * {@code Scheduled backup completed successfully} four hundred milliseconds later. The failure
 * notification sits behind that same flag, so nobody was told either.
 *
 * <p>Both companies ran that way for over two months. Kabengo wrote a 138 MB archive every night
 * wrapped around a zero-byte {@code .sql} -- seven of them, all byte-identical in size, because
 * the only part that changes daily was the part that was empty. Jatelo wrote nothing at all. The
 * cause was mundane (MySQL error 1698: the configured user was {@code root}, which authenticates
 * by unix socket and cannot be reached with a password). The reason it survived two months was
 * this flag.
 *
 * <p>So the rule is: every half that is switched ON has to have produced something, or the whole
 * thing failed and says which part is missing.
 */
class AnIncompleteBackupIsNotASuccessTest {

    private static final Path SOURCE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Backup/Services/BackupService.java");

    @Test
    @DisplayName("success is never set without consulting the halves that ran")
    void successIsNotUnconditional() throws Exception {
        String source = Files.readString(SOURCE);

        /*
         * Catches the shape, not one line: setSuccess(true) with a hardcoded literal is the bug.
         * Computing it from the component flags is the fix, whatever the variable ends up called.
         */
        Matcher matcher = Pattern.compile("result\\.setSuccess\\(\\s*true\\s*\\)").matcher(source);
        List<String> offences = new ArrayList<>();
        while (matcher.find()) {
            int line = (int) source.substring(0, matcher.start()).lines().count() + 1;
            offences.add("BackupService.java:" + line);
        }

        if (!offences.isEmpty()) {
            fail("setSuccess(true) is hardcoded at " + offences
                + ", so a backup reports success no matter which half failed. Derive it from "
                + "isDatabaseBackupSuccess()/isFilesBackupSuccess() against what is enabled.");
        }
    }

    @Test
    @DisplayName("a failed half is checked against whether that half was switched on")
    void onlyEnabledHalvesAreRequired() throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("isDatabaseBackupEnabled() && !result.isDatabaseBackupSuccess()"),
            "a disabled database backup must not count as a failure, and an enabled one that "
            + "produced nothing must");
        assertTrue(source.contains("isFilesBackupEnabled() && !result.isFilesBackupSuccess()"),
            "same for the file half");
    }

    @Test
    @DisplayName("a partial archive is named so it cannot be mistaken for a whole one")
    void aPartialArchiveSaysSo() throws Exception {
        String source = Files.readString(SOURCE);
        assertTrue(source.contains("_PARTIAL"),
            "the archive from a half-failed run is kept, so it must carry the fact in its own "
            + "name -- somebody reaches for a backup at the worst possible moment and will not "
            + "be reading the logs");
    }

    @Test
    @DisplayName("the result object can express a half that worked and a half that did not")
    void theResultCarriesBothHalves() {
        BackupService.BackupResult result = new BackupService.BackupResult();

        result.setDatabaseBackupSuccess(false);
        result.setFilesBackupSuccess(true);
        assertFalse(result.isDatabaseBackupSuccess());
        assertTrue(result.isFilesBackupSuccess());

        /*
         * The default matters: a half that never ran because it threw must read as "did not
         * succeed", not as "fine". A boolean that defaults false is the whole reason the check
         * above can be trusted.
         */
        BackupService.BackupResult fresh = new BackupService.BackupResult();
        assertFalse(fresh.isDatabaseBackupSuccess(),
            "an untouched result must not claim the database half worked");
        assertFalse(fresh.isFilesBackupSuccess(),
            "an untouched result must not claim the file half worked");
    }
}
