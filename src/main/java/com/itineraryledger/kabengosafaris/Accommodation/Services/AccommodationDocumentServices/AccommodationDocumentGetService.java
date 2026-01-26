package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationDocumentServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDocumentDTOs.AccommodationDocumentDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationDocument;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving accommodation documents.
 *
 * Provides:
 * - Get all documents with filters, pagination, and sorting
 * - Get document by ID
 * - Get document by filename
 * - Convert entities to DTOs
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class AccommodationDocumentGetService {

    private final AccommodationDocumentRepository accommodationDocumentRepository;
    private final AccommodationDocumentStorageService storageService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public AccommodationDocumentGetService(
        AccommodationDocumentRepository accommodationDocumentRepository,
        AccommodationDocumentStorageService storageService,
        IdObfuscator idObfuscator
    ) {
        this.accommodationDocumentRepository = accommodationDocumentRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Convert AccommodationDocument entity to DTO
     */
    public AccommodationDocumentDTO toDTO(AccommodationDocument document) {
        String obfuscatedId = idObfuscator.encodeId(document.getId());
        return AccommodationDocumentDTO.builder()
            .id(obfuscatedId)
            .accommodationId(idObfuscator.encodeId(document.getAccommodation().getId()))
            .accommodationName(document.getAccommodation().getName())
            .title(document.getTitle())
            .documentType(document.getDocumentType())
            .documentTypeDisplayName(document.getDocumentType().getDisplayName())
            .documentTypeDescription(document.getDocumentType().getDescription())
            .documentUrl(storageService.constructDocumentUrl(obfuscatedId))
            .fileDocumentUrl(storageService.constructFileDocumentUrl(document.getFileName()))
            .fileName(document.getFileName())
            .originalFileName(document.getFileName() != null ? extractOriginalName(document.getFileName()) : null)
            .fileSize(document.getFileSize())
            .fileSizeFormatted(document.getFileSize() != null ? storageService.formatFileSize(document.getFileSize()) : null)
            .fileType(document.getFileType())
            .description(document.getDescription())
            .version(document.getVersion())
            .notes(document.getNotes())
            .validFrom(document.getValidFrom())
            .validTo(document.getValidTo())
            .isCurrentlyValid(document.isCurrentlyValid())
            .isActive(document.getIsActive())
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .build();
    }

    /**
     * Extract original filename hint from hashed filename
     * Since we hash filenames, this just returns the extension info
     */
    private String extractOriginalName(String hashedFileName) {
        // The hashed filename format is: hash_timestamp.extension
        // We can't recover the original name, but we preserve extension
        if (hashedFileName == null || !hashedFileName.contains(".")) {
            return hashedFileName;
        }
        return hashedFileName; // Just return the stored filename
    }

    /**
     * Get all documents with filters, pagination, and sorting
     *
     * @return ResponseEntity with ApiResponse containing paginated documents
     */
    public ResponseEntity<?> getAllDocuments(
            String obfuscatedAccommodationId,
            String accommodationName,
            AccommodationType accommodationType,
            AccommodationCategory accommodationCategory,
            DocumentType documentType,
            String title,
            String version,
            Boolean isActive,
            Boolean currentlyValid,
            int page,
            int size,
            String sortDirection
    ) {
        // Build specification
        Specification<AccommodationDocument> spec = Specification.unrestricted();

        // Accommodation ID filter
        if (obfuscatedAccommodationId != null && !obfuscatedAccommodationId.isBlank()) {
            try {
                Long accommodationId = idObfuscator.decodeId(obfuscatedAccommodationId);
                spec = spec.and(AccommodationDocumentSpecification.byAccommodationId(accommodationId));
            } catch (Exception e) {
                log.warn("Failed to decode accommodation ID: {}", obfuscatedAccommodationId);
            }
        }

        // Accommodation filters
        if (accommodationName != null && !accommodationName.isBlank()) {
            spec = spec.and(AccommodationDocumentSpecification.byAccommodationName(accommodationName));
        }
        if (accommodationType != null) {
            spec = spec.and(AccommodationDocumentSpecification.byAccommodationType(accommodationType));
        }
        if (accommodationCategory != null) {
            spec = spec.and(AccommodationDocumentSpecification.byAccommodationCategory(accommodationCategory));
        }

        // Document filters
        if (documentType != null) {
            spec = spec.and(AccommodationDocumentSpecification.byDocumentType(documentType));
        }
        if (title != null && !title.isBlank()) {
            spec = spec.and(AccommodationDocumentSpecification.byTitleContains(title));
        }
        if (version != null && !version.isBlank()) {
            spec = spec.and(AccommodationDocumentSpecification.byVersion(version));
        }
        if (isActive != null) {
            spec = spec.and(AccommodationDocumentSpecification.byIsActive(isActive));
        }
        if (currentlyValid != null && currentlyValid) {
            spec = spec.and(AccommodationDocumentSpecification.byCurrentlyValid(LocalDateTime.now()));
        }

        // Sort direction - default DESC for createdAt
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        // Always sort by createdAt
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        // Execute query
        Page<AccommodationDocument> documentPage = accommodationDocumentRepository.findAll(spec, pageable);

        // Convert to DTOs
        List<AccommodationDocumentDTO> documentDTOs = documentPage.getContent().stream()
            .map(this::toDTO)
            .toList();

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("documents", documentDTOs);
        response.put("currentPage", documentPage.getNumber());
        response.put("totalItems", documentPage.getTotalElements());
        response.put("totalPages", documentPage.getTotalPages());
        response.put("pageSize", documentPage.getSize());
        response.put("hasNext", documentPage.hasNext());
        response.put("hasPrevious", documentPage.hasPrevious());

        return ResponseEntity.ok(ApiResponse.success(200, "Documents retrieved successfully", response));
    }

    /**
     * Get a single document by obfuscated ID
     *
     * @param obfuscatedId The obfuscated document ID
     * @return ResponseEntity with ApiResponse containing document or error
     */
    public ResponseEntity<?> getDocumentById(String obfuscatedId) {
        log.info("Getting accommodation document with ID: {}", obfuscatedId);

        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            AccommodationDocument document = accommodationDocumentRepository.findById(id).orElse(null);

            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Document retrieved successfully", toDTO(document)));

        } catch (Exception e) {
            log.warn("Failed to decode document ID: {}", obfuscatedId, e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
            );
        }
    }

    /**
     * Get document entity by filename (for serving file content)
     * Returns the entity for the controller to serve the file
     */
    public AccommodationDocument getDocumentByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        return accommodationDocumentRepository.findByFileName(fileName).orElse(null);
    }
}
