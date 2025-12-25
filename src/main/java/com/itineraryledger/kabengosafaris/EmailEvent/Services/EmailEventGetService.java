package com.itineraryledger.kabengosafaris.EmailEvent.Services;

import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.EmailEventDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailEventRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailTemplateRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailEvent;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
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

    /**
     * Get all email events
     */
    public ResponseEntity<ApiResponse<?>> getAllEmailEvents() {
        try {
            List<EmailEvent> events = emailEventRepository.findAll();

            List<EmailEventDTO> eventDTOs = events.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(
                ApiResponse.success(200, "Email events retrieved successfully", eventDTOs)
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

            return ResponseEntity.ok(
                ApiResponse.success(200, "Email event retrieved successfully", eventDTO)
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
