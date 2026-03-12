package com.itineraryledger.kabengosafaris.Customer.Controller;

import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerNoteDTOs.CreateCustomerNoteDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerNoteDTOs.UpdateCustomerNoteDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NoteType;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NotePriority;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerNoteServices.CustomerNoteGetService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerNoteServices.CreateCustomerNoteService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerNoteServices.DeleteCustomerNoteService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerNoteServices.UpdateCustomerNoteService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CustomerNoteController - REST controller for managing customer notes
 *
 * Provides endpoints for CRUD operations on customer notes with permission-based access control
 */
@RestController
@RequestMapping("/api/customer-notes")
@Slf4j
public class CustomerNoteController {

    private final CreateCustomerNoteService createCustomerNoteService;
    private final UpdateCustomerNoteService updateCustomerNoteService;
    private final DeleteCustomerNoteService deleteCustomerNoteService;
    private final CustomerNoteGetService customerNoteGetService;

    @Autowired
    public CustomerNoteController(
        CreateCustomerNoteService createCustomerNoteService,
        UpdateCustomerNoteService updateCustomerNoteService,
        DeleteCustomerNoteService deleteCustomerNoteService,
        CustomerNoteGetService customerNoteGetService
    ) {
        this.createCustomerNoteService = createCustomerNoteService;
        this.updateCustomerNoteService = updateCustomerNoteService;
        this.deleteCustomerNoteService = deleteCustomerNoteService;
        this.customerNoteGetService = customerNoteGetService;
    }

    /**
     * Create a new customer note
     *
     * @param createCustomerNoteDTO The note data
     * @return ResponseEntity with ApiResponse containing the created note
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_CUSTOMER_NOTE')")
    public ResponseEntity<ApiResponse<?>> createCustomerNote(
        @Valid @RequestBody CreateCustomerNoteDTO createCustomerNoteDTO
    ) {
        log.info("POST /api/customer-notes - Creating new note for customer: {}", createCustomerNoteDTO.getCustomerId());
        return createCustomerNoteService.createCustomerNote(createCustomerNoteDTO);
    }

    /**
     * Update an existing customer note
     *
     * @param idObfuscated The obfuscated note ID
     * @param updateCustomerNoteDTO The updated note data
     * @return ResponseEntity with ApiResponse containing the updated note
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_CUSTOMER_NOTE')")
    public ResponseEntity<ApiResponse<?>> updateCustomerNote(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateCustomerNoteDTO updateCustomerNoteDTO
    ) {
        log.info("PUT /api/customer-notes/{} - Updating note", idObfuscated);
        return updateCustomerNoteService.updateCustomerNote(idObfuscated, updateCustomerNoteDTO);
    }

    /**
     * Mark a follow-up as completed
     *
     * @param idObfuscated The obfuscated note ID
     * @return ResponseEntity with ApiResponse containing the updated note
     */
    @PutMapping("/{idObfuscated}/complete-follow-up")
    @PreAuthorize("hasAuthority('PERM_UPDATE_CUSTOMER_NOTE')")
    public ResponseEntity<ApiResponse<?>> markFollowUpCompleted(
        @PathVariable String idObfuscated
    ) {
        log.info("PUT /api/customer-notes/{}/complete-follow-up - Marking follow-up as completed", idObfuscated);
        return updateCustomerNoteService.markFollowUpCompleted(idObfuscated);
    }

    /**
     * Delete customer notes by list of IDs
     *
     * @param idObfuscatedList List of obfuscated note IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_CUSTOMER_NOTE')")
    public ResponseEntity<ApiResponse<?>> deleteCustomerNotes(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/customer-notes - Deleting {} notes", idObfuscatedList.size());
        return deleteCustomerNoteService.deleteCustomerNotes(idObfuscatedList);
    }

    /**
     * Get a single customer note by ID
     *
     * @param idObfuscated The obfuscated note ID
     * @return ResponseEntity with ApiResponse containing the note
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_CUSTOMER_NOTE')")
    public ResponseEntity<ApiResponse<?>> getCustomerNoteById(
        @PathVariable String idObfuscated,
        @RequestParam(required = false) String scopeParentId
    ) {
        log.info("GET /api/customer-notes/{} - Fetching note by ID", idObfuscated);
        return customerNoteGetService.getCustomerNoteById(idObfuscated, scopeParentId);
    }

    /**
     * Get all customer notes with pagination, sorting, and filtering
     *
     * @param customerId Filter by customer ID (optional)
     * @param noteType Filter by note type
     * @param subject Filter by subject (partial match)
     * @param isPinned Filter by pinned status
     * @param isPrivate Filter by private status
     * @param priority Filter by priority
     * @param followUpCompleted Filter by follow-up completed status
     * @param pendingFollowUpsOnly Filter only notes with pending follow-ups
     * @param overdueFollowUpsOnly Filter only notes with overdue follow-ups
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated notes
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_CUSTOMER_NOTE')")
    public ResponseEntity<ApiResponse<?>> getAllCustomerNotes(
        @RequestParam(required = false) String customerId,
        @RequestParam(required = false) NoteType noteType,
        @RequestParam(required = false) String subject,
        @RequestParam(required = false) Boolean isPinned,
        @RequestParam(required = false) Boolean isPrivate,
        @RequestParam(required = false) NotePriority priority,
        @RequestParam(required = false) Boolean followUpCompleted,
        @RequestParam(required = false) Boolean pendingFollowUpsOnly,
        @RequestParam(required = false) Boolean overdueFollowUpsOnly,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/customer-notes - Fetching all notes with filters");

        return customerNoteGetService.getAllCustomerNotes(
            customerId,
            noteType,
            subject,
            isPinned,
            isPrivate,
            priority,
            followUpCompleted,
            pendingFollowUpsOnly,
            overdueFollowUpsOnly,
            keyword,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

}
