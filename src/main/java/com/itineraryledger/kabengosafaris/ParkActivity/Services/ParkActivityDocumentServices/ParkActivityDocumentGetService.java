package com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityDocumentServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityDocumentDTOs.ParkActivityDocumentDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityDocument;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityDocument.DocumentType;
import com.itineraryledger.kabengosafaris.ParkActivity.Repositories.ParkActivityDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving park activity documents.
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ParkActivityDocumentGetService {

    private final ParkActivityDocumentRepository parkActivityDocumentRepository;
    private final ParkActivityDocumentStorageService storageService;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "title", "documentType", "fileName", "fileSize", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    @Autowired
    public ParkActivityDocumentGetService(
        ParkActivityDocumentRepository parkActivityDocumentRepository,
        ParkActivityDocumentStorageService storageService,
        IdObfuscator idObfuscator
    ) {
        this.parkActivityDocumentRepository = parkActivityDocumentRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
    }

    public ParkActivityDocumentDTO toDTO(ParkActivityDocument document) {
        String obfuscatedId = idObfuscator.encodeId(document.getId());
        return ParkActivityDocumentDTO.builder()
            .id(obfuscatedId)
            .parkId(idObfuscator.encodeId(document.getParkActivity().getPark().getId()))
            .parkName(document.getParkActivity().getPark().getName())
            .activityId(idObfuscator.encodeId(document.getParkActivity().getActivity().getId()))
            .activityName(document.getParkActivity().getActivity().getName())
            .title(document.getTitle())
            .documentType(document.getDocumentType())
            .documentTypeDisplayName(document.getDocumentType().getDisplayName())
            .documentTypeDescription(document.getDocumentType().getDescription())
            .documentUrl(storageService.constructDocumentUrl(obfuscatedId))
            .fileDocumentUrl(storageService.constructFileDocumentUrl(document.getFileName()))
            .fileName(document.getFileName())
            .originalFileName(document.getOriginalFileName())
            .fileSize(document.getFileSize())
            .fileSizeFormatted(document.getFileSize() != null ? storageService.formatFileSize(document.getFileSize()) : null)
            .fileType(document.getFileType())
            .description(document.getDescription())
            .version(document.getVersion())
            .notes(document.getNotes())
            .validFrom(document.getValidFrom())
            .validTo(document.getValidTo())
            .isCurrentlyValid(document.isCurrentlyValid())
            .isActive(document.getIsActive())
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .build();
    }

    public ResponseEntity<?> getAllDocuments(
            String obfuscatedParkId,
            String obfuscatedActivityId,
            DocumentType documentType,
            Boolean isActive,
            String title,
            String version,
            Boolean currentlyValid,
            String parkName,
            String activityName,
            Boolean parkIsActive,
            Boolean activityIsActive,
            Boolean hasTariff,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        Specification<ParkActivityDocument> spec = Specification.unrestricted();

        // Filter by park ID
        if (obfuscatedParkId != null && !obfuscatedParkId.isBlank()) {
            try {
                Long parkId = idObfuscator.decodeId(obfuscatedParkId);
                spec = spec.and(ParkActivityDocumentSpecification.byParkId(parkId));
            } catch (Exception e) {
                log.warn("Failed to decode park ID: {}", obfuscatedParkId);
            }
        }

        // Filter by activity ID
        if (obfuscatedActivityId != null && !obfuscatedActivityId.isBlank()) {
            try {
                Long activityId = idObfuscator.decodeId(obfuscatedActivityId);
                spec = spec.and(ParkActivityDocumentSpecification.byActivityId(activityId));
            } catch (Exception e) {
                log.warn("Failed to decode activity ID: {}", obfuscatedActivityId);
            }
        }

        if (documentType != null) {
            spec = spec.and(ParkActivityDocumentSpecification.byDocumentType(documentType));
        }
        if (isActive != null) {
            spec = spec.and(ParkActivityDocumentSpecification.byIsActive(isActive));
        }
        if (title != null && !title.isBlank()) {
            spec = spec.and(ParkActivityDocumentSpecification.byTitle(title));
        }
        if (version != null && !version.isBlank()) {
            spec = spec.and(ParkActivityDocumentSpecification.byVersion(version));
        }
        if (Boolean.TRUE.equals(currentlyValid)) {
            spec = spec.and(ParkActivityDocumentSpecification.currentlyValid(LocalDateTime.now()));
        }
        if (parkName != null && !parkName.isBlank()) {
            spec = spec.and(ParkActivityDocumentSpecification.byParkName(parkName));
        }
        if (activityName != null && !activityName.isBlank()) {
            spec = spec.and(ParkActivityDocumentSpecification.byActivityName(activityName));
        }
        if (parkIsActive != null) {
            spec = spec.and(ParkActivityDocumentSpecification.byParkIsActive(parkIsActive));
        }
        if (activityIsActive != null) {
            spec = spec.and(ParkActivityDocumentSpecification.byActivityIsActive(activityIsActive));
        }
        if (hasTariff != null) {
            spec = spec.and(ParkActivityDocumentSpecification.byActivityHasTariff(hasTariff));
        }

        // Sorting with validation
        String validatedSortBy = validateSortField(sortBy);
        if (validatedSortBy == null) {
            log.warn("Invalid sort field: {}", sortBy);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
            );
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

        Page<ParkActivityDocument> documentPage = parkActivityDocumentRepository.findAll(spec, pageable);

        List<ParkActivityDocumentDTO> documentDTOs = documentPage.getContent().stream()
            .map(this::toDTO)
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("documents", documentDTOs);
        response.put("currentPage", documentPage.getNumber());
        response.put("totalItems", documentPage.getTotalElements());
        response.put("totalPages", documentPage.getTotalPages());
        response.put("pageSize", documentPage.getSize());
        response.put("hasNext", documentPage.hasNext());
        response.put("hasPrevious", documentPage.hasPrevious());
        response.put("validSortFields", VALID_SORT_FIELDS);
        response.put("currentSortBy", validatedSortBy);
        response.put("currentSortDir", sortDirection != null ? sortDirection : "desc");

        return ResponseEntity.ok(ApiResponse.success(200, "Park activity documents retrieved successfully", response));
    }

    public ResponseEntity<?> getDocumentById(String obfuscatedId) {
        log.info("Getting park activity document with ID: {}", obfuscatedId);

        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            ParkActivityDocument document = parkActivityDocumentRepository.findById(id).orElse(null);

            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park activity document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            ParkActivityDocumentDTO documentDTO = toDTO(document);

            // Circular navigation
            Long nextId = parkActivityDocumentRepository.findNextId(id).orElse(null);
            Long previousId = parkActivityDocumentRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = parkActivityDocumentRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = parkActivityDocumentRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("document", documentDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok(ApiResponse.success(200, "Park activity document retrieved successfully", response));

        } catch (Exception e) {
            log.warn("Failed to decode park activity document ID: {}", obfuscatedId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
            );
        }
    }

    public ResponseEntity<?> getDocumentsByParkActivity(String obfuscatedParkId, String obfuscatedActivityId) {
        log.info("Getting documents for park-activity: parkId={}, activityId={}", obfuscatedParkId, obfuscatedActivityId);

        try {
            Long parkId = idObfuscator.decodeId(obfuscatedParkId);
            Long activityId = idObfuscator.decodeId(obfuscatedActivityId);

            List<ParkActivityDocument> documents = parkActivityDocumentRepository.findByParkActivityOrderByCreatedAtDesc(parkId, activityId);

            List<ParkActivityDocumentDTO> documentDTOs = documents.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Park activity documents retrieved successfully", documentDTOs));

        } catch (Exception e) {
            log.warn("Failed to decode park or activity ID", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid park or activity ID", "INVALID_ID")
            );
        }
    }

    public ResponseEntity<?> getDocumentsByParkId(String obfuscatedParkId) {
        log.info("Getting documents for park: {}", obfuscatedParkId);

        try {
            Long parkId = idObfuscator.decodeId(obfuscatedParkId);
            List<ParkActivityDocument> documents = parkActivityDocumentRepository.findByParkIdOrderByCreatedAtDesc(parkId);

            List<ParkActivityDocumentDTO> documentDTOs = documents.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Park activity documents retrieved successfully", documentDTOs));

        } catch (Exception e) {
            log.warn("Failed to decode park ID: {}", obfuscatedParkId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid park ID", "INVALID_PARK_ID")
            );
        }
    }

    public ResponseEntity<?> getDocumentsByActivityId(String obfuscatedActivityId) {
        log.info("Getting documents for activity: {}", obfuscatedActivityId);

        try {
            Long activityId = idObfuscator.decodeId(obfuscatedActivityId);
            List<ParkActivityDocument> documents = parkActivityDocumentRepository.findByActivityIdOrderByCreatedAtDesc(activityId);

            List<ParkActivityDocumentDTO> documentDTOs = documents.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Park activity documents retrieved successfully", documentDTOs));

        } catch (Exception e) {
            log.warn("Failed to decode activity ID: {}", obfuscatedActivityId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid activity ID", "INVALID_ACTIVITY_ID")
            );
        }
    }

    public ParkActivityDocument getDocumentEntityByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        return parkActivityDocumentRepository.findByFileName(fileName).orElse(null);
    }
}
