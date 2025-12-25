package com.itineraryledger.kabengosafaris.EmailEvent.Services;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for deleting email templates
 *
 * Responsibilities:
 * - Delete templates by list of IDs (batch delete)
 * - Prevent deletion of system default templates (can only be modified or restored)
 * - Prevent deletion of default enabled templates
 * - Delete template files from disk
 * - Atomic operation: if any template is system default or default enabled, no templates are deleted
 * - Handle cascade deletion of database records
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmailTemplateDeleteService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailTemplateService emailTemplateService;
    private final IdObfuscator idObfuscator;

    /**
     * Delete templates by list of obfuscated IDs
     *
     * Validation: If any template in the list is system default or default enabled, no templates will be deleted
     * Returns list of successfully deleted IDs
     *
     * @param eventIdObfuscated Obfuscated email event ID
     * @param templateIdObfuscatedList List of obfuscated template IDs
     * @return ResponseEntity with ApiResponse containing list of deleted IDs
     */
    public ResponseEntity<ApiResponse<?>> deleteTemplates(String eventIdObfuscated, List<String> templateIdObfuscatedList) {
        log.info("Deleting {} templates for event: {}", templateIdObfuscatedList.size(), eventIdObfuscated);

        try {
            Long eventId = idObfuscator.decodeId(eventIdObfuscated);

            // Decode all obfuscated template IDs
            List<Long> templateIds = new ArrayList<>();
            for (String idObfuscated : templateIdObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    templateIds.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode template ID: {}", idObfuscated, e);
                }
            }

            return deleteTemplatesInternal(eventId, templateIds);

        } catch (Exception e) {
            log.error("Error deleting templates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete templates", "TEMPLATES_DELETE_FAILED")
            );
        }
    }

    /**
     * Internal method to delete templates by decoded IDs
     * Validates that no template in the list is system default or default enabled before deletion
     *
     * @param eventId Email event ID
     * @param templateIds List of template IDs to delete
     * @return ResponseEntity with ApiResponse
     */
    private ResponseEntity<ApiResponse<?>> deleteTemplatesInternal(Long eventId, List<Long> templateIds) {
        // First, validate that no template in the list is system default or default enabled
        List<Long> defaultEnabledTemplateIds = new ArrayList<>();
        List<Long> systemDefaultTemplateIds = new ArrayList<>();
        List<EmailTemplate> templatesToDelete = new ArrayList<>();

        for (Long templateId : templateIds) {
            EmailTemplate template = emailTemplateRepository.findById(templateId).orElse(null);

            if (template != null) {
                // Verify template belongs to the email event
                if (!template.getEmailEvent().getId().equals(eventId)) {
                    log.warn("Template {} does not belong to event {}", templateId, eventId);
                    continue;
                }

                // Check if template is system default (cannot be deleted)
                if (Boolean.TRUE.equals(template.getIsSystemDefault())) {
                    systemDefaultTemplateIds.add(templateId);
                }

                // Check if template is default and enabled
                if (Boolean.TRUE.equals(template.getIsDefault()) && Boolean.TRUE.equals(template.getEnabled())) {
                    defaultEnabledTemplateIds.add(templateId);
                }

                templatesToDelete.add(template);
            }
        }

        // If any system default templates found, reject entire operation
        if (!systemDefaultTemplateIds.isEmpty()) {
            log.warn("Cannot delete: {} template(s) in the list are system default", systemDefaultTemplateIds.size());

            Map<String, Object> result = new HashMap<>();
            result.put("deletedCount", 0);
            result.put("message", "Cannot delete any templates: " + systemDefaultTemplateIds.size() + " template(s) in the list are system default templates. System default templates can only be modified or restored, not deleted.");
            result.put("systemDefaultTemplateIds", systemDefaultTemplateIds);

            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    "Cannot delete templates: some are system default",
                    "CANNOT_DELETE_SYSTEM_DEFAULT_TEMPLATES"
                )
            );
        }

        // If any default enabled templates found, reject entire operation
        if (!defaultEnabledTemplateIds.isEmpty()) {
            log.warn("Cannot delete: {} template(s) in the list are default and enabled", defaultEnabledTemplateIds.size());

            Map<String, Object> result = new HashMap<>();
            result.put("deletedCount", 0);
            result.put("message", "Cannot delete any templates: " + defaultEnabledTemplateIds.size() + " template(s) in the list are set as default and enabled. Please change the default template first or disable it.");
            result.put("defaultEnabledTemplateIds", defaultEnabledTemplateIds);

            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    "Cannot delete templates: some are set as default and enabled",
                    "CANNOT_DELETE_DEFAULT_ENABLED_TEMPLATES"
                )
            );
        }

        // Delete templates
        int deletedCount = 0;
        List<String> deletedFileNames = new ArrayList<>();

        for (EmailTemplate template : templatesToDelete) {
            try {
                // Delete template file from disk
                boolean fileDeleted = emailTemplateService.deleteTemplateFile(template.getFileName());
                if (fileDeleted) {
                    log.debug("Template file deleted: {}", template.getFileName());
                    deletedFileNames.add(template.getFileName());
                } else {
                    log.warn("Failed to delete template file: {}", template.getFileName());
                }

                // Delete template from database
                emailTemplateRepository.deleteById(template.getId());
                deletedCount++;
                log.info("Template deleted successfully: {}", template.getId());

            } catch (Exception e) {
                log.error("Error deleting template: {}", template.getId(), e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deletedCount);
        result.put("deletedFileNames", deletedFileNames);

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Templates deleted successfully",
                result
            )
        );
    }
}
