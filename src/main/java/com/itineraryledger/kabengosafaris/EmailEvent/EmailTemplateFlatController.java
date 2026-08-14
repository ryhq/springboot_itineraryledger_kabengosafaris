package com.itineraryledger.kabengosafaris.EmailEvent;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.CreateEmailTemplateDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.UpdateEmailTemplateDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateCreateService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateDeleteService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateListService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateTestService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateUpdateService;
import com.itineraryledger.kabengosafaris.EmailEvent.Specifications.EmailTemplateFilter;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.BulkFlags;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Templates as a resource of their own, at /api/email-templates.
 *
 * The nested surface under an event stays exactly as it was — nothing that calls it needs
 * changing — but a template is a thing somebody maintains, reviews and searches across
 * events, and a resource you can only reach by first knowing its parent cannot have a
 * list page, a record page or a filter of its own.
 *
 * Every write here resolves the template's event and hands off to the same nested service,
 * so there is one implementation of each rule rather than two that drift. In particular
 * the delete guards — a system default can never be removed, and neither can the template
 * an event is currently sending — are enforced in one place and inherited here.
 */
@RestController
@RequestMapping("/api/email-templates")
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateFlatController {

    private final EmailTemplateListService listService;
    private final EmailTemplateCreateService createService;
    private final EmailTemplateUpdateService updateService;
    private final EmailTemplateDeleteService deleteService;
    private final EmailTemplateTestService testService;
    private final EmailTemplateRepository templateRepository;
    private final BulkFlags bulkFlags;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> list(
        @ModelAttribute EmailTemplateFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return listService.list(filter, includeStats, page, size, sortBy, sortDirection);
    }

    /** The record, WITH its body, plus where it sits in the caller's filtered set. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> getOne(
        @PathVariable String id,
        @ModelAttribute EmailTemplateFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return listService.getOne(id, filter, sortBy, sortDirection);
    }

    /**
     * Creating a template needs to know which email it is for, so `eventId` is required in
     * the body — there is no such thing as a template belonging to nothing.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_EMAIL_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> create(@RequestBody CreateTemplateRequest request) {
        if (request == null || request.getEventId() == null || request.getEventId().isBlank()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Which email is this template for? eventId is required",
                    "EVENT_ID_REQUIRED"));
        }
        CreateEmailTemplateDTO dto = new CreateEmailTemplateDTO();
        dto.setName(request.getName());
        dto.setDescription(request.getDescription());
        dto.setContent(request.getContent());
        dto.setIsDefault(request.getIsDefault());
        dto.setEnabled(request.getEnabled());
        return createService.createTemplate(request.getEventId(), dto);
    }

    /**
     * Enabling or disabling a selection in one request.
     *
     * Only `enabled` is offered. Which template an email sends is a choice between siblings
     * — turning several on at once cannot express it, and a bulk control that silently
     * picked a winner would be worse than not having one.
     */
    @org.springframework.web.bind.annotation.PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_TEMPLATE')")
    public ResponseEntity<?> bulkUpdate(@RequestBody BulkFlags.Request request) {
        return bulkFlags.apply("template", templateRepository, request, template -> {
            if (request.getIsActive() != null) template.setEnabled(request.getIsActive());
        });
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> update(
        @PathVariable String id,
        @RequestBody UpdateEmailTemplateDTO body
    ) {
        String eventId = listService.eventIdOf(id);
        if (eventId == null) return notFound();
        return updateService.updateTemplate(eventId, id, body);
    }

    /** Puts a template back to the version that shipped. */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> restore(@PathVariable String id) {
        String eventId = listService.eventIdOf(id);
        if (eventId == null) return notFound();
        return updateService.restoreSystemDefaultTemplate(eventId, id);
    }

    /** Sends this template to the caller's own address, and nowhere else. */
    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> test(@PathVariable String id, Authentication authentication) {
        String eventId = listService.eventIdOf(id);
        if (eventId == null) return notFound();
        return testService.sendTestEmail(eventId, id, authentication);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_EMAIL_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable String id) {
        String eventId = listService.eventIdOf(id);
        if (eventId == null) return notFound();
        return deleteService.deleteTemplates(eventId, List.of(id));
    }

    /**
     * Bare array body, as everywhere else in this API.
     *
     * Every id must belong to the same email: the delete service is scoped to one event,
     * and quietly deleting only the ones that happened to match would be worse than saying
     * so. In practice a selection comes from one event's tab or from a list filtered to it.
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_EMAIL_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> deleteBatch(@RequestBody List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No ids supplied", "NO_IDS"));
        }

        String eventId = listService.eventIdOf(ids.get(0));
        if (eventId == null) return notFound();

        for (String id : ids) {
            String other = listService.eventIdOf(id);
            if (other == null || !other.equals(eventId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Those templates belong to different emails. Delete them one email at a time.",
                        "MIXED_EVENTS"));
            }
        }
        return deleteService.deleteTemplates(eventId, ids);
    }

    private ResponseEntity<ApiResponse<?>> notFound() {
        return ResponseEntity.status(404).body(
            ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND"));
    }

    /** CreateEmailTemplateDTO plus the event it belongs to. */
    @lombok.Data
    public static class CreateTemplateRequest {
        private String eventId;
        private String name;
        private String description;
        private String content;
        private Boolean isDefault;
        private Boolean enabled;
    }
}
