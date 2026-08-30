package com.itineraryledger.kabengosafaris.DataTransfer.Services;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService;
import com.itineraryledger.kabengosafaris.DataTransfer.ModuleTransfer;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Writing a bundle: a zip somebody can read before it touches a live company.
 *
 * A file rather than a direct connection between the two installations, deliberately. Each company
 * runs its own process against its own database behind its own CORS allow-list, and the point of all
 * that is that neither can reach the other; an export that opened a channel between them would be
 * undoing it for convenience. A file also leaves something to inspect, keep and re-run.
 *
 * The manifest names the source and the commit that wrote it, so a bundle found on a disk in six
 * months can still be identified.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BundleExportService {

    /** Bumped when the SHAPE changes in a way an older reader would get wrong. */
    public static final int SCHEMA_VERSION = 1;

    private final List<ModuleTransfer> modules;
    private final CompanyIdentityService identity;
    private final ObjectMapper mapper;

    /**
     * Which build wrote the bundle. Absent when the jar was assembled without the build-info goal,
     * which is the usual case running from an IDE — the same source the version endpoint reads.
     */
    private final java.util.Optional<org.springframework.boot.info.BuildProperties> buildProperties;

    private String buildCommit() {
        return buildProperties
            .map(build -> build.get("sha") == null ? "unknown" : build.get("sha"))
            .orElse("unknown");
    }

    /** What can be exported, for a picker. Supporting modules are not choices. */
    public List<Map<String, Object>> catalogue() {
        return modules.stream()
            .sorted(Comparator.comparingInt(ModuleTransfer::order))
            .filter(m -> !m.isSupporting())
            .map(m -> Map.<String, Object>of(
                "name", m.name(),
                "label", m.label(),
                "count", m.count(),
                "detail", m.detail(),
                "requires", m.requires()))
            .toList();
    }

    /**
     * Everything the chosen modules need, including what they did not ask for.
     *
     * Somebody exporting "parks" means the parks and their rates; they are not thinking about the
     * tariff catalogue or the guest categories, and a bundle without those is a price list whose
     * every line is unplaceable. So the dependencies come along, and the manifest says they did.
     */
    Set<String> expand(Set<String> requested) {
        Set<String> resolved = new LinkedHashSet<>();
        for (ModuleTransfer module : sorted()) {
            if (!requested.contains(module.name())) continue;
            resolved.addAll(module.requires());
            resolved.add(module.name());
        }
        /* a second pass, since a dependency may have dependencies of its own */
        for (ModuleTransfer module : sorted()) {
            if (resolved.contains(module.name())) resolved.addAll(module.requires());
        }
        return resolved;
    }

    private List<ModuleTransfer> sorted() {
        return modules.stream().sorted(Comparator.comparingInt(ModuleTransfer::order)).toList();
    }

    /**
     * Build the bundle into a temporary FILE, and hand back the path.
     *
     * Not a byte[], which is what the first version did and what failed on the first real export:
     * a company with 69 lodges and their galleries produces a zip far larger than it is sensible to
     * hold in memory, twice — once as the zip being built and again as the response body. Written to
     * disk it is bounded by disk.
     *
     * Not streamed straight to the response either, tempting as that is. Everything here is read
     * lazily inside this transaction, and a StreamingResponseBody runs AFTER the controller returns,
     * with the transaction closed and every lazy relation dead. A temp file is the honest middle:
     * built while the data is reachable, streamed once it is complete, deleted by the caller.
     */
    @Transactional(readOnly = true)
    public java.nio.file.Path export(Set<String> requested, boolean includeImages) throws IOException {
        Set<String> chosen = expand(requested);
        List<TransferFile> files = new ArrayList<>();

        ObjectNode manifest = mapper.createObjectNode();
        manifest.put("schemaVersion", SCHEMA_VERSION);
        manifest.put("exportedAt", LocalDateTime.now().toString());
        manifest.put("sourceCompany", identity.snapshot().name());
        manifest.put("sourceVersion", buildCommit());
        manifest.put("includeImages", includeImages);
        ArrayNode moduleRows = manifest.putArray("modules");

        java.nio.file.Path bundle = java.nio.file.Files.createTempFile("bundle-", ".zip");
        try (OutputStream out = java.nio.file.Files.newOutputStream(bundle);
             ZipOutputStream zip = new ZipOutputStream(out)) {

            for (ModuleTransfer module : sorted()) {
                if (!chosen.contains(module.name())) continue;

                JsonNode data = module.export(includeImages, files);
                byte[] json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);

                zip.putNextEntry(new ZipEntry("data/" + module.name() + ".json"));
                zip.write(json);
                zip.closeEntry();

                ObjectNode row = moduleRows.addObject();
                row.put("name", module.name());
                row.put("label", module.label());
                row.put("records", data.size());
                row.put("askedFor", requested.contains(module.name()));
            }

            /*
             * Copied through a buffer rather than read whole: a bundle's files are the large part,
             * and readAllBytes on each one puts the biggest image in the gallery into memory for no
             * reason. Duplicate names are skipped rather than thrown on — two owners can legitimately
             * reference one stored file, and a ZipException there would lose the entire export.
             */
            java.util.Set<String> written = new java.util.HashSet<>();
            for (TransferFile file : files) {
                if (!written.add(file.path())) continue;
                try {
                    zip.putNextEntry(new ZipEntry(file.path()));
                    Files.copy(file.source(), zip);
                    zip.closeEntry();
                } catch (IOException e) {
                    log.warn("Left '{}' out of the bundle: {}", file.path(), e.getMessage());
                }
            }

            /* last, so its counts are final — a reader takes the manifest, not the file order */
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest));
            zip.closeEntry();
        }

        log.info("Exported {} module(s){} for {} — {} KB", chosen.size(),
            includeImages ? " with " + files.size() + " file(s)" : "", identity.snapshot().name(),
            java.nio.file.Files.size(bundle) / 1024);
        return bundle;
    }
}
