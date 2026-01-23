package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.FileSettings.FileSettingGetterServices;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * PdfTemplateStorageService - Handles file-based PDF template storage
 *
 * Responsibilities:
 * - Save and read PDF template files from disk
 * - Load system default templates from resources
 * - Manage template lifecycle (create, update, delete)
 * - Generate unique file names for templates
 */
@Service
@Slf4j
public class PdfTemplateStorageService {

    @Value("${pdf.template.storage.path:/opt/lampp/htdocs/kabengosafaris/ItineraryLedger/pdf-templates/}")
    private String templateStoragePath;

    @Autowired
    private FileSettingGetterServices fileSettingGetterServices;

    /**
     * Initialize template storage directory
     */
    public void initializeStorageDirectory() {
        try {
            Path path = Paths.get(templateStoragePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created PDF template storage directory: {}", templateStoragePath);
            }
        } catch (IOException e) {
            log.error("Failed to create PDF template storage directory: {}", templateStoragePath, e);
            throw new RuntimeException("Failed to initialize PDF template storage directory", e);
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
                log.warn("PDF template file already exists: {}", fileName);
                return false;
            }

            // Check file size using database-driven settings
            long maxFileSize = fileSettingGetterServices.getPdfTemplateMaxFileSize();
            if (templateContent.getBytes().length > maxFileSize) {
                log.warn("PDF template content exceeds maximum file size: {} bytes (limit: {} bytes)",
                    templateContent.getBytes().length, maxFileSize);
                return false;
            }

            Files.write(filePath, templateContent.getBytes());
            log.info("PDF template file saved successfully: {}", fileName);
            return true;

        } catch (IOException e) {
            log.error("Failed to save PDF template file: {}", fileName, e);
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
                log.warn("PDF template file not found: {}", fileName);
                return null;
            }

            String content = Files.readString(filePath);
            log.debug("PDF template file read successfully: {}", fileName);
            return content;

        } catch (IOException e) {
            log.error("Failed to read PDF template file: {}", fileName, e);
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
                log.warn("PDF template file not found for update: {}", fileName);
                return false;
            }

            // Check file size using database-driven settings
            long maxFileSize = fileSettingGetterServices.getPdfTemplateMaxFileSize();
            if (newContent.getBytes().length > maxFileSize) {
                log.warn("New PDF template content exceeds maximum file size: {} bytes (limit: {} bytes)",
                    newContent.getBytes().length, maxFileSize);
                return false;
            }

            Files.write(filePath, newContent.getBytes());
            log.info("PDF template file updated successfully: {}", fileName);
            return true;

        } catch (IOException e) {
            log.error("Failed to update PDF template file: {}", fileName, e);
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
                log.warn("PDF template file not found for deletion: {}", fileName);
                return false;
            }

            Files.delete(filePath);
            log.info("PDF template file deleted successfully: {}", fileName);
            return true;

        } catch (IOException e) {
            log.error("Failed to delete PDF template file: {}", fileName, e);
            return false;
        }
    }

    /**
     * Load system default template from resources
     *
     * @param documentName The name of the PDF document type (e.g., "FULL_ITINERARY")
     * @return Template content from resources, or fallback template if not found
     */
    public String loadSystemDefaultTemplate(String documentName) {
        try {
            String resourcePath = "templates/pdf-templates/" + documentName.toLowerCase() + "_default.html";
            var resource = getClass().getClassLoader().getResourceAsStream(resourcePath);

            if (resource == null) {
                log.warn("System default PDF template not found for document: {}, using fallback", documentName);
                return generateFallbackTemplate(documentName);
            }

            String template = new String(resource.readAllBytes());
            log.info("Loaded system default PDF template for document: {}", documentName);
            return template;

        } catch (Exception e) {
            log.error("Failed to load system default PDF template for document: {}", documentName, e);
            return generateFallbackTemplate(documentName);
        }
    }

    /**
     * Generate a fallback template when system default cannot be loaded
     *
     * @param documentName The name of the PDF document type
     * @return Basic HTML template
     */
    private String generateFallbackTemplate(String documentName) {
        String rootVar = switch (documentName) {
            case "FULL_ITINERARY" -> "itinerary";
            default -> "data";
        };

        return String.format("""
            <!DOCTYPE html>
            <html xmlns:th="http://www.thymeleaf.org">
            <head>
                <meta charset="UTF-8"/>
                <title>%s</title>
                <style>
                    @page {
                        size: A4 portrait;
                        margin: 20mm 15mm;
                    }
                    body {
                        font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                        font-size: 12pt;
                        line-height: 1.5;
                        color: #333;
                    }
                    .header {
                        background: linear-gradient(135deg, #5a1e03 0%%, #3e3117 100%%);
                        color: white;
                        padding: 20px;
                        margin-bottom: 20px;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 24pt;
                    }
                    .content {
                        padding: 0 10px;
                    }
                    .section {
                        margin-bottom: 20px;
                    }
                    .section h2 {
                        color: #5a1e03;
                        border-bottom: 2px solid #5a1e03;
                        padding-bottom: 5px;
                    }
                    table {
                        width: 100%%;
                        border-collapse: collapse;
                        margin: 10px 0;
                    }
                    th, td {
                        border: 1px solid #ddd;
                        padding: 8px;
                        text-align: left;
                    }
                    th {
                        background-color: #f5f5f5;
                    }
                    .footer {
                        margin-top: 30px;
                        padding-top: 10px;
                        border-top: 1px solid #ddd;
                        font-size: 10pt;
                        color: #666;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1 th:text="${%s.name}">Document Title</h1>
                    <p th:if="${%s.code != null}" th:text="${%s.code}">Code</p>
                </div>

                <div class="content">
                    <p>This is the fallback template for the %s document.</p>
                    <p>Please customize this template in the PDF Templates management section.</p>
                </div>

                <div class="footer">
                    <p>Generated on <span th:text="${#temporals.format(#temporals.createNow(), 'dd MMM yyyy HH:mm')}">Date</span></p>
                </div>
            </body>
            </html>
            """, documentName, rootVar, rootVar, rootVar, documentName);
    }

    /**
     * Generate unique file name for template
     *
     * @param documentName The PDF document name
     * @param templateName The template name
     * @return Generated filename
     */
    public String generateFileName(String documentName, String templateName) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);

        // Sanitize names to be filesystem-safe
        String sanitizedDocument = documentName.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
        String sanitizedTemplate = templateName.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();

        return String.format("%s_%s_%s.html", sanitizedDocument, sanitizedTemplate, timestamp);
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

    /**
     * Get the file size in bytes
     *
     * @param fileName The filename
     * @return File size in bytes, or 0 if file not found
     */
    public long getFileSize(String fileName) {
        try {
            Path filePath = Paths.get(templateStoragePath, fileName);
            if (Files.exists(filePath)) {
                return Files.size(filePath);
            }
        } catch (IOException e) {
            log.error("Failed to get file size for: {}", fileName, e);
        }
        return 0L;
    }

    /**
     * Check if a template file exists
     *
     * @param fileName The filename
     * @return true if file exists
     */
    public boolean templateFileExists(String fileName) {
        Path filePath = Paths.get(templateStoragePath, fileName);
        return Files.exists(filePath);
    }
}
