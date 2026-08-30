package com.itineraryledger.kabengosafaris.DataTransfer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.DataTransfer.Services.BundleExportService;
import com.itineraryledger.kabengosafaris.DataTransfer.Services.BundleImportService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Moving reference data from one company to another.
 *
 * Inventory only — parks, activities, accommodations, the rates for each, and the small tables those
 * rates are made of. Nothing that belongs to a customer can leave: no customers, quotes, invoices,
 * payments, safaris, users or mail accounts, and that is enforced by the module list rather than by
 * anybody remembering (see DataTransferSurfaceTest).
 *
 * The shape of the import is preview-then-commit, and the preview is not a description of what would
 * happen — it runs the import and rolls it back. Somebody is pointing this at a company that is
 * already trading, and the only report worth trusting is one produced by the code that does the work.
 *
 * Permissions are matched by NAME against the catalogue, so EXPORT_DATA_BUNDLE and IMPORT_DATA_BUNDLE
 * are declared in custom-permissions.json — a name the catalogue does not know refuses everybody,
 * superadmin included.
 */
@RestController
@RequestMapping("/api/data-transfer")
@RequiredArgsConstructor
@Slf4j
public class DataTransferController {

    private final BundleExportService exportService;
    private final BundleImportService importService;

    /** What can be exported, and how much of it there is. */
    @GetMapping("/modules")
    @PreAuthorize("hasAuthority('PERM_EXPORT_DATA_BUNDLE')")
    public ResponseEntity<ApiResponse<?>> modules() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("modules", exportService.catalogue());
        return ResponseEntity.ok(ApiResponse.success(200, "Exportable modules retrieved successfully", payload));
    }

    /**
     * Write a bundle.
     *
     * A download rather than JSON, because the thing being produced is a file: it gets kept, mailed,
     * inspected, and imported later, possibly by somebody else.
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('PERM_EXPORT_DATA_BUNDLE')")
    public ResponseEntity<?> export(@RequestParam Set<String> modules,
                                    @RequestParam(defaultValue = "false") boolean includeImages) {
        try {
            byte[] bundle = exportService.export(new LinkedHashSet<>(modules), includeImages);
            String fileName = "data-bundle-" + java.time.LocalDate.now() + ".zip";

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(bundle);
        } catch (Exception e) {
            log.error("Could not build the export bundle", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Could not build the bundle: " + e.getMessage(), "EXPORT_FAILED"));
        }
    }

    /**
     * What this bundle WOULD do, produced by doing it and rolling back.
     *
     * Separate from the import rather than a flag on it, so that reaching the write endpoint is
     * always a deliberate second act.
     */
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_IMPORT_DATA_BUNDLE')")
    public ResponseEntity<ApiResponse<?>> preview(@RequestPart("file") MultipartFile file,
                                                  @RequestParam(defaultValue = "SKIP") TransferMode mode) {
        return handle(file, mode, true);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, value = "/import")
    @PreAuthorize("hasAuthority('PERM_IMPORT_DATA_BUNDLE')")
    public ResponseEntity<ApiResponse<?>> importBundle(@RequestPart("file") MultipartFile file,
                                                       @RequestParam(defaultValue = "SKIP") TransferMode mode) {
        return handle(file, mode, false);
    }

    private ResponseEntity<ApiResponse<?>> handle(MultipartFile file, TransferMode mode, boolean previewOnly) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No bundle was uploaded.", "NO_BUNDLE"));
        }
        try (var stream = file.getInputStream()) {
            BundleImportService.Bundle bundle = importService.read(stream);
            TransferReport report = previewOnly
                ? importService.preview(bundle, mode)
                : importService.importBundle(bundle, mode);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("manifest", bundle.getManifest());
            payload.put("mode", mode.name());
            payload.put("preview", previewOnly);
            payload.put("written", report.totalWritten());
            payload.put("unresolved", report.totalUnresolved());
            payload.put("modules", report.asRows());

            String message = previewOnly
                ? "Nothing was written — this is what the bundle would do"
                : "Bundle imported";
            return ResponseEntity.ok(ApiResponse.success(200, message, payload));

        } catch (IllegalArgumentException e) {
            /* a bad bundle is the caller's problem and the message says what is wrong with it */
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, e.getMessage(), "BAD_BUNDLE"));
        } catch (Exception e) {
            log.error("Import failed", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "The import failed: " + e.getMessage(), "IMPORT_FAILED"));
        }
    }
}
