package com.itineraryledger.kabengosafaris.ContactMessage.Services;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.itineraryledger.kabengosafaris.ContactMessage.DTOs.ContactMessageDTO;
import com.itineraryledger.kabengosafaris.ContactMessage.DTOs.ContactMessageListItemDTO;
import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessage;
import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessageStatus;
import com.itineraryledger.kabengosafaris.ContactMessage.Repository.ContactMessageRepository;
import com.itineraryledger.kabengosafaris.ContactMessage.Specifications.ContactMessageFilter;
import com.itineraryledger.kabengosafaris.ContactMessage.Specifications.ContactMessageSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(readOnly = true)
public class ContactMessageGetService {

    private final ContactMessageRepository repository;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "code", "name", "email", "subject", "status", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public ContactMessageGetService(
        ContactMessageRepository repository,
        IdObfuscator idObfuscator,
        com.itineraryledger.kabengosafaris.Response.ListStats listStats,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.listStats = listStats;
        this.recordNavigation = recordNavigation;
        this.repository = repository;
        this.idObfuscator = idObfuscator;
    }

    public ResponseEntity<ApiResponse<?>> getMessageById(String idObfuscated) {
        return getMessageById(idObfuscated, null, null, null);
    }

    /**
     * One message, plus where it sits in the set the caller was looking at.
     *
     * Paging out of an "unanswered" list must stay among unanswered ones.
     */
    public ResponseEntity<ApiResponse<?>> getMessageById(
        String idObfuscated,
        ContactMessageFilter filter,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching contact message with ID: {}", idObfuscated);
        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode message ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid message ID", "INVALID_MESSAGE_ID")
                );
            }

            ContactMessage message = repository.findById(id).orElse(null);
            if (message == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Contact message not found", "MESSAGE_NOT_FOUND")
                );
            }

            ContactMessageDTO dto = convertToDTO(message);

            Specification<ContactMessage> navSpec = buildSpec(
                filter != null ? filter : new ContactMessageFilter());
            String navSortBy = validateSortField(sortBy) != null
                ? validateSortField(sortBy) : "createdAt";
            Map<String, Object> nav = recordNavigation.navigate(
                ContactMessage.class, navSpec, navSortBy, "asc".equalsIgnoreCase(sortDirection), id);
            Long navNext = (Long) nav.get("nextRawId");
            Long navPrevious = (Long) nav.get("previousRawId");

            Long nextId = repository.findNextId(id).orElse(null);
            Long previousId = repository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = repository.findFirstId().orElse(null);
            if (previousId == null) previousId = repository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("message", dto);
            response.put("nextId", navNext != null ? idObfuscator.encodeId(navNext) : null);
            response.put("previousId", navPrevious != null ? idObfuscator.encodeId(navPrevious) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok().body(ApiResponse.success(200, "Contact message retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching contact message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch contact message", "MESSAGE_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> listMessages(
        ContactMessageFilter filter,
        Boolean includeStats,
        Integer pageNumber, Integer pageSize, String sortBy, String sortDirection
    ) {
        log.info("Listing contact messages with filters");
        try {
            pageNumber = (pageNumber != null) ? pageNumber : 0;
            // clamp: an unbounded size is a way to ask for the whole table by accident
            pageSize = (pageSize != null && pageSize > 0) ? Math.min(pageSize, 100) : 20;
            sortDirection = (sortDirection != null && !sortDirection.isEmpty()) ? sortDirection : "desc";

            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(validatedSortBy).descending()
                : Sort.by(validatedSortBy).ascending();
            Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

            Specification<ContactMessage> spec = buildSpec(
                filter != null ? filter : new ContactMessageFilter());

            Page<ContactMessage> page = repository.findAll(spec, pageable);

            List<ContactMessageListItemDTO> dtos = page.getContent().stream()
                .map(this::convertToListItemDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("messages", dtos);
            response.put("currentPage", page.getNumber());
            response.put("totalItems", page.getTotalElements());
            response.put("totalPages", page.getTotalPages());
            response.put("pageSize", page.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection);
            /*
             * Counters for the WHOLE filtered set, from the same specification as
             * the rows, so a card and the table under it cannot disagree.
             */
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok().body(ApiResponse.success(200, "Contact messages retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing contact messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list contact messages", "MESSAGES_LIST_FAILED")
            );
        }
    }

    /** ONE specification, shared by the rows, the counters and the record walk. */
    private Specification<ContactMessage> buildSpec(ContactMessageFilter filter) {
        Specification<ContactMessage> spec = Specification.<ContactMessage>unrestricted()
            .and(ContactMessageSpecification.byStatuses(filter.allStatuses()));

        if (filter.getEmail() != null && !filter.getEmail().isEmpty()) {
            spec = spec.and(ContactMessageSpecification.byEmail(filter.getEmail()));
        }
        if (filter.getSubject() != null && !filter.getSubject().isEmpty()) {
            spec = spec.and(ContactMessageSpecification.bySubject(filter.getSubject()));
        }
        if (filter.getCreatedAfter() != null) {
            spec = spec.and(ContactMessageSpecification.createdAfter(filter.getCreatedAfter()));
        }
        if (filter.getCreatedBefore() != null) {
            spec = spec.and(ContactMessageSpecification.createdBefore(filter.getCreatedBefore()));
        }
        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            spec = spec.and(ContactMessageSpecification.searchKeyword(filter.getKeyword()));
        }
        if (filter.getCustomerId() != null && !filter.getCustomerId().isBlank()) {
            try {
                spec = spec.and(ContactMessageSpecification.byCustomerId(
                    idObfuscator.decodeId(filter.getCustomerId())));
            } catch (Exception e) {
                log.warn("Unreadable customer id on the message filter");
            }
        }

        // the work queues, OR'd
        Specification<ContactMessage> queue = null;
        if (filter.wants("unread")) queue = or(queue, ContactMessageSpecification.unread());
        if (filter.wants("unanswered")) queue = or(queue, ContactMessageSpecification.unanswered());
        if (filter.wants("stale")) queue = or(queue, ContactMessageSpecification.staleFor(2));
        if (filter.wants("known")) queue = or(queue, ContactMessageSpecification.fromKnownCustomer(true));
        if (queue != null) spec = spec.and(queue);

        return spec;
    }

    private Specification<ContactMessage> or(
            Specification<ContactMessage> spec, Specification<ContactMessage> extra) {
        return spec == null ? extra : spec.or(extra);
    }

    /**
     * The cards that head the list, every one of them reachable as a filter.
     *
     * All about what needs answering, because that is what a message list is for.
     */
    private Map<String, Object> buildStats(Specification<ContactMessage> spec) {
        return listStats.of(ContactMessage.class, spec)
            .total()
            .breakdown("byStatus", ContactMessageStatus.values(), ContactMessageSpecification::byStatus)
            .count("unread", ContactMessageSpecification.unread())
            .count("unanswered", ContactMessageSpecification.unanswered())
            .count("stale", ContactMessageSpecification.staleFor(2))
            .count("known", ContactMessageSpecification.fromKnownCustomer(true))
            .recency(ContactMessageSpecification::createdAfter)
            .build();
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    public ContactMessageDTO convertToDTO(ContactMessage message) {
        ContactMessageDTO dto = ContactMessageDTO.builder()
            .id(idObfuscator.encodeId(message.getId()))
            .code(message.getCode())
            .name(message.getName())
            .email(message.getEmail())
            .phone(message.getPhone())
            .subject(message.getSubject())
            .message(message.getMessage())
            .status(message.getStatus())
            .statusDisplayName(message.getStatus() != null ? message.getStatus().getDisplayName() : null)
            .source(message.getSource())
            .preferredLocale(message.getPreferredLocale())
            .adminNotes(message.getAdminNotes())
            .createdAt(message.getCreatedAt())
            .updatedAt(message.getUpdatedAt())
            .respondedAt(message.getRespondedAt())
            .build();

        if (message.getCustomer() != null) {
            dto.setCustomerId(idObfuscator.encodeId(message.getCustomer().getId()));
            dto.setCustomerName(message.getCustomer().getFullName());
        }

        return dto;
    }

    private ContactMessageListItemDTO convertToListItemDTO(ContactMessage message) {
        return ContactMessageListItemDTO.builder()
            .id(idObfuscator.encodeId(message.getId()))
            .code(message.getCode())
            .name(message.getName())
            .email(message.getEmail())
            .subject(message.getSubject())
            .status(message.getStatus())
            .statusDisplayName(message.getStatus() != null ? message.getStatus().getDisplayName() : null)
            .createdAt(message.getCreatedAt())
            .build();
    }
}
