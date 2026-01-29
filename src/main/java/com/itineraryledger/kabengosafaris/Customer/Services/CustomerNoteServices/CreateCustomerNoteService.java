package com.itineraryledger.kabengosafaris.Customer.Services.CustomerNoteServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerNoteRepository;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerNoteDTOs.CustomerNoteDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerNoteDTOs.CreateCustomerNoteDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NoteType;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NotePriority;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CreateCustomerNoteService - Service for creating customer notes
 */
@Service
@Slf4j
public class CreateCustomerNoteService {

    private final CustomerNoteRepository customerNoteRepository;
    private final CustomerRepository customerRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreateCustomerNoteService(
        CustomerNoteRepository customerNoteRepository,
        CustomerRepository customerRepository,
        IdObfuscator idObfuscator
    ) {
        this.customerNoteRepository = customerNoteRepository;
        this.customerRepository = customerRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new customer note
     *
     * @param createDTO The note data
     * @return ResponseEntity with ApiResponse containing the created note
     */
    @Transactional
    @AuditLogAnnotation(
        action = "CREATE_CUSTOMER_NOTE",
        description = "Creating a new customer note",
        entityType = "CustomerNote"
    )
    public ResponseEntity<ApiResponse<?>> createCustomerNote(CreateCustomerNoteDTO createDTO) {
        log.info("Creating new customer note");

        try {
            // Validate input fields
            ResponseEntity<ApiResponse<?>> validationError = validateInputFields(createDTO);
            if (validationError != null) {
                return validationError;
            }

            // Decode customer ID
            Long customerId;
            try {
                customerId = idObfuscator.decodeId(createDTO.getCustomerId());
            } catch (Exception e) {
                log.warn("Invalid customer ID: {}", createDTO.getCustomerId());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid customer ID",
                        "INVALID_CUSTOMER_ID"
                    )
                );
            }

            // Find customer
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) {
                log.warn("Customer not found: {}", customerId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Customer not found",
                        "CUSTOMER_NOT_FOUND"
                    )
                );
            }

            // Create note entity
            CustomerNote note = CustomerNote.builder()
                .customer(customer)
                .noteType(createDTO.getNoteType() != null ? createDTO.getNoteType() : NoteType.GENERAL)
                .subject(createDTO.getSubject())
                .content(createDTO.getContent())
                .followUpDate(createDTO.getFollowUpDate())
                .followUpCompleted(false)
                .isPinned(createDTO.getIsPinned() != null ? createDTO.getIsPinned() : false)
                .isPrivate(createDTO.getIsPrivate() != null ? createDTO.getIsPrivate() : false)
                .priority(createDTO.getPriority() != null ? createDTO.getPriority() : NotePriority.NORMAL)
                .build();

            // Save to database
            CustomerNote savedNote = customerNoteRepository.save(note);

            log.info("Customer note created successfully with ID: {}", savedNote.getId());

            // Create response DTO
            CustomerNoteDTO noteDTO = convertToDTO(savedNote);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Customer note created successfully",
                    noteDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating customer note", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create customer note: " + e.getMessage(),
                    "CUSTOMER_NOTE_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Validate input fields for note creation
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(CreateCustomerNoteDTO dto) {
        // Validate content
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
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
