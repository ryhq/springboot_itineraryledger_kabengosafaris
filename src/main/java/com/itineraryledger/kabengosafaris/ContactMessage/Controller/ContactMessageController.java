package com.itineraryledger.kabengosafaris.ContactMessage.Controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.ContactMessage.DTOs.UpdateContactMessageDTO;
import com.itineraryledger.kabengosafaris.ContactMessage.Specifications.ContactMessageFilter;
import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessageStatus;
import com.itineraryledger.kabengosafaris.ContactMessage.Services.ContactMessageDeleteService;
import com.itineraryledger.kabengosafaris.ContactMessage.Services.ContactMessageGetService;
import com.itineraryledger.kabengosafaris.ContactMessage.Services.ContactMessageUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/contact-messages")
@Slf4j
public class ContactMessageController {

    private final ContactMessageGetService getService;
    private final ContactMessageUpdateService updateService;
    private final ContactMessageDeleteService deleteService;

    @Autowired
    public ContactMessageController(
        ContactMessageGetService getService,
        ContactMessageUpdateService updateService,
        ContactMessageDeleteService deleteService
    ) {
        this.getService = getService;
        this.updateService = updateService;
        this.deleteService = deleteService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_CONTACT_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> listMessages(
        @ModelAttribute ContactMessageFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
        @RequestParam(required = false, defaultValue = "20") Integer pageSize,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/contact-messages - Listing messages with filters");
        return getService.listMessages(filter, includeStats, pageNumber, pageSize, sortBy, sortDirection);
    }

    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_CONTACT_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> getMessageById(
        @PathVariable String idObfuscated,
        // the list's filters and sort, so prev/next walks that same set
        @ModelAttribute ContactMessageFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/contact-messages/{} - Fetching message by ID", idObfuscated);
        return getService.getMessageById(idObfuscated, filter, sortBy, sortDirection);
    }

    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_CONTACT_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> updateMessage(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateContactMessageDTO updateDTO
    ) {
        log.info("PUT /api/contact-messages/{} - Updating message", idObfuscated);
        return updateService.updateMessage(idObfuscated, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_CONTACT_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> deleteMessages(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/contact-messages - Deleting {} messages", idObfuscatedList.size());
        return deleteService.deleteMessages(idObfuscatedList);
    }
}
