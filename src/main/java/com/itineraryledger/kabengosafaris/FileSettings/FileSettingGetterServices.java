package com.itineraryledger.kabengosafaris.FileSettings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for reading file settings with fallback to application.properties values.
 * This service provides getter methods that first check the database, then fall back to
 * default values from application.properties if the database value is not available or inactive.
 */
@Service
public class FileSettingGetterServices {

    // =====================================================================
    // Fallback values from application.properties - GENERAL UPLOAD Settings
    // =====================================================================

    @Value("${file.upload.enabled:true}")
    private Boolean fileUploadEnabled;

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String multipartMaxFileSize;

    @Value("${spring.servlet.multipart.max-request-size:50MB}")
    private String multipartMaxRequestSize;

    @Value("${file.upload.allowed.extensions:pdf,doc,docx,xls,xlsx,csv,txt,zip}")
    private String fileUploadAllowedExtensions;

    @Value("${file.upload.blocked.extensions:exe,bat,sh,cmd,ps1,jar,msi,dll,com,scr,vbs,js}")
    private String fileUploadBlockedExtensions;

    @Value("${file.upload.sanitize.filename:true}")
    private Boolean fileUploadSanitizeFilename;

    @Value("${file.upload.validate.content.type:true}")
    private Boolean fileUploadValidateContentType;

    // =====================================================================
    // Fallback values from application.properties - SPECIFIC FILE TYPE Settings
    // =====================================================================

    @Value("${email.signature.max.file.size:1048576}")
    private Long emailSignatureMaxFileSize;

    @Value("${email.template.max.file.size:2097152}")
    private Long emailTemplateMaxFileSize;

    @Value("${pdf.template.max.file.size:5242880}")
    private Long pdfTemplateMaxFileSize;

    @Autowired
    private FileSettingRepository fileSettingRepository;

    // Pattern for sanitizing filenames
    private static final Pattern UNSAFE_FILENAME_PATTERN = Pattern.compile("[^a-zA-Z0-9._-]");

    // =====================================================================
    // GENERAL UPLOAD Settings Getters
    // =====================================================================

    /**
     * Check if file upload is enabled globally
     * @return true if file upload is enabled
     */
    public Boolean isFileUploadEnabled() {
        FileSetting setting = fileSettingRepository
                .findBySettingKey("file.upload.enabled").orElse(null);
        if (setting == null || !setting.getActive()) {
            return fileUploadEnabled;
        }
        try {
            return Boolean.parseBoolean(setting.getSettingValue());
        } catch (Exception e) {
            return fileUploadEnabled;
        }
    }

    /**
     * Get maximum file size for general file uploads in bytes
     * @return max file size in bytes
     */
    public Long getMaxFileSize() {
        FileSetting setting = fileSettingRepository
                .findBySettingKey("spring.servlet.multipart.max-file-size").orElse(null);
        if (setting == null || !setting.getActive()) {
            return parseDataSize(multipartMaxFileSize);
        }
        try {
            return parseDataSize(setting.getSettingValue());
        } catch (Exception e) {
            return parseDataSize(multipartMaxFileSize);
        }
    }

    /**
     * Get maximum file size as a string (e.g., "10MB")
     * @return max file size string
     */
    public String getMaxFileSizeString() {
        FileSetting setting = fileSettingRepository
                .findBySettingKey("spring.servlet.multipart.max-file-size").orElse(null);
        if (setting == null || !setting.getActive()) {
            return multipartMaxFileSize;
        }
        return setting.getSettingValue();
    }

    /**
     * Get maximum request size for multipart uploads in bytes
     * @return max request size in bytes
     */
    public Long getMaxRequestSize() {
        FileSetting setting = fileSettingRepository
                .findBySettingKey("spring.servlet.multipart.max-request-size").orElse(null);
        if (setting == null || !setting.getActive()) {
            return parseDataSize(multipartMaxRequestSize);
        }
        try {
            return parseDataSize(setting.getSettingValue());
        } catch (Exception e) {
            return parseDataSize(multipartMaxRequestSize);
        }
    }

    /**
     * Get maximum request size as a string (e.g., "50MB")
     * @return max request size string
     */
    public String getMaxRequestSizeString() {
        FileSetting setting = fileSettingRepository
                .findBySettingKey("spring.servlet.multipart.max-request-size").orElse(null);
        if (setting == null || !setting.getActive()) {
            return multipartMaxRequestSize;
        }
        return setting.getSettingValue();
    }

