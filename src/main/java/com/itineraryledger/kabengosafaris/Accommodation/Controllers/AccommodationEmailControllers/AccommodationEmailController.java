package com.itineraryledger.kabengosafaris.Accommodation.Controllers.AccommodationEmailControllers;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationEmailDTOs.CreateAccommodationEmailDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationEmailDTOs.UpdateAccommodationEmailDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationEmail.EmailType;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationEmailServices.AccommodationEmailGetService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationEmailServices.CreateAccommodationEmailService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationEmailServices.DeleteAccommodationEmailService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationEmailServices.UpdateAccommodationEmailService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AccommodationEmailController - REST controller for managing accommodation emails
 *
 * Provides endpoints for CRUD operations on accommodation emails with permission-based access control
 */
@RestController
@RequestMapping("/api/accommodation-emails")
@Slf4j
public class AccommodationEmailController {

    private final CreateAccommodationEmailService createAccommodationEmailService;
    private final UpdateAccommodationEmailService updateAccommodationEmailService;
    private final DeleteAccommodationEmailService deleteAccommodationEmailService;
    private final AccommodationEmailGetService accommodationEmailGetService;

    @Autowired
    public AccommodationEmailController(
        CreateAccommodationEmailService createAccommodationEmailService,
        UpdateAccommodationEmailService updateAccommodationEmailService,
        DeleteAccommodationEmailService deleteAccommodationEmailService,
        AccommodationEmailGetService accommodationEmailGetService
    ) {
        this.createAccommodationEmailService = createAccommodationEmailService;
        this.updateAccommodationEmailService = updateAccommodationEmailService;
        this.deleteAccommodationEmailService = deleteAccommodationEmailService;
        this.accommodationEmailGetService = accommodationEmailGetService;
    }

    /**
     * Create a new accommodation email
     *
     * @param createAccommodationEmailDTO The email data
     * @return ResponseEntity with ApiResponse containing the created email
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ACCOMMODATION_EMAIL')")
    public ResponseEntity<ApiResponse<?>> createAccommodationEmail(
        @Valid @RequestBody CreateAccommodationEmailDTO createAccommodationEmailDTO
    ) {
        log.info("POST /api/accommodation-emails - Creating new email: {}", createAccommodationEmailDTO.getEmail());
        return createAccommodationEmailService.createAccommodationEmail(createAccommodationEmailDTO);
    }

    /**
     * Update an existing accommodation email
     *
     * @param idObfuscated The obfuscated email ID
     * @param updateAccommodationEmailDTO The updated email data
     * @return ResponseEntity with ApiResponse containing the updated email
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACCOMMODATION_EMAIL')")
    public ResponseEntity<ApiResponse<?>> updateAccommodationEmail(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateAccommodationEmailDTO updateAccommodationEmailDTO
    ) {
        log.info("PUT /api/accommodation-emails/{} - Updating email", idObfuscated);
        return updateAccommodationEmailService.updateAccommodationEmail(idObfuscated, updateAccommodationEmailDTO);
    }

    /**
     * Delete accommodation emails by list of IDs
     *
     * @param idObfuscatedList List of obfuscated email IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ACCOMMODATION_EMAIL')")
    public ResponseEntity<ApiResponse<?>> deleteAccommodationEmails(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/accommodation-emails - Deleting {} emails", idObfuscatedList.size());
        return deleteAccommodationEmailService.deleteAccommodationEmails(idObfuscatedList);
    }

    /**
     * Get a single accommodation email by ID
     *
     * @param idObfuscated The obfuscated email ID
     * @return ResponseEntity with ApiResponse containing the email
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_EMAIL')")
    public ResponseEntity<ApiResponse<?>> getAccommodationEmailById(
        @PathVariable String idObfuscated
    ) {
        log.info("GET /api/accommodation-emails/{} - Fetching email by ID", idObfuscated);
        return accommodationEmailGetService.getAccommodationEmailById(idObfuscated);
    }

    /**
     * Get all accommodation emails with pagination, sorting, and filtering
     *
     * @param accommodationId Filter by accommodation ID (optional)
     * @param email Filter by email address (partial match)
     * @param emailType Filter by email type
     * @param isPrimary Filter by primary status
     * @param isActive Filter by active status
     * @param label Filter by label (partial match)
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated emails
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_EMAIL')")
    public ResponseEntity<ApiResponse<?>> getAllAccommodationEmails(
        @RequestParam(required = false) String accommodationId,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) EmailType emailType,
        @RequestParam(required = false) Boolean isPrimary,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String label,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/accommodation-emails - Fetching all emails with filters");

        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        Sort sort = sortDirection.equalsIgnoreCase("asc")
            ? Sort.by(sortField).ascending()
            : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return accommodationEmailGetService.getAllAccommodationEmails(
            accommodationId,
            email,
            emailType,
            isPrimary,
            isActive,
            label,
            keyword,
            sortBy,
            sortDirection,
            pageable
        );
    }

    /**
     * Get all emails for a specific accommodation
     *
     * @param accommodationId Required accommodation ID
     * @param email Filter by email address (partial match)
     * @param emailType Filter by email type
     * @param isPrimary Filter by primary status
     * @param isActive Filter by active status
     * @param label Filter by label (partial match)
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated emails
     */
    @GetMapping("/accommodation/{accommodationId}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_EMAIL')")
    public ResponseEntity<ApiResponse<?>> getAllAccommodationsEmails(
        @PathVariable @NotBlank(message = "Accommodation ID is required") String accommodationId,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) EmailType emailType,
        @RequestParam(required = false) Boolean isPrimary,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String label,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/accommodation-emails/accommodation/{} - Fetching emails for accommodation", accommodationId);

        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        Sort sort = sortDirection.equalsIgnoreCase("asc")
            ? Sort.by(sortField).ascending()
            : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return accommodationEmailGetService.getAllAccommodationsEmails(
            accommodationId,
            email,
            emailType,
            isPrimary,
            isActive,
            label,
            keyword,
            sortBy,
            sortDirection,
            pageable
        );
    }
}
