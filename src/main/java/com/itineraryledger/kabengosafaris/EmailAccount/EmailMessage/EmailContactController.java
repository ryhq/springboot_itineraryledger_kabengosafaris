package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.CreateEmailContactDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailContactCreateService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailContactDeleteService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailContactGetService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailContactUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/email-accounts/{accountId}/contacts")
@Validated
@RequiredArgsConstructor
public class EmailContactController {

    private final EmailContactGetService emailContactGetService;
    private final EmailContactCreateService emailContactCreateService;
    private final EmailContactUpdateService emailContactUpdateService;
    private final EmailContactDeleteService emailContactDeleteService;

    /**
     * Autocomplete search — used by compose TO/CC/BCC fields
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_CONTACT')")
    public ResponseEntity<ApiResponse<?>> searchContacts(
            @PathVariable("accountId") String accountId,
            @RequestParam(value = "q", required = false) String search,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return emailContactGetService.searchContacts(accountId, search, limit);
    }

    /**
     * List all contacts (paginated, sortable, filterable via Specification)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_CONTACT')")
    public ResponseEntity<ApiResponse<?>> getContacts(
            @PathVariable("accountId") String accountId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "sortBy", defaultValue = "frequency") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            @RequestParam(value = "isStarred", required = false) Boolean isStarred,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "search", required = false) String search) {
        return emailContactGetService.getContacts(accountId, page, size, sortBy, sortDirection, isStarred, source, search);
    }

    /**
     * Get single contact with circular navigation
     */
    @GetMapping("/{contactId}")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_CONTACT')")
    public ResponseEntity<ApiResponse<?>> getContact(
            @PathVariable("accountId") String accountId,
            @PathVariable("contactId") String contactId) {
        return emailContactGetService.getContact(accountId, contactId);
    }

    /**
     * Create a contact manually
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_EMAIL_CONTACT')")
    public ResponseEntity<ApiResponse<?>> createContact(
            @PathVariable("accountId") String accountId,
            @Valid @RequestBody CreateEmailContactDTO dto) {
        return emailContactCreateService.createContact(accountId, dto);
    }

    /**
     * Update a contact
     */
    @PutMapping("/{contactId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_CONTACT')")
    public ResponseEntity<ApiResponse<?>> updateContact(
            @PathVariable("accountId") String accountId,
            @PathVariable("contactId") String contactId,
            @Valid @RequestBody CreateEmailContactDTO dto) {
        return emailContactUpdateService.updateContact(accountId, contactId, dto);
    }

    /**
     * Toggle star on a contact
     */
    @PutMapping("/{contactId}/star")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_CONTACT')")
    public ResponseEntity<ApiResponse<?>> toggleStar(
            @PathVariable("accountId") String accountId,
            @PathVariable("contactId") String contactId) {
        return emailContactUpdateService.toggleStar(accountId, contactId);
    }

    /**
     * Delete a contact
     */
    @DeleteMapping("/{contactId}")
    @PreAuthorize("hasAuthority('PERM_DELETE_EMAIL_CONTACT')")
    public ResponseEntity<ApiResponse<?>> deleteContact(
            @PathVariable("accountId") String accountId,
            @PathVariable("contactId") String contactId) {
        return emailContactDeleteService.deleteContact(accountId, contactId);
    }
}