    /**
     * Get maximum file size in megabytes (for display purposes)
     * @return max file size in MB
     */
    public double getMaxFileSizeInMB() {
        return getMaxFileSize() / (1024.0 * 1024.0);
    }

    /**
     * Get maximum request size in megabytes (for display purposes)
     * @return max request size in MB
     */
    public double getMaxRequestSizeInMB() {
        return getMaxRequestSize() / (1024.0 * 1024.0);
    }

    /**
     * Parse data size string (e.g., "10MB", "5KB", "1GB") to bytes
     * @param sizeString the size string
     * @return size in bytes
     */
    private Long parseDataSize(String sizeString) {
        if (sizeString == null || sizeString.isBlank()) {
            return 10485760L; // Default 10MB
        }
        String normalized = sizeString.trim().toUpperCase();
        try {
            if (normalized.endsWith("GB")) {
                return Long.parseLong(normalized.replace("GB", "").trim()) * 1024 * 1024 * 1024;
            } else if (normalized.endsWith("MB")) {
                return Long.parseLong(normalized.replace("MB", "").trim()) * 1024 * 1024;
            } else if (normalized.endsWith("KB")) {
                return Long.parseLong(normalized.replace("KB", "").trim()) * 1024;
            } else if (normalized.endsWith("B")) {
                return Long.parseLong(normalized.replace("B", "").trim());
            } else {
                // Assume bytes if no suffix
                return Long.parseLong(normalized);
            }
        } catch (NumberFormatException e) {
            return 10485760L; // Default 10MB on parse error
        }
    }

    /**
     * Get allowed file extensions as comma-separated string
     * @return comma-separated extensions
     */
    public String getAllowedExtensionsString() {
        FileSetting setting = fileSettingRepository
                .findBySettingKey("file.upload.allowed.extensions").orElse(null);
        if (setting == null || !setting.getActive()) {
            return fileUploadAllowedExtensions;
        }
        String value = setting.getSettingValue();
        return (value != null && !value.isBlank()) ? value : fileUploadAllowedExtensions;
    }

