package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceDocumentServices;

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

import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDocumentDTOs.InvoiceDocumentDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceDocumentRepository;
import com.itineraryledger.kabengosafaris.Invoice.Specifications.InvoiceDocumentSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving invoice documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceDocumentGetService {

    private final InvoiceDocumentRepository invoiceDocumentRepository;
    private final InvoiceDocumentStorageService storageService;
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
            String invoiceId,
            DocumentType documentType,
            Boolean isActive,
            Boolean isGenerated,
            String title,
            String version,
            Boolean currentlyValid,
            String invoiceCode,
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
        log.info("Fetching invoice documents with filters");

        try {
            Long decodedInvoiceId = null;
            if (invoiceId != null && !invoiceId.isBlank()) {
                try {
                    decodedInvoiceId = idObfuscator.decodeId(invoiceId);
                } catch (Exception e) {
                    log.warn("Failed to decode invoice ID: {}", invoiceId);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid invoice ID", "INVALID_INVOICE_ID")
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

            Specification<InvoiceDocument> spec = InvoiceDocumentSpecification.byInvoiceId(decodedInvoiceId)
                .and(InvoiceDocumentSpecification.byDocumentType(documentType))
                .and(InvoiceDocumentSpecification.byIsActive(isActive))
                .and(InvoiceDocumentSpecification.byIsGenerated(isGenerated))
                .and(InvoiceDocumentSpecification.byTitleContains(title))
                .and(InvoiceDocumentSpecification.byVersion(version))
                .and(InvoiceDocumentSpecification.byInvoiceCode(invoiceCode));

            if (Boolean.TRUE.equals(currentlyValid)) {
                spec = spec.and(InvoiceDocumentSpecification.byCurrentlyValid(LocalDateTime.now()));
            }

            /*
             * The list page's facets. Each ORs inside its own dimension and ANDs
             * across — asking for "receipt or invoice PDF, expiring this month"
             * is one question, not two lists intersected by hand.
             */
            spec = spec.and(InvoiceDocumentSpecification.byDocumentTypes(documentTypes));

            if (statuses != null && !statuses.isEmpty()) {
                List<Boolean> states = statuses.stream()
                    .map(state -> "active".equalsIgnoreCase(state) ? Boolean.TRUE
                        : "inactive".equalsIgnoreCase(state) ? Boolean.FALSE : null)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
                // active AND inactive is every document: a contradiction cancels
                if (states.size() == 1) {
                    spec = spec.and(InvoiceDocumentSpecification.byIsActive(states.get(0)));
                }
            }

            if (validity != null && !validity.isEmpty()) {
                List<Specification<InvoiceDocument>> any = new java.util.ArrayList<>();
                if (validity.contains("expired")) any.add(InvoiceDocumentSpecification.expired());
                if (validity.contains("expiring")) any.add(InvoiceDocumentSpecification.expiringWithin(30));
                if (validity.contains("no-expiry")) any.add(InvoiceDocumentSpecification.noExpiry());
                if (!any.isEmpty()) {
                    Specification<InvoiceDocument> combined = any.get(0);
                    for (int i = 1; i < any.size(); i++) combined = combined.or(any.get(i));
                    spec = spec.and(combined);
                }
            }

            if (createdAfter != null) {
                spec = spec.and(InvoiceDocumentSpecification.createdAfter(createdAfter));
            }
            if (keyword != null && !keyword.isBlank()) {
                spec = spec.and(InvoiceDocumentSpecification.searchKeyword(keyword));
            }

            Page<InvoiceDocument> documentPage = invoiceDocumentRepository.findAll(spec, pageable);
            Page<InvoiceDocumentDTO> dtoPage = documentPage.map(this::toDTO);

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
                ApiResponse.success(200, "Invoice documents retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching invoice documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch invoice documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    private Map<String, Object> computeStats(Specification<InvoiceDocument> base) {
        return listStats.of(InvoiceDocument.class, base)
            .total()
            .count("active", InvoiceDocumentSpecification.byIsActive(true))
            .complement("inactive", "active")
            .count("expired", InvoiceDocumentSpecification.expired())
            .count("expiringSoon", InvoiceDocumentSpecification.expiringWithin(30))
            .count("noExpiry", InvoiceDocumentSpecification.noExpiry())
            .recency(InvoiceDocumentSpecification::createdAfter)
            .build();
    }

    public ResponseEntity<ApiResponse<?>> getDocumentById(String obfuscatedId) {
        return getDocumentById(obfuscatedId, null);
    }

    /**
     * One document, and where it sits in the set the caller came from.
     *
     * @param scopeParentId the invoice, when opened from inside one — paging then
     *                      stays among that invoice's documents
     */
    public ResponseEntity<ApiResponse<?>> getDocumentById(String obfuscatedId, String scopeParentId) {
        log.info("Fetching invoice document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode invoice document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            InvoiceDocument document = invoiceDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            InvoiceDocumentDTO documentDTO = toDTO(document);

            /*
             * Prev/next walks the SAME set the caller was looking at — this
             * invoice's documents when scoped, everything otherwise — and returns
             * the position, so the record page can show 'N of M'. It used to walk
             * a raw id-ordered query with no position at all.
             */
            Long scopedParentId = null;
            if (scopeParentId != null && !scopeParentId.isBlank()) {
                try {
                    scopedParentId = idObfuscator.decodeId(scopeParentId);
                } catch (Exception ex) {
                    log.warn("Invalid scopeParentId {}, walking every document instead", scopeParentId);
                }
            }
            Specification<InvoiceDocument> navSpec = scopedParentId != null
                ? InvoiceDocumentSpecification.byInvoiceId(scopedParentId)
                : Specification.unrestricted();

            Map<String, Object> nav = recordNavigation.navigate(
                InvoiceDocument.class, navSpec, DEFAULT_SORT_FIELD, false, id);
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
                ApiResponse.success(200, "Invoice document retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching invoice document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch invoice document", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentsByInvoiceId(String invoiceId) {
        log.info("Fetching documents for invoice: {}", invoiceId);

        try {
            Long decodedInvoiceId;
            try {
                decodedInvoiceId = idObfuscator.decodeId(invoiceId);
            } catch (Exception e) {
                log.warn("Failed to decode invoice ID: {}", invoiceId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid invoice ID", "INVALID_INVOICE_ID")
                );
            }

            List<InvoiceDocument> documents = invoiceDocumentRepository.findByInvoiceIdOrderByCreatedAtDesc(decodedInvoiceId);
            List<InvoiceDocumentDTO> documentDTOs = documents.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice documents retrieved successfully", documentDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching invoice documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch invoice documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public InvoiceDocument getDocumentEntityById(String obfuscatedId) {
        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            return invoiceDocumentRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get invoice document entity: {}", obfuscatedId, e);
            return null;
        }
    }

    public InvoiceDocument getDocumentEntityByFileName(String fileName) {
        return invoiceDocumentRepository.findByFileName(fileName).orElse(null);
    }

    public InvoiceDocumentDTO toDTO(InvoiceDocument document) {
        if (document == null) {
            return null;
        }

        String obfuscatedId = idObfuscator.encodeId(document.getId());
        String invoiceObfuscatedId = document.getInvoice() != null
            ? idObfuscator.encodeId(document.getInvoice().getId())
            : null;

        LocalDateTime now = LocalDateTime.now();
        Boolean isCurrentlyValid = document.getIsActive() &&
            (document.getValidFrom() == null || !document.getValidFrom().isAfter(now)) &&
            (document.getValidTo() == null || !document.getValidTo().isBefore(now));

        return InvoiceDocumentDTO.builder()
            .id(obfuscatedId)
            .invoiceId(invoiceObfuscatedId)
            .invoiceCode(document.getInvoice() != null ? document.getInvoice().getInvoiceCode() : null)
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
            .fileType(document.getFileType())
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
