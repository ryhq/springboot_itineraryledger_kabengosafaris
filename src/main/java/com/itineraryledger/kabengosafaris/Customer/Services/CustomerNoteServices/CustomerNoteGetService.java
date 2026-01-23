package com.itineraryledger.kabengosafaris.Customer.Services.CustomerNoteServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerNoteRepository;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerNoteDTOs.CustomerNoteDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NoteType;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NotePriority;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * CustomerNoteGetService - Service for retrieving customer notes
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class CustomerNoteGetService {

    private final CustomerNoteRepository customerNoteRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CustomerNoteGetService(
        CustomerNoteRepository customerNoteRepository,
        IdObfuscator idObfuscator
    ) {
        this.customerNoteRepository = customerNoteRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get customer note by obfuscated ID
     *
     * @param idObfuscated The obfuscated note ID
     * @return ResponseEntity with ApiResponse containing the note
     */
    public ResponseEntity<ApiResponse<?>> getCustomerNoteById(String idObfuscated) {
        log.info("Fetching customer note with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode note ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid note ID",
                        "INVALID_NOTE_ID"
                    )
                );
            }

            // Find note
            CustomerNote note = customerNoteRepository.findById(id).orElse(null);
            if (note == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Customer note not found",
                        "CUSTOMER_NOTE_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            CustomerNoteDTO noteDTO = convertToDTO(note);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Customer note retrieved successfully",
                    noteDTO
                )
            );

        } catch (Exception e) {
            log.error("Error fetching customer note", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch customer note",
                    "CUSTOMER_NOTE_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all customer notes with filtering and pagination
     * customerId is an optional filter parameter
     *
     * @param customerId Optional obfuscated customer ID filter
     * @param noteType Filter by note type (optional)
     * @param subject Filter by subject (optional)
     * @param isPinned Filter by pinned status (optional)
     * @param isPrivate Filter by private status (optional)
     * @param priority Filter by priority (optional)
     * @param followUpCompleted Filter by follow-up completed status (optional)
     * @param createdById Filter by creator ID (optional)
     * @param relatedEntityType Filter by related entity type (optional)
     * @param relatedEntityId Filter by related entity ID (optional)
     * @param keyword Search keyword across multiple fields (optional)
     * @param pageable Pagination and sorting parameters
     * @return ResponseEntity with ApiResponse containing paginated notes
     */
    public ResponseEntity<ApiResponse<?>> getAllCustomerNotes(
        String customerId,
        NoteType noteType,
        String subject,
        Boolean isPinned,
        Boolean isPrivate,
        NotePriority priority,
        Boolean followUpCompleted,
        String createdById,
        String relatedEntityType,
        String relatedEntityId,
        String keyword,
        Pageable pageable
    ) {
        log.info("Fetching all customer notes with optional filters");

        try {
            // Build specification
            Specification<CustomerNote> spec = Specification.unrestricted();

            // Add optional customer ID filter
            if (customerId != null && !customerId.isEmpty()) {
                try {
                    Long decodedCustomerId = idObfuscator.decodeId(customerId);
                    spec = spec.and(CustomerNoteSpecification.hasCustomerId(decodedCustomerId));
                } catch (Exception e) {
                    log.warn("Failed to decode customer ID: {}", customerId, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Invalid customer ID",
                            "INVALID_CUSTOMER_ID"
                        )
                    );
                }
            }

            // Add other optional filters
            if (noteType != null) {
                spec = spec.and(CustomerNoteSpecification.hasNoteType(noteType));
            }
            if (subject != null && !subject.isEmpty()) {
                spec = spec.and(CustomerNoteSpecification.subjectLike(subject));
            }
            if (isPinned != null) {
                spec = spec.and(CustomerNoteSpecification.isPinned(isPinned));
            }
            if (isPrivate != null) {
                spec = spec.and(CustomerNoteSpecification.isPrivate(isPrivate));
            }
            if (priority != null) {
                spec = spec.and(CustomerNoteSpecification.hasPriority(priority));
            }
            if (followUpCompleted != null) {
                spec = spec.and(CustomerNoteSpecification.isFollowUpCompleted(followUpCompleted));
            }
            if (createdById != null && !createdById.isEmpty()) {
                try {
                    Long decodedCreatedById = idObfuscator.decodeId(createdById);
                    spec = spec.and(CustomerNoteSpecification.createdBy(decodedCreatedById));
                } catch (Exception e) {
                    log.warn("Failed to decode created by ID: {}", createdById, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Invalid created by ID",
                            "INVALID_CREATED_BY_ID"
                        )
                    );
                }
            }
            if (relatedEntityType != null && !relatedEntityType.isEmpty()) {
                spec = spec.and(CustomerNoteSpecification.hasRelatedEntityType(relatedEntityType));
            }
            if (relatedEntityId != null && !relatedEntityId.isEmpty()) {
                try {
                    Long decodedRelatedEntityId = idObfuscator.decodeId(relatedEntityId);
                    spec = spec.and(CustomerNoteSpecification.hasRelatedEntityId(decodedRelatedEntityId));
                } catch (Exception e) {
                    log.warn("Failed to decode related entity ID: {}", relatedEntityId, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Invalid related entity ID",
                            "INVALID_RELATED_ENTITY_ID"
                        )
                    );
                }
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(CustomerNoteSpecification.searchKeyword(keyword));
            }

            // Fetch paginated results
            Page<CustomerNote> notePage = customerNoteRepository.findAll(spec, pageable);

            // Convert to DTOs
            Page<CustomerNoteDTO> noteDTOPage = notePage.map(this::convertToDTO);

            // Build response with pagination metadata
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("notes", noteDTOPage.getContent());
            responseData.put("currentPage", noteDTOPage.getNumber());
            responseData.put("totalItems", noteDTOPage.getTotalElements());
            responseData.put("totalPages", noteDTOPage.getTotalPages());
            responseData.put("pageSize", noteDTOPage.getSize());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Customer notes retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching customer notes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch customer notes",
                    "CUSTOMER_NOTES_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all notes for a specific customer
     * customerId is REQUIRED
     *
     * @param customerId Required obfuscated customer ID
     * @param noteType Filter by note type (optional)
     * @param subject Filter by subject (optional)
     * @param isPinned Filter by pinned status (optional)
     * @param isPrivate Filter by private status (optional)
     * @param priority Filter by priority (optional)
     * @param followUpCompleted Filter by follow-up completed status (optional)
     * @param createdById Filter by creator ID (optional)
     * @param relatedEntityType Filter by related entity type (optional)
     * @param relatedEntityId Filter by related entity ID (optional)
     * @param keyword Search keyword across multiple fields (optional)
     * @param pageable Pagination and sorting parameters
     * @return ResponseEntity with ApiResponse containing paginated notes for the customer
     */
    public ResponseEntity<ApiResponse<?>> getCustomersNotes(
        @NotBlank(message = "Customer ID is required") String customerId,
        NoteType noteType,
        String subject,
        Boolean isPinned,
        Boolean isPrivate,
        NotePriority priority,
        Boolean followUpCompleted,
        String createdById,
        String relatedEntityType,
        String relatedEntityId,
        String keyword,
        Pageable pageable
    ) {
        log.info("Fetching notes for customer: {}", customerId);

        try {
            // Decode customer ID (required)
            Long decodedCustomerId;
            try {
                decodedCustomerId = idObfuscator.decodeId(customerId);
            } catch (Exception e) {
                log.warn("Failed to decode customer ID: {}", customerId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid customer ID",
                        "INVALID_CUSTOMER_ID"
                    )
                );
            }

            // Build specification with required customer ID filter
            Specification<CustomerNote> spec = CustomerNoteSpecification.hasCustomerId(decodedCustomerId);

            // Add optional filters
            if (noteType != null) {
                spec = spec.and(CustomerNoteSpecification.hasNoteType(noteType));
            }
            if (subject != null && !subject.isEmpty()) {
                spec = spec.and(CustomerNoteSpecification.subjectLike(subject));
            }
            if (isPinned != null) {
                spec = spec.and(CustomerNoteSpecification.isPinned(isPinned));
            }
            if (isPrivate != null) {
                spec = spec.and(CustomerNoteSpecification.isPrivate(isPrivate));
            }
            if (priority != null) {
                spec = spec.and(CustomerNoteSpecification.hasPriority(priority));
            }
            if (followUpCompleted != null) {
                spec = spec.and(CustomerNoteSpecification.isFollowUpCompleted(followUpCompleted));
            }
            if (createdById != null && !createdById.isEmpty()) {
                try {
                    Long decodedCreatedById = idObfuscator.decodeId(createdById);
                    spec = spec.and(CustomerNoteSpecification.createdBy(decodedCreatedById));
                } catch (Exception e) {
                    log.warn("Failed to decode created by ID: {}", createdById, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Invalid created by ID",
                            "INVALID_CREATED_BY_ID"
                        )
                    );
                }
            }
            if (relatedEntityType != null && !relatedEntityType.isEmpty()) {
                spec = spec.and(CustomerNoteSpecification.hasRelatedEntityType(relatedEntityType));
            }
            if (relatedEntityId != null && !relatedEntityId.isEmpty()) {
                try {
                    Long decodedRelatedEntityId = idObfuscator.decodeId(relatedEntityId);
                    spec = spec.and(CustomerNoteSpecification.hasRelatedEntityId(decodedRelatedEntityId));
                } catch (Exception e) {
                    log.warn("Failed to decode related entity ID: {}", relatedEntityId, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Invalid related entity ID",
                            "INVALID_RELATED_ENTITY_ID"
                        )
                    );
                }
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(CustomerNoteSpecification.searchKeyword(keyword));
            }

            // Fetch paginated results
            Page<CustomerNote> notePage = customerNoteRepository.findAll(spec, pageable);

            // Convert to DTOs
            Page<CustomerNoteDTO> noteDTOPage = notePage.map(this::convertToDTO);

            // Build response with pagination metadata
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("notes", noteDTOPage.getContent());
            responseData.put("currentPage", noteDTOPage.getNumber());
            responseData.put("totalItems", noteDTOPage.getTotalElements());
            responseData.put("totalPages", noteDTOPage.getTotalPages());
            responseData.put("pageSize", noteDTOPage.getSize());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Customer notes retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching customer notes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch customer notes",
                    "CUSTOMER_NOTES_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get notes with pending follow-ups for a customer
     *
     * @param customerId Required obfuscated customer ID
     * @param pageable Pagination and sorting parameters
     * @return ResponseEntity with ApiResponse containing paginated pending follow-ups
     */
    public ResponseEntity<ApiResponse<?>> getPendingFollowUps(
        @NotBlank(message = "Customer ID is required") String customerId,
        Pageable pageable
    ) {
        log.info("Fetching pending follow-ups for customer: {}", customerId);

        try {
            // Decode customer ID (required)
            Long decodedCustomerId;
            try {
                decodedCustomerId = idObfuscator.decodeId(customerId);
            } catch (Exception e) {
                log.warn("Failed to decode customer ID: {}", customerId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid customer ID",
                        "INVALID_CUSTOMER_ID"
                    )
                );
            }

            // Build specification for pending follow-ups
            Specification<CustomerNote> spec = CustomerNoteSpecification.hasCustomerId(decodedCustomerId)
                .and(CustomerNoteSpecification.hasPendingFollowUp());

            // Fetch paginated results
            Page<CustomerNote> notePage = customerNoteRepository.findAll(spec, pageable);

            // Convert to DTOs
            Page<CustomerNoteDTO> noteDTOPage = notePage.map(this::convertToDTO);

            // Build response with pagination metadata
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("notes", noteDTOPage.getContent());
            responseData.put("currentPage", noteDTOPage.getNumber());
            responseData.put("totalItems", noteDTOPage.getTotalElements());
            responseData.put("totalPages", noteDTOPage.getTotalPages());
            responseData.put("pageSize", noteDTOPage.getSize());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Pending follow-ups retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching pending follow-ups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch pending follow-ups",
                    "PENDING_FOLLOW_UPS_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get overdue follow-ups for a customer
     *
     * @param customerId Required obfuscated customer ID
     * @param pageable Pagination and sorting parameters
     * @return ResponseEntity with ApiResponse containing paginated overdue follow-ups
     */
    public ResponseEntity<ApiResponse<?>> getOverdueFollowUps(
        @NotBlank(message = "Customer ID is required") String customerId,
        Pageable pageable
    ) {
        log.info("Fetching overdue follow-ups for customer: {}", customerId);

        try {
            // Decode customer ID (required)
            Long decodedCustomerId;
            try {
                decodedCustomerId = idObfuscator.decodeId(customerId);
            } catch (Exception e) {
                log.warn("Failed to decode customer ID: {}", customerId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid customer ID",
                        "INVALID_CUSTOMER_ID"
                    )
                );
            }

            // Build specification for overdue follow-ups
            Specification<CustomerNote> spec = CustomerNoteSpecification.hasCustomerId(decodedCustomerId)
                .and(CustomerNoteSpecification.hasOverdueFollowUp());

            // Fetch paginated results
            Page<CustomerNote> notePage = customerNoteRepository.findAll(spec, pageable);

            // Convert to DTOs
            Page<CustomerNoteDTO> noteDTOPage = notePage.map(this::convertToDTO);

            // Build response with pagination metadata
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("notes", noteDTOPage.getContent());
            responseData.put("currentPage", noteDTOPage.getNumber());
            responseData.put("totalItems", noteDTOPage.getTotalElements());
            responseData.put("totalPages", noteDTOPage.getTotalPages());
            responseData.put("pageSize", noteDTOPage.getSize());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Overdue follow-ups retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching overdue follow-ups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch overdue follow-ups",
                    "OVERDUE_FOLLOW_UPS_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Convert CustomerNote entity to DTO
     */
    public CustomerNoteDTO convertToDTO(CustomerNote note) {
        return CustomerNoteDTO.builder()
            .id(idObfuscator.encodeId(note.getId()))
            .customerId(idObfuscator.encodeId(note.getCustomer().getId()))
            .customerDisplayName(note.getCustomer().getDisplayName())
            .noteType(note.getNoteType())
            .noteTypeDisplayName(note.getNoteType() != null ? note.getNoteType().getDisplayName() : null)
            .noteTypeDescription(note.getNoteType() != null ? note.getNoteType().getDescription() : null)
            .subject(note.getSubject())
            .content(note.getContent())
            .followUpDate(note.getFollowUpDate())
            .followUpCompleted(note.getFollowUpCompleted())
            .followUpCompletedAt(note.getFollowUpCompletedAt())
            .followUpCompletedById(note.getFollowUpCompletedBy() != null ? idObfuscator.encodeId(note.getFollowUpCompletedBy()) : null)
            .isFollowUpOverdue(note.isFollowUpOverdue())
            .isPinned(note.getIsPinned())
            .isPrivate(note.getIsPrivate())
            .priority(note.getPriority())
            .priorityDisplayName(note.getPriority() != null ? note.getPriority().getDisplayName() : null)
            .priorityDescription(note.getPriority() != null ? note.getPriority().getDescription() : null)
            .relatedEntityType(note.getRelatedEntityType())
            .relatedEntityId(note.getRelatedEntityId() != null ? idObfuscator.encodeId(note.getRelatedEntityId()) : null)
            .createdById(note.getCreatedBy() != null ? idObfuscator.encodeId(note.getCreatedBy()) : null)
            .createdByName(note.getCreatedByName())
            .createdAt(note.getCreatedAt())
            .updatedAt(note.getUpdatedAt())
            .build();
    }
}
