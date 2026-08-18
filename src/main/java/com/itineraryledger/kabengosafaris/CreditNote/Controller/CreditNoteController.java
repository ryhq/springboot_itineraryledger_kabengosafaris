package com.itineraryledger.kabengosafaris.CreditNote.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CreateCreditNoteDTO;
import com.itineraryledger.kabengosafaris.CreditNote.DTOs.UpdateCreditNoteDTO;
import com.itineraryledger.kabengosafaris.CreditNote.Enums.CreditNoteStatus;
import com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices.CreditNoteCreateService;
import com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices.CreditNoteDeleteService;
import com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices.CreditNoteGetService;
import com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices.CreditNoteUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/credit-notes")
@Slf4j
@RequiredArgsConstructor
public class CreditNoteController {

    private final CreditNoteCreateService creditNoteCreateService;
    private final CreditNoteUpdateService creditNoteUpdateService;
    private final CreditNoteDeleteService creditNoteDeleteService;
    private final CreditNoteGetService creditNoteGetService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<?>> createCreditNote(@Valid @RequestBody CreateCreditNoteDTO createDTO) {
        log.info("POST /api/credit-notes - Creating new credit note");
        return creditNoteCreateService.createCreditNote(createDTO);
    }

    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<?>> updateCreditNote(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateCreditNoteDTO updateDTO
    ) {
        log.info("PUT /api/credit-notes/{} - Updating credit note", idObfuscated);
        return creditNoteUpdateService.updateCreditNote(idObfuscated, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<?>> deleteCreditNotes(@RequestBody List<String> idObfuscatedList) {
        log.info("DELETE /api/credit-notes - Deleting {} credit notes", idObfuscatedList.size());
        return creditNoteDeleteService.deleteCreditNotes(idObfuscatedList);
    }

    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<?>> getCreditNoteById(
        @PathVariable String idObfuscated,
        /* the list's filters travel with the record so its arrows stay in that set */
        @RequestParam(required = false) String creditNoteCode,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) CreditNoteStatus status,
        @RequestParam(required = false) List<CreditNoteStatus> statuses,
        @RequestParam(required = false) List<String> statusGroups,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String invoiceId,
        @RequestParam(required = false) String customerId,
        @RequestParam(required = false) LocalDate issueDateFrom,
        @RequestParam(required = false) LocalDate issueDateTo,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/credit-notes/{} - Fetching credit note by ID", idObfuscated);
        return creditNoteGetService.getCreditNoteById(idObfuscated, creditNoteCode, title, status,
            statuses, statusGroups, keyword, invoiceId, customerId, issueDateFrom, issueDateTo,
            isActive, sortBy, sortDirection);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_CREDIT_NOTE')")
    public ResponseEntity<ApiResponse<?>> getAllCreditNotes(
        @RequestParam(required = false) String creditNoteCode,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) CreditNoteStatus status,
        /* several at once: "confirmed or sent" is one question about outstanding credit */
        @RequestParam(required = false) List<CreditNoteStatus> statuses,
        /* stages rather than states: "outstanding" is confirmed-or-sent, asked as one question */
        @RequestParam(required = false) List<String> statusGroups,
        /* free text over the code, the title, the reason, the customer and the invoice */
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false) String invoiceId,
        @RequestParam(required = false) String customerId,
        @RequestParam(required = false) LocalDate issueDateFrom,
        @RequestParam(required = false) LocalDate issueDateTo,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/credit-notes - Fetching all credit notes with filters");
        return creditNoteGetService.getAllCreditNotes(
            creditNoteCode, title, status, statuses, statusGroups, keyword, includeStats, invoiceId,
            customerId, issueDateFrom, issueDateTo, isActive, page, size, sortBy, sortDirection
        );
    }
}
