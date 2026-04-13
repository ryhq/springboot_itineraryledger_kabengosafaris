package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceDocumentServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.FileSettings.FileSettingGetterServices;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Service for handling invoice document file storage.
 */
@Service
@Slf4j
public class InvoiceDocumentStorageService {

    @Value("${invoice.document.storage.path:./data/invoice-documents/}")
    private String storagePath;

    @Value("${app.base.url:http://localhost:4450}")
    private String appBaseUrl;

    @Autowired
    private FileSettingGetterServices fileSettingGetterServices;

    public void initializeStorageDirectory() {
        try {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created invoice document storage directory: {}", storagePath);
            }
        } catch (IOException e) {
            log.error("Failed to create invoice document storage directory: {}", storagePath, e);
            throw new RuntimeException("Failed to initialize invoice document storage directory", e);
        }
    }

    public String validateDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "No file provided";
        }

        String originalFilename = file.getOriginalFilename();
        long fileSize = file.getSize();

        return fileSettingGetterServices.validateFileUpload(originalFilename, fileSize);
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

    public Long getMaxFileSize() {
        return fileSettingGetterServices.getMaxFileSize();
    }

    public String getMaxFileSizeString() {
        return fileSettingGetterServices.getMaxFileSizeString();
    }

    public Long getMaxRequestSize() {
        return fileSettingGetterServices.getMaxRequestSize();
    }

    public String getMaxRequestSizeString() {
        return fileSettingGetterServices.getMaxRequestSizeString();
    }

    public String getAllowedExtensionsString() {
        return fileSettingGetterServices.getAllowedExtensionsString();
    }

    public String saveDocument(MultipartFile file) {
        try {
            initializeStorageDirectory();

            String validationError = validateDocument(file);
            if (validationError != null) {
                log.warn("Document validation failed: {}", validationError);
                return null;
            }

            String originalFilename = file.getOriginalFilename();
            String extension = getExtension(originalFilename);
            String generatedFilename = generateHashedFilename(originalFilename, extension);

            Path targetPath = Paths.get(storagePath, generatedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Invoice document saved successfully: {} (original: {})", generatedFilename, originalFilename);
            return generatedFilename;

        } catch (IOException e) {
            log.error("Failed to save invoice document: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    /**
     * Save a document from raw byte array (for system-generated PDFs).
     *
     * @param bytes The document content as byte array
     * @param originalFilename The original filename (used for extension extraction and hashing)
     * @return The saved filename (hashed), or null if save failed
     */
    public String saveDocumentBytes(byte[] bytes, String originalFilename) {
        try {
            initializeStorageDirectory();

            if (bytes == null || bytes.length == 0) {
                log.warn("Cannot save empty document bytes");
                return null;
            }

            String extension = getExtension(originalFilename);
            if (extension.equals("bin")) {
                extension = "pdf"; // Default to PDF for generated documents
            }
            String generatedFilename = generateHashedFilename(originalFilename, extension);

            Path targetPath = Paths.get(storagePath, generatedFilename);
            Files.write(targetPath, bytes);

            log.info("Generated document saved successfully: {} (original: {}, size: {} bytes)",
                     generatedFilename, originalFilename, bytes.length);
            return generatedFilename;

        } catch (IOException e) {
            log.error("Failed to save generated document: {}", originalFilename, e);
            return null;
        }
    }

    public boolean deleteDocument(String fileName) {
        try {
            Path filePath = Paths.get(storagePath, fileName);

            if (!Files.exists(filePath)) {
                log.warn("Document file not found for deletion: {}", fileName);
                return false;
            }

            Files.delete(filePath);
            log.info("Invoice document deleted successfully: {}", fileName);
            return true;

        } catch (IOException e) {
            log.error("Failed to delete invoice document: {}", fileName, e);
            return false;
        }
    }

    public boolean documentExists(String fileName) {
        Path filePath = Paths.get(storagePath, fileName);
        return Files.exists(filePath);
    }

    public String constructDocumentUrl(String obfuscatedId) {
        if (obfuscatedId == null || obfuscatedId.isBlank()) {
            return null;
        }
        String normalizedBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
        return normalizedBaseUrl + "/api/invoice-documents/" + obfuscatedId + "/file";
    }

    public String constructFileDocumentUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String normalizedBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
        return normalizedBaseUrl + "/api/invoice-documents/file/" + fileName;
    }

    public String getBaseUrl() {
        return appBaseUrl;
    }

    public byte[] readDocumentBytes(String fileName) {
        try {
            Path filePath = Paths.get(storagePath, fileName);
            if (!Files.exists(filePath)) {
                log.warn("Document file not found: {}", fileName);
                return null;
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read document file: {}", fileName, e);
            return null;
        }
    }

    public String getStoragePath() {
        return storagePath;
    }

    private String generateHashedFilename(String originalFilename, String extension) {
        try {
            long timestamp = System.currentTimeMillis();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(originalFilename.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString() + "_" + timestamp + "." + extension.toLowerCase();

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available, falling back to timestamp-based naming", e);
            return System.currentTimeMillis() + "_" + System.nanoTime() + "." + extension.toLowerCase();
        }
    }

    public String getExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "bin";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "bin";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
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
            case "mp4" -> "video/mp4";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "html", "htm" -> "text/html";
            case "xml" -> "application/xml";
            case "json" -> "application/json";
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
