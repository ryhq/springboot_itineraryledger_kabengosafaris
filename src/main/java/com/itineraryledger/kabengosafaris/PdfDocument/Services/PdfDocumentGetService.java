package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.PdfDocument.DTOs.PdfDocumentDTO;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfDocument;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
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
     * Get all PDF document types
     */
    public ResponseEntity<ApiResponse<?>> getAllDocuments(String sortBy, String sortDirection) {
        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort.Direction direction = Sort.Direction.DESC;
            if ("asc".equalsIgnoreCase(sortDirection)) {
                direction = Sort.Direction.ASC;
            }

            List<PdfDocument> documents = pdfDocumentRepository.findAll(Sort.by(direction, validatedSortBy));

            List<PdfDocumentDTO> dtos = documents.stream()
                .map(this::mapToDTO)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("pdfDocuments", dtos);
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDir", sortDirection != null ? sortDirection : "desc");

            log.info("Retrieved {} PDF document types", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(200, "PDF documents retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error retrieving PDF documents", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to retrieve PDF documents", "PDF_DOCUMENTS_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a PDF document by ID
     */
    public ResponseEntity<ApiResponse<?>> getDocumentById(String idObfuscated) {
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
            PdfDocumentDTO dto = mapToDTO(document);

            // Navigation IDs
            Long nextId = pdfDocumentRepository.findNextId(id).orElse(null);
            Long previousId = pdfDocumentRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = pdfDocumentRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = pdfDocumentRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("pdfDocument", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

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
            .variablesJson(document.getVariablesJson())
            .templateCount(templateCount)
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .build();
    }
}
