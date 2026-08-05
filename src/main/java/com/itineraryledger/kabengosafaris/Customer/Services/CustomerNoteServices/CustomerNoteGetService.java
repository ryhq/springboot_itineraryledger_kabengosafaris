package com.itineraryledger.kabengosafaris.Customer.Services.CustomerNoteServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerNoteRepository;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerNoteDTOs.CustomerNoteDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NoteType;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NotePriority;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CustomerNoteGetService - Service for retrieving customer notes
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class CustomerNoteGetService {

    private final CustomerNoteRepository customerNoteRepository;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final IdObfuscator idObfuscator;

    /*
     * followUpCompleted and followUpDate are sortable because the follow-up queue
     * is the reason this list exists — the panel defaults to ordering by when a
     * note is due, and a rejected sort field is a 400, not a fallback.
     */
    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "subject", "noteType", "priority", "isPinned", "isPrivate",
        "followUpDate", "followUpCompleted", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public CustomerNoteGetService(
        CustomerNoteRepository customerNoteRepository,
        IdObfuscator idObfuscator,
        com.itineraryledger.kabengosafaris.Response.ListStats listStats,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.customerNoteRepository = customerNoteRepository;
        this.idObfuscator = idObfuscator;
        this.listStats = listStats;
        this.recordNavigation = recordNavigation;
    }

    /**
     * Get customer note by obfuscated ID
     *
     * @param idObfuscated The obfuscated note ID
     * @return ResponseEntity with ApiResponse containing the note
     */
    public ResponseEntity<ApiResponse<?>> getCustomerNoteById(String idObfuscated, String scopeParentId) {
        return getCustomerNoteById(idObfuscated, scopeParentId, null, null, null, null, null, null, null);
    }

    /** A record plus prev/next over the SAME filtered set the list was showing. */
    public ResponseEntity<ApiResponse<?>> getCustomerNoteById(
        String idObfuscated,
        String scopeParentId,
        String keyword,
        java.util.List<CustomerNote.NoteType> noteTypes,
        java.util.List<CustomerNote.NotePriority> priorities,
        java.util.List<String> queues,
        java.time.LocalDateTime createdAfter,
        String sortBy,
        String sortDirection
    ) {
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

            // Decode optional scope parent ID for scoped navigation
            Long decodedParentId = null;
            if (scopeParentId != null && !scopeParentId.isEmpty()) {
                try {
                    decodedParentId = idObfuscator.decodeId(scopeParentId);
                } catch (Exception ex) {
                    log.warn("Invalid scopeParentId: {}, falling back to global navigation", scopeParentId);
                }
            }

            // Circular navigation (scoped if parent provided, global otherwise)
            // walk the caller's filtered + sorted set, scoped to the parent when given
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                CustomerNote.class,
                navigationSpec(decodedParentId, keyword, noteTypes, priorities, queues, createdAfter),
                validateSortField(sortBy) != null ? validateSortField(sortBy) : "createdAt",
                "asc".equalsIgnoreCase(sortDirection),
                id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("note", noteDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Customer note retrieved successfully",
                    response
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
     * @param pendingFollowUpsOnly Filter only notes with pending follow-ups (optional)
     * @param overdueFollowUpsOnly Filter only notes with overdue follow-ups (optional)
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
        Boolean pendingFollowUpsOnly,
        Boolean overdueFollowUpsOnly,
        String keyword,
        java.util.List<CustomerNote.NoteType> noteTypes,
        java.util.List<CustomerNote.NotePriority> priorities,
        java.util.List<String> queues,
        java.time.LocalDateTime createdAfter,
        java.time.LocalDateTime createdBefore,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
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
            if (pendingFollowUpsOnly != null && pendingFollowUpsOnly) {
                spec = spec.and(CustomerNoteSpecification.hasPendingFollowUp());
            }
            if (overdueFollowUpsOnly != null && overdueFollowUpsOnly) {
                spec = spec.and(CustomerNoteSpecification.hasOverdueFollowUp());
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(CustomerNoteSpecification.searchKeyword(keyword));
            }

            // Pagination
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            // Fetch paginated results
            // multi-value facets, so every stat card is also a filter
            if (noteTypes != null && !noteTypes.isEmpty()) spec = spec.and(CustomerNoteSpecification.noteTypeIn(noteTypes));
            if (priorities != null && !priorities.isEmpty()) spec = spec.and(CustomerNoteSpecification.priorityIn(priorities));
            if (queues != null && !queues.isEmpty()) {
                if (queues.contains("pending")) spec = spec.and(CustomerNoteSpecification.hasPendingFollowUp());
                if (queues.contains("overdue")) spec = spec.and(CustomerNoteSpecification.hasOverdueFollowUp());
                if (queues.contains("pinned")) spec = spec.and(CustomerNoteSpecification.isPinned(true));
                if (queues.contains("private")) spec = spec.and(CustomerNoteSpecification.isPrivate(true));
                if (queues.contains("done")) spec = spec.and(CustomerNoteSpecification.isFollowUpCompleted(true));
            }
            if (createdAfter != null) spec = spec.and(CustomerNoteSpecification.createdAfter(createdAfter));
            if (createdBefore != null) spec = spec.and(CustomerNoteSpecification.createdBefore(createdBefore));
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
            responseData.put("validSortFields", VALID_SORT_FIELDS);
            responseData.put("currentSortBy", validatedSortBy);
            responseData.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            if (!Boolean.FALSE.equals(includeStats)) {
                responseData.put("stats", computeStats(spec));
            }

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

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
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

    /** Dashboard counters for the CURRENT filter set (stats on every list). */
    private Map<String, Object> computeStats(Specification<CustomerNote> base) {
        return listStats.of(CustomerNote.class, base)
            .total()
            .count("pending", CustomerNoteSpecification.hasPendingFollowUp())
            .count("overdue", CustomerNoteSpecification.hasOverdueFollowUp())
            .count("dueWithin7Days", CustomerNoteSpecification.followUpDueWithin(7))
            .count("completed", CustomerNoteSpecification.isFollowUpCompleted(true))
            .count("pinned", CustomerNoteSpecification.isPinned(true))
            .count("privateNotes", CustomerNoteSpecification.isPrivate(true))
            .breakdown("byNoteType", CustomerNote.NoteType.values(), CustomerNoteSpecification::hasNoteType)
            .breakdown("byPriority", CustomerNote.NotePriority.values(), CustomerNoteSpecification::hasPriority)
            .recency(CustomerNoteSpecification::createdAfter)
            .build();
    }

    /**
     * The filter chain the record pager walks — the same dimensions the list
     * offers, so paging from a filtered list stays inside those matches.
     */
    private Specification<CustomerNote> navigationSpec(
        Long decodedParentId,
        String keyword,
        java.util.List<CustomerNote.NoteType> noteTypes,
        java.util.List<CustomerNote.NotePriority> priorities,
        java.util.List<String> queues,
        java.time.LocalDateTime createdAfter
    ) {
        Specification<CustomerNote> spec = Specification.unrestricted();
        if (decodedParentId != null) spec = spec.and(CustomerNoteSpecification.hasCustomerId(decodedParentId));
        if (keyword != null && !keyword.isEmpty()) spec = spec.and(CustomerNoteSpecification.searchKeyword(keyword));
        if (noteTypes != null && !noteTypes.isEmpty()) spec = spec.and(CustomerNoteSpecification.noteTypeIn(noteTypes));
        if (priorities != null && !priorities.isEmpty()) spec = spec.and(CustomerNoteSpecification.priorityIn(priorities));
        if (queues != null && !queues.isEmpty()) {
            if (queues.contains("pending")) spec = spec.and(CustomerNoteSpecification.hasPendingFollowUp());
            if (queues.contains("overdue")) spec = spec.and(CustomerNoteSpecification.hasOverdueFollowUp());
            if (queues.contains("pinned")) spec = spec.and(CustomerNoteSpecification.isPinned(true));
            if (queues.contains("private")) spec = spec.and(CustomerNoteSpecification.isPrivate(true));
            if (queues.contains("done")) spec = spec.and(CustomerNoteSpecification.isFollowUpCompleted(true));
        }
        if (createdAfter != null) spec = spec.and(CustomerNoteSpecification.createdAfter(createdAfter));
        return spec;
    }
}
