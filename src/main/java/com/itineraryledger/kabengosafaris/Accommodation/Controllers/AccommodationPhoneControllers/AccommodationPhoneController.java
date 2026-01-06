package com.itineraryledger.kabengosafaris.Accommodation.Controllers.AccommodationPhoneControllers;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationPhoneDTOs.CreateAccommodationPhoneDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationPhoneDTOs.UpdateAccommodationPhoneDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationPhone.PhoneType;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationPhoneServices.AccommodationPhoneGetService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationPhoneServices.CreateAccommodationPhoneService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationPhoneServices.DeleteAccommodationPhoneService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationPhoneServices.UpdateAccommodationPhoneService;
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
 * AccommodationPhoneController - REST controller for managing accommodation phones
 *
 * Provides endpoints for CRUD operations on accommodation phones with permission-based access control
 */
@RestController
@RequestMapping("/api/accommodation-phones")
@Slf4j
public class AccommodationPhoneController {

    private final CreateAccommodationPhoneService createAccommodationPhoneService;
    private final UpdateAccommodationPhoneService updateAccommodationPhoneService;
    private final DeleteAccommodationPhoneService deleteAccommodationPhoneService;
    private final AccommodationPhoneGetService accommodationPhoneGetService;

    @Autowired
    public AccommodationPhoneController(
        CreateAccommodationPhoneService createAccommodationPhoneService,
        UpdateAccommodationPhoneService updateAccommodationPhoneService,
        DeleteAccommodationPhoneService deleteAccommodationPhoneService,
        AccommodationPhoneGetService accommodationPhoneGetService
    ) {
        this.createAccommodationPhoneService = createAccommodationPhoneService;
        this.updateAccommodationPhoneService = updateAccommodationPhoneService;
        this.deleteAccommodationPhoneService = deleteAccommodationPhoneService;
        this.accommodationPhoneGetService = accommodationPhoneGetService;
    }

    /**
     * Create a new accommodation phone
     *
     * @param createAccommodationPhoneDTO The phone data
     * @return ResponseEntity with ApiResponse containing the created phone
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_ACCOMMODATION_PHONE')")
    public ResponseEntity<ApiResponse<?>> createAccommodationPhone(
        @Valid @RequestBody CreateAccommodationPhoneDTO createAccommodationPhoneDTO
    ) {
        log.info("POST /api/accommodation-phones - Creating new phone: {}", createAccommodationPhoneDTO.getPhoneNumber());
        return createAccommodationPhoneService.createAccommodationPhone(createAccommodationPhoneDTO);
    }

    /**
     * Update an existing accommodation phone
     *
     * @param idObfuscated The obfuscated phone ID
     * @param updateAccommodationPhoneDTO The updated phone data
     * @return ResponseEntity with ApiResponse containing the updated phone
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACCOMMODATION_PHONE')")
    public ResponseEntity<ApiResponse<?>> updateAccommodationPhone(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateAccommodationPhoneDTO updateAccommodationPhoneDTO
    ) {
        log.info("PUT /api/accommodation-phones/{} - Updating phone", idObfuscated);
        return updateAccommodationPhoneService.updateAccommodationPhone(idObfuscated, updateAccommodationPhoneDTO);
    }

    /**
     * Delete accommodation phones by list of IDs
     *
     * @param idObfuscatedList List of obfuscated phone IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ACCOMMODATION_PHONE')")
    public ResponseEntity<ApiResponse<?>> deleteAccommodationPhones(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/accommodation-phones - Deleting {} phones", idObfuscatedList.size());
        return deleteAccommodationPhoneService.deleteAccommodationPhones(idObfuscatedList);
    }

    /**
     * Get a single accommodation phone by ID
     *
     * @param idObfuscated The obfuscated phone ID
     * @return ResponseEntity with ApiResponse containing the phone
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_PHONE')")
    public ResponseEntity<ApiResponse<?>> getAccommodationPhoneById(
        @PathVariable String idObfuscated
    ) {
        log.info("GET /api/accommodation-phones/{} - Fetching phone by ID", idObfuscated);
        return accommodationPhoneGetService.getAccommodationPhoneById(idObfuscated);
    }

    /**
     * Get all accommodation phones with pagination, sorting, and filtering
     *
     * @param accommodationId Filter by accommodation ID (optional)
     * @param phoneNumber Filter by phone number (partial match)
     * @param countryCode Filter by country code
     * @param phoneType Filter by phone type
     * @param isPrimary Filter by primary status
     * @param isWhatsApp Filter by WhatsApp status
     * @param isActive Filter by active status
     * @param label Filter by label (partial match)
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated phones
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_PHONE')")
    public ResponseEntity<ApiResponse<?>> getAllAccommodationPhones(
        @RequestParam(required = false) String accommodationId,
        @RequestParam(required = false) String phoneNumber,
        @RequestParam(required = false) String countryCode,
        @RequestParam(required = false) PhoneType phoneType,
        @RequestParam(required = false) Boolean isPrimary,
        @RequestParam(required = false) Boolean isWhatsApp,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String label,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/accommodation-phones - Fetching all phones with filters");

        Sort sort = sortDirection.equalsIgnoreCase("asc")
            ? Sort.by("createdAt").ascending()
            : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return accommodationPhoneGetService.getAllAccommodationPhones(
            accommodationId,
            phoneNumber,
            countryCode,
            phoneType,
            isPrimary,
            isWhatsApp,
            isActive,
            label,
            keyword,
            pageable
        );
    }

    /**
     * Get all phones for a specific accommodation
     *
     * @param accommodationId Required accommodation ID
     * @param phoneNumber Filter by phone number (partial match)
     * @param countryCode Filter by country code
     * @param phoneType Filter by phone type
     * @param isPrimary Filter by primary status
     * @param isWhatsApp Filter by WhatsApp status
     * @param isActive Filter by active status
     * @param label Filter by label (partial match)
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated phones
     */
    @GetMapping("/accommodation/{accommodationId}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_PHONE')")
    public ResponseEntity<ApiResponse<?>> getAllAccommodationsPhones(
        @PathVariable @NotBlank(message = "Accommodation ID is required") String accommodationId,
        @RequestParam(required = false) String phoneNumber,
        @RequestParam(required = false) String countryCode,
        @RequestParam(required = false) PhoneType phoneType,
        @RequestParam(required = false) Boolean isPrimary,
        @RequestParam(required = false) Boolean isWhatsApp,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String label,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/accommodation-phones/accommodation/{} - Fetching phones for accommodation", accommodationId);

        Sort sort = sortDirection.equalsIgnoreCase("asc")
            ? Sort.by("createdAt").ascending()
            : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return accommodationPhoneGetService.getAllAccommodationsPhones(
            accommodationId,
            phoneNumber,
            countryCode,
            phoneType,
            isPrimary,
            isWhatsApp,
            isActive,
            label,
            keyword,
            pageable
        );
    }
}
