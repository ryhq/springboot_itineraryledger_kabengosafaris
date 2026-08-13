package com.itineraryledger.kabengosafaris.EmailEvent.Services;

import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.EmailEventDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailEventRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailTemplateRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailEvent;
import com.itineraryledger.kabengosafaris.EmailEvent.Specifications.EmailEventFilter;
import com.itineraryledger.kabengosafaris.EmailEvent.Specifications.EmailEventSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for retrieving email events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailEventGetService {

    private final EmailEventRepository emailEventRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final IdObfuscator idObfuscator;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "enabled", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * The list: the rows, the counters and the sort, in one response.
     *
     * It used to return every event as a bare array with no total, so the table under it
     * reported "1–10 of 0" while showing rows. Now on the house contract like everything
     * else, with the two counters that matter — an event switched off and an event with
     * nothing to send are both silent, and neither is visible from the row itself.
     */
    public ResponseEntity<ApiResponse<?>> getAllEmailEvents(
        EmailEventFilter filter,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        try {
            EmailEventFilter resolved = filter != null ? filter : new EmailEventFilter();

            String validatedSortBy = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
            Pageable pageable = PageRequest.of(
                page == null || page < 0 ? 0 : page, pageSize, Sort.by(direction, validatedSortBy));

            Specification<EmailEvent> spec = buildSpec(resolved);
            Page<EmailEvent> found = emailEventRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("emailEvents", found.getContent().stream().map(this::convertToDTO).toList());
            response.put("currentPage", found.getNumber());
            response.put("totalItems", found.getTotalElements());
            response.put("totalPages", found.getTotalPages());
            response.put("pageSize", found.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", direction.name().toLowerCase());
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok(
                ApiResponse.success(200, "Email events retrieved successfully", response)
            );
        } catch (Exception e) {
            log.error("Error listing email events", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to list email events", "EMAIL_EVENTS_LIST_FAILED"));
        }
    }

    /** ONE specification, shared by the rows, the counters and the record walk. */
    private Specification<EmailEvent> buildSpec(EmailEventFilter filter) {
        Specification<EmailEvent> spec = Specification.<EmailEvent>unrestricted()
            .and(EmailEventSpecification.searchKeyword(filter.effectiveKeyword()))
            .and(EmailEventSpecification.nameLike(filter.getName()));

        // contradictory pairs cancel to no constraint, as everywhere else
        boolean wantsOn = filter.hasStatus("enabled");
        boolean wantsOff = filter.hasStatus("disabled");
        if (wantsOn != wantsOff) {
            spec = spec.and(EmailEventSpecification.isEnabled(wantsOn));
        } else if (filter.getEnabled() != null) {
            spec = spec.and(EmailEventSpecification.isEnabled(filter.getEnabled()));
        }

        Specification<EmailEvent> quality = null;
        if (filter.wants("noTemplates")) quality = EmailEventSpecification.hasNoTemplates();
        if (filter.wants("nothingToSend")) {
            Specification<EmailEvent> extra = EmailEventSpecification.hasNoEnabledTemplate();
            quality = quality == null ? extra : quality.or(extra);
        }
        if (filter.wants("noSystemDefault")) {
            Specification<EmailEvent> extra = EmailEventSpecification.hasNoSystemDefault();
            quality = quality == null ? extra : quality.or(extra);
        }
        if (quality != null) spec = spec.and(quality);

        return spec;
    }

    /**
     * The cards that head the list.
     *
     * "Nothing to send" is the one worth having: an event that is switched on, looks
     * healthy in every column, and has no enabled template behind it sends nothing at all.
     */
    private Map<String, Object> buildStats(Specification<EmailEvent> spec) {
        return listStats.of(EmailEvent.class, spec)
            .total()
            .count("enabled", EmailEventSpecification.isEnabled(true))
            .complement("disabled", "enabled")
            .count("nothingToSend", EmailEventSpecification.hasNoEnabledTemplate())
            .count("noTemplates", EmailEventSpecification.hasNoTemplates())
            .count("noSystemDefault", EmailEventSpecification.hasNoSystemDefault())
            .build();
    }

    /**
     * Get email event by ID
     */
    public ResponseEntity<ApiResponse<?>> getEmailEventById(
        String eventIdObfuscated,
        EmailEventFilter filter,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long eventId = idObfuscator.decodeId(eventIdObfuscated);

            EmailEvent event = emailEventRepository.findById(eventId)
                .orElse(null);

            if (event == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Email event not found", "EMAIL_EVENT_NOT_FOUND")
                );
            }

            EmailEventDTO eventDTO = convertToDTO(event);

            /*
             * Walk the SAME set the list was showing, and say where in it we are. Paging by
             * raw id meant the arrows traversed a different list from the one on screen.
             */
            Specification<EmailEvent> navSpec = buildSpec(filter != null ? filter : new EmailEventFilter());
            String navSortBy = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Map<String, Object> nav = recordNavigation.navigate(
                EmailEvent.class, navSpec, navSortBy, !"desc".equalsIgnoreCase(sortDirection), eventId);

            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("emailEvent", eventDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(
                ApiResponse.success(200, "Email event retrieved successfully", response)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid event ID format", "INVALID_EVENT_ID")
            );
        } catch (Exception e) {
            log.error("Error retrieving email event", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to retrieve email event", "EMAIL_EVENT_RETRIEVAL_FAILED")
            );
        }
    }

    /**
     * Convert EmailEvent entity to DTO
     */
    private EmailEventDTO convertToDTO(EmailEvent event) {
        Long templateCount = emailTemplateRepository.countByEmailEventId(event.getId());
        boolean hasSystemDefault = emailTemplateRepository.hasSystemDefaultTemplate(event.getId());

        return EmailEventDTO.builder()
            .id(idObfuscator.encodeId(event.getId()))
            .name(event.getName())
            .description(event.getDescription())
            .enabled(event.getEnabled())
            .variablesJson(event.getVariablesJson())
            .templateCount(templateCount)
            .hasSystemDefaultTemplate(hasSystemDefault)
            .createdAt(event.getCreatedAt())
            .updatedAt(event.getUpdatedAt())
            .build();
    }
}
