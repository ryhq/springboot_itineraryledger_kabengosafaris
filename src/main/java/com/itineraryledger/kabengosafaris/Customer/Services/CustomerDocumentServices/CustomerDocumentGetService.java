package com.itineraryledger.kabengosafaris.Customer.Services.CustomerDocumentServices;

import java.time.LocalDateTime;
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

import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerDocumentDTOs.CustomerDocumentDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving customer documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerDocumentGetService {

    private final CustomerDocumentRepository customerDocumentRepository;
    private final CustomerDocumentStorageService storageService;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> getAllDocuments(
            String customerId,
            DocumentType documentType,
            Boolean isActive,
            String title,
            String documentNumber,
            String version,
            Boolean currentlyValid,
            String customerName,
            CustomerType customerType,
            String email,
            Boolean identityDocumentsOnly,
            Boolean travelDocumentsOnly,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        log.info("Fetching customer documents with filters");

        try {
            Long decodedCustomerId = null;
            if (customerId != null && !customerId.isBlank()) {
                try {
                    decodedCustomerId = idObfuscator.decodeId(customerId);
                } catch (Exception e) {
                    log.warn("Failed to decode customer ID: {}", customerId);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid customer ID", "INVALID_CUSTOMER_ID")
                    );
                }
            }

            Sort sort = Sort.by(
                sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
            );
            Pageable pageable = PageRequest.of(page, size, sort);

            Specification<CustomerDocument> spec = CustomerDocumentSpecification.byCustomerId(decodedCustomerId)
                .and(CustomerDocumentSpecification.byDocumentType(documentType))
                .and(CustomerDocumentSpecification.byIsActive(isActive))
                .and(CustomerDocumentSpecification.byTitleContains(title))
                .and(CustomerDocumentSpecification.byDocumentNumber(documentNumber))
                .and(CustomerDocumentSpecification.byVersion(version))
                .and(CustomerDocumentSpecification.byCustomerName(customerName))
                .and(CustomerDocumentSpecification.byCustomerType(customerType))
                .and(CustomerDocumentSpecification.byCustomerEmail(email));

            if (Boolean.TRUE.equals(currentlyValid)) {
                spec = spec.and(CustomerDocumentSpecification.byCurrentlyValid(LocalDateTime.now()));
            }

            if (Boolean.TRUE.equals(identityDocumentsOnly)) {
                spec = spec.and(CustomerDocumentSpecification.byIdentityDocument());
            }

            if (Boolean.TRUE.equals(travelDocumentsOnly)) {
                spec = spec.and(CustomerDocumentSpecification.byTravelDocument());
            }

            Page<CustomerDocument> documentPage = customerDocumentRepository.findAll(spec, pageable);
            Page<CustomerDocumentDTO> dtoPage = documentPage.map(this::toDTO);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Customer documents retrieved successfully", Map.of(
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
            log.error("Error fetching customer documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch customer documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentById(String obfuscatedId) {
        log.info("Fetching customer document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode customer document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            CustomerDocument document = customerDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Customer document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            CustomerDocumentDTO documentDTO = toDTO(document);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Customer document retrieved successfully", documentDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching customer document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch customer document", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentsByCustomerId(
            String customerId,
            DocumentType documentType,
            Boolean isActive,
            String title,
            String documentNumber,
            String version,
            Boolean currentlyValid,
            Boolean identityDocumentsOnly,
            Boolean travelDocumentsOnly,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        log.info("Fetching documents for customer: {}", customerId);

        try {
            // Validate that customerId is provided (required)
            if (customerId == null || customerId.isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Customer ID is required", "CUSTOMER_ID_REQUIRED")
                );
            }

            // Decode customer ID (required)
            Long decodedCustomerId;
            try {
                decodedCustomerId = idObfuscator.decodeId(customerId);
            } catch (Exception e) {
                log.warn("Failed to decode customer ID: {}", customerId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid customer ID", "INVALID_CUSTOMER_ID")
                );
            }

            Sort sort = Sort.by(
                sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
            );
            Pageable pageable = PageRequest.of(page, size, sort);

            // Build specification with required customer ID filter
            Specification<CustomerDocument> spec = CustomerDocumentSpecification.byCustomerId(decodedCustomerId)
                .and(CustomerDocumentSpecification.byDocumentType(documentType))
                .and(CustomerDocumentSpecification.byIsActive(isActive))
                .and(CustomerDocumentSpecification.byTitleContains(title))
                .and(CustomerDocumentSpecification.byDocumentNumber(documentNumber))
                .and(CustomerDocumentSpecification.byVersion(version));

            if (Boolean.TRUE.equals(currentlyValid)) {
                spec = spec.and(CustomerDocumentSpecification.byCurrentlyValid(LocalDateTime.now()));
            }

            if (Boolean.TRUE.equals(identityDocumentsOnly)) {
                spec = spec.and(CustomerDocumentSpecification.byIdentityDocument());
            }

            if (Boolean.TRUE.equals(travelDocumentsOnly)) {
                spec = spec.and(CustomerDocumentSpecification.byTravelDocument());
            }

            Page<CustomerDocument> documentPage = customerDocumentRepository.findAll(spec, pageable);
            Page<CustomerDocumentDTO> dtoPage = documentPage.map(this::toDTO);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Customer documents retrieved successfully", Map.of(
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
            log.error("Error fetching customer documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch customer documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public CustomerDocument getDocumentEntityById(String obfuscatedId) {
        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            return customerDocumentRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get customer document entity: {}", obfuscatedId, e);
            return null;
        }
    }

    public CustomerDocument getDocumentEntityByFileName(String fileName) {
        return customerDocumentRepository.findByFileName(fileName).orElse(null);
    }

    public CustomerDocumentDTO toDTO(CustomerDocument document) {
        if (document == null) {
            return null;
        }

        String obfuscatedId = idObfuscator.encodeId(document.getId());
        String customerObfuscatedId = document.getCustomer() != null
            ? idObfuscator.encodeId(document.getCustomer().getId())
            : null;

        String customerName = null;
        if (document.getCustomer() != null) {
            customerName = document.getCustomer().getFirstName() + " " + document.getCustomer().getLastName();
        }

        LocalDateTime now = LocalDateTime.now();
        Boolean isCurrentlyValid = document.getIsActive() &&
            (document.getValidFrom() == null || !document.getValidFrom().isAfter(now)) &&
            (document.getValidTo() == null || !document.getValidTo().isBefore(now));

        return CustomerDocumentDTO.builder()
            .id(obfuscatedId)
            .customerId(customerObfuscatedId)
            .customerName(customerName)
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
            .documentNumber(document.getDocumentNumber())
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
}
