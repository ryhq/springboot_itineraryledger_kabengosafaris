package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.EmailAccountSignature;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.SignatureVariable;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Repository.EmailAccountSignatureRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * EmailSignatureService - Handles file-based signature storage and variable substitution
 *
 * Responsibilities:
 * - Save and read signature files from disk
 * - Manage variable substitution in signatures
 * - Handle signature lifecycle (create, update, delete)
 * - Support for predefined variables: {senderName}, {userAccountName}, {currentDate}, {currentTime}
 */
@Service
@Slf4j
public class EmailAccountSignatureService {

    @Value("${email.signature.storage.path:/opt/lampp/htdocs/kabengosafaris/ItineraryLedger/email-signatures/}")
    private String signatureStoragePath;

    @Value("${email.signature.max.file.size:1048576}")
    private long maxFileSize;

    @Value("${email.signature.allowed.extensions:html,txt}")
    private String allowedExtensions;

    @Autowired
    private EmailAccountSignatureRepository emailAccountSignatureRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initialize signature storage directory
     */
    public void initializeStorageDirectory() {
        try {
            Path path = Paths.get(signatureStoragePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created signature storage directory: {}", signatureStoragePath);
            }
        } catch (IOException e) {
            log.error("Failed to create signature storage directory: {}", signatureStoragePath, e);
            throw new RuntimeException("Failed to initialize signature storage directory", e);
        }
    }

    /**
     * Save signature file to disk
     *
     * @param signatureContent HTML/text content of the signature
     * @param fileName Filename to save as
     * @return true if successful, false otherwise
     */
    public boolean saveSignatureFile(String signatureContent, String fileName) {
        try {
            initializeStorageDirectory();

            Path filePath = Paths.get(signatureStoragePath, fileName);

            // Check if file already exists
            if (Files.exists(filePath)) {
                log.warn("Signature file already exists: {}", fileName);
                return false;
            }

            // Check file size
            if (signatureContent.getBytes().length > maxFileSize) {
                log.warn("Signature content exceeds maximum file size: {} bytes", signatureContent.getBytes().length);
                return false;
            }

            Files.write(filePath, signatureContent.getBytes());
            log.info("Signature file saved successfully: {}", fileName);
            return true;

        } catch (IOException e) {
            log.error("Failed to save signature file: {}", fileName, e);
            return false;
        }
    }

    /**
     * Read signature file from disk
     *
     * @param fileName Filename to read
     * @return Signature content as string, or null if not found
     */
    public String readSignatureFile(String fileName) {
        try {
            Path filePath = Paths.get(signatureStoragePath, fileName);

            if (!Files.exists(filePath)) {
                log.warn("Signature file not found: {}", fileName);
                return null;
            }

            String content = Files.readString(filePath);
            log.debug("Signature file read successfully: {}", fileName);
            return content;

        } catch (IOException e) {
            log.error("Failed to read signature file: {}", fileName, e);
            return null;
        }
    }

    /**
     * Update signature file on disk
     *
     * @param fileName Filename to update
     * @param newContent New signature content
     * @return true if successful, false otherwise
     */
    public boolean updateSignatureFile(String fileName, String newContent) {
        try {
            Path filePath = Paths.get(signatureStoragePath, fileName);

            if (!Files.exists(filePath)) {
                log.warn("Signature file not found for update: {}", fileName);
                return false;
            }

            // Check file size
            if (newContent.getBytes().length > maxFileSize) {
                log.warn("New signature content exceeds maximum file size: {} bytes", newContent.getBytes().length);
                return false;
            }

            Files.write(filePath, newContent.getBytes());
            log.info("Signature file updated successfully: {}", fileName);
            return true;

        } catch (IOException e) {
            log.error("Failed to update signature file: {}", fileName, e);
            return false;
        }
    }

    /**
     * Delete signature file from disk
     *
     * @param fileName Filename to delete
     * @return true if successful, false otherwise
     */
    public boolean deleteSignatureFile(String fileName) {
        try {
            Path filePath = Paths.get(signatureStoragePath, fileName);

            if (!Files.exists(filePath)) {
                log.warn("Signature file not found for deletion: {}", fileName);
                return false;
            }

            Files.delete(filePath);
            log.info("Signature file deleted successfully: {}", fileName);
            return true;

        } catch (IOException e) {
            log.error("Failed to delete signature file: {}", fileName, e);
            return false;
        }
    }

    /**
     * Get signature with variables substituted
     *
     * @param emailSignature The EmailSignature entity
     * @param variables Map of variable names to values
     * @return Signature content with variables replaced
     */
    public String getSignatureWithVariables(EmailAccountSignature emailSignature, Map<String, String> variables) {
        String content = readSignatureFile(emailSignature.getFileName());

        if (content == null) {
            log.error("Failed to read signature file: {}", emailSignature.getFileName());
            return "";
        }

        return replaceVariables(content, emailSignature, variables);
    }

