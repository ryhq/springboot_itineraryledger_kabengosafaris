package com.itineraryledger.kabengosafaris.Health;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Can this instance still read and write the files it owns?
 *
 * Every image, document, signature, email template and PDF template in this app is a file on disk
 * beside the database row that names it, so a data directory that is missing, read-only or full is
 * not a degraded feature — it is a bill whose invoice cannot be opened and a safari whose voucher
 * cannot be produced. Both of those turned up as opaque 500s during development, which is exactly
 * the failure this reports before anybody notices.
 *
 * It writes a probe file rather than trusting {@code Files.isWritable}: a full disk, an exhausted
 * inode table and a stale NFS handle all pass the permission check and fail the write.
 */
@Component("storage")
@Slf4j
public class StorageHealthIndicator implements HealthIndicator {

    /** The root every per-instance storage path hangs off. One company, one root. */
    @Value("${app.data.dir:./data}")
    private String dataDir;

    /** Below this, a deploy should be refused rather than filling the disk mid-upload. */
    @Value("${app.data.min-free-bytes:104857600}") // 100 MB
    private long minFreeBytes;

    @Override
    public Health health() {
        Path root = Paths.get(dataDir).toAbsolutePath().normalize();
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("path", root.toString());

        if (!Files.isDirectory(root)) {
            return Health.down().withDetails(detail)
                .withDetail("reason", "the data directory does not exist").build();
        }

        Path probe = root.resolve(".health-probe");
        try {
            Files.writeString(probe, "ok");
            Files.deleteIfExists(probe);
        } catch (IOException e) {
            /* the real reason matters: read-only mount, no inodes and no space all land here */
            return Health.down().withDetails(detail)
                .withDetail("reason", "cannot write to the data directory")
                .withDetail("error", e.getClass().getSimpleName() + ": " + e.getMessage())
                .build();
        }

        try {
            long free = Files.getFileStore(root).getUsableSpace();
            detail.put("freeBytes", free);
            detail.put("freeHuman", human(free));
            detail.put("minFreeBytes", minFreeBytes);
            if (free < minFreeBytes) {
                return Health.down().withDetails(detail)
                    .withDetail("reason", "less free space than an upload needs").build();
            }
        } catch (IOException e) {
            /* the write worked, so serving is fine; we just cannot say how much room is left */
            detail.put("freeBytes", "unknown");
        }

        return Health.up().withDetails(detail).build();
    }

    private static String human(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024d;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024d;
        if (mb < 1024) return String.format("%.1f MB", mb);
        return String.format("%.1f GB", mb / 1024d);
    }
}
