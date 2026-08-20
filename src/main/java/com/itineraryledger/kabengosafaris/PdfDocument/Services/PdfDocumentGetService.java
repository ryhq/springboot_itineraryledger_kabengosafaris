package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.PdfDocument.DTOs.PdfDocumentDTO;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfDocument;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Specification.PdfDocumentFilter;
import com.itineraryledger.kabengosafaris.PdfDocument.Specification.PdfDocumentSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving PDF document types
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PdfDocumentGetService {

    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfTemplateRepository pdfTemplateRepository;
    private final IdObfuscator idObfuscator;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "displayName", "enabled", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * The list: the rows, the counters and the sort, in one response.
     *
     * It used to return every document type as a bare array with no total, so the table
     * under it reported "1–10 of 0" while showing rows. Now on the house contract, with the
     * counters that matter — a type switched off produces nothing, and a type switched ON
     * with no enabled template has nothing to produce it from, which looks identical from
     * every other column.
     */
    public ResponseEntity<ApiResponse<?>> getAllDocuments(
        PdfDocumentFilter filter,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        try {
            PdfDocumentFilter resolved = filter != null ? filter : new PdfDocumentFilter();

            String validatedSortBy = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
            Pageable pageable = PageRequest.of(
                page == null || page < 0 ? 0 : page, pageSize, Sort.by(direction, validatedSortBy));

            Specification<PdfDocument> spec = buildSpec(resolved);
            Page<PdfDocument> found = pdfDocumentRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("pdfDocuments", found.getContent().stream().map(this::mapToDTO).toList());
            response.put("currentPage", found.getNumber());
            response.put("totalItems", found.getTotalElements());
            response.put("totalPages", found.getTotalPages());
            response.put("pageSize", found.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", direction.name().toLowerCase());
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "PDF documents retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing PDF documents", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to list PDF documents", "PDF_DOCUMENTS_LIST_FAILED"));
        }
    }

    /** ONE specification, shared by the rows, the counters and the record walk. */
    private Specification<PdfDocument> buildSpec(PdfDocumentFilter filter) {
        Specification<PdfDocument> spec = Specification.<PdfDocument>unrestricted()
            .and(PdfDocumentSpecification.searchKeyword(filter.effectiveKeyword()))
            .and(PdfDocumentSpecification.nameLike(filter.getName()));

        // contradictory pairs cancel to no constraint, as everywhere else
        boolean wantsOn = filter.hasStatus("enabled");
        boolean wantsOff = filter.hasStatus("disabled");
        if (wantsOn != wantsOff) {
            spec = spec.and(PdfDocumentSpecification.isEnabled(wantsOn));
        } else if (filter.getEnabled() != null) {
            spec = spec.and(PdfDocumentSpecification.isEnabled(filter.getEnabled()));
        }

        Specification<PdfDocument> quality = null;
        if (filter.wants("noTemplates")) quality = PdfDocumentSpecification.hasNoTemplates();
        if (filter.wants("nothingToRender")) {
            Specification<PdfDocument> extra = PdfDocumentSpecification.hasNoEnabledTemplate();
            quality = quality == null ? extra : quality.or(extra);
        }
        if (filter.wants("noSystemDefault")) {
            Specification<PdfDocument> extra = PdfDocumentSpecification.hasNoSystemDefault();
            quality = quality == null ? extra : quality.or(extra);
        }
        if (quality != null) spec = spec.and(quality);

        return spec;
    }

    /** The cards that head the list. */
    private Map<String, Object> buildStats(Specification<PdfDocument> spec) {
        return listStats.of(PdfDocument.class, spec)
            .total()
            .count("enabled", PdfDocumentSpecification.isEnabled(true))
            .complement("disabled", "enabled")
            .count("nothingToRender", PdfDocumentSpecification.hasNoEnabledTemplate())
            .count("noTemplates", PdfDocumentSpecification.hasNoTemplates())
            .count("noSystemDefault", PdfDocumentSpecification.hasNoSystemDefault())
            .build();
    }

    /**
     * Get a PDF document by ID
     */
    public ResponseEntity<ApiResponse<?>> getDocumentById(
        String idObfuscated,
        PdfDocumentFilter filter,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            PdfDocument document = pdfDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "PDF document not found", "DOCUMENT_NOT_FOUND")
                );
            }
            Map<String, Object> response = new HashMap<>();
            response.put("pdfDocument", mapToDTO(document));

            /*
             * Walk the SAME set the list was showing, and say where in it we are. Paging by
             * raw id meant the arrows traversed a different list from the one on screen.
             */
            Specification<PdfDocument> navSpec = buildSpec(filter != null ? filter : new PdfDocumentFilter());
            String navSortBy = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Map<String, Object> nav = recordNavigation.navigate(
                PdfDocument.class, navSpec, navSortBy, !"desc".equalsIgnoreCase(sortDirection), id);

            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "PDF document retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error retrieving PDF document: {}", idObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to retrieve PDF document", "PDF_DOCUMENT_FETCH_FAILED")
            );
        }
    }

    /**
     * Get the variable schema for a document type
     */
    public ResponseEntity<ApiResponse<?>> getDocumentSchema(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            PdfDocument document = pdfDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "PDF document not found", "DOCUMENT_NOT_FOUND")
                );
            }
            return ResponseEntity.ok(ApiResponse.success(200,
                "Schema retrieved successfully",
                document.getVariablesJson()));

        } catch (Exception e) {
            log.error("Error retrieving document schema: {}", idObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to retrieve schema", "SCHEMA_FETCH_FAILED")
            );
        }
    }

    /**
     * Map PdfDocument entity to DTO
     */
    private PdfDocumentDTO mapToDTO(PdfDocument document) {
        int templateCount = (int) pdfTemplateRepository.countByPdfDocumentId(document.getId());

        return PdfDocumentDTO.builder()
            .id(idObfuscator.encodeId(document.getId()))
            .name(document.getName())
            .displayName(document.getDisplayName())
            .description(document.getDescription())
            .dataSourceClass(document.getDataSourceClass())
            .rootVariableName(document.getRootVariableName())
            .enabled(document.getEnabled())
            /* ${company.*} resolves in every PDF template, so every document declares it */
            .variablesJson(com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyVariableMerger
                .mergePdfVariables(document.getVariablesJson()))
            .templateCount(templateCount)
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .build();
    }
}
