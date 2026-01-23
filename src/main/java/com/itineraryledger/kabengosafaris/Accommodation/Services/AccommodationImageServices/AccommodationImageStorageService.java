package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.ImageSettings.ImageSettingGetterServices;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service for handling accommodation image file storage.
 *
 * Responsibilities:
 * - Save uploaded images to the filesystem with generated filenames
 * - Delete images from the filesystem
 * - Validate image uploads using ImageSettingGetterServices
 * - Generate unique filenames for stored images
 * - Construct full URLs from stored filenames
 */
@Service
@Slf4j
public class AccommodationImageStorageService {

    @Value("${accommodation.image.storage.path:/opt/lampp/htdocs/kabengosafaris/ItineraryLedger/accommodation-images/}")
    private String storagePath;

    @Value("${app.base.url:http://localhost:4450}")
    private String appBaseUrl;

    @Autowired
    private ImageSettingGetterServices imageSettingGetterServices;

    /**
     * Initialize storage directory if it doesn't exist
     */
    public void initializeStorageDirectory() {
        try {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created accommodation image storage directory: {}", storagePath);
            }
        } catch (IOException e) {
            log.error("Failed to create accommodation image storage directory: {}", storagePath, e);
            throw new RuntimeException("Failed to initialize accommodation image storage directory", e);
        }
    }

    /**
     * Validate an image file before upload
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

        // Use ImageSettingGetterServices for validation
        return imageSettingGetterServices.validateImageUpload(originalFilename, fileSize);
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

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getExtension(originalFilename);
            String generatedFilename = generateFilename(extension);

            // Save file
            Path targetPath = Paths.get(storagePath, generatedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Accommodation image saved successfully: {} (original: {})", generatedFilename, originalFilename);
            return generatedFilename;

        } catch (IOException e) {
            log.error("Failed to save accommodation image: {}", file.getOriginalFilename(), e);
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
            log.info("Accommodation image deleted successfully: {}", fileName);
            return true;

        } catch (IOException e) {
            log.error("Failed to delete accommodation image: {}", fileName, e);
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
     * URL format: {app.base.url}/api/accommodation-images/{obfuscatedId}/file
     *
     * @param obfuscatedId the obfuscated image ID
     * @return full URL to the image API endpoint
     */
    public String constructImageUrl(String obfuscatedId) {
        if (obfuscatedId == null || obfuscatedId.isBlank()) {
            return null;
        }
        // Construct: app.base.url + /api/accommodation-images/{id}/file
        String normalizedBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
        return normalizedBaseUrl + "/api/accommodation-images/" + obfuscatedId + "/file";
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
     * Generate a unique filename with the given extension
     *
     * @param extension file extension (e.g., "jpg", "png")
     * @return generated filename
     */
    private String generateFilename(String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + "." + extension.toLowerCase();
    }

    /**
     * Extract file extension from filename
     *
     * @param filename the original filename
     * @return extension without dot, or "jpg" as default
     */
    private String getExtension(String filename) {
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