    /**
     * Get allowed file extensions as list
     * @return list of allowed extensions
     */
    public List<String> getAllowedExtensions() {
        String extensions = getAllowedExtensionsString();
        return Arrays.asList(extensions.split(","))
                .stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Get blocked file extensions as comma-separated string
     * @return comma-separated blocked extensions
     */
    public String getBlockedExtensionsString() {
        FileSetting setting = fileSettingRepository
                .findBySettingKey("file.upload.blocked.extensions").orElse(null);
        if (setting == null || !setting.getActive()) {
            return fileUploadBlockedExtensions;
        }
        String value = setting.getSettingValue();
        return (value != null && !value.isBlank()) ? value : fileUploadBlockedExtensions;
    }

    /**
     * Get blocked file extensions as list
     * @return list of blocked extensions
     */
    public List<String> getBlockedExtensions() {
        String extensions = getBlockedExtensionsString();
        return Arrays.asList(extensions.split(","))
                .stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Check if filename sanitization is enabled
     * @return true if sanitization is enabled
     */
    public Boolean isSanitizeFilenameEnabled() {
        FileSetting setting = fileSettingRepository
                .findBySettingKey("file.upload.sanitize.filename").orElse(null);
        if (setting == null || !setting.getActive()) {
            return fileUploadSanitizeFilename;
        }
        try {
            return Boolean.parseBoolean(setting.getSettingValue());
        } catch (Exception e) {
            return fileUploadSanitizeFilename;
        }
    }

    /**
     * Check if content type validation is enabled
     * @return true if validation is enabled
     */
    public Boolean isValidateContentTypeEnabled() {
        FileSetting setting = fileSettingRepository
                .findBySettingKey("file.upload.validate.content.type").orElse(null);
        if (setting == null || !setting.getActive()) {
            return fileUploadValidateContentType;
        }
        try {
            return Boolean.parseBoolean(setting.getSettingValue());
        } catch (Exception e) {
            return fileUploadValidateContentType;
        }
    }

    // =====================================================================
    // EMAIL SIGNATURE Settings Getters
    // =====================================================================

    /**
     * Get maximum file size for email signature uploads in bytes
     * @return max file size in bytes
     */
    public Long getEmailSignatureMaxFileSize() {
        FileSetting setting = fileSettingRepository
                .findBySettingKey("file.email.signature.max.file.size").orElse(null);
        if (setting == null || !setting.getActive()) {
            return emailSignatureMaxFileSize;
        }
        try {
            return Long.parseLong(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return emailSignatureMaxFileSize;
        }
    }

    /**
     * Get email signature max file size in MB
     * @return max file size in MB
     */
    public double getEmailSignatureMaxFileSizeInMB() {
        return getEmailSignatureMaxFileSize() / (1024.0 * 1024.0);
    }

    // =====================================================================
    // EMAIL TEMPLATE Settings Getters
    // =====================================================================

    /**
     * Get maximum file size for email template uploads in bytes
     * @return max file size in bytes
     */
    public Long getEmailTemplateMaxFileSize() {
        FileSetting setting = fileSettingRepository
                .findBySettingKey("file.email.template.max.file.size").orElse(null);
        if (setting == null || !setting.getActive()) {
            return emailTemplateMaxFileSize;
        }
        try {
            return Long.parseLong(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return emailTemplateMaxFileSize;
        }
    }

    /**
     * Get email template max file size in MB
     * @return max file size in MB
     */
    public double getEmailTemplateMaxFileSizeInMB() {
        return getEmailTemplateMaxFileSize() / (1024.0 * 1024.0);
    }

    // =====================================================================
    // PDF TEMPLATE Settings Getters
    // =====================================================================

    /**
     * Get maximum file size for PDF template uploads in bytes
     * @return max file size in bytes
     */
    public Long getPdfTemplateMaxFileSize() {
        FileSetting setting = fileSettingRepository
                .findBySettingKey("file.pdf.template.max.file.size").orElse(null);
        if (setting == null || !setting.getActive()) {
            return pdfTemplateMaxFileSize;
        }
        try {
            return Long.parseLong(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return pdfTemplateMaxFileSize;
        }
    }

    /**
     * Get PDF template max file size in MB
     * @return max file size in MB
     */
    public double getPdfTemplateMaxFileSizeInMB() {
        return getPdfTemplateMaxFileSize() / (1024.0 * 1024.0);
    }

    // =====================================================================
    // VALIDATION Helper Methods
    // =====================================================================

    /**
     * Check if an extension is allowed
     * @param extension the extension to check (with or without dot)
     * @return true if allowed
     */
    public boolean isExtensionAllowed(String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        String normalizedExt = extension.toLowerCase().trim();
        if (normalizedExt.startsWith(".")) {
            normalizedExt = normalizedExt.substring(1);
        }
        // First check if blocked
        if (getBlockedExtensions().contains(normalizedExt)) {
            return false;
        }
        // Then check if in allowed list
        return getAllowedExtensions().contains(normalizedExt);
    }

    /**
     * Check if an extension is blocked (dangerous)
     * @param extension the extension to check
     * @return true if blocked
     */
    public boolean isExtensionBlocked(String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        String normalizedExt = extension.toLowerCase().trim();
        if (normalizedExt.startsWith(".")) {
            normalizedExt = normalizedExt.substring(1);
        }
        return getBlockedExtensions().contains(normalizedExt);
    }

    /**
     * Extract extension from filename
     * @param filename the filename
     * @return extension without dot, or empty string if none
     */
    public String getExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * Check if a filename has an allowed extension
     * @param filename the filename to check
     * @return true if extension is allowed
     */
    public boolean isFilenameAllowed(String filename) {
        String extension = getExtension(filename);
        if (extension.isEmpty()) {
            return false;
        }
        return isExtensionAllowed(extension);
    }

    /**
     * Check if file size is within allowed limit
     * @param fileSizeInBytes the file size
     * @return true if within limit
     */
    public boolean isFileSizeAllowed(long fileSizeInBytes) {
        return fileSizeInBytes <= getMaxFileSize();
    }

    /**
     * Check if file size is within email signature limit
     * @param fileSizeInBytes the file size
     * @return true if within limit
     */
    public boolean isEmailSignatureFileSizeAllowed(long fileSizeInBytes) {
        return fileSizeInBytes <= getEmailSignatureMaxFileSize();
    }

    /**
     * Check if file size is within email template limit
     * @param fileSizeInBytes the file size
     * @return true if within limit
     */
    public boolean isEmailTemplateFileSizeAllowed(long fileSizeInBytes) {
        return fileSizeInBytes <= getEmailTemplateMaxFileSize();
    }

    /**
     * Check if file size is within PDF template limit
     * @param fileSizeInBytes the file size
     * @return true if within limit
     */
    public boolean isPdfTemplateFileSizeAllowed(long fileSizeInBytes) {
        return fileSizeInBytes <= getPdfTemplateMaxFileSize();
    }

    /**
     * Sanitize a filename by removing unsafe characters
     * @param filename the original filename
     * @return sanitized filename
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed";
        }

        // Get the extension
        String extension = getExtension(filename);
        String nameWithoutExtension = filename;
        if (!extension.isEmpty()) {
            nameWithoutExtension = filename.substring(0, filename.lastIndexOf('.'));
        }

        // Replace unsafe characters
        String sanitized = UNSAFE_FILENAME_PATTERN.matcher(nameWithoutExtension).replaceAll("_");

        // Remove multiple consecutive underscores
        sanitized = sanitized.replaceAll("_+", "_");

        // Remove leading/trailing underscores
        sanitized = sanitized.replaceAll("^_+|_+$", "");

        // If empty after sanitization, use default name
        if (sanitized.isEmpty()) {
            sanitized = "file";
        }

        // Re-add extension
        if (!extension.isEmpty()) {
            sanitized = sanitized + "." + extension;
        }

        return sanitized;
    }

    /**
     * Validate a general file upload
     * @param filename the filename
     * @param fileSizeInBytes the file size in bytes
     * @return null if valid, error message if invalid
     */
    public String validateFileUpload(String filename, long fileSizeInBytes) {
        if (!isFileUploadEnabled()) {
            return "File uploads are currently disabled";
        }

        String extension = getExtension(filename);
        if (extension.isEmpty()) {
            return "File must have an extension";
        }

        if (isExtensionBlocked(extension)) {
            return String.format("File type '%s' is blocked for security reasons", extension);
        }

        if (!isExtensionAllowed(extension)) {
            return String.format("File type '%s' is not allowed. Allowed types: %s",
                extension, getAllowedExtensionsString());
        }

        if (!isFileSizeAllowed(fileSizeInBytes)) {
            return String.format("File size exceeds maximum allowed size of %.2f MB", getMaxFileSizeInMB());
        }

        return null; // Valid
    }

    /**
     * Validate an email signature upload
     * @param filename the filename
     * @param fileSizeInBytes the file size in bytes
     * @return null if valid, error message if invalid
     */
    public String validateEmailSignatureUpload(String filename, long fileSizeInBytes) {
        if (!isFileUploadEnabled()) {
            return "File uploads are currently disabled";
        }

        if (!isEmailSignatureFileSizeAllowed(fileSizeInBytes)) {
            return String.format("Email signature file size exceeds maximum allowed size of %.2f MB",
                getEmailSignatureMaxFileSizeInMB());
        }

        return null; // Valid
    }

    /**
     * Validate an email template upload
     * @param filename the filename
     * @param fileSizeInBytes the file size in bytes
     * @return null if valid, error message if invalid
     */
    public String validateEmailTemplateUpload(String filename, long fileSizeInBytes) {
        if (!isFileUploadEnabled()) {
            return "File uploads are currently disabled";
        }

        if (!isEmailTemplateFileSizeAllowed(fileSizeInBytes)) {
            return String.format("Email template file size exceeds maximum allowed size of %.2f MB",
                getEmailTemplateMaxFileSizeInMB());
        }

        return null; // Valid
    }

    /**
     * Validate a PDF template upload
     * @param filename the filename
     * @param fileSizeInBytes the file size in bytes
     * @return null if valid, error message if invalid
     */
    public String validatePdfTemplateUpload(String filename, long fileSizeInBytes) {
        if (!isFileUploadEnabled()) {
            return "File uploads are currently disabled";
        }

        if (!isPdfTemplateFileSizeAllowed(fileSizeInBytes)) {
            return String.format("PDF template file size exceeds maximum allowed size of %.2f MB",
                getPdfTemplateMaxFileSizeInMB());
        }

        return null; // Valid
    }
}
