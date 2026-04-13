package com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.FileSettings.FileSettingGetterServices;
import com.itineraryledger.kabengosafaris.ImageSettings.ImageSettingGetterServices;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Service for handling hero image file storage.
 *
 * Responsibilities:
 * - Save uploaded images to the filesystem with SHA-256 hashed filenames
 * - Delete images from the filesystem
 * - Validate image uploads using ImageSettingGetterServices and FileSettingGetterServices
 * - Generate unique filenames for stored images
 * - Construct full URLs from stored filenames
 */
@Service
@Slf4j
public class HeroImageStorageService {

    @Value("${hero.image.storage.path:./data/hero-images/}")
    private String storagePath;

    @Value("${app.base.url:http://localhost:4450}")
    private String appBaseUrl;

    @Autowired
    private ImageSettingGetterServices imageSettingGetterServices;

    @Autowired
    private FileSettingGetterServices fileSettingGetterServices;

    /**
     * Initialize storage directory if it doesn't exist
     */
    public void initializeStorageDirectory() {
        try {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created hero image storage directory: {}", storagePath);
            }
        } catch (IOException e) {
            log.error("Failed to create hero image storage directory: {}", storagePath, e);
            throw new RuntimeException("Failed to initialize hero image storage directory", e);
        }
    }

    /**
     * Validate an image file before upload
     * Uses both ImageSettingGetterServices for format validation
     * and FileSettingGetterServices for size validation
     *
     * @param file the multipart file to validate
     * @return null if valid, error message if invalid
     */
    public String validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "No file provided";
        }

        String originalFilename = file.getOriginalFilename();
        long fileSize = file.getSize();

        // Validate file size using FileSettingGetterServices
        Long maxFileSize = fileSettingGetterServices.getMaxFileSize();
        if (fileSize > maxFileSize) {
            return String.format("File size (%s) exceeds maximum allowed size (%s)",
                formatFileSize(fileSize),
                fileSettingGetterServices.getMaxFileSizeString());
        }

        // Validate image format using ImageSettingGetterServices
        return imageSettingGetterServices.validateImageUpload(originalFilename, fileSize);
    }

    /**
     * Validate total request size for multiple file uploads
     *
     * @param totalSize total size of all files in bytes
     * @return null if valid, error message if invalid
     */
    public String validateRequestSize(long totalSize) {
        Long maxRequestSize = fileSettingGetterServices.getMaxRequestSize();
        if (totalSize > maxRequestSize) {
            return String.format("Total request size (%s) exceeds maximum allowed size (%s)",
                formatFileSize(totalSize),
                fileSettingGetterServices.getMaxRequestSizeString());
        }
        return null;
    }

    /**
     * Get maximum file size in bytes
     *
     * @return max file size in bytes
     */
    public Long getMaxFileSize() {
        return fileSettingGetterServices.getMaxFileSize();
    }

    /**
     * Get maximum file size as string (e.g., "10MB")
     *
     * @return max file size string
     */
    public String getMaxFileSizeString() {
        return fileSettingGetterServices.getMaxFileSizeString();
    }

    /**
     * Get maximum request size in bytes
     *
     * @return max request size in bytes
     */
    public Long getMaxRequestSize() {
        return fileSettingGetterServices.getMaxRequestSize();
    }

    /**
     * Get maximum request size as string (e.g., "50MB")
     *
     * @return max request size string
     */
    public String getMaxRequestSizeString() {
        return fileSettingGetterServices.getMaxRequestSizeString();
    }

    /**
     * Save an uploaded image file to storage
     *
     * @param file the multipart file to save
     * @return the generated filename (without path), or null if failed
     */
    public String saveImage(MultipartFile file) {
        try {
            initializeStorageDirectory();

            // Validate the image
            String validationError = validateImage(file);
            if (validationError != null) {
                log.warn("Image validation failed: {}", validationError);
                return null;
            }

            // Generate SHA-256 hashed filename
            String originalFilename = file.getOriginalFilename();
            String extension = getExtension(originalFilename);
            String generatedFilename = generateHashedFilename(originalFilename, extension);

            // Save file
            Path targetPath = Paths.get(storagePath, generatedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Hero image saved successfully: {} (original: {})", generatedFilename, originalFilename);
            return generatedFilename;

        } catch (IOException e) {
            log.error("Failed to save hero image: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    /**
     * Delete an image file from storage
     *
     * @param fileName the filename to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteImage(String fileName) {
        try {
            Path filePath = Paths.get(storagePath, fileName);

            if (!Files.exists(filePath)) {
                log.warn("Image file not found for deletion: {}", fileName);
                return false;
            }

            Files.delete(filePath);
            log.info("Hero image deleted successfully: {}", fileName);
            return true;

        } catch (IOException e) {
            log.error("Failed to delete hero image: {}", fileName, e);
            return false;
        }
    }

    /**
     * Check if an image file exists
     *
     * @param fileName the filename to check
     * @return true if exists, false otherwise
     */
    public boolean imageExists(String fileName) {
        Path filePath = Paths.get(storagePath, fileName);
        return Files.exists(filePath);
    }

    /**
     * Construct full URL from obfuscated image ID
     * URL format: {app.base.url}/api/hero-images/{obfuscatedId}/file
     *
     * @param obfuscatedId the obfuscated image ID
     * @return full URL to the image API endpoint
     */
    public String constructImageUrl(String obfuscatedId) {
        if (obfuscatedId == null || obfuscatedId.isBlank()) {
            return null;
        }
        // Construct: app.base.url + /api/hero-images/{id}/file
        String normalizedBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
        return normalizedBaseUrl + "/api/hero-images/" + obfuscatedId + "/file";
    }

    /**
     * Construct full URL from filename
     * URL format: {app.base.url}/api/hero-images/file/{fileName}
     *
     * @param fileName the stored filename
     * @return full URL to the image API endpoint
     */
    public String constructFileImageUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        // Construct: app.base.url + /api/hero-images/file/{fileName}
        String normalizedBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
        return normalizedBaseUrl + "/api/hero-images/file/" + fileName;
    }

    /**
     * Get the configured base URL
     *
     * @return application base URL
     */
    public String getBaseUrl() {
        return appBaseUrl;
    }

    /**
     * Read image file as bytes
     *
     * @param fileName the stored filename
     * @return byte array of the image, or null if not found
     */
    public byte[] readImageBytes(String fileName) {
        try {
            Path filePath = Paths.get(storagePath, fileName);
            if (!Files.exists(filePath)) {
                log.warn("Image file not found: {}", fileName);
                return null;
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read image file: {}", fileName, e);
            return null;
        }
    }

    /**
     * Get the configured storage path
     *
     * @return storage path for images
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * Generate a SHA-256 hashed filename
     * Format: SHA-256(originalFileName)_timestamp.extension
     * Example: f2e5a046548d723b59874286c9d528188b50881b215a49341b03998d7f2d00eb_1701304567890.jpg
     *
     * @param originalFilename the original filename
     * @param extension file extension (e.g., "jpg", "png")
     * @return generated hashed filename
     */
    private String generateHashedFilename(String originalFilename, String extension) {
        try {
            // Get current timestamp
            long timestamp = System.currentTimeMillis();

            // Create SHA-256 hash of original filename
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(originalFilename.getBytes(StandardCharsets.UTF_8));

            // Convert hash bytes to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            // Combine: hash_timestamp.extension
            return hexString.toString() + "_" + timestamp + "." + extension.toLowerCase();

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available, falling back to timestamp-based naming", e);
            // Fallback to timestamp-based naming
            return System.currentTimeMillis() + "_" + System.nanoTime() + "." + extension.toLowerCase();
        }
    }

    /**
     * Extract file extension from filename
     *
     * @param filename the original filename
     * @return extension without dot, or "jpg" as default
     */
    public String getExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "jpg";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "jpg";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * Get MIME type from filename
     *
     * @param filename the filename
     * @return MIME type string
     */
    public String getMimeType(String filename) {
        String extension = getExtension(filename);
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }

    /**
     * Format file size for human readability
     *
     * @param bytes file size in bytes
     * @return formatted string (e.g., "5.2 KB")
     */
    public String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
