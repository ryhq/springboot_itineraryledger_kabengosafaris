package com.itineraryledger.kabengosafaris.EmailEvent.Services;

import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.EmailTemplateDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.UpdateEmailTemplateDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailTemplateRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailTemplate;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for updating email templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateUpdateService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailTemplateService emailTemplateService;
    private final EmailTemplateGetService emailTemplateGetService;
    private final IdObfuscator idObfuscator;

    /**
     * Update a template
     *
     * @param eventIdObfuscated Obfuscated email event ID
     * @param templateIdObfuscated Obfuscated template ID
     * @param updateDTO The DTO with fields to update (only provided fields will be updated)
     * @return ResponseEntity with updated template or error
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> updateTemplate(String eventIdObfuscated, String templateIdObfuscated, UpdateEmailTemplateDTO updateDTO) {
        log.info("Updating template: {} for event: {}", templateIdObfuscated, eventIdObfuscated);

        try {
            Long eventId = idObfuscator.decodeId(eventIdObfuscated);
            Long templateId = idObfuscator.decodeId(templateIdObfuscated);

            // Find template
            EmailTemplate template = emailTemplateRepository.findById(templateId).orElse(null);
            if (template == null || !template.getEmailEvent().getId().equals(eventId)) {
                log.warn("Template not found or does not belong to event: {}", eventId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND")
                );
            }

            String oldFileName = template.getFileName();
            String currentName = template.getName();
            String newFileName = oldFileName;

            // Update name if provided and validate uniqueness
            if (updateDTO.getName() != null && !updateDTO.getName().isBlank()) {
                // Check if name is different from current name
                if (!updateDTO.getName().equals(currentName)) {
                    // Validate that new name is unique for this event
                    if (emailTemplateRepository.existsByEmailEventIdAndName(eventId, updateDTO.getName())) {
                        log.warn("Template name already exists for event: {} with name: {}", eventId, updateDTO.getName());
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Template with this name already exists for this event", "TEMPLATE_ALREADY_EXISTS")
                        );
                    }

                    // Generate new filename with updated name
                    newFileName = emailTemplateService.generateFileName(template.getEmailEvent().getName(), updateDTO.getName());
                    log.debug("Name updated from {} to {}, filename will change from {} to {}",
                        currentName, updateDTO.getName(), oldFileName, newFileName);

                    // Read current content from old file
                    String currentContent = emailTemplateService.readTemplateFile(oldFileName);
                    if (currentContent == null) {
                        log.error("Failed to read existing template file: {}", oldFileName);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.error(500, "Failed to read existing template file", "TEMPLATE_FILE_READ_FAILED")
                        );
                    }

                    // Save content to new filename
                    boolean savedNew = emailTemplateService.saveTemplateFile(currentContent, newFileName);
                    if (!savedNew) {
                        log.error("Failed to save template file with new name: {}", newFileName);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.error(500, "Failed to save template file with new name", "TEMPLATE_FILE_SAVE_FAILED")
                        );
                    }

                    // Delete old file
                    boolean deletedOld = emailTemplateService.deleteTemplateFile(oldFileName);
                    if (!deletedOld) {
                        log.warn("Failed to delete old template file: {}", oldFileName);
                    }

                    // Update template entity with new name and filename
                    template.setName(updateDTO.getName());
                    template.setFileName(newFileName);

                    // Update file size
                    long fileSize = currentContent.getBytes().length;
                    template.setFileSize(fileSize);
                }
            }

            // Update content if provided
            if (updateDTO.getContent() != null && !updateDTO.getContent().isBlank()) {
                boolean updated = emailTemplateService.updateTemplateFile(template.getFileName(), updateDTO.getContent());
                if (!updated) {
                    log.error("Failed to update template file: {}", template.getFileName());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResponse.error(500, "Failed to update template file", "TEMPLATE_FILE_UPDATE_FAILED")
                    );
                }
                // Update file size
                long fileSize = updateDTO.getContent().getBytes().length;
                template.setFileSize(fileSize);
            }

            // Update description if provided
            if (updateDTO.getDescription() != null) {
                template.setDescription(updateDTO.getDescription());
            }

            // Update isDefault if provided
            if (updateDTO.getIsDefault() != null) {
                boolean newDefault = updateDTO.getIsDefault();

                // If setting as default, clear other defaults for this event
                if (newDefault && !Boolean.TRUE.equals(template.getIsDefault())) {
                    clearOtherDefaults(eventId, templateId);
                }

                template.setIsDefault(newDefault);
            }

            // Update enabled if provided
            if (updateDTO.getEnabled() != null) {
                template.setEnabled(updateDTO.getEnabled());
            }

            // Save template
            EmailTemplate updatedTemplate = emailTemplateRepository.save(template);

            String content = emailTemplateService.readTemplateFile(template.getFileName());
            if (content == null) {
                return ResponseEntity.internalServerError().body(
                    ApiResponse.error(500, "Failed to read template content", "TEMPLATE_CONTENT_READ_FAILED")
                );
            }

            EmailTemplateDTO templateDTO = emailTemplateGetService.convertToDTOWithContent(updatedTemplate, content);

            log.info("Template updated successfully: {}", templateId);
            
            return ResponseEntity.ok(ApiResponse.success(200, "Template updated successfully", templateDTO));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
            );
        } catch (Exception e) {
            log.error("Error updating template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update template", "TEMPLATE_UPDATE_FAILED")
            );
        }
    }

    /**
     * Restore system default template to its original template
     * This endpoint allows users to reset a modified system default template back to the original default template
     *
     * @param eventIdObfuscated Obfuscated email event ID
     * @param templateIdObfuscated Obfuscated template ID
     * @return ResponseEntity with updated template or error
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> restoreSystemDefaultTemplate(String eventIdObfuscated, String templateIdObfuscated) {
        log.info("Restoring system default template: {} for event: {}", templateIdObfuscated, eventIdObfuscated);

        try {
            Long eventId = idObfuscator.decodeId(eventIdObfuscated);
            Long templateId = idObfuscator.decodeId(templateIdObfuscated);

            // Find template
            EmailTemplate template = emailTemplateRepository.findById(templateId).orElse(null);
            if (template == null || !template.getEmailEvent().getId().equals(eventId)) {
                log.warn("Template not found or does not belong to event: {}", eventId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND")
                );
            }

            // Verify that this is a system default template
            if (!Boolean.TRUE.equals(template.getIsSystemDefault())) {
                log.warn("Template is not a system default template: {}", templateId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Template is not a system default template", "NOT_SYSTEM_DEFAULT_TEMPLATE")
                );
            }

            // Load system default template from resources
            String defaultContent = emailTemplateService.loadSystemDefaultTemplate(template.getEmailEvent().getName());

            // Update template file on disk
            boolean updated = emailTemplateService.updateTemplateFile(template.getFileName(), defaultContent);
            if (!updated) {
                log.error("Failed to update template file: {}", template.getFileName());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to update template file", "TEMPLATE_FILE_UPDATE_FAILED")
                );
            }

            // Update file size
            long fileSize = defaultContent.getBytes().length;
            template.setFileSize(fileSize);

            // Reset description to default
            template.setDescription("System default template - created automatically");

            // Save updated template
            EmailTemplate restoredTemplate = emailTemplateRepository.save(template);

            log.info("System default template restored successfully: {}", templateId);

            // Read content from file
            String content = emailTemplateService.readTemplateFile(template.getFileName());
            if (content == null) {
                return ResponseEntity.internalServerError().body(
                    ApiResponse.error(500, "Failed to read template content", "TEMPLATE_CONTENT_READ_FAILED")
                );
            }

            EmailTemplateDTO templateDTO = emailTemplateGetService.convertToDTOWithContent(restoredTemplate, content);
            return ResponseEntity.ok(ApiResponse.success(200, "System default template restored successfully", templateDTO));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
            );
        } catch (Exception e) {
            log.error("Error restoring system default template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to restore system default template", "TEMPLATE_RESTORE_FAILED")
            );
        }
    }

    /**
     * Clear all default flags for other templates in the same event, except the specified template
     */
    private void clearOtherDefaults(Long eventId, Long excludeTemplateId) {
        emailTemplateRepository.findByEmailEventId(eventId).forEach(template -> {
            if (!template.getId().equals(excludeTemplateId) && Boolean.TRUE.equals(template.getIsDefault())) {
                template.setIsDefault(false);
                emailTemplateRepository.save(template);
            }
        });
    }
}
