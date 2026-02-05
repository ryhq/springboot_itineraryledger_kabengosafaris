package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceDocumentServices;

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

    public ResponseEntity<ApiResponse<?>> getAllDocuments(
            String invoiceId,
            DocumentType documentType,
            Boolean isActive,
            Boolean isGenerated,
            String title,
            String version,
            Boolean currentlyValid,
            String invoiceCode,
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

            Sort sort = Sort.by(
                sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
            );
            Pageable pageable = PageRequest.of(page, size, sort);

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

            Page<InvoiceDocument> documentPage = invoiceDocumentRepository.findAll(spec, pageable);
            Page<InvoiceDocumentDTO> dtoPage = documentPage.map(this::toDTO);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice documents retrieved successfully", Map.of(
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
            log.error("Error fetching invoice documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch invoice documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentById(String obfuscatedId) {
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

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice document retrieved successfully", documentDTO)
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
