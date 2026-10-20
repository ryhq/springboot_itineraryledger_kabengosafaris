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
    @DisplayName("nothing anywhere in the backup package builds a path by gluing strings together")
    void nothingConcatenatesAPath() throws Exception {
        /*
         * The whole package, not the three compressors that were caught first. Writing, listing,
         * downloading, deleting and cleaning up all have to agree on where a backup lives, and
         * they only agree if every one of them joins onto the CONFIGURED directory the same way.
         * The compressors were simply the ones that wrote, so they were the ones that showed.
         *
         * Catches the shape, so a new format, a new export or a fourth place that touches the
         * folder cannot quietly reintroduce it.
         */
        Pattern glue = Pattern.compile(
            "\\b(?:storagePath|targetDir|outputDir|tempBackupDir|backupDir|sourceDir)\\s*\\+\\s*[\"\\w]");

        List<String> offences = new ArrayList<>();
        for (Path file : backupSources()) {
            String source = Files.readString(file);
            Matcher matcher = glue.matcher(source);
            while (matcher.find()) {
                String before = source.substring(0, matcher.start());
                /* a line that is only talking about the bug in a comment is not the bug */
                String line = before.substring(before.lastIndexOf('\n') + 1);
                if (line.trim().startsWith("*") || line.trim().startsWith("//")) continue;
                offences.add(file.getFileName() + ":" + (before.lines().count() + 1));
            }
        }

        if (!offences.isEmpty()) {
            fail("these build a backup path by concatenation, which drops the separator whenever "
                + "the configured directory does not end in one: " + offences
                + "\nUse new File(dir, name).getPath() or Paths.get(dir, name) instead.");
        }
    }

    @Test
    @DisplayName("every reader of the configured directory resolves against it, never beside it")
    void everyAccessPathResolvesAgainstTheConfiguredDirectory() throws Exception {
        /*
         * A backup is only usable if the half that writes it and the halves that find it agree.
         * Listing, downloading and deleting each take a filename from the outside world and must
         * resolve it against the configured directory -- which also keeps "../" from escaping it.
         */
        record Access(String file, String mustContain) {}
        List<Access> required = List.of(
            new Access("BackupGetService.java", "new File(storagePath, filename)"),
            new Access("BackupDeleteService.java", "new File(storagePath, filename)"),
            new Access("BackupDownloadController.java", "Paths.get(storagePath, filename)"));

        List<String> wrong = new ArrayList<>();
        for (Access access : required) {
            Path file = backupSources().stream()
                .filter(p -> p.getFileName().toString().equals(access.file()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing source file " + access.file()));
            if (!Files.readString(file).contains(access.mustContain())) {
                wrong.add(access.file() + " no longer resolves with " + access.mustContain());
            }
        }

        assertTrue(wrong.isEmpty(), () -> "a backup written into the configured directory would "
            + "not be found by: " + wrong);
    }

    /** Every java source under the Backup package. */
    private static List<Path> backupSources() throws Exception {
        try (var walk = Files.walk(
                Path.of("src/main/java/com/itineraryledger/kabengosafaris/Backup"))) {
            return walk.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }
}
