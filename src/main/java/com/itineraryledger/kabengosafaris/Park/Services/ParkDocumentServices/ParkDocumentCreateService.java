package com.itineraryledger.kabengosafaris.Park.Services.ParkDocumentServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkDocumentDTOs.CreateParkDocumentDTO;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkDocumentDTOs.ParkDocumentDTO;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkDocument;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for creating park documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ParkDocumentCreateService {

    private final ParkDocumentRepository parkDocumentRepository;
    private final ParkRepository parkRepository;
    private final ParkDocumentStorageService storageService;
    private final ParkDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "CREATE_PARK_DOCUMENTS",
        description = "Uploading park documents",
        entityType = "ParkDocument"
    )
    public ResponseEntity<ApiResponse<?>> uploadDocuments(List<CreateParkDocumentDTO> documentDTOs) {
        log.info("Uploading {} park documents", documentDTOs != null ? documentDTOs.size() : 0);

        try {
            if (documentDTOs == null || documentDTOs.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No documents provided", "NO_DOCUMENTS_PROVIDED")
                );
            }

            long totalSize = 0;
            for (CreateParkDocumentDTO dto : documentDTOs) {
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
                CreateParkDocumentDTO dto = documentDTOs.get(i);

                if (dto.getParkId() == null || dto.getParkId().isBlank()) {
                    validationErrors.add(String.format("Document %d: Park ID is required", i + 1));
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
                    Long parkId = idObfuscator.decodeId(dto.getParkId());
                    if (!parkRepository.existsById(parkId)) {
                        validationErrors.add(String.format("Document %d: Park not found", i + 1));
                    }
                } catch (Exception e) {
                    validationErrors.add(String.format("Document %d: Invalid Park ID", i + 1));
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join("; ", validationErrors);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, errorMessage, "VALIDATION_ERROR")
                );
            }

            List<ParkDocumentDTO> createdDocuments = new ArrayList<>();
            List<String> savedFileNames = new ArrayList<>();

            try {
                for (CreateParkDocumentDTO dto : documentDTOs) {
                    Long parkId = idObfuscator.decodeId(dto.getParkId());
                    Park park = parkRepository.findById(parkId).orElse(null);

                    if (park == null) {
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

                    ParkDocument document = ParkDocument.builder()
                        .park(park)
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
                        .build();

                    document = parkDocumentRepository.save(document);
                    createdDocuments.add(getService.toDTO(document));
                }

                log.info("{} park documents created successfully", createdDocuments.size());

                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, createdDocuments.size() + " document(s) uploaded successfully", createdDocuments)
                );

            } catch (Exception e) {
                rollbackSavedFiles(savedFileNames);
                log.error("Failed to create park document records, rolled back {} files", savedFileNames.size(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create document records", "DATABASE_ERROR")
                );
            }

        } catch (Exception e) {
            log.error("Error uploading park documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to upload park documents", "PARK_DOCUMENT_UPLOAD_FAILED")
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
}
