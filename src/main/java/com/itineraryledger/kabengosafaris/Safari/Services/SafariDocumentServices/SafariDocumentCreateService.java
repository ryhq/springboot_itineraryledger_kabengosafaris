package com.itineraryledger.kabengosafaris.Safari.Services.SafariDocumentServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDocumentDTOs.CreateSafariDocumentDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDocumentDTOs.SafariDocumentDTO;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariDocumentRepository;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for creating safari documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SafariDocumentCreateService {

    private final SafariDocumentRepository safariDocumentRepository;
    private final SafariRepository safariRepository;
    private final SafariDocumentStorageService storageService;
    private final SafariDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "CREATE_SAFARI_DOCUMENTS",
        description = "Uploading safari documents",
        entityType = "SafariDocument"
    )
    public ResponseEntity<ApiResponse<?>> uploadDocuments(List<CreateSafariDocumentDTO> documentDTOs) {
        log.info("Uploading {} safari documents", documentDTOs != null ? documentDTOs.size() : 0);

        try {
            if (documentDTOs == null || documentDTOs.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No documents provided", "NO_DOCUMENTS_PROVIDED")
                );
            }

            long totalSize = 0;
            for (CreateSafariDocumentDTO dto : documentDTOs) {
                if (dto.getDocument() != null) {
                    totalSize += dto.getDocument().getSize();
                }
            }

            String requestSizeError = storageService.validateRequestSize(totalSize);
            if (requestSizeError != null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, requestSizeError, "REQUEST_SIZE_EXCEEDED")
                );
            }

            List<String> validationErrors = new ArrayList<>();
            for (int i = 0; i < documentDTOs.size(); i++) {
                CreateSafariDocumentDTO dto = documentDTOs.get(i);

                if (dto.getSafariId() == null || dto.getSafariId().isBlank()) {
                    validationErrors.add(String.format("Document %d: Safari ID is required", i + 1));
                    continue;
                }

                if (dto.getTitle() == null || dto.getTitle().isBlank()) {
                    validationErrors.add(String.format("Document %d: Title is required", i + 1));
                    continue;
                }

                if (dto.getDocument() == null || dto.getDocument().isEmpty()) {
                    validationErrors.add(String.format("Document %d: Document file is required", i + 1));
                    continue;
                }

                String documentError = storageService.validateDocument(dto.getDocument());
                if (documentError != null) {
                    String filename = dto.getDocument().getOriginalFilename() != null
                        ? dto.getDocument().getOriginalFilename()
                        : "unknown";
                    validationErrors.add(String.format("Document %d (%s): %s", i + 1, filename, documentError));
                }

                try {
                    Long safariId = idObfuscator.decodeId(dto.getSafariId());
                    if (!safariRepository.existsById(safariId)) {
                        validationErrors.add(String.format("Document %d: Safari not found", i + 1));
                    }
                } catch (Exception e) {
                    validationErrors.add(String.format("Document %d: Invalid Safari ID", i + 1));
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join("; ", validationErrors);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, errorMessage, "VALIDATION_ERROR")
                );
            }

            List<SafariDocumentDTO> createdDocuments = new ArrayList<>();
            List<String> savedFileNames = new ArrayList<>();

            try {
                for (CreateSafariDocumentDTO dto : documentDTOs) {
                    Long safariId = idObfuscator.decodeId(dto.getSafariId());
                    Safari safari = safariRepository.findById(safariId).orElse(null);

                    if (safari == null) {
                        continue;
                    }

                    MultipartFile file = dto.getDocument();
                    String savedFileName = storageService.saveDocument(file);
                    if (savedFileName == null) {
                        rollbackSavedFiles(savedFileNames);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.error(500, "Failed to save document file: " + file.getOriginalFilename(), "STORAGE_ERROR")
                        );
                    }
                    savedFileNames.add(savedFileName);

                    SafariDocument document = SafariDocument.builder()
                        .safari(safari)
                        .title(dto.getTitle())
                        .documentType(dto.getDocumentType() != null ? dto.getDocumentType() : DocumentType.OTHER)
                        .fileUrl(storageService.constructFileDocumentUrl(savedFileName))
                        .fileName(savedFileName)
                        .originalFileName(file.getOriginalFilename())
                        .fileSize(file.getSize())
                        .fileType(storageService.getMimeType(savedFileName))
                        .description(dto.getDescription())
                        .version(dto.getVersion())
                        .validFrom(dto.getValidFrom())
                        .validTo(dto.getValidTo())
                        .notes(dto.getNotes())
                        .isActive(true)
                        .isGenerated(false) // User uploads are never system-generated
                        .build();

                    document = safariDocumentRepository.save(document);
                    createdDocuments.add(getService.toDTO(document));
                }

                log.info("{} safari documents created successfully", createdDocuments.size());

                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, createdDocuments.size() + " document(s) uploaded successfully", createdDocuments)
                );

            } catch (Exception e) {
                rollbackSavedFiles(savedFileNames);
                log.error("Failed to create safari document records, rolled back {} files", savedFileNames.size(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create document records", "DATABASE_ERROR")
                );
            }

        } catch (Exception e) {
            log.error("Error uploading safari documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to upload safari documents", "SAFARI_DOCUMENT_UPLOAD_FAILED")
            );
        }
    }

    private void rollbackSavedFiles(List<String> fileNames) {
        for (String fileName : fileNames) {
            try {
                storageService.deleteDocument(fileName);
                log.debug("Rolled back file: {}", fileName);
            } catch (Exception e) {
                log.warn("Failed to rollback file: {}", fileName, e);
            }
        }
    }

    // =========================================================================
    // SYSTEM-GENERATED DOCUMENT METHODS
    // =========================================================================

    /**
     * Save a system-generated PDF document (e.g., quotation, invoice, travel plan).
     * This method is intended for internal use by PDF generation services.
     *
     * @param safariId The safari ID (not obfuscated)
     * @param pdfBytes The generated PDF content
     * @param originalFileName The display filename (e.g., "quotation-SAF-001.pdf")
     * @param documentType The type of document
     * @param title The document title
     * @param description Optional description
     * @param version Optional version string
     * @param notes Optional notes
     * @return The created SafariDocumentDTO, or null if failed
     */
    @AuditLogAnnotation(
        action = "CREATE_GENERATED_SAFARI_DOCUMENT",
        description = "Saving system-generated safari document",
        entityType = "SafariDocument"
    )
    public SafariDocumentDTO saveGeneratedDocument(
            Long safariId,
            byte[] pdfBytes,
            String originalFileName,
            DocumentType documentType,
            String title,
            String description,
            String version,
            String notes
    ) {
        log.info("Saving generated document for safari {}: {}", safariId, title);

        try {
            if (safariId == null) {
                log.error("Safari ID is required for generated document");
                return null;
            }

            if (pdfBytes == null || pdfBytes.length == 0) {
                log.error("PDF content is required for generated document");
                return null;
            }

            if (title == null || title.isBlank()) {
                log.error("Title is required for generated document");
                return null;
            }

            Safari safari = safariRepository.findById(safariId).orElse(null);
            if (safari == null) {
                log.error("Safari not found: {}", safariId);
                return null;
            }

            // Save the PDF bytes to storage
            String savedFileName = storageService.saveDocumentBytes(pdfBytes, originalFileName);
            if (savedFileName == null) {
                log.error("Failed to save generated PDF file: {}", originalFileName);
                return null;
            }

            // Create the document record
            SafariDocument document = SafariDocument.builder()
                .safari(safari)
                .title(title)
                .documentType(documentType != null ? documentType : DocumentType.OTHER)
                .fileUrl(storageService.constructFileDocumentUrl(savedFileName))
                .fileName(savedFileName)
                .originalFileName(originalFileName != null ? originalFileName : "generated-document.pdf")
                .fileSize((long) pdfBytes.length)
                .fileType("application/pdf")
                .description(description)
                .version(version)
                .notes(notes)
                .isActive(true)
                .isGenerated(true) // System-generated documents
                .build();

            document = safariDocumentRepository.save(document);
            log.info("Generated document saved successfully: {} (ID: {})", title, document.getId());

            return getService.toDTO(document);

        } catch (Exception e) {
            log.error("Error saving generated document for safari {}", safariId, e);
            return null;
        }
    }

    /**
     * Simplified overload for saving a generated document with minimal parameters.
     */
    public SafariDocumentDTO saveGeneratedDocument(
            Long safariId,
            byte[] pdfBytes,
            String originalFileName,
            DocumentType documentType,
            String title
    ) {
        return saveGeneratedDocument(safariId, pdfBytes, originalFileName, documentType, title, null, null, null);
    }

    /**
     * Save a generated document using obfuscated safari ID.
     * Useful when called from controllers or external services.
     */
    public SafariDocumentDTO saveGeneratedDocumentByObfuscatedId(
            String obfuscatedSafariId,
            byte[] pdfBytes,
            String originalFileName,
            DocumentType documentType,
            String title,
            String description,
            String version,
            String notes
    ) {
        try {
            Long safariId = idObfuscator.decodeId(obfuscatedSafariId);
            return saveGeneratedDocument(safariId, pdfBytes, originalFileName, documentType, title, description, version, notes);
        } catch (Exception e) {
            log.error("Failed to decode safari ID: {}", obfuscatedSafariId, e);
            return null;
        }
    }
}