    /**
     * Replace variables in signature content
     * Supports both predefined and custom variables
     *
     * @param content Original signature content
     * @param emailSignature The EmailSignature entity
     * @param providedVariables Map of variable names to values
     * @return Content with variables replaced
     */
    public String replaceVariables(String content, EmailAccountSignature emailSignature, Map<String, String> providedVariables) {
        try {
            List<SignatureVariable> signatureVariables = parseVariablesJson(emailSignature.getVariablesJson());

            Map<String, String> variablesToUse = new HashMap<>();

            // Process each defined variable
            for (SignatureVariable var : signatureVariables) {
                String value = providedVariables.getOrDefault(var.getName(), var.getDefaultValue());

                // Handle special variables
                if (SignatureVariable.CURRENT_DATE.equals(var.getName())) {
                    value = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } else if (SignatureVariable.CURRENT_TIME.equals(var.getName())) {
                    value = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                }

                variablesToUse.put(var.getName(), value);
            }

            // Replace variables in content
            String result = content;
            for (Map.Entry<String, String> entry : variablesToUse.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }

            log.debug("Variables replaced successfully for signature: {}", emailSignature.getName());
            return result;

        } catch (Exception e) {
            log.error("Error replacing variables in signature: {}", emailSignature.getName(), e);
            return content;
        }
    }

    /**
     * Parse variables JSON string to list of SignatureVariable objects
     *
     * @param variablesJson JSON string containing variables
     * @return List of SignatureVariable objects
     */
    public List<SignatureVariable> parseVariablesJson(String variablesJson) {
        try {
            if (variablesJson == null || variablesJson.isEmpty()) {
                return List.of();
            }

            return objectMapper.readValue(
                    variablesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SignatureVariable.class)
            );

        } catch (Exception e) {
            log.error("Error parsing variables JSON: {}", variablesJson, e);
            return List.of();
        }
    }

    /**
     * Convert list of SignatureVariable objects to JSON string
     *
     * @param variables List of variables
     * @return JSON string representation
     */
    public String variablesToJson(List<SignatureVariable> variables) {
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (Exception e) {
            log.error("Error converting variables to JSON", e);
            return "[]";
        }
    }

    /**
     * Generate a unique filename for signature storage
     *
     * @param emailAccountName Name of the email account
     * @param signatureName Name of the signature
     * @return Generated filename
     */
    public String generateFileName(String emailAccountName, String signatureName) {
        String timestamp = System.currentTimeMillis() + "";
        return String.format("%s_%s_%s.html",
                emailAccountName.toLowerCase().replaceAll("[^a-z0-9]", ""),
                signatureName.toLowerCase().replaceAll("[^a-z0-9]", ""),
                timestamp
        );
    }

    /**
     * Get default signature for an email account with variables substituted
     *
     * @param emailAccountId Email account ID
     * @param variables Map of variable names to values
     * @return Signature content with variables replaced, or null if no default signature
     */
    public String getDefaultSignatureForAccount(Long emailAccountId, Map<String, String> variables) {
        var defaultSignature = emailAccountSignatureRepository.findByEmailAccountIdAndIsDefaultTrue(emailAccountId);

        if (defaultSignature.isEmpty()) {
            log.debug("No default signature found for email account: {}", emailAccountId);
            return null;
        }

        EmailAccountSignature signature = defaultSignature.get();

        if (!signature.getEnabled()) {
            log.debug("Default signature is disabled for email account: {}", emailAccountId);
            return null;
        }

        return getSignatureWithVariables(signature, variables);
    }

    /**
     * Get file size of stored signature
     *
     * @param fileName Filename
     * @return File size in bytes, or 0 if not found
     */
    public long getFileSize(String fileName) {
        try {
            Path filePath = Paths.get(signatureStoragePath, fileName);
            return Files.size(filePath);
        } catch (IOException e) {
            log.error("Failed to get file size: {}", fileName, e);
            return 0;
        }
    }

    /**
     * Validate file extension
     *
     * @param fileName Filename
     * @return true if extension is allowed
     */
    public boolean isValidFileExtension(String fileName) {
        String[] extensions = allowedExtensions.split(",");
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        for (String ext : extensions) {
            if (ext.trim().equalsIgnoreCase(fileExtension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate default signature HTML template
     * This is the system default signature that is created automatically for new email accounts
     * Loads the template from resources/templates/email-signatures/default_signature_template.html
     *
     * @param emailAccountName Name of the email account
     * @param emailAddress Email address
     * @return HTML content for default signature
     */
    public String generateDefaultSignatureTemplate(String emailAccountName, String emailAddress) {
        try {
            // Load template from resources
            var resource = getClass().getClassLoader().getResourceAsStream("templates/email-signatures/default_signature_template.html");

            if (resource == null) {
                log.error("Default signature template not found in resources");
                return generateFallbackSignature(emailAccountName, emailAddress);
            }

            String template = new String(resource.readAllBytes());

            String result = template;

            log.debug("Default signature template loaded and populated successfully");
            return result;

        } catch (Exception e) {
            log.error("Error loading default signature template", e);
            return generateFallbackSignature(emailAccountName, emailAddress);
        }
    }

    /**
     * Generate a simple fallback signature if template loading fails
     *
     * @param emailAccountName Name of the email account
     * @param emailAddress Email address
     * @return Simple HTML signature
     */
    private String generateFallbackSignature(String emailAccountName, String emailAddress) {
        return String.format(
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head><meta charset=\"UTF-8\"></head>\n" +
            "<body>\n" +
            "    <div style=\"font-family: Arial, sans-serif; font-size: 14px; color: #333;\">\n" +
            "        <hr style=\"border-top: 1px solid #e0e0e0;\">\n" +
            "        <p><strong>%s</strong></p>\n" +
            "        <p><a href=\"mailto:%s\">%s</a></p>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>",
            emailAccountName,
            emailAddress,
            emailAddress
        );
    }
}
