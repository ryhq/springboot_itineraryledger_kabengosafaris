package com.itineraryledger.kabengosafaris.Backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A backup that misses its own directory is not retained, and nothing says so.
 *
 * <p>The three compressors built their output path as {@code targetDir + backupName}, which is
 * right only while the configured storage path happens to end in a separator. The default
 * property does. The production override, {@code APP_BACKUP_DIR=/opt/<app>/backups}, does not. So
 * every nightly backup landed as a sibling of the directory with the two names run together:
 * {@code /opt/kabengosafaris/backupskabengosafaris_backup_20261020_020001.zip}.
 *
 * <p>The expensive part was not the ugly name. Retention scans the storage directory, and these
 * were never in it, so "maximum backups to retain" pruned nothing for two months: 65 files, 7.8 GB,
 * filling the disk to 100% until a deploy died with {@code tar: Wrote only 4608 of 10240 bytes}.
 * The setting that existed to prevent exactly this was looking in the right place at the wrong
 * files.
 *
 * <p>So this asserts the property the configuration cannot guarantee: wherever the storage path
 * points and however it is spelled, the file comes out INSIDE it.
 */
class ABackupLandsInsideItsOwnDirectoryTest {

    private static final Path SOURCE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/Backup/Services/BackupService.java");

    @Test
    @DisplayName("a storage path without a trailing separator still puts the file inside the directory")
    void theFileLandsInsideTheDirectory() {
        for (String directory : List.of("/opt/kabengosafaris/backups", "/opt/kabengosafaris/backups/")) {
            for (String extension : List.of(".zip", ".tar.gz", ".tar")) {
                String built = new File(directory, "kabengosafaris_backup_20261020_020001" + extension)
                    .getPath();

                assertEquals("backups", new File(built).getParentFile().getName(),
                    "built from \"" + directory + "\", the backup landed at " + built
                    + " instead of inside the backups directory");
                assertTrue(new File(built).getName().startsWith("kabengosafaris_backup_"),
                    "the directory name has been run together with the file name in " + built);
            }
        }
    }

    @Test
    @DisplayName("plain concatenation loses the separator, which is the bug this replaced")
    void concatenationIsWhatWentWrong() {
        String directory = "/opt/kabengosafaris/backups";
        String concatenated = directory + "kabengosafaris_backup_20261020_020001.zip";

        assertEquals("/opt/kabengosafaris", new File(concatenated).getParent(),
            "this is the shape that caused it: the file becomes a sibling of the directory it "
            + "was meant to go in, so retention never sees it");
    }

    @Test
    @DisplayName("no compressor builds its output path by gluing strings together")
    void noCompressorConcatenatesItsPath() throws Exception {
        String source = Files.readString(SOURCE);

        /*
         * Catches the shape rather than the three known lines, so a fourth compression format
         * added later cannot quietly reintroduce it.
         */
        Matcher matcher = Pattern.compile(
            "String\\s+\\w*(?:[Ff]ile)?Path\\s*=\\s*targetDir\\s*\\+").matcher(source);

        List<String> offences = new ArrayList<>();
        while (matcher.find()) {
            int line = (int) source.substring(0, matcher.start()).lines().count() + 1;
            offences.add("BackupService.java:" + line);
        }

        if (!offences.isEmpty()) {
            fail("these build a backup path by concatenating targetDir, which drops the separator "
                + "whenever the configured path does not end in one: " + offences
                + "\nUse new File(targetDir, name).getPath() instead.");
        }
    }
}
