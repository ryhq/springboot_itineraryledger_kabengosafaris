package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationDocumentServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDocumentDTOs.AccommodationDocumentDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDocumentDTOs.CreateAccommodationDocumentDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationDocument;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationDocumentRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for creating accommodation documents.
 *
 * Provides:
 * - Bulk upload documents using list of CreateAccommodationDocumentDTO
 */
@Service
@Slf4j
@Transactional
public class AccommodationDocumentCreateService {

    private final AccommodationDocumentRepository accommodationDocumentRepository;
    private final AccommodationRepository accommodationRepository;
    private final AccommodationDocumentStorageService storageService;
    private final AccommodationDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public AccommodationDocumentCreateService(
        AccommodationDocumentRepository accommodationDocumentRepository,
        AccommodationRepository accommodationRepository,
        AccommodationDocumentStorageService storageService,
        AccommodationDocumentGetService getService,
        IdObfuscator idObfuscator
    ) {
        this.accommodationDocumentRepository = accommodationDocumentRepository;
        this.accommodationRepository = accommodationRepository;
        this.storageService = storageService;
        this.getService = getService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Upload multiple documents for accommodations
     *
     * @param documentDTOs List of CreateAccommodationDocumentDTO containing document files and metadata
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @AuditLogAnnotation(
        action = "CREATE_ACCOMMODATION_DOCUMENTS",
        description = "Uploading accommodation documents",
        entityType = "AccommodationDocument"
    )
    public ResponseEntity<ApiResponse<?>> uploadDocuments(List<CreateAccommodationDocumentDTO> documentDTOs) {
        log.info("Uploading {} accommodation documents", documentDTOs != null ? documentDTOs.size() : 0);

        try {
            // Validate input
            if (documentDTOs == null || documentDTOs.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No documents provided",
                        "NO_DOCUMENTS_PROVIDED"
                    )
                );
            }

            // Calculate total request size
            long totalSize = 0;
            for (CreateAccommodationDocumentDTO dto : documentDTOs) {
                if (dto.getDocument() != null) {
                    totalSize += dto.getDocument().getSize();
                }
            }

            // Validate total request size
            String requestSizeError = storageService.validateRequestSize(totalSize);
            if (requestSizeError != null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        requestSizeError,
                        "REQUEST_SIZE_EXCEEDED"
                    )
                );
            }

            // Validate all documents first
            List<String> validationErrors = new ArrayList<>();
            for (int i = 0; i < documentDTOs.size(); i++) {
                CreateAccommodationDocumentDTO dto = documentDTOs.get(i);

                // Validate accommodation ID
                if (dto.getAccommodationId() == null || dto.getAccommodationId().isBlank()) {
                    validationErrors.add(String.format("Document %d: Accommodation ID is required", i + 1));
                    continue;
                }

                // Validate document file
                if (dto.getDocument() == null || dto.getDocument().isEmpty()) {
                    validationErrors.add(String.format("Document %d: Document file is required", i + 1));
                    continue;
                }

                // Validate title
                if (dto.getTitle() == null || dto.getTitle().isBlank()) {
                    validationErrors.add(String.format("Document %d: Title is required", i + 1));
                    continue;
                }

                // Validate document type
                if (dto.getDocumentType() == null) {
                    validationErrors.add(String.format("Document %d: Document type is required", i + 1));
                    continue;
                }

                // Validate document file (size and extension)
                String documentError = storageService.validateDocument(dto.getDocument());
                if (documentError != null) {
                    String filename = dto.getDocument().getOriginalFilename() != null
                        ? dto.getDocument().getOriginalFilename()
                        : "unknown";
                    validationErrors.add(String.format("Document %d (%s): %s", i + 1, filename, documentError));
                }

                // Validate accommodation exists
                try {
                    Long accommodationId = idObfuscator.decodeId(dto.getAccommodationId());
                    if (!accommodationRepository.existsById(accommodationId)) {
                        validationErrors.add(String.format("Document %d: Accommodation not found", i + 1));
                    }
                } catch (Exception e) {
                    validationErrors.add(String.format("Document %d: Invalid Accommodation ID", i + 1));
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join("; ", validationErrors);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        errorMessage,
                        "VALIDATION_ERROR"
                    )
                );
            }

            // Process each document
            List<AccommodationDocumentDTO> createdDocuments = new ArrayList<>();
            List<String> savedFileNames = new ArrayList<>();

            try {
                for (CreateAccommodationDocumentDTO dto : documentDTOs) {
                    Long accommodationId = idObfuscator.decodeId(dto.getAccommodationId());
                    Accommodation accommodation = accommodationRepository.findById(accommodationId).orElse(null);

                    if (accommodation == null) {
                        continue; // Already validated, skip if somehow null
                    }

                    // Save document file to storage (with SHA-256 hashed filename)
                    MultipartFile file = dto.getDocument();
                    String savedFileName = storageService.saveDocument(file);
                    if (savedFileName == null) {
                        // Rollback previously saved files
                        rollbackSavedFiles(savedFileNames);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.error(
                                500,
                                "Failed to save document file: " + file.getOriginalFilename(),
                                "STORAGE_ERROR"
                            )
                        );
                    }
                    savedFileNames.add(savedFileName);

                    // Create entity
                    AccommodationDocument document = AccommodationDocument.builder()
                        .accommodation(accommodation)
                        .title(dto.getTitle())
                        .documentType(dto.getDocumentType())
                        .fileUrl(storageService.constructFileDocumentUrl(savedFileName))
                        .fileName(savedFileName)
                        .fileSize(file.getSize())
                        .fileType(storageService.getMimeType(savedFileName))
                        .description(dto.getDescription())
                        .version(dto.getVersion())
                        .notes(dto.getNotes())
                        .validFrom(dto.getValidFrom())
                        .validTo(dto.getValidTo())
                        .isActive(true)
                        .build();

                    document = accommodationDocumentRepository.save(document);
                    createdDocuments.add(getService.toDTO(document));
                }

                log.info("{} accommodation documents created successfully", createdDocuments.size());

                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(
                        201,
                        createdDocuments.size() + " document(s) uploaded successfully",
                        createdDocuments
                    )
                );

            } catch (Exception e) {
                // Rollback: delete saved files if database operations fail
                rollbackSavedFiles(savedFileNames);
                log.error("Failed to create accommodation document records, rolled back {} files",
                    savedFileNames.size(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                        500,
                        "Failed to create document records",
                        "DATABASE_ERROR"
                    )
                );
            }

        } catch (Exception e) {
            log.error("Error uploading accommodation documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to upload accommodation documents",
                    "ACCOMMODATION_DOCUMENT_UPLOAD_FAILED"
                )
            );
        }
    }

    /**
     * Rollback saved files on error
     */
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
}
