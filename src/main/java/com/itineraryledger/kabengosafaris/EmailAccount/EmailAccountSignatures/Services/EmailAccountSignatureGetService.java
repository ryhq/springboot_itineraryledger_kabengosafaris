package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.DTOs.EmailAccountSignatureDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.EmailAccountSignature;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Repository.EmailAccountSignatureRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * EmailSignatureGetService - Service for retrieving email signatures
 *
 * Responsibilities:
 * - Retrieve signatures with pagination, filtering and sorting
 * - Get individual signatures
 * - Preview signatures with variable substitution
 * - Convert entities to DTOs with obfuscated IDs
 */
@Service
@Slf4j
public class EmailAccountSignatureGetService {

    @Autowired
    private EmailAccountRepository emailAccountRepository;

    @Autowired
    private EmailAccountSignatureRepository emailAccountSignatureRepository;

    @Autowired
    private EmailAccountSignatureService emailAccountSignatureService;

    @Autowired
    private IdObfuscator idObfuscator;

    /**
     * Get all signatures for an email account with pagination, filtering and sorting
     *
     * @param emailAccountIdObfuscated Obfuscated email account ID
     * @param enabled Filter by enabled status (optional)
     * @param isDefault Filter by default status (optional)
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortDir Sort direction ("asc" or "desc")
     * @return ResponseEntity with paginated signatures
     */
    public ResponseEntity<ApiResponse<?>> getAllSignatures(String emailAccountIdObfuscated, Boolean enabled,
            Boolean isDefault, int page, int size, String sortDir) {
        log.debug("Fetching signatures for email account: {} with filters - enabled: {}, isDefault: {}, page: {}, size: {}, sortDir: {}",
                emailAccountIdObfuscated, enabled, isDefault, page, size, sortDir);

        try {
            Long emailAccountId = idObfuscator.decodeId(emailAccountIdObfuscated);

            // Validate email account exists
            EmailAccount emailAccount = emailAccountRepository.findById(emailAccountId).orElse(null);
            if (emailAccount == null) {
                log.warn("Email account not found: {}", emailAccountId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND")
                );
            }

            // Validate pagination
            if (page < 0) {
                log.warn("Invalid page number: {}", page);
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Page number cannot be negative", "INVALID_PAGE")
                );
            }
            if (size <= 0) {
                log.warn("Invalid page size: {}", size);
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Page size must be greater than 0", "INVALID_SIZE")
                );
            }

            // Setup sorting
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

            // Build dynamic specification
            Specification<EmailAccountSignature> specification = Specification.unrestricted();

            specification = specification.and(EmailAccountSignatureSpecification.emailAccountId(emailAccountId));

            if (enabled != null) {
                specification = specification.and(EmailAccountSignatureSpecification.enabled(enabled));
            }

            if (isDefault != null) {
                specification = specification.and(EmailAccountSignatureSpecification.isDefault(isDefault));
            }

            // Fetch signatures with specification and pagination
            Page<EmailAccountSignature> signaturesPage = emailAccountSignatureRepository.findAll(specification, pageable);

            var dtos = signaturesPage.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("signatures", dtos);
            response.put("currentPage", signaturesPage.getNumber());
            response.put("totalItems", signaturesPage.getTotalElements());
            response.put("totalPages", signaturesPage.getTotalPages());

