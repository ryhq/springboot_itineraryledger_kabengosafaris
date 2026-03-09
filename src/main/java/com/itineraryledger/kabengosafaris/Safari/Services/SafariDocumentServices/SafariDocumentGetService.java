package com.itineraryledger.kabengosafaris.Safari.Services.SafariDocumentServices;

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

import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDocumentDTOs.SafariDocumentDTO;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving safari documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SafariDocumentGetService {

    private final SafariDocumentRepository safariDocumentRepository;
    private final SafariDocumentStorageService storageService;
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
            String safariId,
            DocumentType documentType,
            Boolean isActive,
            Boolean isGenerated,
            String title,
            String version,
            Boolean currentlyValid,
            String safariName,
            String safariCode,
            Boolean safariIsActive,
            SafariState safariState,
            Boolean quotationDocumentsOnly,
            Boolean travelDocumentsOnly,
            Boolean voucherDocumentsOnly,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        log.info("Fetching safari documents with filters");

        try {
            Long decodedSafariId = null;
            if (safariId != null && !safariId.isBlank()) {
                try {
                    decodedSafariId = idObfuscator.decodeId(safariId);
                } catch (Exception e) {
                    log.warn("Failed to decode safari ID: {}", safariId);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
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

            Specification<SafariDocument> spec = SafariDocumentSpecification.bySafariId(decodedSafariId)
                .and(SafariDocumentSpecification.byDocumentType(documentType))
                .and(SafariDocumentSpecification.byIsActive(isActive))
                .and(SafariDocumentSpecification.byIsGenerated(isGenerated))
                .and(SafariDocumentSpecification.byTitleContains(title))
                .and(SafariDocumentSpecification.byVersion(version))
                .and(SafariDocumentSpecification.bySafariName(safariName))
                .and(SafariDocumentSpecification.bySafariCode(safariCode))
                .and(SafariDocumentSpecification.bySafariIsActive(safariIsActive))
                .and(SafariDocumentSpecification.bySafariState(safariState));

            if (Boolean.TRUE.equals(currentlyValid)) {
                spec = spec.and(SafariDocumentSpecification.byCurrentlyValid(LocalDateTime.now()));
            }

            if (Boolean.TRUE.equals(quotationDocumentsOnly)) {
                spec = spec.and(SafariDocumentSpecification.byQuotationDocuments());
            }

            if (Boolean.TRUE.equals(travelDocumentsOnly)) {
                spec = spec.and(SafariDocumentSpecification.byTravelDocuments());
            }

            if (Boolean.TRUE.equals(voucherDocumentsOnly)) {
                spec = spec.and(SafariDocumentSpecification.byVoucherDocuments());
            }

            Page<SafariDocument> documentPage = safariDocumentRepository.findAll(spec, pageable);
            Page<SafariDocumentDTO> dtoPage = documentPage.map(this::toDTO);

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

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari documents retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching safari documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch safari documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentById(String obfuscatedId) {
        log.info("Fetching safari document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode safari document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            SafariDocument document = safariDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            SafariDocumentDTO documentDTO = toDTO(document);

            // Circular navigation
            Long nextId = safariDocumentRepository.findNextId(id).orElse(null);
            Long previousId = safariDocumentRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = safariDocumentRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = safariDocumentRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("document", documentDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari document retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching safari document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch safari document", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentsBySafariId(String safariId) {
        log.info("Fetching documents for safari: {}", safariId);

        try {
            Long decodedSafariId;
            try {
                decodedSafariId = idObfuscator.decodeId(safariId);
            } catch (Exception e) {
                log.warn("Failed to decode safari ID: {}", safariId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            List<SafariDocument> documents = safariDocumentRepository.findBySafariIdOrderByCreatedAtDesc(decodedSafariId);
            List<SafariDocumentDTO> documentDTOs = documents.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari documents retrieved successfully", documentDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching safari documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch safari documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public SafariDocument getDocumentEntityById(String obfuscatedId) {
        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            return safariDocumentRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get safari document entity: {}", obfuscatedId, e);
            return null;
        }
    }

    public SafariDocument getDocumentEntityByFileName(String fileName) {
        return safariDocumentRepository.findByFileName(fileName).orElse(null);
    }

    public SafariDocumentDTO toDTO(SafariDocument document) {
        if (document == null) {
            return null;
        }

        String obfuscatedId = idObfuscator.encodeId(document.getId());
        Safari safari = document.getSafari();
        String safariObfuscatedId = safari != null
            ? idObfuscator.encodeId(safari.getId())
            : null;

        LocalDateTime now = LocalDateTime.now();
        Boolean isCurrentlyValid = document.getIsActive() &&
            (document.getValidFrom() == null || !document.getValidFrom().isAfter(now)) &&
            (document.getValidTo() == null || !document.getValidTo().isBefore(now));

        return SafariDocumentDTO.builder()
            .id(obfuscatedId)
            .safariId(safariObfuscatedId)
            .safariName(safari != null ? safari.getName() : null)
            .safariCode(safari != null ? safari.getCode() : null)
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
            .isGenerated(document.getIsGenerated())
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .build();
    }
}
