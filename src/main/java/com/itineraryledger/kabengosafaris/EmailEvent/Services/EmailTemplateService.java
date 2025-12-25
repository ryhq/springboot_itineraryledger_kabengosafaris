package com.itineraryledger.kabengosafaris.EmailEvent.Services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * EmailTemplateService - Handles file-based email template storage
 *
 * Responsibilities:
 * - Save and read email template files from disk
 * - Load system default templates from resources
 * - Manage template lifecycle (create, update, delete)
 * - Generate unique file names for templates
 */
@Service
@Slf4j
public class EmailTemplateService {

    @Value("${email.template.storage.path:/opt/lampp/htdocs/kabengosafaris/ItineraryLedger/email-templates/}")
    private String templateStoragePath;

    @Value("${email.template.max.file.size:2097152}")
    private long maxFileSize; // 2MB default

    /**
     * Initialize template storage directory
     */
    public void initializeStorageDirectory() {
        try {
            Path path = Paths.get(templateStoragePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created email template storage directory: {}", templateStoragePath);
            }
        } catch (IOException e) {
            log.error("Failed to create email template storage directory: {}", templateStoragePath, e);
            throw new RuntimeException("Failed to initialize email template storage directory", e);
        }
    }

    /**
     * Save template file to disk
     *
     * @param templateContent HTML content of the template
     * @param fileName Filename to save as
     * @return true if successful, false otherwise
     */
    public boolean saveTemplateFile(String templateContent, String fileName) {
        try {
            initializeStorageDirectory();

            Path filePath = Paths.get(templateStoragePath, fileName);

            // Check if file already exists
            if (Files.exists(filePath)) {
                log.warn("Template file already exists: {}", fileName);
                return false;
            }

            // Check file size
            if (templateContent.getBytes().length > maxFileSize) {
                log.warn("Template content exceeds maximum file size: {} bytes", templateContent.getBytes().length);
                return false;
            }

            Files.write(filePath, templateContent.getBytes());
            log.info("Template file saved successfully: {}", fileName);
            return true;

        } catch (IOException e) {
            log.error("Failed to save template file: {}", fileName, e);
            return false;
        }
    }

    /**
     * Read template file from disk
     *
     * @param fileName Filename to read
     * @return Template content as string, or null if not found
     */
    public String readTemplateFile(String fileName) {
        try {
            Path filePath = Paths.get(templateStoragePath, fileName);

            if (!Files.exists(filePath)) {
                log.warn("Template file not found: {}", fileName);
                return null;
            }

            String content = Files.readString(filePath);
            log.debug("Template file read successfully: {}", fileName);
            return content;

        } catch (IOException e) {
            log.error("Failed to read template file: {}", fileName, e);
            return null;
        }
    }

    /**
     * Update template file on disk
     *
     * @param fileName Filename to update
     * @param newContent New template content
     * @return true if successful, false otherwise
     */
    public boolean updateTemplateFile(String fileName, String newContent) {
        try {
            Path filePath = Paths.get(templateStoragePath, fileName);

            if (!Files.exists(filePath)) {
                log.warn("Template file not found for update: {}", fileName);
                return false;
            }

            // Check file size
            if (newContent.getBytes().length > maxFileSize) {
                log.warn("New template content exceeds maximum file size: {} bytes", newContent.getBytes().length);
                return false;
            }

            Files.write(filePath, newContent.getBytes());
            log.info("Template file updated successfully: {}", fileName);
            return true;

        } catch (IOException e) {
            log.error("Failed to update template file: {}", fileName, e);
            return false;
        }
    }

    /**
     * Delete template file from disk
     *
     * @param fileName Filename to delete
     * @return true if successful, false otherwise
     */
    public boolean deleteTemplateFile(String fileName) {
        try {
            Path filePath = Paths.get(templateStoragePath, fileName);

            if (!Files.exists(filePath)) {
                log.warn("Template file not found for deletion: {}", fileName);
                return false;
            }

            Files.delete(filePath);
            log.info("Template file deleted successfully: {}", fileName);
            return true;

        } catch (IOException e) {
            log.error("Failed to delete template file: {}", fileName, e);
            return false;
        }
    }

    /**
     * Load system default template from resources
     *
     * @param eventName The name of the email event (e.g., "USER_REGISTRATION")
     * @return Template content from resources, or fallback template if not found
     */
    public String loadSystemDefaultTemplate(String eventName) {
        try {
            String resourcePath = "templates/email-templates/" + eventName.toLowerCase() + "_default.html";
            var resource = getClass().getClassLoader().getResourceAsStream(resourcePath);

            if (resource == null) {
                log.warn("System default template not found for event: {}, using fallback", eventName);
                return generateFallbackTemplate(eventName);
            }

            String template = new String(resource.readAllBytes());
            log.info("Loaded system default template for event: {}", eventName);
            return template;

        } catch (Exception e) {
            log.error("Failed to load system default template for event: {}", eventName, e);
            return generateFallbackTemplate(eventName);
        }
    }

    /**
     * Generate a fallback template when system default cannot be loaded
     *
     * @param eventName The name of the email event
     * @return Basic HTML template
     */
    private String generateFallbackTemplate(String eventName) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>%s</title>
            </head>
            <body>
                <h1>%s</h1>
                <p>This is the fallback template for the %s event.</p>
                <p>Please customize this template to fit your needs.</p>
            </body>
            </html>
            """, eventName, eventName.replace("_", " "), eventName);
    }

    /**
     * Generate unique file name for template
     *
     * @param eventName The email event name
     * @param templateName The template name
     * @return Generated filename
     */
    public String generateFileName(String eventName, String templateName) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);

        // Sanitize names to be filesystem-safe
        String sanitizedEvent = eventName.replaceAll("[^a-zA-Z0-9_-]", "_");
        String sanitizedTemplate = templateName.replaceAll("[^a-zA-Z0-9_-]", "_");

        return String.format("%s_%s_%s.html", sanitizedEvent, sanitizedTemplate, timestamp);
    }

    /**
     * Format file size for human readability
     *
     * @param bytes File size in bytes
     * @return Formatted string (e.g., "5.2 KB")
     */
    public String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
