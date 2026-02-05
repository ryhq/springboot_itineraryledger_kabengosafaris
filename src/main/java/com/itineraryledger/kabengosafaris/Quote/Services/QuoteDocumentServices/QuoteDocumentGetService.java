package com.itineraryledger.kabengosafaris.Quote.Services.QuoteDocumentServices;

import java.time.LocalDateTime;
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

import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDocumentDTOs.QuoteDocumentDTO;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteDocumentRepository;
import com.itineraryledger.kabengosafaris.Quote.Specifications.QuoteDocumentSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving quote documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteDocumentGetService {

    private final QuoteDocumentRepository quoteDocumentRepository;
    private final QuoteDocumentStorageService storageService;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> getAllDocuments(
            String quoteId,
            DocumentType documentType,
            Boolean isActive,
            Boolean isGenerated,
            String title,
            String version,
            Boolean currentlyValid,
            String quoteCode,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        log.info("Fetching quote documents with filters");

        try {
            Long decodedQuoteId = null;
            if (quoteId != null && !quoteId.isBlank()) {
                try {
                    decodedQuoteId = idObfuscator.decodeId(quoteId);
                } catch (Exception e) {
                    log.warn("Failed to decode quote ID: {}", quoteId);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID")
                    );
                }
            }

            Sort sort = Sort.by(
                sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
            );
            Pageable pageable = PageRequest.of(page, size, sort);

            Specification<QuoteDocument> spec = QuoteDocumentSpecification.byQuoteId(decodedQuoteId)
                .and(QuoteDocumentSpecification.byDocumentType(documentType))
                .and(QuoteDocumentSpecification.byIsActive(isActive))
                .and(QuoteDocumentSpecification.byIsGenerated(isGenerated))
                .and(QuoteDocumentSpecification.byTitleContains(title))
                .and(QuoteDocumentSpecification.byVersion(version))
                .and(QuoteDocumentSpecification.byQuoteCode(quoteCode));

            if (Boolean.TRUE.equals(currentlyValid)) {
                spec = spec.and(QuoteDocumentSpecification.byCurrentlyValid(LocalDateTime.now()));
            }

            Page<QuoteDocument> documentPage = quoteDocumentRepository.findAll(spec, pageable);
            Page<QuoteDocumentDTO> dtoPage = documentPage.map(this::toDTO);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote documents retrieved successfully", Map.of(
                    "documents", dtoPage.getContent(),
                    "currentPage", dtoPage.getNumber(),
                    "totalPages", dtoPage.getTotalPages(),
                    "totalElements", dtoPage.getTotalElements(),
                    "pageSize", dtoPage.getSize(),
                    "hasNext", dtoPage.hasNext(),
                    "hasPrevious", dtoPage.hasPrevious()
                ))
            );

        } catch (Exception e) {
            log.error("Error fetching quote documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch quote documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentById(String obfuscatedId) {
        log.info("Fetching quote document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode quote document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            QuoteDocument document = quoteDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            QuoteDocumentDTO documentDTO = toDTO(document);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote document retrieved successfully", documentDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching quote document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch quote document", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentsByQuoteId(String quoteId) {
        log.info("Fetching documents for quote: {}", quoteId);

        try {
            Long decodedQuoteId;
            try {
                decodedQuoteId = idObfuscator.decodeId(quoteId);
            } catch (Exception e) {
                log.warn("Failed to decode quote ID: {}", quoteId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID")
                );
            }

            List<QuoteDocument> documents = quoteDocumentRepository.findByQuoteIdOrderByCreatedAtDesc(decodedQuoteId);
            List<QuoteDocumentDTO> documentDTOs = documents.stream()
                .map(this::toDTO)
                .toList();

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote documents retrieved successfully", documentDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching quote documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch quote documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public QuoteDocument getDocumentEntityById(String obfuscatedId) {
        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            return quoteDocumentRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get quote document entity: {}", obfuscatedId, e);
            return null;
        }
    }

    public QuoteDocument getDocumentEntityByFileName(String fileName) {
        return quoteDocumentRepository.findByFileName(fileName).orElse(null);
    }

    public QuoteDocumentDTO toDTO(QuoteDocument document) {
        if (document == null) {
            return null;
        }

        String obfuscatedId = idObfuscator.encodeId(document.getId());
        String quoteObfuscatedId = document.getQuote() != null
            ? idObfuscator.encodeId(document.getQuote().getId())
            : null;

        LocalDateTime now = LocalDateTime.now();
        Boolean isCurrentlyValid = document.getIsActive() &&
            (document.getValidFrom() == null || !document.getValidFrom().isAfter(now)) &&
            (document.getValidTo() == null || !document.getValidTo().isBefore(now));

        return QuoteDocumentDTO.builder()
            .id(obfuscatedId)
            .quoteId(quoteObfuscatedId)
            .quoteCode(document.getQuote() != null ? document.getQuote().getQuoteCode() : null)
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
