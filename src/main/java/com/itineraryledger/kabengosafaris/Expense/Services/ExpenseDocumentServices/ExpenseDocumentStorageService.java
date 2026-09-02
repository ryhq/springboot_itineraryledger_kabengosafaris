package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseDocumentServices;

import com.itineraryledger.kabengosafaris.FileSettings.FileSettingGetterServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * File-storage service for expense documents. Mirrors
 * InvoiceDocumentStorageService verbatim — same hashing, same validation
 * pipeline (FileSettingGetterServices), same MIME table, same hashed-filename
 * scheme. Different on-disk root only.
 */
@Service
@Slf4j
public class ExpenseDocumentStorageService {

    /* The nested default matters: if the key is ever missing from the properties again, the files
     * still land under the data root instead of a release-relative ./data that nothing tests. */
    @Value("${expense.document.storage.path:${app.data.dir:./data}/expense-documents/}")
    private String storagePath;

    @Value("${app.base.url:http://localhost:4450}")
    private String appBaseUrl;

    @Autowired
    private FileSettingGetterServices fileSettingGetterServices;

    /**
     * Creates the directory and proves it can be written to.
     *
     * <p>The old version said only "Failed to initialize expense document storage directory",
     * which named neither the path it tried nor why the filesystem refused -- and a relative
     * path means the answer depends on the service's working directory, so there was nothing to
     * go on from the panel. Both now travel in the message: whoever sees the toast sees the
     * absolute path and the refusal.
     *
     * <p>It probes with a real file rather than trusting {@code Files.isWritable}: a directory
     * that exists but belongs to another account, a read-only mount and a full disk all pass
     * that check and then fail at the copy, where the failure used to become a silent null.
     */
    public void initializeStorageDirectory() {
        Path path = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException(refusal("create the expense document directory", path, e), e);
        }

        Path probe = path.resolve(".write-probe");
        try {
            Files.writeString(probe, "ok");
        } catch (IOException e) {
            throw new RuntimeException(refusal("write to the expense document directory", path, e), e);
        } finally {
            try {
                Files.deleteIfExists(probe);
            } catch (IOException ignored) {
                /* a probe we could write but not remove is not a reason to refuse an upload */
            }
        }
    }

    private String refusal(String what, Path path, Exception cause) {
        String detail = cause.getMessage() == null ? cause.getClass().getSimpleName()
                : cause.getClass().getSimpleName() + ": " + cause.getMessage();
        log.error("Could not {} {}", what, path, cause);
        return "Could not " + what + " " + path + " (" + detail + ")";
    }

    public String validateDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) return "No file provided";
        return fileSettingGetterServices.validateFileUpload(file.getOriginalFilename(), file.getSize());
    }

    public String validateRequestSize(long totalSize) {
        Long maxRequestSize = fileSettingGetterServices.getMaxRequestSize();
        if (totalSize > maxRequestSize) {
            return String.format("Total request size (%s) exceeds maximum allowed size (%s)",
                formatFileSize(totalSize),
                fileSettingGetterServices.getMaxRequestSizeString());
        }
        return null;
    }

    public Long getMaxFileSize()        { return fileSettingGetterServices.getMaxFileSize(); }
    public String getMaxFileSizeString(){ return fileSettingGetterServices.getMaxFileSizeString(); }
    public Long getMaxRequestSize()     { return fileSettingGetterServices.getMaxRequestSize(); }
    public String getMaxRequestSizeString() { return fileSettingGetterServices.getMaxRequestSizeString(); }
    public String getAllowedExtensionsString() { return fileSettingGetterServices.getAllowedExtensionsString(); }
    public String getStoragePath()      { return storagePath; }
    public String getBaseUrl()          { return appBaseUrl; }

    public String saveDocument(MultipartFile file) {
        try {
            initializeStorageDirectory();
            String validationError = validateDocument(file);
            if (validationError != null) {
                log.warn("Expense document validation failed: {}", validationError);
                return null;
            }

            String originalFilename = file.getOriginalFilename();
            String extension = getExtension(originalFilename);
            String generatedFilename = generateHashedFilename(originalFilename, extension);

            Path targetPath = Paths.get(storagePath, generatedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Expense document saved: {} (original: {})", generatedFilename, originalFilename);
            return generatedFilename;
        } catch (IOException e) {
            log.error("Failed to save expense document: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    public boolean deleteDocument(String fileName) {
        try {
            Path filePath = Paths.get(storagePath, fileName);
            if (!Files.exists(filePath)) {
                log.warn("Expense document file not found for deletion: {}", fileName);
                return false;
            }
            Files.delete(filePath);
            log.info("Expense document deleted: {}", fileName);
            return true;
        } catch (IOException e) {
            log.error("Failed to delete expense document: {}", fileName, e);
            return false;
        }
    }

    public boolean documentExists(String fileName) {
        return Files.exists(Paths.get(storagePath, fileName));
    }

    public byte[] readDocumentBytes(String fileName) {
        try {
            Path filePath = Paths.get(storagePath, fileName);
            if (!Files.exists(filePath)) {
                log.warn("Expense document not found: {}", fileName);
                return null;
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read expense document: {}", fileName, e);
            return null;
        }
    }

    public String constructDocumentUrl(String obfuscatedId) {
        if (obfuscatedId == null || obfuscatedId.isBlank()) return null;
        String normalizedBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
        return normalizedBaseUrl + "/api/expense-documents/" + obfuscatedId + "/file";
    }

    private String generateHashedFilename(String originalFilename, String extension) {
        try {
            long timestamp = System.currentTimeMillis();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(originalFilename.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString() + "_" + timestamp + "." + extension.toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 unavailable, falling back to timestamp naming", e);
            return System.currentTimeMillis() + "_" + System.nanoTime() + "." + extension.toLowerCase();
        }
    }

    public String getExtension(String filename) {
        if (filename == null || filename.isBlank()) return "bin";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) return "bin";
        return filename.substring(lastDot + 1).toLowerCase();
    }

    public String getMimeType(String filename) {
        String extension = getExtension(filename);
        return switch (extension.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "csv" -> "text/csv";
            case "txt" -> "text/plain";
            case "zip" -> "application/zip";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "heic" -> "image/heic";
            case "html", "htm" -> "text/html";
            default -> "application/octet-stream";
        };
    }

    public String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
