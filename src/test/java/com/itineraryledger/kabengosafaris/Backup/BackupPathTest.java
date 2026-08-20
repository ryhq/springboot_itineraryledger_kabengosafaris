package com.itineraryledger.kabengosafaris.Backup;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * That a backup can be OPENED, not merely listed.
 *
 * The configured storage path may or may not end in a separator — an environment file written by hand
 * usually does not — and this code glued directory to filename with `+`. Listing kept working, because
 * a directory path is valid without its trailing slash, so six backups appeared on screen and every
 * one of them answered 404 when opened. A list that lies is worse than an empty one.
 */
class BackupPathTest {

    @Test
    @DisplayName("a file is found whether or not the configured directory ends in a slash")
    void separatorIsNotEveryCallSitesProblem(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("backup_20260818_020001.zip"), "x");

        for (String configured : List.of(dir.toString(), dir + "/", dir + "//")) {
            /* what the services do now */
            assertTrue(new java.io.File(configured, "backup_20260818_020001.zip").exists(),
                "not found with the path configured as '" + configured + "'");

            /* and what they used to do, kept here so nobody 'fixes' this in config instead */
            if (!configured.endsWith("/")) {
                assertFalse(new java.io.File(configured + "backup_20260818_020001.zip").exists(),
                    "concatenation cannot work without a trailing slash — that was the bug");
            }
        }
    }

    @Test
    @DisplayName("no backup code builds a path by gluing strings together")
    void noConcatenatedPaths() throws IOException {
        Path backupPackage = Paths.get("src/main/java/com/itineraryledger/kabengosafaris/Backup");
        List<String> offences = new ArrayList<>();

        try (Stream<Path> files = Files.walk(backupPackage)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    String trimmed = line.trim();
                    if (trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")) continue;
                    if (line.matches(".*(storagePath|backupDir)\\s*\\+\\s*(filename|fileName|backupFileName).*")) {
                        offences.add(backupPackage.relativize(file) + ":" + (i + 1) + "  " + trimmed);
                    }
                }
            }
        }

        assertTrue(offences.isEmpty(), () -> """
            a path is being built by concatenation again:

            %s

            Use new File(dir, name) or Paths.get(dir, name). Whether the configured directory ends in a
            separator is not something every call site should have to know.
            """.formatted(String.join("\n", offences)));
    }
}
