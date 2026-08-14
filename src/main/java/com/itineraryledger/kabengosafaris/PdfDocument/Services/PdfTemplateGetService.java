package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.PdfDocument.DTOs.PdfTemplateDTO;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfTemplate;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Specification.PdfTemplateSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
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
 * Service for retrieving PDF templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PdfTemplateGetService {

    private final PdfTemplateRepository pdfTemplateRepository;
    private final PdfTemplateStorageService storageService;
    private final IdObfuscator idObfuscator;
    private final ListStats listStats;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "version", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    /**
     * Get all templates with filtering and pagination
     */
    /**
     * The cards that head the list.
     *
     * "Not the one used" is the counter worth having: several templates can exist for a
     * document while exactly one renders it, so a carefully edited template that is not the
     * default has changed nothing anybody will ever receive.
     */
    private java.util.Map<String, Object> buildStats(Specification<PdfTemplate> spec) {
        return listStats.of(PdfTemplate.class, spec)
            .total()
            .count("enabled", PdfTemplateSpecification.enabled(true))
            .complement("disabled", "enabled")
            .count("used", PdfTemplateSpecification.isDefault(true))
            .count("original", PdfTemplateSpecification.isSystemDefault(true))
            .count("custom", PdfTemplateSpecification.isSystemDefault(false))
            .build();
    }

    public ResponseEntity<ApiResponse<?>> getAllTemplates(
        String documentIdObfuscated,
        String documentType,
        String rootVariableName,
        Boolean enabled,
        Boolean isDefault,
        Boolean isSystemDefault,
        String name,
        String paperSize,
        String orientation,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection,
        Boolean includeStats
    ) {
        try {
            // Build specification
            Specification<PdfTemplate> spec = Specification.unrestricted();

            if (documentIdObfuscated != null) {
                Long documentId = idObfuscator.decodeId(documentIdObfuscated);
                if (documentId != null) {
                    spec = spec.and(PdfTemplateSpecification.pdfDocumentId(documentId));
                }
            }
            if (documentType != null && !documentType.isBlank()) {
                spec = spec.and(PdfTemplateSpecification.documentTypeName(documentType));
            }
            if (rootVariableName != null && !rootVariableName.isBlank()) {
                spec = spec.and(PdfTemplateSpecification.rootVariableName(rootVariableName));
            }
            if (enabled != null) {
                spec = spec.and(PdfTemplateSpecification.enabled(enabled));
            }
            if (isDefault != null) {
                spec = spec.and(PdfTemplateSpecification.isDefault(isDefault));
            }
            if (isSystemDefault != null) {
                spec = spec.and(PdfTemplateSpecification.isSystemDefault(isSystemDefault));
            }
            if (name != null && !name.isBlank()) {
                spec = spec.and(PdfTemplateSpecification.nameLike(name));
            }
            if (paperSize != null && !paperSize.isBlank()) {
                spec = spec.and(PdfTemplateSpecification.paperSize(paperSize));
            }
            if (orientation != null && !orientation.isBlank()) {
                spec = spec.and(PdfTemplateSpecification.orientation(orientation));
            }

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            // Create pageable
            Sort sort = Sort.by("desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC, validatedSortBy);
            Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 10, sort);

            // Execute query
            Page<PdfTemplate> templatePage = pdfTemplateRepository.findAll(spec, pageable);

            List<PdfTemplateDTO> dtos = templatePage.getContent().stream()
                .map(this::mapToDTO)
                .toList();

            // Build response with pagination info
            Map<String, Object> response = new HashMap<>();
            /*
             * `templates` and `totalItems` are the house names; `content`, `totalElements`
             * and `size` stay beside them so nothing already reading this response breaks.
             */
            response.put("templates", dtos);
            response.put("totalItems", templatePage.getTotalElements());
            response.put("pageSize", templatePage.getSize());
            response.put("content", dtos);
            response.put("totalElements", templatePage.getTotalElements());
            response.put("totalPages", templatePage.getTotalPages());
            response.put("currentPage", templatePage.getNumber());
            response.put("size", templatePage.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Templates retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error retrieving templates", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to retrieve templates", "TEMPLATES_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a template by ID
     */
    public ResponseEntity<ApiResponse<?>> getTemplateById(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid template ID", "INVALID_TEMPLATE_ID")
                );
            }

            PdfTemplate template = pdfTemplateRepository.findById(id).orElse(null);
            if (template == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND")
                );
            }

            /*
             * The record carries its layout.
             *
             * The list deliberately does not — a layout is a whole HTML document and twenty of
             * them is megabytes for a table of names — but the record IS the layout, and
             * fetching it separately meant the editor opened on a blank page while the record
             * claimed to have loaded.
             */
            PdfTemplateDTO dto = mapToDTO(template);
            dto.setContent(storageService.readTemplateFile(template.getFileName()));

            // Circular navigation
            Long nextId = pdfTemplateRepository.findNextId(id).orElse(null);
            Long previousId = pdfTemplateRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = pdfTemplateRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = pdfTemplateRepository.findLastId().orElse(null);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("template", dto);
            responseData.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            responseData.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok(ApiResponse.success(200, "Template retrieved successfully", responseData));

        } catch (Exception e) {
            log.error("Error retrieving template: {}", idObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to retrieve template", "TEMPLATE_FETCH_FAILED")
            );
        }
    }

    /**
     * Get template content (HTML)
     */
    public ResponseEntity<ApiResponse<?>> getTemplateContent(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid template ID", "INVALID_TEMPLATE_ID")
                );
            }

            PdfTemplate template = pdfTemplateRepository.findById(id).orElse(null);
            if (template == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND")
                );
            }

            String content = storageService.readTemplateFile(template.getFileName());
            if (content == null) {
                return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to read template file", "FILE_READ_FAILED")
                );
            }

            PdfTemplateDTO dto = mapToDTO(template);
            dto.setContent(content);

            // Circular navigation scoped to the same PDF document
            Long docId = template.getPdfDocument().getId();
            Long nextId = pdfTemplateRepository.findNextIdByDocumentId(docId, id).orElse(null);
            Long previousId = pdfTemplateRepository.findPreviousIdByDocumentId(docId, id).orElse(null);
            if (nextId == null) nextId = pdfTemplateRepository.findFirstIdByDocumentId(docId).orElse(null);
            if (previousId == null) previousId = pdfTemplateRepository.findLastIdByDocumentId(docId).orElse(null);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("template", dto);
            responseData.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            responseData.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok(ApiResponse.success(200, "Template content retrieved successfully", responseData));

        } catch (Exception e) {
            log.error("Error retrieving template content: {}", idObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to retrieve template content", "TEMPLATE_CONTENT_FETCH_FAILED")
            );
        }
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * Map entity to DTO
     */
    private PdfTemplateDTO mapToDTO(PdfTemplate template) {
        return PdfTemplateDTO.builder()
            .id(idObfuscator.encodeId(template.getId()))
            .pdfDocumentId(idObfuscator.encodeId(template.getPdfDocument().getId()))
            .pdfDocumentName(template.getPdfDocument().getName())
            .pdfDocumentDisplayName(template.getPdfDocument().getDisplayName())
            .name(template.getName())
            .description(template.getDescription())
            .fileName(template.getFileName())
            .paperSize(template.getPaperSize())
            .paperSizeDisplayName(template.getPaperSize().getDisplayName())
            .orientation(template.getOrientation())
            .orientationDisplayName(template.getOrientation().getDisplayName())
            .marginTop(template.getMarginTop())
            .marginBottom(template.getMarginBottom())
            .marginLeft(template.getMarginLeft())
            .marginRight(template.getMarginRight())
            .isDefault(template.getIsDefault())
            .isSystemDefault(template.getIsSystemDefault())
            .enabled(template.getEnabled())
            .fileSize(template.getFileSize())
            .fileSizeFormatted(storageService.formatFileSize(template.getFileSize()))
            .version(template.getVersion())
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();
    }
}