            log.info("Successfully fetched {} signatures on page {}", dtos.size(), page);
            return ResponseEntity.ok(ApiResponse.success(200, "Signatures retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching signatures", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch signatures", "SIGNATURES_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a single signature by ID
     *
     * @param emailAccountIdObfuscated Obfuscated email account ID
     * @param signatureIdObfuscated Obfuscated signature ID
     * @return ResponseEntity with signature details
     */
    public ResponseEntity<ApiResponse<?>> getSignature(String emailAccountIdObfuscated, String signatureIdObfuscated) {
        log.debug("Fetching signature: {}", signatureIdObfuscated);

        try {
            Long emailAccountId = idObfuscator.decodeId(emailAccountIdObfuscated);
            Long signatureId = idObfuscator.decodeId(signatureIdObfuscated);

            EmailAccountSignature signature = emailAccountSignatureRepository.findById(signatureId).orElse(null);
            if (signature == null || !signature.getEmailAccount().getId().equals(emailAccountId)) {
                log.warn("Signature not found or does not belong to account: {}", emailAccountId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Signature not found", "SIGNATURE_NOT_FOUND")
                );
            }

            log.debug("Signature retrieved successfully: {}", signatureId);
            return ResponseEntity.ok(ApiResponse.success(200, "Signature retrieved successfully", convertToDTO(signature)));

        } catch (Exception e) {
            log.error("Error fetching signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch signature", "SIGNATURE_FETCH_FAILED")
            );
        }
    }

    /**
     * Get signature preview with variable substitution
     *
     * @param emailAccountIdObfuscated Obfuscated email account ID
     * @param signatureIdObfuscated Obfuscated signature ID
     * @param variables Map of variable values for substitution (optional)
     * @return ResponseEntity with signature preview
     */
    public ResponseEntity<ApiResponse<?>> getSignaturePreview(String emailAccountIdObfuscated,
            String signatureIdObfuscated, Map<String, String> variables) {
        log.debug("Fetching signature preview: {}", signatureIdObfuscated);

        try {
            Long emailAccountId = idObfuscator.decodeId(emailAccountIdObfuscated);
            Long signatureId = idObfuscator.decodeId(signatureIdObfuscated);

            EmailAccountSignature signature = emailAccountSignatureRepository.findById(signatureId).orElse(null);
            if (signature == null || !signature.getEmailAccount().getId().equals(emailAccountId)) {
                log.warn("Signature not found or does not belong to account: {}", emailAccountId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Signature not found", "SIGNATURE_NOT_FOUND")
                );
            }

            // Get signature content with variables replaced
            String signatureContent = emailAccountSignatureService.getSignatureWithVariables(signature, variables != null ? variables : Map.of());

            Map<String, Object> response = new HashMap<>();
            response.put("id", idObfuscator.encodeId(signature.getId()));
            response.put("name", signature.getName());
            response.put("signature", signatureContent);
            response.put("variables", emailAccountSignatureService.parseVariablesJson(signature.getVariablesJson()));

            log.debug("Signature preview retrieved successfully: {}", signatureId);
            return ResponseEntity.ok(ApiResponse.success(200, "Signature preview retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching signature preview", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch signature preview", "SIGNATURE_PREVIEW_FAILED")
            );
        }
    }

    /**
     * Get signature content with all metadata (for WYSIWYG editor)
     *
     * @param emailAccountIdObfuscated Obfuscated email account ID
     * @param signatureIdObfuscated Obfuscated signature ID
     * @return ResponseEntity with full signature details
     */
    public ResponseEntity<ApiResponse<?>> getSignatureContent(String emailAccountIdObfuscated, String signatureIdObfuscated) {
        log.debug("Fetching signature content: {}", signatureIdObfuscated);

        try {
            Long emailAccountId = idObfuscator.decodeId(emailAccountIdObfuscated);
            Long signatureId = idObfuscator.decodeId(signatureIdObfuscated);

            EmailAccountSignature signature = emailAccountSignatureRepository.findById(signatureId).orElse(null);
            if (signature == null || !signature.getEmailAccount().getId().equals(emailAccountId)) {
                log.warn("Signature not found or does not belong to account: {}", emailAccountId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Signature not found", "SIGNATURE_NOT_FOUND")
                );
            }

            // Read raw signature content from file
            String content = emailAccountSignatureService.readSignatureFile(signature.getFileName());
            if (content == null) {
                log.error("Failed to read signature file: {}", signature.getFileName());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResponse.error(500, "Failed to read signature file", "SIGNATURE_FILE_READ_FAILED")
                );
            }

            // Build DTO with content included
            EmailAccountSignatureDTO signatureDTO = EmailAccountSignatureDTO.builder()
                    .id(idObfuscator.encodeId(signature.getId()))
                    .emailAccountId(idObfuscator.encodeId(signature.getEmailAccount().getId()))
                    .name(signature.getName())
                    .description(signature.getDescription())
                    .content(content)
                    .fileName(signature.getFileName())
                    .isDefault(signature.getIsDefault())
                    .enabled(signature.getEnabled())
                    .isSystemDefault(signature.getIsSystemDefault())
                    .variables(emailAccountSignatureService.parseVariablesJson(signature.getVariablesJson()))
                    .fileSize(signature.getFileSize())
                    .createdAt(signature.getCreatedAt())
                    .updatedAt(signature.getUpdatedAt())
                    .build();

            log.debug("Signature content retrieved successfully: {}", signatureId);
            return ResponseEntity.ok(ApiResponse.success(200, "Signature content retrieved successfully", signatureDTO));

        } catch (Exception e) {
            log.error("Error fetching signature content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch signature content", "SIGNATURE_CONTENT_FETCH_FAILED")
            );
        }
    }

