package com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryDocumentServices;

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

import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDocumentDTOs.ItineraryDocumentDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving itinerary documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItineraryDocumentGetService {

    private final ItineraryDocumentRepository itineraryDocumentRepository;
    private final ItineraryDocumentStorageService storageService;
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
            String itineraryId,
            DocumentType documentType,
            Boolean isActive,
            Boolean isGenerated,
            String title,
            String version,
            Boolean currentlyValid,
            String itineraryName,
            String itineraryCode,
            Boolean itineraryIsActive,
            ItineraryStatus itineraryStatus,
            TripType tripType,
            BudgetCategory budgetCategory,
            Boolean quotationDocumentsOnly,
            Boolean travelDocumentsOnly,
            Boolean voucherDocumentsOnly,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        log.info("Fetching itinerary documents with filters");

        try {
            Long decodedItineraryId = null;
            if (itineraryId != null && !itineraryId.isBlank()) {
                try {
                    decodedItineraryId = idObfuscator.decodeId(itineraryId);
                } catch (Exception e) {
                    log.warn("Failed to decode itinerary ID: {}", itineraryId);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
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

            Specification<ItineraryDocument> spec = ItineraryDocumentSpecification.byItineraryId(decodedItineraryId)
                .and(ItineraryDocumentSpecification.byDocumentType(documentType))
                .and(ItineraryDocumentSpecification.byIsActive(isActive))
                .and(ItineraryDocumentSpecification.byIsGenerated(isGenerated))
                .and(ItineraryDocumentSpecification.byTitleContains(title))
                .and(ItineraryDocumentSpecification.byVersion(version))
                .and(ItineraryDocumentSpecification.byItineraryName(itineraryName))
                .and(ItineraryDocumentSpecification.byItineraryCode(itineraryCode))
                .and(ItineraryDocumentSpecification.byItineraryIsActive(itineraryIsActive))
                .and(ItineraryDocumentSpecification.byItineraryStatus(itineraryStatus))
                .and(ItineraryDocumentSpecification.byItineraryTripType(tripType))
                .and(ItineraryDocumentSpecification.byItineraryBudgetCategory(budgetCategory));

            if (Boolean.TRUE.equals(currentlyValid)) {
                spec = spec.and(ItineraryDocumentSpecification.byCurrentlyValid(LocalDateTime.now()));
            }

            if (Boolean.TRUE.equals(quotationDocumentsOnly)) {
                spec = spec.and(ItineraryDocumentSpecification.byQuotationDocuments());
            }

            if (Boolean.TRUE.equals(travelDocumentsOnly)) {
                spec = spec.and(ItineraryDocumentSpecification.byTravelDocuments());
            }

            if (Boolean.TRUE.equals(voucherDocumentsOnly)) {
                spec = spec.and(ItineraryDocumentSpecification.byVoucherDocuments());
            }

            Page<ItineraryDocument> documentPage = itineraryDocumentRepository.findAll(spec, pageable);
            Page<ItineraryDocumentDTO> dtoPage = documentPage.map(this::toDTO);

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
            response.put("currentSortDir", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary documents retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching itinerary documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch itinerary documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentById(String obfuscatedId) {
        log.info("Fetching itinerary document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode itinerary document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            ItineraryDocument document = itineraryDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            ItineraryDocumentDTO documentDTO = toDTO(document);

            // Circular navigation
            Long nextId = itineraryDocumentRepository.findNextId(id).orElse(null);
            Long previousId = itineraryDocumentRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = itineraryDocumentRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = itineraryDocumentRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("document", documentDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary document retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching itinerary document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch itinerary document", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentsByItineraryId(String itineraryId) {
        log.info("Fetching documents for itinerary: {}", itineraryId);

        try {
            Long decodedItineraryId;
            try {
                decodedItineraryId = idObfuscator.decodeId(itineraryId);
            } catch (Exception e) {
                log.warn("Failed to decode itinerary ID: {}", itineraryId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
                );
            }

            List<ItineraryDocument> documents = itineraryDocumentRepository.findByItineraryIdOrderByCreatedAtDesc(decodedItineraryId);
            List<ItineraryDocumentDTO> documentDTOs = documents.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary documents retrieved successfully", documentDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching itinerary documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch itinerary documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ItineraryDocument getDocumentEntityById(String obfuscatedId) {
        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            return itineraryDocumentRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get itinerary document entity: {}", obfuscatedId, e);
            return null;
        }
    }

    public ItineraryDocument getDocumentEntityByFileName(String fileName) {
        return itineraryDocumentRepository.findByFileName(fileName).orElse(null);
    }

    public ItineraryDocumentDTO toDTO(ItineraryDocument document) {
        if (document == null) {
            return null;
        }

        String obfuscatedId = idObfuscator.encodeId(document.getId());
        Itinerary itinerary = document.getItinerary();
        String itineraryObfuscatedId = itinerary != null
            ? idObfuscator.encodeId(itinerary.getId())
            : null;

        LocalDateTime now = LocalDateTime.now();
        Boolean isCurrentlyValid = document.getIsActive() &&
            (document.getValidFrom() == null || !document.getValidFrom().isAfter(now)) &&
            (document.getValidTo() == null || !document.getValidTo().isBefore(now));

        return ItineraryDocumentDTO.builder()
            .id(obfuscatedId)
            .itineraryId(itineraryObfuscatedId)
            .itineraryName(itinerary != null ? itinerary.getName() : null)
            .itineraryCode(itinerary != null ? itinerary.getCode() : null)
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
