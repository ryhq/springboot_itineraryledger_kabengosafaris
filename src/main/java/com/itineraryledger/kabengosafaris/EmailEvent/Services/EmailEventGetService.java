package com.itineraryledger.kabengosafaris.EmailEvent.Services;

import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.EmailEventDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailEventRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailTemplateRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailEvent;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
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
     * Get all email events
     */
    public ResponseEntity<ApiResponse<?>> getAllEmailEvents(String sortBy, String sortDirection) {
        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort.Direction direction = Sort.Direction.DESC;
            if ("asc".equalsIgnoreCase(sortDirection)) {
                direction = Sort.Direction.ASC;
            }

            List<EmailEvent> events = emailEventRepository.findAll(Sort.by(direction, validatedSortBy));

            List<EmailEventDTO> eventDTOs = events.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("emailEvents", eventDTOs);
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDir", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok(
                ApiResponse.success(200, "Email events retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error retrieving email events", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to retrieve email events", "EMAIL_EVENTS_RETRIEVAL_FAILED")
            );
        }
    }

    /**
     * Get email event by ID
     */
    public ResponseEntity<ApiResponse<?>> getEmailEventById(String eventIdObfuscated) {
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

            // Navigation IDs
            Long nextId = emailEventRepository.findNextId(eventId).orElse(null);
            Long previousId = emailEventRepository.findPreviousId(eventId).orElse(null);
            if (nextId == null) nextId = emailEventRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = emailEventRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("emailEvent", eventDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

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
