package com.itineraryledger.kabengosafaris.Backup;

import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSettingsGetterServices;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller for downloading backup files.
 *
 * <p>Streams the file straight from disk via {@link RandomAccessFile} so the
 * JVM never buffers the whole archive into memory, and honours
 * {@code Range: bytes=…} so browsers' built-in download managers — and
 * future chunked/resumable clients — can pause, resume, and retry without
 * re-fetching the entire backup.
 *
 * <p>Auth is the normal {@code Authorization: Bearer <JWT>} header. No
 * separate download token — staff with {@code PERM_DOWNLOAD_BACKUP} can
 * hit this endpoint with their session JWT directly.
 */
@RestController
@RequestMapping("/api/backups")
@RequiredArgsConstructor
@Slf4j
public class BackupDownloadController {

    /** Stream chunk size when copying from RandomAccessFile to the socket. */
    private static final int BUFFER_SIZE = 64 * 1024;

    private final BackupSettingsGetterServices backupSettings;

    @Value("${backup.storage.path:/opt/lampp/htdocs/kabengosafaris/backups/}")
    private String storagePath;

    @GetMapping("/download/{filename}")
    @PreAuthorize("hasAuthority('PERM_DOWNLOAD_BACKUP')")
    public void downloadBackup(
            @PathVariable String filename,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        log.info("Download request for backup file: {} (range={})", filename, rangeHeader);

        // 1. Filename + directory-traversal validation
        if (!isValidFilename(filename)) {
            log.warn("Invalid filename attempted: {}", filename);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }
        Path filePath = Paths.get(storagePath, filename).normalize();
        String backupDir = Paths.get(storagePath).normalize().toString();
        if (!filePath.toString().startsWith(backupDir)) {
            log.warn("Directory traversal attempt detected: {}", filename);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }
        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            log.warn("Backup file not found or not a file: {}", filename);
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        long fileSize = file.length();
        long start = 0;
        long end = fileSize - 1;
        boolean partial = false;

        // 2. Parse the Range header. RFC 7233 forms we honour:
        //   bytes=START-END   (closed range)
        //   bytes=START-      (open end → through EOF)
        //   bytes=-SUFFIX     (last SUFFIX bytes)
        // Multipart ranges (multiple comma-separated ranges) are intentionally
        // unsupported — they're rare in practice and a lot more code.
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String spec = rangeHeader.substring("bytes=".length()).trim();
            int dash = spec.indexOf('-');
            if (dash < 0 || spec.contains(",")) {
                send416(response, fileSize);
                return;
            }
            String startStr = spec.substring(0, dash).trim();
            String endStr = spec.substring(dash + 1).trim();
            try {
                if (startStr.isEmpty()) {
                    long suffix = Long.parseLong(endStr);
                    if (suffix <= 0) { send416(response, fileSize); return; }
                    start = Math.max(0, fileSize - suffix);
                    end = fileSize - 1;
                } else {
                    start = Long.parseLong(startStr);
                    end = endStr.isEmpty() ? fileSize - 1 : Long.parseLong(endStr);
                }
            } catch (NumberFormatException nfe) {
                send416(response, fileSize);
                return;
            }
            if (start < 0 || end >= fileSize || start > end) {
                send416(response, fileSize);
                return;
            }
            partial = true;
        }

        long contentLength = end - start + 1;

        // 3. Response headers — Accept-Ranges advertises range support for
        //    download managers; Content-Disposition forces a download dialog
        //    rather than inline rendering.
        response.setStatus(partial ? HttpStatus.PARTIAL_CONTENT.value() : HttpStatus.OK.value());
        response.setContentType(determineContentType(filename));
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getName() + "\"");
        response.setContentLengthLong(contentLength);
        if (partial) {
            response.setHeader(HttpHeaders.CONTENT_RANGE,
                    "bytes " + start + "-" + end + "/" + fileSize);
        }

        // 4. HEAD requests want the headers only — Spring will deliver them
        //    above. Stop before opening the file.
        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        // 5. Stream the slice straight from disk to the response socket.
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             OutputStream out = response.getOutputStream()) {
            raf.seek(start);
            byte[] buffer = new byte[BUFFER_SIZE];
            long remaining = contentLength;
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = raf.read(buffer, 0, toRead);
                if (read == -1) break;
                out.write(buffer, 0, read);
                remaining -= read;
            }
            out.flush();
        } catch (IOException e) {
            // Most common cause is the client disconnecting mid-download
            // (browser cancel, network drop). Log at debug — there is
            // nothing useful we can send to the client at this point because
            // the response headers have already been flushed.
            log.debug("Backup download stream closed for {}: {}", filename, e.getMessage());
        }
    }

    private void send416(HttpServletResponse response, long fileSize) {
        response.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize);
    }

    private boolean isValidFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) return false;
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) return false;
        if (filename.contains("\0")) return false;

        String expectedPrefix = backupSettings.getFilenamePrefix();
        if (!filename.startsWith(expectedPrefix)) return false;

        return filename.endsWith(".zip") || filename.matches(".*_\\d{8}_\\d{6}$");
    }

    private String determineContentType(String filename) {
        if (filename.endsWith(".zip")) return "application/zip";
        if (filename.endsWith(".tar.gz")) return "application/gzip";
        if (filename.endsWith(".sql")) return "application/sql";
        return "application/octet-stream";
    }
}
