package com.itineraryledger.kabengosafaris.Customer.Services.CustomerDocumentServices;

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
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;
    private final CustomerDocumentStorageService storageService;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "title", "documentType", "fileName", "fileSize", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

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
            java.util.List<CustomerDocument.DocumentType> documentTypes,
            java.util.List<String> statuses,
            java.util.List<String> validity,
            java.time.LocalDateTime createdAfter,
            java.time.LocalDateTime createdBefore,
            Boolean includeStats,
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

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort sort = Sort.by(
                sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                validatedSortBy
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

            // multi-value facets, so every stat card is also a filter

            if (documentTypes != null && !documentTypes.isEmpty()) spec = spec.and(CustomerDocumentSpecification.documentTypeIn(documentTypes));

            if (statuses != null && !statuses.isEmpty()) {

                java.util.List<Boolean> states = new java.util.ArrayList<>();

                if (statuses.contains("active")) states.add(true);

                if (statuses.contains("inactive")) states.add(false);

                if (states.size() == 1) spec = spec.and(CustomerDocumentSpecification.byIsActive(states.get(0)));

            }

            if (validity != null && !validity.isEmpty()) {

                if (validity.contains("expired")) spec = spec.and(CustomerDocumentSpecification.expired());

                if (validity.contains("expiring")) spec = spec.and(CustomerDocumentSpecification.expiringWithin(30));

                if (validity.contains("no-expiry")) spec = spec.and(CustomerDocumentSpecification.noExpiry());

            }

            if (createdAfter != null) spec = spec.and(CustomerDocumentSpecification.createdAfter(createdAfter));

            if (createdBefore != null) spec = spec.and(CustomerDocumentSpecification.createdBefore(createdBefore));

            Page<CustomerDocument> documentPage = customerDocumentRepository.findAll(spec, pageable);
            Page<CustomerDocumentDTO> dtoPage = documentPage.map(this::toDTO);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("documents", dtoPage.getContent());
            responseData.put("currentPage", dtoPage.getNumber());
            responseData.put("totalPages", dtoPage.getTotalPages());
            responseData.put("totalElements", dtoPage.getTotalElements());
            responseData.put("pageSize", dtoPage.getSize());
            responseData.put("hasNext", dtoPage.hasNext());
            responseData.put("hasPrevious", dtoPage.hasPrevious());
            responseData.put("validSortFields", VALID_SORT_FIELDS);
            responseData.put("currentSortBy", validatedSortBy);
            responseData.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            if (!Boolean.FALSE.equals(includeStats)) {
                responseData.put("stats", computeStats(spec));
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Customer documents retrieved successfully", responseData)
            );

        } catch (Exception e) {
            log.error("Error fetching customer documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch customer documents", "DOCUMENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDocumentById(String obfuscatedId, String scopeParentId) {
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

            // Decode optional scope parent ID for scoped navigation
            Long decodedParentId = null;
            if (scopeParentId != null && !scopeParentId.isEmpty()) {
                try {
                    decodedParentId = idObfuscator.decodeId(scopeParentId);
                } catch (Exception ex) {
                    log.warn("Invalid scopeParentId: {}, falling back to global navigation", scopeParentId);
                }
            }

            // Circular navigation (scoped if parent provided, global otherwise)
            Long nextId, previousId;
            if (decodedParentId != null) {
                nextId = customerDocumentRepository.findNextIdByParent(id, decodedParentId).orElse(null);
                previousId = customerDocumentRepository.findPreviousIdByParent(id, decodedParentId).orElse(null);
                if (nextId == null) nextId = customerDocumentRepository.findFirstIdByParent(decodedParentId).orElse(null);
                if (previousId == null) previousId = customerDocumentRepository.findLastIdByParent(decodedParentId).orElse(null);
            } else {
                nextId = customerDocumentRepository.findNextId(id).orElse(null);
                previousId = customerDocumentRepository.findPreviousId(id).orElse(null);
                if (nextId == null) nextId = customerDocumentRepository.findFirstId().orElse(null);
                if (previousId == null) previousId = customerDocumentRepository.findLastId().orElse(null);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("document", documentDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            // the "3 of 6" readout, over the SAME set the caller was looking at:
            // scoped to this customer when scopeParentId was given, else the whole list
            response.putAll(recordNavigation.positionOf(
                CustomerDocument.class,
                decodedParentId != null ? CustomerDocumentSpecification.byCustomerId(decodedParentId) : null,
                "id",
                false,
                id));
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Customer document retrieved successfully", response)
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

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort sort = Sort.by(
                sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                validatedSortBy
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

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("documents", dtoPage.getContent());
            responseData.put("currentPage", dtoPage.getNumber());
            responseData.put("totalPages", dtoPage.getTotalPages());
            responseData.put("totalElements", dtoPage.getTotalElements());
            responseData.put("pageSize", dtoPage.getSize());
            responseData.put("hasNext", dtoPage.hasNext());
            responseData.put("hasPrevious", dtoPage.hasPrevious());
            responseData.put("validSortFields", VALID_SORT_FIELDS);
            responseData.put("currentSortBy", validatedSortBy);
            responseData.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Customer documents retrieved successfully", responseData)
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

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
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

    /** Dashboard counters for the CURRENT filter set (stats on every list). */
    private Map<String, Object> computeStats(Specification<CustomerDocument> base) {
        return listStats.of(CustomerDocument.class, base)
            .total()
            .count("active", CustomerDocumentSpecification.byIsActive(true)).complement("inactive", "active")
            .count("expired", CustomerDocumentSpecification.expired())
            .count("expiringSoon", CustomerDocumentSpecification.expiringWithin(30))
            .count("noExpiry", CustomerDocumentSpecification.noExpiry())
            .count("identityDocuments", CustomerDocumentSpecification.byIdentityDocument())
            .count("travelDocuments", CustomerDocumentSpecification.byTravelDocument())
            .breakdown("byDocumentType", CustomerDocument.DocumentType.values(), CustomerDocumentSpecification::byDocumentType)
            .recency(CustomerDocumentSpecification::createdAfter)
            .build();
    }
}
