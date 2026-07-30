package com.itineraryledger.kabengosafaris.Park.Services.ParkDocumentServices;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Park.DTOs.ParkDocumentDTOs.ParkDocumentDTO;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkDocument;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving park documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParkDocumentGetService {

    private final ParkDocumentRepository parkDocumentRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final ParkDocumentStorageService storageService;
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

    public ResponseEntity<ApiResponse<?>> getAllDocuments(
            String parkId,
            DocumentType documentType,
            Boolean isActive,
            String title,
            String version,
            Boolean currentlyValid,
            String parkName,
            ParkType parkType,
            String region,
            Boolean tariffDocumentsOnly,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        log.info("Fetching park documents with filters");

        try {
            Long decodedParkId = null;
            if (parkId != null && !parkId.isBlank()) {
                try {
                    decodedParkId = idObfuscator.decodeId(parkId);
                } catch (Exception e) {
                    log.warn("Failed to decode park ID: {}", parkId);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid park ID", "INVALID_PARK_ID")
                    );
                }
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

            Specification<ParkDocument> spec = ParkDocumentSpecification.byParkId(decodedParkId)
                .and(ParkDocumentSpecification.byDocumentType(documentType))
                .and(ParkDocumentSpecification.byIsActive(isActive))
                .and(ParkDocumentSpecification.byTitleContains(title))
                .and(ParkDocumentSpecification.byVersion(version))
                .and(ParkDocumentSpecification.byParkName(parkName))
                .and(ParkDocumentSpecification.byParkType(parkType))
                .and(ParkDocumentSpecification.byParkRegion(region));

            if (Boolean.TRUE.equals(currentlyValid)) {
                spec = spec.and(ParkDocumentSpecification.byCurrentlyValid(LocalDateTime.now()));
            }

            if (Boolean.TRUE.equals(tariffDocumentsOnly)) {
                spec = spec.and(ParkDocumentSpecification.byTariffDocument());
            }

            Page<ParkDocument> documentPage = parkDocumentRepository.findAll(spec, pageable);
            Page<ParkDocumentDTO> dtoPage = documentPage.map(this::toDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("documents", dtoPage.getContent());
            response.put("currentPage", dtoPage.getNumber());
            response.put("totalPages", dtoPage.getTotalPages());
            response.put("totalElements", dtoPage.getTotalElements());
            response.put("pageSize", dtoPage.getSize());
            response.put("hasNext", dtoPage.hasNext());
            response.put("hasPrevious", dtoPage.hasPrevious());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
        // counters share the SAME spec as the rows, so cards and table always agree
        response.put("stats", computeStats(spec));

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park documents retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching park documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentById(String obfuscatedId, String scopeParentId) {
        log.info("Fetching park document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode park document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            ParkDocument document = parkDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            ParkDocumentDTO documentDTO = toDTO(document);

            // Decode optional scope parent ID for scoped navigation
            Long decodedParentId = null;
            if (scopeParentId != null && !scopeParentId.isEmpty()) {
                try {
                    decodedParentId = idObfuscator.decodeId(scopeParentId);
                } catch (Exception ex) {
                    log.warn("Invalid scopeParentId: {}, falling back to global navigation", scopeParentId);
                }
            }

            // Circular navigation (scoped if parent provided, global otherwise)
            Long nextId, previousId;
            if (decodedParentId != null) {
                nextId = parkDocumentRepository.findNextIdByParent(id, decodedParentId).orElse(null);
                previousId = parkDocumentRepository.findPreviousIdByParent(id, decodedParentId).orElse(null);
                if (nextId == null) nextId = parkDocumentRepository.findFirstIdByParent(decodedParentId).orElse(null);
                if (previousId == null) previousId = parkDocumentRepository.findLastIdByParent(decodedParentId).orElse(null);
            } else {
                nextId = parkDocumentRepository.findNextId(id).orElse(null);
                previousId = parkDocumentRepository.findPreviousId(id).orElse(null);
                if (nextId == null) nextId = parkDocumentRepository.findFirstId().orElse(null);
                if (previousId == null) previousId = parkDocumentRepository.findLastId().orElse(null);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("document", documentDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park document retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching park document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park document", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentsByParkId(String parkId) {
        log.info("Fetching documents for park: {}", parkId);

        try {
            Long decodedParkId;
            try {
                decodedParkId = idObfuscator.decodeId(parkId);
            } catch (Exception e) {
                log.warn("Failed to decode park ID: {}", parkId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park ID", "INVALID_PARK_ID")
                );
            }

            List<ParkDocument> documents = parkDocumentRepository.findByParkIdOrderByCreatedAtDesc(decodedParkId);
            List<ParkDocumentDTO> documentDTOs = documents.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park documents retrieved successfully", documentDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching park documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ParkDocument getDocumentEntityById(String obfuscatedId) {
        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            return parkDocumentRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get park document entity: {}", obfuscatedId, e);
            return null;
        }
    }

    public ParkDocument getDocumentEntityByFileName(String fileName) {
        return parkDocumentRepository.findByFileName(fileName).orElse(null);
    }

    public ParkDocumentDTO toDTO(ParkDocument document) {
        if (document == null) {
            return null;
        }

        String obfuscatedId = idObfuscator.encodeId(document.getId());
        String parkObfuscatedId = document.getPark() != null
            ? idObfuscator.encodeId(document.getPark().getId())
            : null;

        LocalDateTime now = LocalDateTime.now();
        Boolean isCurrentlyValid = document.getIsActive() &&
            (document.getValidFrom() == null || !document.getValidFrom().isAfter(now)) &&
            (document.getValidTo() == null || !document.getValidTo().isBefore(now));

        return ParkDocumentDTO.builder()
            .id(obfuscatedId)
            .parkId(parkObfuscatedId)
            .parkName(document.getPark() != null ? document.getPark().getName() : null)
            .title(document.getTitle())
            .documentType(document.getDocumentType())
            .documentTypeDisplayName(document.getDocumentType() != null ? document.getDocumentType().getDisplayName() : null)
            .documentTypeDescription(document.getDocumentType() != null ? document.getDocumentType().getDescription() : null)
            .documentUrl(storageService.constructDocumentUrl(obfuscatedId))
            .fileDocumentUrl(storageService.constructFileDocumentUrl(document.getFileName()))
            .fileName(document.getFileName())
            .originalFileName(document.getOriginalFileName())
            .fileSize(document.getFileSize())
            .fileSizeFormatted(document.getFileSize() != null ? storageService.formatFileSize(document.getFileSize()) : null)
            .fileType(storageService.getExtension(document.getFileName()))
            .description(document.getDescription())
            .version(document.getVersion())
            .notes(document.getNotes())
            .validFrom(document.getValidFrom())
            .validTo(document.getValidTo())
            .isCurrentlyValid(isCurrentlyValid)
            .isActive(document.getIsActive())
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .build();
    }

    /** Dashboard counters for the CURRENT filter set (see CLAUDE.md: stats on every list). */
    private Map<String, Object> computeStats(Specification<ParkDocument> base) {
        return listStats.of(ParkDocument.class, base)
            .total()
            .count("active", ParkDocumentSpecification.byIsActive(true))
            .complement("inactive", "active")
            .count("expired", ParkDocumentSpecification.expired())
            .count("expiringSoon", ParkDocumentSpecification.expiringWithin(30))
            .count("noExpiry", ParkDocumentSpecification.noExpiry())
            .recency(ParkDocumentSpecification::createdAfter)
            .build();
    }
}
