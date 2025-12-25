package com.itineraryledger.kabengosafaris.EmailEvent.Services;

import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.EmailEventDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.UpdateEmailEventDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailEventRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailTemplateRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailEvent;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for updating email events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailEventUpdateService {

    private final EmailEventRepository emailEventRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Update email event
     * Only description and enabled status can be updated
     * The name cannot be changed as it's used as a unique identifier
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> updateEmailEvent(String eventIdObfuscated, UpdateEmailEventDTO updateDTO) {
        try {
            Long eventId = idObfuscator.decodeId(eventIdObfuscated);

            EmailEvent event = emailEventRepository.findById(eventId)
                .orElse(null);

            if (event == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Email event not found", "EMAIL_EVENT_NOT_FOUND")
                );
            }

            // Update description if provided
            if (updateDTO.getDescription() != null) {
                event.setDescription(updateDTO.getDescription());
            }

            // Update enabled status if provided
            if (updateDTO.getEnabled() != null) {
                event.setEnabled(updateDTO.getEnabled());
            }

            // Save updated event
            EmailEvent updatedEvent = emailEventRepository.save(event);

            // Convert to DTO
            EmailEventDTO eventDTO = convertToDTO(updatedEvent);

            return ResponseEntity.ok(
                ApiResponse.success(200, "Email event updated successfully", eventDTO)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid event ID format", "INVALID_EVENT_ID")
            );
        } catch (Exception e) {
            log.error("Error updating email event", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to update email event", "EMAIL_EVENT_UPDATE_FAILED")
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
            .templateCount(templateCount)
            .hasSystemDefaultTemplate(hasSystemDefault)
            .createdAt(event.getCreatedAt())
            .updatedAt(event.getUpdatedAt())
            .build();
    }
}
