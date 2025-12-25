package com.itineraryledger.kabengosafaris.EmailEvent.Services;

import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.EmailTemplateDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailTemplateRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailTemplate;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for retrieving email templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateGetService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailTemplateService emailTemplateService;
    private final IdObfuscator idObfuscator;

    /**
     * Get all templates for an email event with pagination, filtering and sorting
     *
     * @param eventIdObfuscated Obfuscated email event ID
     * @param enabled Filter by enabled status (optional)
     * @param isDefault Filter by default status (optional)
     * @param isSystemDefault Filter by system default status (optional)
     * @param name Filter by name (partial match, optional)
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortDir Sort direction ("asc" or "desc")
     * @return ResponseEntity with paginated templates
     */
    public ResponseEntity<ApiResponse<?>> getAllTemplates(
            String eventIdObfuscated,
            Boolean enabled,
            Boolean isDefault,
            Boolean isSystemDefault,
            String name,
            int page,
            int size,
            String sortDir) {

        log.debug("Fetching templates for event: {} with filters - enabled: {}, isDefault: {}, isSystemDefault: {}, name: {}, page: {}, size: {}, sortDir: {}",
            eventIdObfuscated, enabled, isDefault, isSystemDefault, name, page, size, sortDir);

        try {
            Long eventId = idObfuscator.decodeId(eventIdObfuscated);

            // Validate pagination
            if (page < 0) {
                log.warn("Invalid page number: {}", page);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Page number cannot be negative", "INVALID_PAGE")
                );
            }
            if (size <= 0) {
                log.warn("Invalid page size: {}", size);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Page size must be greater than 0", "INVALID_SIZE")
                );
            }

            // Setup sorting
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

            // Build dynamic specification
            Specification<EmailTemplate> specification = EmailTemplateSpecification.emailEventId(eventId);

            if (enabled != null) {
                specification = specification.and(EmailTemplateSpecification.enabled(enabled));
            }

            if (isDefault != null) {
                specification = specification.and(EmailTemplateSpecification.isDefault(isDefault));
            }

            if (isSystemDefault != null) {
                specification = specification.and(EmailTemplateSpecification.isSystemDefault(isSystemDefault));
            }

            if (name != null && !name.isBlank()) {
                specification = specification.and(EmailTemplateSpecification.nameLike(name));
            }

            // Fetch templates with specification and pagination
            Page<EmailTemplate> templatesPage = emailTemplateRepository.findAll(specification, pageable);

            var dtos = templatesPage.getContent().stream()
                .map(this::convertToDTO)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("templates", dtos);
            response.put("currentPage", templatesPage.getNumber());
            response.put("totalItems", templatesPage.getTotalElements());
            response.put("totalPages", templatesPage.getTotalPages());

            log.info("Successfully fetched {} templates on page {}", dtos.size(), page);
            return ResponseEntity.ok(ApiResponse.success(200, "Templates retrieved successfully", response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid event ID format", "INVALID_EVENT_ID")
            );
        } catch (Exception e) {
            log.error("Error fetching templates", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to fetch templates", "TEMPLATES_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a single template by ID
     *
     * @param eventIdObfuscated Obfuscated email event ID
     * @param templateIdObfuscated Obfuscated template ID
     * @return ResponseEntity with template details
     */
    public ResponseEntity<ApiResponse<?>> getTemplate(String eventIdObfuscated, String templateIdObfuscated) {
        log.debug("Fetching template: {}", templateIdObfuscated);

        try {
            Long eventId = idObfuscator.decodeId(eventIdObfuscated);
            Long templateId = idObfuscator.decodeId(templateIdObfuscated);

            EmailTemplate template = emailTemplateRepository.findById(templateId).orElse(null);
            if (template == null || !template.getEmailEvent().getId().equals(eventId)) {
                log.warn("Template not found or does not belong to event: {}", eventId);
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND")
                );
            }

            log.debug("Template retrieved successfully: {}", templateId);
            return ResponseEntity.ok(ApiResponse.success(200, "Template retrieved successfully", convertToDTO(template)));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
            );
        } catch (Exception e) {
            log.error("Error fetching template", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to fetch template", "TEMPLATE_FETCH_FAILED")
            );
        }
    }

    /**
     * Get template with content included
     *
     * @param eventIdObfuscated Obfuscated email event ID
     * @param templateIdObfuscated Obfuscated template ID
     * @return ResponseEntity with template details including content
     */
    public ResponseEntity<ApiResponse<?>> getTemplateContent(String eventIdObfuscated, String templateIdObfuscated) {
        log.debug("Fetching template content: {}", templateIdObfuscated);

        try {
            Long eventId = idObfuscator.decodeId(eventIdObfuscated);
            Long templateId = idObfuscator.decodeId(templateIdObfuscated);

            EmailTemplate template = emailTemplateRepository.findById(templateId).orElse(null);
            if (template == null || !template.getEmailEvent().getId().equals(eventId)) {
                log.warn("Template not found or does not belong to event: {}", eventId);
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND")
                );
            }

            // Read content from file
            String content = emailTemplateService.readTemplateFile(template.getFileName());
            if (content == null) {
                return ResponseEntity.internalServerError().body(
                    ApiResponse.error(500, "Failed to read template content", "TEMPLATE_CONTENT_READ_FAILED")
                );
            }

            EmailTemplateDTO templateDTO = convertToDTOWithContent(template, content);

            return ResponseEntity.ok(ApiResponse.success(200, "Template content retrieved successfully", templateDTO));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
            );
        } catch (Exception e) {
            log.error("Error fetching template content", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to fetch template content", "TEMPLATE_CONTENT_FETCH_FAILED")
            );
        }
    }

    /**
     * Get template content as HTML file (for download or inline display)
     *
     * @param eventIdObfuscated Obfuscated email event ID
     * @param templateIdObfuscated Obfuscated template ID
     * @param download Whether to download or display inline
     * @return ResponseEntity with HTML content
     */
    public ResponseEntity<?> getTemplateContentFile(String eventIdObfuscated, String templateIdObfuscated, boolean download) {
        log.debug("Fetching template content file: {}, download: {}", templateIdObfuscated, download);

        try {
            Long eventId = idObfuscator.decodeId(eventIdObfuscated);
            Long templateId = idObfuscator.decodeId(templateIdObfuscated);

            EmailTemplate template = emailTemplateRepository.findById(templateId).orElse(null);
            if (template == null || !template.getEmailEvent().getId().equals(eventId)) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND")
                );
            }

            // Read content from file
            String content = emailTemplateService.readTemplateFile(template.getFileName());
            if (content == null) {
                return ResponseEntity.internalServerError().body(
                    ApiResponse.error(500, "Failed to read template content", "TEMPLATE_CONTENT_READ_FAILED")
                );
            }

            // Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);

            if (download) {
                headers.setContentDispositionFormData("attachment", template.getFileName());
            } else {
                headers.add("Content-Disposition", "inline; filename=\"" + template.getFileName() + "\"");
            }

            return ResponseEntity.ok()
                .headers(headers)
                .body(content);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
            );
        } catch (Exception e) {
            log.error("Error fetching template content file", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to fetch template content file", "TEMPLATE_CONTENT_FILE_FETCH_FAILED")
            );
        }
    }

    /**
     * Get template content file by file name
     *
     * @param eventIdObfuscated Obfuscated email event ID
     * @param fileName Template file name
     * @param download Whether to download or display inline
     * @return ResponseEntity with HTML content
     */
    public ResponseEntity<?> getTemplateContentFileByName(String eventIdObfuscated, String fileName, boolean download) {
        log.debug("Fetching template content file by name: {}, download: {}", fileName, download);

        try {
            Long eventId = idObfuscator.decodeId(eventIdObfuscated);

            EmailTemplate template = emailTemplateRepository.findByEmailEventIdAndFileName(eventId, fileName).orElse(null);
            if (template == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND")
                );
            }

            // Read content from file
            String content = emailTemplateService.readTemplateFile(template.getFileName());
            if (content == null) {
                return ResponseEntity.internalServerError().body(
                    ApiResponse.error(500, "Failed to read template content", "TEMPLATE_CONTENT_READ_FAILED")
                );
            }

            // Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);

            if (download) {
                headers.setContentDispositionFormData("attachment", template.getFileName());
            } else {
                headers.add("Content-Disposition", "inline; filename=\"" + template.getFileName() + "\"");
            }

            return ResponseEntity.ok()
                .headers(headers)
                .body(content);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid event ID format", "INVALID_EVENT_ID")
            );
        } catch (Exception e) {
            log.error("Error fetching template content file by name", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to fetch template content file", "TEMPLATE_CONTENT_FILE_FETCH_FAILED")
            );
        }
    }

    /**
     * Convert EmailTemplate entity to DTO (without content)
     */
    public EmailTemplateDTO convertToDTO(EmailTemplate template) {
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

    /**
     * Convert EmailTemplate entity to DTO (with content)
     */
    public EmailTemplateDTO convertToDTOWithContent(EmailTemplate template, String content) {
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
            .content(content)
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();
    }
}
