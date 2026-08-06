package com.itineraryledger.kabengosafaris.Customer.Services.CustomerNoteServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerNoteRepository;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerNoteDTOs.CustomerNoteDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerNoteDTOs.UpdateCustomerNoteDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * UpdateCustomerNoteService - Service for updating customer notes
 */
@Service
@Slf4j
@Transactional
public class UpdateCustomerNoteService {

    private final CustomerNoteRepository customerNoteRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public UpdateCustomerNoteService(
        CustomerNoteRepository customerNoteRepository,
        IdObfuscator idObfuscator
    ) {
        this.customerNoteRepository = customerNoteRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update a customer note by obfuscated ID
     *
     * @param idObfuscated The obfuscated note ID
     * @param updateDTO The updated note data
     * @return ResponseEntity with ApiResponse containing the updated note
     */
    @AuditLogAnnotation(
        action = "UPDATE_CUSTOMER_NOTE",
        description = "Updating customer note",
        entityType = "CustomerNote",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateCustomerNote(String idObfuscated, UpdateCustomerNoteDTO updateDTO) {
        log.info("Updating customer note with ID: {}", idObfuscated);

        try {
            // Validate that at least one field is provided for update
            ResponseEntity<ApiResponse<?>> validationError = validateAtLeastOneFieldProvided(updateDTO);
            if (validationError != null) {
                return validationError;
            }

            // Validate input fields
            validationError = validateInputFields(updateDTO);
            if (validationError != null) {
                return validationError;
            }

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

            return updateCustomerNoteById(id, updateDTO);

        } catch (Exception e) {
            log.error("Error updating customer note", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update customer note",
                    "CUSTOMER_NOTE_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Mark a follow-up as completed
     *
     * @param idObfuscated The obfuscated note ID
     * @return ResponseEntity with ApiResponse containing the updated note
     */
    @AuditLogAnnotation(
        action = "COMPLETE_CUSTOMER_NOTE_FOLLOW_UP",
        description = "Marking customer note follow-up as completed",
        entityType = "CustomerNote",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> markFollowUpCompleted(String idObfuscated) {
        log.info("Marking follow-up as completed for note: {}", idObfuscated);

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

            // Check if note has a follow-up
            if (note.getFollowUpDate() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Note does not have a follow-up date",
                        "NO_FOLLOW_UP_DATE"
                    )
                );
            }

            // Check if already completed
            if (note.getFollowUpCompleted() != null && note.getFollowUpCompleted()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Follow-up is already completed",
                        "FOLLOW_UP_ALREADY_COMPLETED"
                    )
                );
            }

            // Mark as completed
            note.setFollowUpCompleted(true);
            note.setFollowUpCompletedAt(LocalDateTime.now());

            // Save updated note
            note = customerNoteRepository.save(note);

            // Convert to DTO
            CustomerNoteDTO noteDTO = convertToDTO(note);

            log.info("Follow-up marked as completed for note: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Follow-up marked as completed successfully",
                    noteDTO
                )
            );

        } catch (Exception e) {
            log.error("Error marking follow-up as completed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to mark follow-up as completed",
                    "FOLLOW_UP_COMPLETE_FAILED"
                )
            );
        }
    }

    /**
     * Update a customer note by ID (internal method)
     */
    private ResponseEntity<ApiResponse<?>> updateCustomerNoteById(Long id, UpdateCustomerNoteDTO updateDTO) {
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

        // Update fields if provided
        if (updateDTO.getNoteType() != null) {
            note.setNoteType(updateDTO.getNoteType().isBlank() ? null : com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NoteType.valueOf(updateDTO.getNoteType().trim()));
        }
        if (updateDTO.getSubject() != null) {
            note.setSubject(updateDTO.getSubject());
        }
        if (updateDTO.getContent() != null) {
            note.setContent(updateDTO.getContent());
        }
        if (updateDTO.getIsPinned() != null) {
            note.setIsPinned(updateDTO.getIsPinned());
        }
        if (updateDTO.getIsPrivate() != null) {
            note.setIsPrivate(updateDTO.getIsPrivate());
        }
        if (updateDTO.getPriority() != null) {
            note.setPriority(updateDTO.getPriority().isBlank() ? null : com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NotePriority.valueOf(updateDTO.getPriority().trim()));
        }

        // Save updated note
        note = customerNoteRepository.save(note);

        // Convert to DTO
        CustomerNoteDTO noteDTO = convertToDTO(note);

        log.info("Customer note updated successfully: {}", note.getId());

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Customer note updated successfully",
                noteDTO
            )
        );
    }

    /**
     * Validate that at least one field is provided for update
     */
    private ResponseEntity<ApiResponse<?>> validateAtLeastOneFieldProvided(UpdateCustomerNoteDTO dto) {
        boolean hasUpdate =
            dto.getNoteType() != null ||
            dto.getSubject() != null ||
            dto.getContent() != null ||
            dto.getIsPinned() != null ||
            dto.getIsPrivate() != null ||
            dto.getPriority() != null;

        if (!hasUpdate) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "At least one field must be provided for update", "NO_FIELDS_TO_UPDATE")
            );
        }

        return null;
    }

    /**
     * Validate input fields for note update
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(UpdateCustomerNoteDTO dto) {
        // Validate content if provided
        if (dto.getContent() != null && dto.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Note content cannot be empty", "INVALID_CONTENT")
            );
        }

        // Validate subject length if provided
        if (dto.getSubject() != null && dto.getSubject().length() > 200) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Subject cannot exceed 200 characters", "SUBJECT_TOO_LONG")
            );
        }

        return null; // No validation errors
    }

    /**
     * Convert CustomerNote entity to DTO
     */
    private CustomerNoteDTO convertToDTO(CustomerNote note) {
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
            .isFollowUpOverdue(note.isFollowUpOverdue())
            .isPinned(note.getIsPinned())
            .isPrivate(note.getIsPrivate())
            .priority(note.getPriority())
            .priorityDisplayName(note.getPriority() != null ? note.getPriority().getDisplayName() : null)
            .priorityDescription(note.getPriority() != null ? note.getPriority().getDescription() : null)
            .createdAt(note.getCreatedAt())
            .updatedAt(note.getUpdatedAt())
            .build();
    }
}
