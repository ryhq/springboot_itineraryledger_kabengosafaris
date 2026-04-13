package com.itineraryledger.kabengosafaris.Activity.Services.ActivityImageServices;

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
 * Service for handling activity image file storage.
 */
@Service
@Slf4j
public class ActivityImageStorageService {

    @Value("${activity.image.storage.path:./data/activity-images/}")
    private String storagePath;

    @Value("${app.base.url:http://localhost:4450}")
    private String appBaseUrl;

    @Autowired
    private ImageSettingGetterServices imageSettingGetterServices;

    @Autowired
    private FileSettingGetterServices fileSettingGetterServices;

    public void initializeStorageDirectory() {
        try {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created activity image storage directory: {}", storagePath);
            }
        } catch (IOException e) {
            log.error("Failed to create activity image storage directory: {}", storagePath, e);
            throw new RuntimeException("Failed to initialize activity image storage directory", e);
        }
    }

    public String validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "No file provided";
        }

        String originalFilename = file.getOriginalFilename();
        long fileSize = file.getSize();

        return imageSettingGetterServices.validateImageUpload(originalFilename, fileSize);
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
        return imageSettingGetterServices.getMaxFileSize();
    }

    public String getMaxFileSizeString() {
        return formatFileSize(imageSettingGetterServices.getMaxFileSize());
    }

    public Long getMaxRequestSize() {
        return fileSettingGetterServices.getMaxRequestSize();
    }

    public String getMaxRequestSizeString() {
        return fileSettingGetterServices.getMaxRequestSizeString();
    }

    public String getAllowedFormatsString() {
        return imageSettingGetterServices.getAllowedFormatsString();
    }

    public String saveImage(MultipartFile file) {
        try {
            initializeStorageDirectory();

            String validationError = validateImage(file);
            if (validationError != null) {
                log.warn("Image validation failed: {}", validationError);
                return null;
            }

            String originalFilename = file.getOriginalFilename();
            String extension = getExtension(originalFilename);
            String generatedFilename = generateHashedFilename(originalFilename, extension);

            Path targetPath = Paths.get(storagePath, generatedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Activity image saved successfully: {} (original: {})", generatedFilename, originalFilename);
            return generatedFilename;

        } catch (IOException e) {
            log.error("Failed to save activity image: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    public boolean deleteImage(String fileName) {
        try {
            Path filePath = Paths.get(storagePath, fileName);

            if (!Files.exists(filePath)) {
                log.warn("Image file not found for deletion: {}", fileName);
                return false;
            }

            Files.delete(filePath);
            log.info("Activity image deleted successfully: {}", fileName);
            return true;

        } catch (IOException e) {
            log.error("Failed to delete activity image: {}", fileName, e);
            return false;
        }
    }

    public boolean imageExists(String fileName) {
        Path filePath = Paths.get(storagePath, fileName);
        return Files.exists(filePath);
    }

    public String constructImageUrl(String obfuscatedId) {
        if (obfuscatedId == null || obfuscatedId.isBlank()) {
            return null;
        }
        String normalizedBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
        return normalizedBaseUrl + "/api/activity-images/" + obfuscatedId + "/file";
    }

    public String constructFileImageUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String normalizedBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
        return normalizedBaseUrl + "/api/activity-images/file/" + fileName;
    }

    public String getBaseUrl() {
        return appBaseUrl;
    }

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
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "bmp" -> "image/bmp";
            case "ico" -> "image/x-icon";
            case "tiff", "tif" -> "image/tiff";
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
