package com.itineraryledger.kabengosafaris.Activity.Services.ActivityDocumentServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDocumentDTOs.ActivityDocumentDTO;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDocumentDTOs.CreateActivityDocumentDTO;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityDocument;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Activity.Repositories.ActivityDocumentRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for creating activity documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ActivityDocumentCreateService {

    private final ActivityDocumentRepository activityDocumentRepository;
    private final ActivityRepository activityRepository;
    private final ActivityDocumentStorageService storageService;
    private final ActivityDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "CREATE_ACTIVITY_DOCUMENTS",
        description = "Uploading activity documents",
        entityType = "ActivityDocument"
    )
    public ResponseEntity<ApiResponse<?>> uploadDocuments(List<CreateActivityDocumentDTO> documentDTOs) {
        log.info("Uploading {} activity documents", documentDTOs != null ? documentDTOs.size() : 0);

        try {
            if (documentDTOs == null || documentDTOs.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No documents provided", "NO_DOCUMENTS_PROVIDED")
                );
            }

            long totalSize = 0;
            for (CreateActivityDocumentDTO dto : documentDTOs) {
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
                CreateActivityDocumentDTO dto = documentDTOs.get(i);

                if (dto.getActivityId() == null || dto.getActivityId().isBlank()) {
                    validationErrors.add(String.format("Document %d: Activity ID is required", i + 1));
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
                    Long activityId = idObfuscator.decodeId(dto.getActivityId());
                    if (!activityRepository.existsById(activityId)) {
                        validationErrors.add(String.format("Document %d: Activity not found", i + 1));
                    }
                } catch (Exception e) {
                    validationErrors.add(String.format("Document %d: Invalid Activity ID", i + 1));
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join("; ", validationErrors);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, errorMessage, "VALIDATION_ERROR")
                );
            }

            List<ActivityDocumentDTO> createdDocuments = new ArrayList<>();
            List<String> savedFileNames = new ArrayList<>();

            try {
                for (CreateActivityDocumentDTO dto : documentDTOs) {
                    Long activityId = idObfuscator.decodeId(dto.getActivityId());
                    Activity activity = activityRepository.findById(activityId).orElse(null);

                    if (activity == null) {
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

                    ActivityDocument document = ActivityDocument.builder()
                        .activity(activity)
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

                    document = activityDocumentRepository.save(document);
                    createdDocuments.add(getService.toDTO(document));
                }

                log.info("{} activity documents created successfully", createdDocuments.size());

                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, createdDocuments.size() + " document(s) uploaded successfully", createdDocuments)
                );

            } catch (Exception e) {
                rollbackSavedFiles(savedFileNames);
                log.error("Failed to create activity document records, rolled back {} files", savedFileNames.size(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create document records", "DATABASE_ERROR")
                );
            }

        } catch (Exception e) {
            log.error("Error uploading activity documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to upload activity documents", "ACTIVITY_DOCUMENT_UPLOAD_FAILED")
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
