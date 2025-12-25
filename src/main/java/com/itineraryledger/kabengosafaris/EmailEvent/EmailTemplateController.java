package com.itineraryledger.kabengosafaris.EmailEvent;

import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.CreateEmailTemplateDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.UpdateEmailTemplateDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.*;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Email Template management endpoints
 *
 * Provides endpoints for:
 * - Creating custom email templates for events
 * - Retrieving templates with pagination, filtering, and sorting
 * - Updating template content and settings
 * - Managing default templates
 * - Restoring system default templates to original state
 * - Deleting templates (with protection for system defaults)
 * - Accessing template content as HTML files
 * - Testing templates by sending test emails to authenticated users
 */
@RestController
@RequestMapping("/api/email-events/{eventId}/templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateCreateService emailTemplateCreateService;
    private final EmailTemplateGetService emailTemplateGetService;
    private final EmailTemplateUpdateService emailTemplateUpdateService;
    private final EmailTemplateDeleteService emailTemplateDeleteService;
    private final EmailTemplateTestService emailTemplateTestService;

    /**
     * Create a new email template for an event
     *
     * @param eventId The obfuscated email event ID
     * @param createDTO The request DTO with template details
     * @return ResponseEntity with ApiResponse containing created template
     *
     * Example request:
     * POST /api/email-events/{eventId}/templates
     * {
     *   "name": "Welcome_Email_Premium",
     *   "description": "Premium user welcome email template",
     *   "content": "<!DOCTYPE html><html>...</html>",
     *   "isDefault": false,
     *   "enabled": true,
     *   "variablesJson": "[{\"name\":\"userName\",\"description\":\"User's full name\",\"example\":\"John Doe\"}]"
     * }
     *
     * Example response:
     * {
     *   "status": 201,
     *   "message": "Template created successfully",
     *   "data": {
     *     "id": "encoded_id",
     *     "emailEventId": "encoded_event_id",
     *     "emailEventName": "USER_REGISTRATION",
     *     "name": "Welcome_Email_Premium",
     *     "fileName": "USER_REGISTRATION_Welcome_Email_Premium_20250115_143000.html",
     *     "isDefault": false,
     *     "isSystemDefault": false,
     *     "enabled": true,
     *     "fileSize": 2048,
     *     "fileSizeFormatted": "2.0 KB"
     *   }
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createTemplate(
            @PathVariable String eventId,
            @Valid @RequestBody CreateEmailTemplateDTO createDTO) {
        return emailTemplateCreateService.createTemplate(eventId, createDTO);
    }

    /**
     * Get all templates for an email event with pagination, filtering, and sorting
     *
     * @param eventId The obfuscated email event ID
     * @param enabled Filter by enabled status (optional)
     * @param isDefault Filter by default status (optional)
     * @param isSystemDefault Filter by system default status (optional)
     * @param name Filter by name (partial match, optional)
     * @param page Page number (0-based), default: 0
     * @param size Page size, default: 10
     * @param sortDir Sort direction: "asc" or "desc", default: "desc"
     * @return ResponseEntity with paginated templates
     *
     * Example request:
     * GET /api/email-events/{eventId}/templates?enabled=true&page=0&size=10&sortDir=desc
     *
     * Example response:
     * {
     *   "status": 200,
     *   "message": "Templates retrieved successfully",
     *   "data": {
     *     "templates": [...],
     *     "currentPage": 0,
     *     "totalItems": 15,
     *     "totalPages": 2
     *   }
     * }
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllTemplates(
            @PathVariable String eventId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean isDefault,
            @RequestParam(required = false) Boolean isSystemDefault,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return emailTemplateGetService.getAllTemplates(eventId, enabled, isDefault, isSystemDefault, name, page, size, sortDir);
    }

    /**
     * Get a single template by ID (without content)
     *
     * @param eventId The obfuscated email event ID
     * @param templateId The obfuscated template ID
     * @return ResponseEntity with template details
     *
     * Example request:
     * GET /api/email-events/{eventId}/templates/{templateId}
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<ApiResponse<?>> getTemplate(
            @PathVariable String eventId,
            @PathVariable String templateId) {
        return emailTemplateGetService.getTemplate(eventId, templateId);
    }

    /**
     * Get template with content included
     *
     * @param eventId The obfuscated email event ID
     * @param templateId The obfuscated template ID
     * @return ResponseEntity with template details including HTML content
     *
     * Example request:
     * GET /api/email-events/{eventId}/templates/{templateId}/content
     *
     * Example response:
     * {
     *   "status": 200,
     *   "message": "Template content retrieved successfully",
     *   "data": {
     *     "id": "encoded_id",
     *     "name": "Welcome_Email_Premium",
     *     "content": "<!DOCTYPE html><html>...</html>",
     *     ...
     *   }
     * }
     */
    @GetMapping("/{templateId}/content")
    public ResponseEntity<ApiResponse<?>> getTemplateContent(
            @PathVariable String eventId,
            @PathVariable String templateId) {
        return emailTemplateGetService.getTemplateContent(eventId, templateId);
    }

    /**
     * Get template content as HTML file (for download or inline display)
     *
     * @param eventId The obfuscated email event ID
     * @param templateId The obfuscated template ID
     * @param download Whether to download (true) or display inline (false), default: false
     * @return ResponseEntity with HTML content and appropriate headers
     *
     * Example requests:
     * GET /api/email-events/{eventId}/templates/{templateId}/file (inline display)
     * GET /api/email-events/{eventId}/templates/{templateId}/file?download=true (download)
     */
    @GetMapping("/{templateId}/file")
    public ResponseEntity<?> getTemplateContentFile(
            @PathVariable String eventId,
            @PathVariable String templateId,
            @RequestParam(defaultValue = "false") boolean download) {
        return emailTemplateGetService.getTemplateContentFile(eventId, templateId, download);
    }

    /**
     * Get template content file by file name
     *
     * @param eventId The obfuscated email event ID
     * @param fileName The template file name
     * @param download Whether to download (true) or display inline (false), default: false
     * @return ResponseEntity with HTML content and appropriate headers
     *
     * Example request:
     * GET /api/email-events/{eventId}/templates/file/USER_REGISTRATION_Welcome_Email_20250115_143000.html
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<?> getTemplateContentFileByName(
            @PathVariable String eventId,
            @PathVariable String fileName,
            @RequestParam(defaultValue = "false") boolean download) {
        return emailTemplateGetService.getTemplateContentFileByName(eventId, fileName, download);
    }

    /**
     * Update template
     *
     * All fields in the DTO are optional - only provided fields will be updated.
     *
     * @param eventId The obfuscated email event ID
     * @param templateId The obfuscated template ID
     * @param updateDTO The update DTO with fields to modify
     * @return ResponseEntity with updated template
     *
     * Example request:
     * PUT /api/email-events/{eventId}/templates/{templateId}
     * {
     *   "name": "Updated_Template_Name",
     *   "description": "Updated description",
     *   "content": "<!DOCTYPE html><html>...</html>",
     *   "isDefault": true,
     *   "enabled": false,
     *   "variablesJson": "[...]"
     * }
     */
    @PutMapping("/{templateId}")
    public ResponseEntity<ApiResponse<?>> updateTemplate(
            @PathVariable String eventId,
            @PathVariable String templateId,
            @RequestBody UpdateEmailTemplateDTO updateDTO) {
        return emailTemplateUpdateService.updateTemplate(eventId, templateId, updateDTO);
    }

    /**
     * Restore system default template to original state
     *
     * This endpoint resets a system default template back to its original template
     * loaded from resources. Only works for templates with isSystemDefault=true.
     *
     * @param eventId The obfuscated email event ID
     * @param templateId The obfuscated template ID
     * @return ResponseEntity with restored template
     *
     * Example request:
     * POST /api/email-events/{eventId}/templates/{templateId}/restore
     *
     * Example response:
     * {
     *   "status": 200,
     *   "message": "System default template restored successfully",
     *   "data": {
     *     "id": "encoded_id",
     *     "name": "System_Default",
     *     "description": "System default template - created automatically",
     *     "isSystemDefault": true,
     *     ...
     *   }
     * }
     */
    @PostMapping("/{templateId}/restore")
    public ResponseEntity<ApiResponse<?>> restoreSystemDefaultTemplate(
            @PathVariable String eventId,
            @PathVariable String templateId) {
        return emailTemplateUpdateService.restoreSystemDefaultTemplate(eventId, templateId);
    }

    /**
     * Delete templates by list of IDs (batch delete)
     *
     * Validation: If any template in the list is system default or default enabled,
     * no templates will be deleted (atomic operation).
     *
     * @param eventId The obfuscated email event ID
     * @param templateIds List of obfuscated template IDs to delete
     * @return ResponseEntity with deletion results
     *
     * Example request:
     * DELETE /api/email-events/{eventId}/templates
     * ["encoded_id_1", "encoded_id_2", "encoded_id_3"]
     *
     * Example response:
     * {
     *   "status": 200,
     *   "message": "Templates deleted successfully",
     *   "data": {
     *     "deletedCount": 3,
     *     "deletedFileNames": ["file1.html", "file2.html", "file3.html"]
     *   }
     * }
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<?>> deleteTemplates(
            @PathVariable String eventId,
            @RequestBody List<String> templateIds) {
        return emailTemplateDeleteService.deleteTemplates(eventId, templateIds);
    }

    /**
     * Send a test email using a specific template to the authenticated user
     *
     * This endpoint is useful for:
     * - Testing specific email templates during development
     * - Previewing how a template will look before setting it as default
     * - Verifying template rendering with real variable data
     * - Testing template changes without affecting production emails
     *
     * The email will be sent to the authenticated user's email address with
     * test/sample data appropriate for the event type. The subject line will
     * be prefixed with [TEST] to distinguish test emails.
     *
     * @param eventId The obfuscated email event ID
     * @param templateId The obfuscated template ID to test
     * @param authentication The authenticated user (automatically injected by Spring Security)
     * @return ResponseEntity with test result
     *
     * Example request:
     * POST /api/email-events/{eventId}/templates/{templateId}/test
     *
     * Example response:
     * {
     *   "status": 200,
     *   "message": "Test email sent successfully to user@example.com",
     *   "data": {
     *     "eventName": "USER_REGISTRATION",
     *     "templateName": "Welcome_Email_Premium",
     *     "recipientEmail": "user@example.com",
     *     "subject": "[TEST] Welcome to Kabengosafaris - Activate Your Account",
     *     "sentAt": "2025-01-15T14:30:00"
     *   }
     * }
     */
    @PostMapping("/{templateId}/test")
    public ResponseEntity<ApiResponse<?>> sendTestEmail(
            @PathVariable String eventId,
            @PathVariable String templateId,
            Authentication authentication) {
        return emailTemplateTestService.sendTestEmail(eventId, templateId, authentication);
    }
}
