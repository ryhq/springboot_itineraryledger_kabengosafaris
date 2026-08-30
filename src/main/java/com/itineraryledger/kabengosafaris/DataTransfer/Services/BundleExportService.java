package com.itineraryledger.kabengosafaris.DataTransfer.Services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    @Transactional(readOnly = true)
    public byte[] export(Set<String> requested, boolean includeImages) throws IOException {
        Set<String> chosen = expand(requested);
        List<TransferFile> files = new ArrayList<>();

        ObjectNode manifest = mapper.createObjectNode();
        manifest.put("schemaVersion", SCHEMA_VERSION);
        manifest.put("exportedAt", LocalDateTime.now().toString());
        manifest.put("sourceCompany", identity.snapshot().name());
        manifest.put("sourceVersion", buildCommit());
        manifest.put("includeImages", includeImages);
        ArrayNode moduleRows = manifest.putArray("modules");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {

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

            for (TransferFile file : files) {
                zip.putNextEntry(new ZipEntry(file.path()));
                zip.write(Files.readAllBytes(file.source()));
                zip.closeEntry();
            }

            /* last, so its counts are final — a reader takes the manifest, not the file order */
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest));
            zip.closeEntry();
        }

        log.info("Exported {} module(s){} for {}", chosen.size(),
            includeImages ? " with " + files.size() + " file(s)" : "", identity.snapshot().name());
        return out.toByteArray();
    }
}
