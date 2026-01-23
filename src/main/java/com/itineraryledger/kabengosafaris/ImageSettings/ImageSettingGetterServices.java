package com.itineraryledger.kabengosafaris.ImageSettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Service for reading image settings with fallback to application.properties values.
 * This service provides getter methods that first check the database, then fall back to
 * default values from application.properties if the database value is not available or inactive.
 */
@Service
public class ImageSettingGetterServices {

    // =====================================================================
    // Fallback values from application.properties - UPLOAD Settings
    // =====================================================================

    @Value("${image.upload.enabled:true}")
    private Boolean imageUploadEnabled;

    @Value("${image.upload.max.file.size:5242880}")
    private Long imageUploadMaxFileSize;

    @Value("${image.upload.allowed.formats:jpg,jpeg,png,webp,gif}")
    private String imageUploadAllowedFormats;

    @Autowired
    private ImageSettingRepository imageSettingRepository;

    // =====================================================================
    // UPLOAD Settings Getters
    // =====================================================================

    /**
     * Check if image upload is enabled
     * @return true if image upload is enabled
     */
    public Boolean isImageUploadEnabled() {
        ImageSetting setting = imageSettingRepository
                .findBySettingKey("image.upload.enabled").orElse(null);
        if (setting == null || !setting.getActive()) {
            return imageUploadEnabled;
        }
        try {
            return Boolean.parseBoolean(setting.getSettingValue());
        } catch (Exception e) {
            return imageUploadEnabled;
        }
    }

    /**
     * Get maximum file size for image uploads in bytes
     * @return max file size in bytes
     */
    public Long getMaxFileSize() {
        ImageSetting setting = imageSettingRepository
                .findBySettingKey("image.upload.max.file.size").orElse(null);
        if (setting == null || !setting.getActive()) {
            return imageUploadMaxFileSize;
        }
        try {
            return Long.parseLong(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return imageUploadMaxFileSize;
        }
    }

    /**
     * Get maximum file size in megabytes (for display purposes)
     * @return max file size in MB
     */
    public double getMaxFileSizeInMB() {
        return getMaxFileSize() / (1024.0 * 1024.0);
    }

    /**
     * Get allowed image formats as comma-separated string
     * @return comma-separated format extensions
     */
    public String getAllowedFormatsString() {
        ImageSetting setting = imageSettingRepository
                .findBySettingKey("image.upload.allowed.formats").orElse(null);
        if (setting == null || !setting.getActive()) {
            return imageUploadAllowedFormats;
        }
        String value = setting.getSettingValue();
        return (value != null && !value.isBlank()) ? value : imageUploadAllowedFormats;
    }

    /**
     * Get allowed image formats as list
     * @return list of allowed format extensions
     */
    public List<String> getAllowedFormats() {
        String formats = getAllowedFormatsString();
        return Arrays.asList(formats.split(","))
                .stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Check if a file format is allowed
     * @param format the format extension to check (e.g., "jpg", "png")
     * @return true if allowed
     */
    public boolean isFormatAllowed(String format) {
        if (format == null || format.isBlank()) {
            return false;
        }
        String normalizedFormat = format.toLowerCase().trim();
        // Remove leading dot if present
        if (normalizedFormat.startsWith(".")) {
            normalizedFormat = normalizedFormat.substring(1);
        }
        return getAllowedFormats().contains(normalizedFormat);
    }

    /**
     * Check if a filename has an allowed extension
     * @param filename the filename to check
     * @return true if the file extension is allowed
     */
    public boolean isFilenameAllowed(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return false;
        }
        String extension = filename.substring(lastDotIndex + 1);
        return isFormatAllowed(extension);
    }

    /**
     * Check if a file size is within the allowed limit
     * @param fileSizeInBytes the file size to check
     * @return true if within limit
     */
    public boolean isFileSizeAllowed(long fileSizeInBytes) {
        return fileSizeInBytes <= getMaxFileSize();
    }

    /**
     * Validate an image upload
     * @param filename the filename
     * @param fileSizeInBytes the file size in bytes
     * @return null if valid, error message if invalid
     */
    public String validateImageUpload(String filename, long fileSizeInBytes) {
        if (!isImageUploadEnabled()) {
            return "Image uploads are currently disabled";
        }
        if (!isFilenameAllowed(filename)) {
            return String.format("File format not allowed. Allowed formats: %s", getAllowedFormatsString());
        }
        if (!isFileSizeAllowed(fileSizeInBytes)) {
            return String.format("File size exceeds maximum allowed size of %.2f MB", getMaxFileSizeInMB());
        }
        return null; // Valid
    }
}
