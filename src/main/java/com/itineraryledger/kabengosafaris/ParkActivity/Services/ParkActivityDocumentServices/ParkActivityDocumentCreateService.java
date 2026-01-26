package com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityDocumentServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityId;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityRepository;
import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityDocumentDTOs.CreateParkActivityDocumentDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityDocumentDTOs.ParkActivityDocumentDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityDocument;
import com.itineraryledger.kabengosafaris.ParkActivity.Repositories.ParkActivityDocumentRepository;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for creating park activity documents.
 */
@Service
@Slf4j
@Transactional
public class ParkActivityDocumentCreateService {

    private final ParkActivityDocumentRepository parkActivityDocumentRepository;
    private final ParkActivityRepository parkActivityRepository;
    private final ParkRepository parkRepository;
    private final ActivityRepository activityRepository;
    private final ParkActivityDocumentStorageService storageService;
    private final ParkActivityDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ParkActivityDocumentCreateService(
        ParkActivityDocumentRepository parkActivityDocumentRepository,
        ParkActivityRepository parkActivityRepository,
        ParkRepository parkRepository,
        ActivityRepository activityRepository,
        ParkActivityDocumentStorageService storageService,
        ParkActivityDocumentGetService getService,
        IdObfuscator idObfuscator
    ) {
        this.parkActivityDocumentRepository = parkActivityDocumentRepository;
        this.parkActivityRepository = parkActivityRepository;
        this.parkRepository = parkRepository;
        this.activityRepository = activityRepository;
        this.storageService = storageService;
        this.getService = getService;
        this.idObfuscator = idObfuscator;
    }

    @AuditLogAnnotation(
        action = "CREATE_PARK_ACTIVITY_DOCUMENTS",
        description = "Uploading park activity documents",
        entityType = "ParkActivityDocument"
    )
    public ResponseEntity<ApiResponse<?>> uploadDocuments(List<CreateParkActivityDocumentDTO> documentDTOs) {
        log.info("Uploading {} park activity documents", documentDTOs != null ? documentDTOs.size() : 0);

        try {
            if (documentDTOs == null || documentDTOs.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No documents provided", "NO_DOCUMENTS_PROVIDED")
                );
            }

            // Validate total request size
            long totalSize = 0;
            for (CreateParkActivityDocumentDTO dto : documentDTOs) {
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

            // Validate each document
            List<String> validationErrors = new ArrayList<>();
            for (int i = 0; i < documentDTOs.size(); i++) {
                CreateParkActivityDocumentDTO dto = documentDTOs.get(i);

                if (dto.getParkId() == null || dto.getParkId().isBlank()) {
                    validationErrors.add(String.format("Document %d: Park ID is required", i + 1));
                    continue;
                }

                if (dto.getActivityId() == null || dto.getActivityId().isBlank()) {
                    validationErrors.add(String.format("Document %d: Activity ID is required", i + 1));
                    continue;
                }

                if (dto.getDocument() == null || dto.getDocument().isEmpty()) {
                    validationErrors.add(String.format("Document %d: Document file is required", i + 1));
                    continue;
                }

                if (dto.getTitle() == null || dto.getTitle().isBlank()) {
                    validationErrors.add(String.format("Document %d: Title is required", i + 1));
                    continue;
                }

                if (dto.getDocumentType() == null) {
                    validationErrors.add(String.format("Document %d: Document type is required", i + 1));
                    continue;
                }

                String documentError = storageService.validateDocument(dto.getDocument());
                if (documentError != null) {
                    String filename = dto.getDocument().getOriginalFilename() != null
                        ? dto.getDocument().getOriginalFilename()
                        : "unknown";
                    validationErrors.add(String.format("Document %d (%s): %s", i + 1, filename, documentError));
                }

                // Validate park-activity relationship exists
                try {
                    Long parkId = idObfuscator.decodeId(dto.getParkId());
                    Long activityId = idObfuscator.decodeId(dto.getActivityId());

                    Park park = parkRepository.findById(parkId).orElse(null);
                    Activity activity = activityRepository.findById(activityId).orElse(null);

                    if (park == null) {
                        validationErrors.add(String.format("Document %d: Park not found", i + 1));
                        continue;
                    }

                    if (activity == null) {
                        validationErrors.add(String.format("Document %d: Activity not found", i + 1));
                        continue;
                    }

                    // Check if ParkActivity relationship exists
                    ParkActivityId parkActivityId = new ParkActivityId(park, activity);
                    if (!parkActivityRepository.existsById(parkActivityId)) {
                        validationErrors.add(String.format("Document %d: Park-Activity relationship does not exist. The activity must be associated with the park first.", i + 1));
                    }
                } catch (Exception e) {
                    validationErrors.add(String.format("Document %d: Invalid Park or Activity ID", i + 1));
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join("; ", validationErrors);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, errorMessage, "VALIDATION_ERROR")
                );
            }

            // Save documents
            List<ParkActivityDocumentDTO> createdDocuments = new ArrayList<>();
            List<String> savedFileNames = new ArrayList<>();

            try {
                for (CreateParkActivityDocumentDTO dto : documentDTOs) {
                    Long parkId = idObfuscator.decodeId(dto.getParkId());
                    Long activityId = idObfuscator.decodeId(dto.getActivityId());

                    Park park = parkRepository.findById(parkId).orElse(null);
                    Activity activity = activityRepository.findById(activityId).orElse(null);

                    if (park == null || activity == null) {
                        continue;
                    }

                    ParkActivityId parkActivityId = new ParkActivityId(park, activity);
                    ParkActivity parkActivity = parkActivityRepository.findById(parkActivityId).orElse(null);

                    if (parkActivity == null) {
                        continue;
                    }

                    // Save file
                    MultipartFile file = dto.getDocument();
                    String savedFileName = storageService.saveDocument(file);
                    if (savedFileName == null) {
                        rollbackSavedFiles(savedFileNames);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.error(500, "Failed to save document file: " + file.getOriginalFilename(), "STORAGE_ERROR")
                        );
                    }
                    savedFileNames.add(savedFileName);

                    // Create entity
                    ParkActivityDocument document = ParkActivityDocument.builder()
                        .parkActivity(parkActivity)
                        .title(dto.getTitle())
                        .documentType(dto.getDocumentType())
                        .fileUrl(storageService.constructDocumentUrl(savedFileName))
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

                    document = parkActivityDocumentRepository.save(document);
                    createdDocuments.add(getService.toDTO(document));
                }

                log.info("{} park activity documents created successfully", createdDocuments.size());

                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, createdDocuments.size() + " document(s) uploaded successfully", createdDocuments)
                );

            } catch (Exception e) {
                rollbackSavedFiles(savedFileNames);
                log.error("Failed to create park activity document records, rolled back {} files", savedFileNames.size(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create document records", "DATABASE_ERROR")
                );
            }

        } catch (Exception e) {
            log.error("Error uploading park activity documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to upload park activity documents", "PARK_ACTIVITY_DOCUMENT_UPLOAD_FAILED")
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
