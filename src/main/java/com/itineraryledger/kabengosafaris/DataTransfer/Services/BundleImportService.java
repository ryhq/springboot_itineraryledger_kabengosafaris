package com.itineraryledger.kabengosafaris.DataTransfer.Services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.DataTransfer.ModuleTransfer;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferContext;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferMode;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferReport;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reading a bundle into this company.
 *
 * The preview is the part worth explaining. It does not describe what an import WOULD do — it runs
 * the import, collects the report, and rolls the transaction back. That is deliberate, and it is the
 * lesson from a fault earlier in this codebase where a "show me what will be sent" path had its own
 * substitution loop and therefore showed something no customer would ever receive. A preview written
 * as a second implementation is a second implementation to keep in step, and it will drift; a
 * preview that executes the real one cannot be wrong about it.
 *
 * The cost is honest and small: the work is done twice when somebody goes on to import. For a rate
 * sheet that is a second or two, and it buys a report that is true.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BundleImportService {

    private final List<ModuleTransfer> modules;
    private final ObjectMapper mapper;

    /** A bundle, unpacked and understood, before anything is written. */
    @Getter
    public static class Bundle {
        private final JsonNode manifest;
        private final Map<String, JsonNode> data;
        private final Path files;

        Bundle(JsonNode manifest, Map<String, JsonNode> data, Path files) {
            this.manifest = manifest;
            this.data = data;
            this.files = files;
        }
    }

    /**
     * Unpack a zip into memory, and its files into a scratch directory.
     *
     * Entry names are checked rather than trusted. A bundle is a file somebody was handed, and a zip
     * entry called `../../etc/anything` is a well-known way to write outside the directory you meant
     * — old enough to have a name, and still worth the four lines.
     */
    public Bundle read(InputStream source) throws IOException {
        Path scratch = Files.createTempDirectory("bundle-");
        Map<String, JsonNode> data = new LinkedHashMap<>();
        JsonNode manifest = null;

        try (ZipInputStream zip = new ZipInputStream(source)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();

                if ("manifest.json".equals(name)) {
                    manifest = mapper.readTree(zip.readAllBytes());
                } else if (name.startsWith("data/") && name.endsWith(".json")) {
                    String module = name.substring("data/".length(), name.length() - ".json".length());
                    data.put(module, mapper.readTree(zip.readAllBytes()));
                } else if (name.startsWith("files/")) {
                    Path target = scratch.resolve(name).normalize();
                    if (!target.startsWith(scratch)) {
                        throw new IllegalArgumentException(
                            "The bundle contains a file path that points outside it: " + name);
                    }
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target);
                }
            }
        }

        if (manifest == null) {
            throw new IllegalArgumentException(
                "That file has no manifest, so it is not an export from this system.");
        }
        int version = manifest.path("schemaVersion").asInt(0);
        if (version > BundleExportService.SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                "This bundle was written by a newer version (schema " + version + ", this one reads "
                    + BundleExportService.SCHEMA_VERSION + "). Update this installation first — "
                    + "importing it here would silently drop whatever is new.");
        }
        return new Bundle(manifest, data, scratch);
    }

    /**
     * Run it and throw the result away, so the report is the truth rather than a guess.
     *
     * Rolled back by marking the transaction rather than by throwing, so the report survives — an
     * exception would take it with it, and the report is the entire product of a preview.
     */
    @Transactional
    public TransferReport preview(Bundle bundle, TransferMode mode) {
        TransferReport report = run(bundle, mode);
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        log.info("Previewed an import: {} record(s) would be written, {} unresolved",
            report.totalWritten(), report.totalUnresolved());
        return report;
    }

    /**
     * The real thing.
     *
     * One transaction for the whole bundle: a half-imported rate sheet — the lodges in, their rates
     * not — is harder to reason about than one that plainly did not happen. Per-record faults do NOT
     * roll it back; they are reported, because one lodge naming a season this company has not got is
     * not a reason to reject the other three hundred.
     */
    @Transactional
    public TransferReport importBundle(Bundle bundle, TransferMode mode) {
        TransferReport report = run(bundle, mode);
        log.info("Imported {} record(s), {} unresolved", report.totalWritten(), report.totalUnresolved());
        return report;
    }

    private TransferReport run(Bundle bundle, TransferMode mode) {
        TransferReport report = new TransferReport();
        boolean hasFiles = bundle.getFiles() != null && Files.exists(bundle.getFiles().resolve("files"));

        TransferContext context = new TransferContext(mode, report,
            hasFiles ? bundle.getFiles() : null, hasFiles);

        /*
         * Dependency order, always. The bundle's own file order is not to be trusted — a zip can be
         * rebuilt by hand — and a rate read before its season is a rate that cannot be placed.
         */
        for (ModuleTransfer module : modules.stream()
                .sorted(Comparator.comparingInt(ModuleTransfer::order)).toList()) {
            JsonNode data = bundle.getData().get(module.name());
            if (data == null) continue;
            module.importInto(data, context);
        }
        return report;
    }

    /** What is in the bundle, without reading a single record into the company. */
    public Map<String, Object> describe(Bundle bundle) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("manifest", bundle.getManifest());
        Map<String, Integer> counts = new LinkedHashMap<>();
        bundle.getData().forEach((name, node) -> counts.put(name, node.size()));
        summary.put("records", counts);
        return summary;
    }
}