    /**
     * Download signature HTML file
     * Returns the raw HTML file for download or preview in browser
     *
     * @param emailAccountIdObfuscated Obfuscated email account ID
     * @param signatureIdObfuscated Obfuscated signature ID
     * @param download Whether to force download (true) or display inline (false)
     * @return ResponseEntity with HTML content and appropriate headers
     */
    public ResponseEntity<?> downloadSignatureFile(String emailAccountIdObfuscated, String signatureIdObfuscated, boolean download) {
        log.debug("Downloading signature file: {}, download mode: {}", signatureIdObfuscated, download);

        try {
            Long emailAccountId = idObfuscator.decodeId(emailAccountIdObfuscated);
            Long signatureId = idObfuscator.decodeId(signatureIdObfuscated);

            EmailAccountSignature signature = emailAccountSignatureRepository.findById(signatureId).orElse(null);
            if (signature == null || !signature.getEmailAccount().getId().equals(emailAccountId)) {
                log.warn("Signature not found or does not belong to account: {}", emailAccountId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Signature not found", "SIGNATURE_NOT_FOUND")
                );
            }

            // Read signature content from file
            String content = emailAccountSignatureService.readSignatureFile(signature.getFileName());
            if (content == null) {
                log.error("Failed to read signature file: {}", signature.getFileName());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResponse.error(500, "Failed to read signature file", "SIGNATURE_FILE_READ_FAILED")
                );
            }

            // Set up response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.set(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8");

            // Sanitize filename for download (remove special characters, spaces)
            String sanitizedName = signature.getName().replaceAll("[^a-zA-Z0-9_-]", "_");
            String filename = sanitizedName + ".html";

            if (download) {
                // Force download
                headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                );
            } else {
                // Display inline in browser
                headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.inline()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                );
            }

            log.debug("Signature file downloaded successfully: {}", signatureId);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);

        } catch (Exception e) {
            log.error("Error downloading signature file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to download signature file", "SIGNATURE_DOWNLOAD_FAILED")
            );
        }
    }

    /**
     * Get signature HTML file by filename
     * Returns the raw HTML file using the filename from the signature DTO
     *
     * @param emailAccountIdObfuscated Obfuscated email account ID
     * @param fileName The signature filename (e.g., "developerks_developerks_1764855656760.html")
     * @param download Whether to force download (true) or display inline (false)
     * @return ResponseEntity with HTML content and appropriate headers
     */
    public ResponseEntity<?> getSignatureFileByName(String emailAccountIdObfuscated, String fileName, boolean download) {
        log.debug("Getting signature file by name: {}, download mode: {}", fileName, download);

        try {
            Long emailAccountId = idObfuscator.decodeId(emailAccountIdObfuscated);

            // Find signature by filename and email account ID
            EmailAccountSignature signature = emailAccountSignatureRepository.findByFileNameAndEmailAccountId(fileName, emailAccountId);
            if (signature == null) {
                log.warn("Signature file not found or does not belong to account: {} - {}", emailAccountId, fileName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Signature file not found", "SIGNATURE_FILE_NOT_FOUND")
                );
            }

            // Read signature content from file
            String content = emailAccountSignatureService.readSignatureFile(fileName);
            if (content == null) {
                log.error("Failed to read signature file: {}", fileName);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResponse.error(500, "Failed to read signature file", "SIGNATURE_FILE_READ_FAILED")
                );
            }

            // Set up response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.set(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8");

            if (download) {
                // Force download
                headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                );
            } else {
                // Display inline in browser
                headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.inline()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                );
            }

            log.debug("Signature file retrieved successfully by filename: {}", fileName);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);

        } catch (Exception e) {
            log.error("Error getting signature file by name", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to retrieve signature file", "SIGNATURE_FILE_RETRIEVE_FAILED")
            );
        }
    }

    /**
     * Convert EmailSignature entity to DTO with obfuscated ID
     *
     * @param signature The entity to convert
     * @return EmailSignatureDTO with obfuscated ID
     */
    public EmailAccountSignatureDTO convertToDTO(EmailAccountSignature signature) {
        return EmailAccountSignatureDTO.builder()
                .id(idObfuscator.encodeId(signature.getId()))
                .emailAccountId(idObfuscator.encodeId(signature.getEmailAccount().getId()))
                .name(signature.getName())
                .description(signature.getDescription())
                .fileName(signature.getFileName())
                .isDefault(signature.getIsDefault())
                .enabled(signature.getEnabled())
                .isSystemDefault(signature.getIsSystemDefault())
                .variables(emailAccountSignatureService.parseVariablesJson(signature.getVariablesJson()))
                .fileSize(signature.getFileSize())
                .createdAt(signature.getCreatedAt())
                .updatedAt(signature.getUpdatedAt())
                .build();
    }
}
