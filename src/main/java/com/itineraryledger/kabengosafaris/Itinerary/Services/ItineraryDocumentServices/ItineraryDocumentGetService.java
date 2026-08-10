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
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

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
            List<DocumentType> documentTypes,
            List<String> statuses,
            List<String> validity,
            LocalDateTime createdAfter,
            String keyword,
            Boolean includeStats,
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

            /*
             * The multi-value facets. Every stat card on the list page is one of
             * these, so a card that cannot be clicked would be decoration.
             */
            spec = spec.and(ItineraryDocumentSpecification.byDocumentTypes(documentTypes));

            if (statuses != null && !statuses.isEmpty()) {
                List<Boolean> states = new java.util.ArrayList<>();
                if (statuses.contains("active")) states.add(true);
                if (statuses.contains("inactive")) states.add(false);
                // active + inactive is everything: contradictory pairs cancel out
                if (states.size() == 1) {
                    spec = spec.and(ItineraryDocumentSpecification.byIsActive(states.get(0)));
                }
            }

            if (validity != null && !validity.isEmpty()) {
                List<Specification<ItineraryDocument>> any = new java.util.ArrayList<>();
                if (validity.contains("expired")) any.add(ItineraryDocumentSpecification.expired());
                if (validity.contains("expiring")) any.add(ItineraryDocumentSpecification.expiringWithin(30));
                if (validity.contains("no-expiry")) any.add(ItineraryDocumentSpecification.noExpiry());
                if (!any.isEmpty()) {
                    Specification<ItineraryDocument> combined = any.get(0);
                    for (int i = 1; i < any.size(); i++) combined = combined.or(any.get(i));
                    spec = spec.and(combined);
                }
            }

            if (createdAfter != null) {
                spec = spec.and(ItineraryDocumentSpecification.createdAfter(createdAfter));
            }
            if (keyword != null && !keyword.isBlank()) {
                spec = spec.and(ItineraryDocumentSpecification.searchKeyword(keyword));
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
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            // the list page shows totalItems; totalElements stays for older callers
            response.put("totalItems", dtoPage.getTotalElements());
            /*
             * Counters for the WHOLE filtered set, from the same specification as
             * the rows — without them the page can only summarise what it loaded,
             * and the "All filtered / This page" toggle has to stay hidden.
             */
            if (includeStats == null || includeStats) {
                response.put("stats", computeStats(spec));
            }

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

    /** Dashboard counters for the current filter set. */
    private Map<String, Object> computeStats(Specification<ItineraryDocument> base) {
        return listStats.of(ItineraryDocument.class, base)
            .total()
            .count("active", ItineraryDocumentSpecification.byIsActive(true))
            .complement("inactive", "active")
            .count("expired", ItineraryDocumentSpecification.expired())
            .count("expiringSoon", ItineraryDocumentSpecification.expiringWithin(30))
            .count("noExpiry", ItineraryDocumentSpecification.noExpiry())
            .recency(ItineraryDocumentSpecification::createdAfter)
            .build();
    }

    public ResponseEntity<ApiResponse<?>> getDocumentById(String obfuscatedId) {
        return getDocumentById(obfuscatedId, null);
    }

    /**
     * One document, and where it sits in the set the caller came from.
     *
     * @param scopeParentId the itinerary, when opened from inside one — paging
     *                      then stays among that itinerary's documents
     */
    public ResponseEntity<ApiResponse<?>> getDocumentById(String obfuscatedId, String scopeParentId) {
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

            /*
             * Prev/next walks the SAME set the caller was looking at — this
             * itinerary's documents when scoped, everything otherwise — and
             * returns the position, so the record page can show 'N of M'. It used
             * to walk a raw id-ordered query with no position at all.
             */
            Long scopedParentId = null;
            if (scopeParentId != null && !scopeParentId.isBlank()) {
                try {
                    scopedParentId = idObfuscator.decodeId(scopeParentId);
                } catch (Exception ex) {
                    log.warn("Invalid scopeParentId {}, walking every document instead", scopeParentId);
                }
            }
            Specification<ItineraryDocument> navSpec = scopedParentId != null
                ? ItineraryDocumentSpecification.byItineraryId(scopedParentId)
                : Specification.unrestricted();

            Map<String, Object> nav = recordNavigation.navigate(
                ItineraryDocument.class, navSpec, DEFAULT_SORT_FIELD, false, id);
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("document", documentDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));
            response.put("scopeParentId", scopeParentId);

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
