package com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CreditNoteDTO;
import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNote;
import com.itineraryledger.kabengosafaris.CreditNote.Enums.CreditNoteStatus;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteRepository;
import com.itineraryledger.kabengosafaris.CreditNote.Specifications.CreditNoteSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving credit notes with filtering, pagination, and sorting
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreditNoteGetService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "creditNoteCode", "title", "status", "issueDate", "sentDate", "consumedDate", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private final CreditNoteRepository creditNoteRepository;
    private final IdObfuscator idObfuscator;
    private final CreditNoteCreateService creditNoteCreateService;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    /**
     * Get a single credit note by obfuscated ID
     *
     * @param idObfuscated The obfuscated credit note ID
     * @return ResponseEntity with ApiResponse containing the credit note
     */
    public ResponseEntity<ApiResponse<?>> getCreditNoteById(
        String idObfuscated,
        String creditNoteCode,
        String title,
        CreditNoteStatus status,
        String invoiceId,
        String customerId,
        LocalDate issueDateFrom,
        LocalDate issueDateTo,
        Boolean isActive,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching credit note with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode credit note ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid credit note ID", "INVALID_CREDIT_NOTE_ID")
                );
            }

            // Find credit note
            CreditNote creditNote = creditNoteRepository.findById(id).orElse(null);
            if (creditNote == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Credit note not found", "CREDIT_NOTE_NOT_FOUND")
                );
            }

            // Convert to DTO
            CreditNoteDTO creditNoteDTO = creditNoteCreateService.convertToDTO(creditNote);

            /*
             * Circular navigation over the caller's filtered, sorted set — scoped to the
             * parent when one is given. The id-ordered walk this replaces stepped through a
             * different set from the one on screen and could not say where you were in it.
             */
            String validatedSortBy = validateSortField(sortBy);
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                CreditNote.class,
                buildSpec(creditNoteCode, title, status, invoiceId, customerId, issueDateFrom, issueDateTo, isActive),
                validatedSortBy != null ? validatedSortBy : "createdAt",
                "asc".equalsIgnoreCase(sortDirection),
                id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("creditNote", creditNoteDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Credit note retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching credit note", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch credit note", "CREDIT_NOTE_FETCH_FAILED")
            );
        }
    }

    /**
     * Get all credit notes with pagination, sorting, and filtering
     *
     * @param creditNoteCode Filter by credit note code (partial match)
     * @param title Filter by title (partial match)
     * @param status Filter by status
     * @param invoiceId Filter by invoice ID (obfuscated)
     * @param customerId Filter by customer ID (obfuscated)
     * @param issueDateFrom Filter by issue date from
     * @param issueDateTo Filter by issue date to
     * @param isActive Filter by active status
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated credit notes
     */
    public ResponseEntity<ApiResponse<?>> getAllCreditNotes(
        String creditNoteCode,
        String title,
        CreditNoteStatus status,
        String invoiceId,
        String customerId,
        LocalDate issueDateFrom,
        LocalDate issueDateTo,
        Boolean isActive,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching all credit notes with filters");

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            // Build specification for filtering
            Specification<CreditNote> spec = buildSpec(creditNoteCode, title, status, invoiceId, customerId, issueDateFrom, issueDateTo, isActive);

            // Set default pagination values
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Set sorting
            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            // Create pageable
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            // Fetch credit notes
            Page<CreditNote> creditNotePage = creditNoteRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<CreditNoteDTO> creditNoteDTOs = creditNotePage.getContent().stream()
                .map(creditNoteCreateService::convertToDTO)
                .collect(Collectors.toList());

            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("creditNotes", creditNoteDTOs);
            response.put("currentPage", creditNotePage.getNumber());
            response.put("totalItems", creditNotePage.getTotalElements());
            response.put("totalPages", creditNotePage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", direction.name().toLowerCase());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Credit notes retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching credit notes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch credit notes", "CREDIT_NOTES_FETCH_FAILED")
            );
        }
    }

    /**
     * Get all credit notes for a specific invoice
     *
     * @param invoiceIdObfuscated The obfuscated invoice ID
     * @return ResponseEntity with ApiResponse containing list of credit notes
     */
    public ResponseEntity<ApiResponse<?>> getByInvoiceId(String invoiceIdObfuscated) {
        log.info("Fetching credit notes for invoice: {}", invoiceIdObfuscated);

        try {
            Long invoiceId;
            try {
                invoiceId = idObfuscator.decodeId(invoiceIdObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode invoice ID: {}", invoiceIdObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid invoice ID", "INVALID_INVOICE_ID")
                );
            }

            List<CreditNote> creditNotes = creditNoteRepository.findByInvoiceId(invoiceId);

            List<CreditNoteDTO> creditNoteDTOs = creditNotes.stream()
                .map(creditNoteCreateService::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Credit notes retrieved successfully", creditNoteDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching credit notes by invoice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch credit notes", "CREDIT_NOTES_FETCH_FAILED")
            );
        }
    }

    /**
     * Validate and return the sort field, or null if invalid
     */
    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * The ONE description of the filtered set, shared by the rows and by the record
     * arrows — paging that walked a different set from the one on screen would be
     * worse than no arrows (see CLAUDE.md).
     */
    private Specification<CreditNote> buildSpec(
        String creditNoteCode,
        String title,
        CreditNoteStatus status,
        String invoiceId,
        String customerId,
        LocalDate issueDateFrom,
        LocalDate issueDateTo,
        Boolean isActive
    ) {
        Specification<CreditNote> spec = Specification.unrestricted();

            if (creditNoteCode != null && !creditNoteCode.isEmpty()) {
                spec = spec.and(CreditNoteSpecification.byCreditNoteCode(creditNoteCode));
            }
            if (title != null && !title.isEmpty()) {
                spec = spec.and(CreditNoteSpecification.byTitle(title));
            }
            if (status != null) {
                spec = spec.and(CreditNoteSpecification.byStatus(status));
            }
            if (isActive != null) {
                spec = spec.and(CreditNoteSpecification.byIsActive(isActive));
            }
            if (issueDateFrom != null) {
                spec = spec.and(CreditNoteSpecification.byIssueDateFrom(issueDateFrom));
            }
            if (issueDateTo != null) {
                spec = spec.and(CreditNoteSpecification.byIssueDateTo(issueDateTo));
            }

            // Apply relationship filters (decode IDs)
            if (invoiceId != null && !invoiceId.isEmpty()) {
                try {
                    Long decodedId = idObfuscator.decodeId(invoiceId);
                    spec = spec.and(CreditNoteSpecification.byInvoiceId(decodedId));
                } catch (Exception e) {
                    log.warn("Failed to decode invoice ID: {}", invoiceId, e);
                }
            }
            if (customerId != null && !customerId.isEmpty()) {
                try {
                    Long decodedId = idObfuscator.decodeId(customerId);
                    spec = spec.and(CreditNoteSpecification.byCustomerId(decodedId));
                } catch (Exception e) {
                    log.warn("Failed to decode customer ID: {}", customerId, e);
                }
            }

        return spec;
    }
}
