package com.itineraryledger.kabengosafaris.EmailEvent.Services;

import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.CreateEmailTemplateDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.EmailTemplateDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailEventRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailTemplateRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailEvent;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailTemplate;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for creating email templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateCreateService {

    private final EmailEventRepository emailEventRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailTemplateService emailTemplateService;
    private final IdObfuscator idObfuscator;

    /**
     * Create a new email template for an event
     *
     * @param eventIdObfuscated The obfuscated email event ID
     * @param createDTO The DTO with template details
     * @return ResponseEntity with ApiResponse containing created template
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> createTemplate(String eventIdObfuscated, @Valid CreateEmailTemplateDTO createDTO) {
        log.info("Creating template for email event: {}", eventIdObfuscated);

        try {
            // Decode email event ID
            Long eventId = idObfuscator.decodeId(eventIdObfuscated);

            // Verify email event exists
            EmailEvent emailEvent = emailEventRepository.findById(eventId).orElse(null);
            if (emailEvent == null) {
                log.warn("Email event not found: {}", eventId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email event not found", "EMAIL_EVENT_NOT_FOUND")
                );
            }

            // Check if template name already exists for this event
            if (emailTemplateRepository.existsByEmailEventIdAndName(eventId, createDTO.getName())) {
                log.warn("Template already exists for event: {} with name: {}", eventId, createDTO.getName());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Template with this name already exists for this event", "TEMPLATE_ALREADY_EXISTS")
                );
            }

            // Generate filename
            String fileName = emailTemplateService.generateFileName(emailEvent.getName(), createDTO.getName());

            // Save template file to disk
            boolean fileSaved = emailTemplateService.saveTemplateFile(createDTO.getContent(), fileName);
            if (!fileSaved) {
                log.error("Failed to save template file: {}", fileName);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to save template file", "TEMPLATE_FILE_SAVE_FAILED")
                );
            }

            // Get file size
            long fileSize = createDTO.getContent().getBytes().length;

            // Set default values for optional fields
            boolean isDefault = Boolean.TRUE.equals(createDTO.getIsDefault());
            boolean enabled = createDTO.getEnabled() == null || Boolean.TRUE.equals(createDTO.getEnabled());

            // Create EmailTemplate entity
            EmailTemplate template = EmailTemplate.builder()
                .emailEvent(emailEvent)
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .fileName(fileName)
                .isDefault(isDefault)
                .isSystemDefault(false) // User-created templates are never system default
                .enabled(enabled)
                .fileSize(fileSize)
                .build();

            // If marking as default, clear other defaults for this event
            if (isDefault) {
                clearOtherDefaults(eventId);
            }

            // Save template to database
            EmailTemplate savedTemplate = emailTemplateRepository.save(template);

            log.info("Template created successfully: {}", savedTemplate.getId());

            // Convert to DTO
            EmailTemplateDTO templateDTO = convertToDTO(savedTemplate);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Template created successfully", templateDTO)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid event ID format", "INVALID_EVENT_ID")
            );
        } catch (Exception e) {
            log.error("Error creating template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create template", "TEMPLATE_CREATE_FAILED")
            );
        }
    }

    /**
     * Create system default template for an email event
     * Called during system initialization
     *
     * @param emailEvent The email event
     * @return true if successful, false otherwise
     */
    @Transactional
    public boolean createSystemDefaultTemplate(EmailEvent emailEvent) {
        try {
            // Check if system default already exists
            if (emailTemplateRepository.hasSystemDefaultTemplate(emailEvent.getId())) {
                log.info("System default template already exists for event: {}", emailEvent.getName());
                return true;
            }

            // Load default content from resources
            String defaultContent = emailTemplateService.loadSystemDefaultTemplate(emailEvent.getName());

            // Generate filename
            String fileName = emailTemplateService.generateFileName(emailEvent.getName(), "System_Default");

            // Save template file to disk
            boolean fileSaved = emailTemplateService.saveTemplateFile(defaultContent, fileName);
            if (!fileSaved) {
                log.error("Failed to save system default template file for event: {}", emailEvent.getName());
                return false;
            }

            // Get file size
            long fileSize = defaultContent.getBytes().length;

            // Create system default template
            EmailTemplate template = EmailTemplate.builder()
                .emailEvent(emailEvent)
                .name("System_Default")
                .description("System default template - created automatically")
                .fileName(fileName)
                .isDefault(true)
                .isSystemDefault(true)
                .enabled(true)
                .fileSize(fileSize)
                .build();

            // Clear other defaults (should be none on first creation)
            clearOtherDefaults(emailEvent.getId());

            // Save to database
            emailTemplateRepository.save(template);

            log.info("System default template created successfully for event: {}", emailEvent.getName());
            return true;

        } catch (Exception e) {
            log.error("Failed to create system default template for event: {}", emailEvent.getName(), e);
            return false;
        }
    }

    /**
     * Clear all default flags for other templates in the same event
     */
    private void clearOtherDefaults(Long eventId) {
        emailTemplateRepository.findByEmailEventId(eventId).forEach(template -> {
            if (Boolean.TRUE.equals(template.getIsDefault())) {
                template.setIsDefault(false);
                emailTemplateRepository.save(template);
            }
        });
    }

    /**
     * Convert EmailTemplate entity to DTO
     */
    private EmailTemplateDTO convertToDTO(EmailTemplate template) {
        return EmailTemplateDTO.builder()
            .id(idObfuscator.encodeId(template.getId()))
            .emailEventId(idObfuscator.encodeId(template.getEmailEvent().getId()))
            .emailEventName(template.getEmailEvent().getName())
            .name(template.getName())
            .description(template.getDescription())
            .fileName(template.getFileName())
            .isDefault(template.getIsDefault())
            .isSystemDefault(template.getIsSystemDefault())
            .enabled(template.getEnabled())
            .fileSize(template.getFileSize())
            .fileSizeFormatted(emailTemplateService.formatFileSize(template.getFileSize()))
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();
    }
}
