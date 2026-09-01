package com.itineraryledger.kabengosafaris.DataTransfer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import java.util.concurrent.Semaphore;
import org.springframework.http.HttpStatus;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.CannotAcquireLockException;
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
            java.nio.file.Path bundle = exportService.export(new LinkedHashSet<>(modules), includeImages);
            String fileName = "data-bundle-" + java.time.LocalDate.now() + ".zip";
            long size = java.nio.file.Files.size(bundle);

            /*
             * A plain Resource, written synchronously by Spring — NOT a StreamingResponseBody.
             *
             * The streaming version runs after the controller returns, on an async dispatch, which
             * put any failure there outside this try block entirely: the export kept answering 500
             * with the global handler's "An unexpected error occurred" and my specific message never
             * appeared. Whatever was wrong, being unable to see it was worse. A Resource is copied
             * before the method's frame is gone, so a fault here says what it was.
             *
             * Memory stays bounded either way — the bundle is a file on disk and Spring copies it
             * through a buffer. Content-Length is set so a large download shows real progress.
             */
            java.io.InputStream stream = new java.io.FilterInputStream(
                    java.nio.file.Files.newInputStream(bundle)) {
                @Override
                public void close() throws java.io.IOException {
                    super.close();
                    /* the bundle exists to be sent once; leaving it behind fills the disk slowly */
                    java.nio.file.Files.deleteIfExists(bundle);
                }
            };

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(size)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(new org.springframework.core.io.InputStreamResource(stream));
        } catch (Throwable e) {
            /*
             * Throwable, and the exception's CLASS in the message.
             *
             * Three rounds were spent on an export that answered 500 while every message reaching
             * the screen was somebody's generic sentence — the global handler's, then mine. This
             * endpoint is behind an export permission, so the person reading it is an administrator
             * of this company, and telling them "NullPointerException: this.maxAge is null" costs
             * nothing and saves a deploy. e.getMessage() is null for plenty of exceptions, which is
             * why the class name is not optional.
             */
            log.error("Could not build the export bundle", e);
            String detail = e.getClass().getSimpleName()
                + (e.getMessage() == null ? "" : ": " + e.getMessage());
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Could not build the bundle — " + detail, "EXPORT_FAILED"));
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

    /**
     * One bundle at a time.
     *
     * A preview does the whole import and rolls it back, so it holds row locks across every table
     * the bundle touches for as long as it runs — thousands of rates is tens of seconds. A second
     * request arriving in that window waits on those locks and dies at MySQL's lock wait timeout,
     * reporting an insert that was never the problem.
     *
     * Which is exactly what "Check this bundle" followed by "Import" does. Serialising is the fix:
     * two full-bundle transactions have nothing to gain from running at once, and the caller is
     * told to wait rather than handed a lock timeout.
     */
    private static final Semaphore ONE_BUNDLE_AT_A_TIME = new Semaphore(1);

    private ResponseEntity<ApiResponse<?>> handle(MultipartFile file, TransferMode mode, boolean previewOnly) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No bundle was uploaded.", "NO_BUNDLE"));
        }
        if (!ONE_BUNDLE_AT_A_TIME.tryAcquire()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(409,
                "Another bundle is being read right now. Checking a bundle writes every record and "
                    + "rolls it back, so it holds the same rows this one needs — wait for it to "
                    + "finish and try again.",
                "TRANSFER_IN_PROGRESS"));
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
        } catch (Throwable e) {
            log.error("Import failed", e);
            if (isLockContention(e)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(409,
                    "The import could not get at the records it needed — something else was "
                        + "holding them. Nothing was written. Wait a moment and try again.",
                    "TRANSFER_CONTENDED"));
            }
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "The import failed — " + describe(e), "IMPORT_FAILED"));
        } finally {
            ONE_BUNDLE_AT_A_TIME.release();
        }
    }

    /**
     * Whether this failure is two transactions competing rather than a bad bundle.
     *
     * Worth separating because the cure is "try again", which no amount of reading the SQL in the
     * message would suggest.
     */
    static boolean isLockContention(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof PessimisticLockingFailureException
                || cause instanceof CannotAcquireLockException) {
                return true;
            }
            String message = cause.getMessage();
            if (message != null && (message.contains("Lock wait timeout exceeded")
                || message.contains("Deadlock found"))) {
                return true;
            }
            if (cause.getCause() == cause) break;
        }
        return false;
    }

    /**
     * A failure, said in one line, without the statement.
     *
     * The detail matters — an import that fails silently is worse — but a parameterised INSERT
     * printed twice tells the office nothing and pushes the actual sentence off the screen. The
     * class name and the first line of the message are what a person can act on or quote.
     */
    static String describe(Throwable failure) {
        String message = failure.getMessage();
        if (message == null) return failure.getClass().getSimpleName();
        int statement = message.indexOf(" [insert into");
        if (statement < 0) statement = message.indexOf(" [update ");
        if (statement < 0) statement = message.indexOf("; SQL [");
        if (statement > 0) message = message.substring(0, statement);
        message = message.split("\\R")[0].trim();
        int cap = 300;
        if (message.length() > cap) message = message.substring(0, cap) + "…";
        return failure.getClass().getSimpleName() + ": " + message;
    }
}
