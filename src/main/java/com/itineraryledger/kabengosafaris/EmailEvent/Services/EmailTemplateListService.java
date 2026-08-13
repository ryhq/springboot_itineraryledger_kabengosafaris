package com.itineraryledger.kabengosafaris.EmailEvent.Services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.EmailTemplateDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailTemplateRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailTemplate;
import com.itineraryledger.kabengosafaris.EmailEvent.Specifications.EmailTemplateFilter;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Every template, across every event.
 *
 * The nested list under an event answers "what can this email send". This is the list of
 * the wording itself — which is a thing somebody maintains, reviews and searches, not
 * only a detail of the event it hangs off. Without it there is no way to ask "which
 * templates has somebody rewritten" or "what is switched off" except by opening eighteen
 * events one at a time.
 *
 * Content is deliberately absent from the rows. A template body is a whole HTML document;
 * twenty of them in a list response is megabytes for a table that shows a name and a
 * flag. The record fetch carries it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EmailTemplateListService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "enabled", "isDefault", "isSystemDefault", "fileSize", "createdAt", "updatedAt");
    private static final String DEFAULT_SORT_FIELD = "name";

    private final EmailTemplateRepository templateRepository;
    private final EmailTemplateGetService getService;
    private final EmailTemplateService templateService;
    private final IdObfuscator idObfuscator;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    public ResponseEntity<ApiResponse<?>> list(
        EmailTemplateFilter filter,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        try {
            EmailTemplateFilter resolved = filter != null ? filter : new EmailTemplateFilter();

            String resolvedSort = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
            Pageable pageable = PageRequest.of(
                page == null || page < 0 ? 0 : page, pageSize, Sort.by(direction, resolvedSort));

            Specification<EmailTemplate> spec = buildSpec(resolved);
            Page<EmailTemplate> found = templateRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("templates", found.getContent().stream().map(getService::convertToDTO).toList());
            response.put("currentPage", found.getNumber());
            response.put("totalItems", found.getTotalElements());
            response.put("totalPages", found.getTotalPages());
            response.put("pageSize", found.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", resolvedSort);
            response.put("currentSortDirection", direction.name().toLowerCase());
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Templates retrieved", response));
        } catch (Exception e) {
            log.error("Error listing templates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list templates", "TEMPLATES_LIST_FAILED"));
        }
    }

    /** One template WITH its body, and where it sits in the set the caller came from. */
    public ResponseEntity<ApiResponse<?>> getOne(
        String idObfuscated,
        EmailTemplateFilter filter,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            EmailTemplate template = templateRepository.findById(id).orElse(null);
            if (template == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND"));
            }

            /*
             * The body lives on disk, not in the row. A read failure is reported rather than
             * quietly returning a template with no content — an empty editor looks exactly
             * like a template somebody emptied, and one of those is safe to save over.
             */
            String content = templateService.readTemplateFile(template.getFileName());
            if (content == null) {
                return ResponseEntity.status(500).body(
                    ApiResponse.error(500,
                        "The template's file could not be read, so its content is unavailable",
                        "TEMPLATE_CONTENT_READ_FAILED"));
            }

            Specification<EmailTemplate> navSpec =
                buildSpec(filter != null ? filter : new EmailTemplateFilter());
            String navSortBy = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Map<String, Object> nav = recordNavigation.navigate(
                EmailTemplate.class, navSpec, navSortBy, !"desc".equalsIgnoreCase(sortDirection), id);

            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            EmailTemplateDTO dto = getService.convertToDTOWithContent(template, content);

            Map<String, Object> response = new HashMap<>();
            response.put("template", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Template retrieved", response));
        } catch (Exception e) {
            log.error("Error fetching template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch template", "TEMPLATE_FETCH_FAILED"));
        }
    }

    /** The event a template belongs to, obfuscated — the write services are nested under it. */
    public String eventIdOf(String templateIdObfuscated) {
        try {
            Long id = idObfuscator.decodeId(templateIdObfuscated);
            EmailTemplate template = templateRepository.findById(id).orElse(null);
            if (template == null || template.getEmailEvent() == null) return null;
            return idObfuscator.encodeId(template.getEmailEvent().getId());
        } catch (Exception e) {
            log.warn("Unreadable template id: {}", templateIdObfuscated);
            return null;
        }
    }

    /** ONE specification, shared by the rows, the counters and the record walk. */
    private Specification<EmailTemplate> buildSpec(EmailTemplateFilter filter) {
        Specification<EmailTemplate> spec = Specification.unrestricted();

        String keyword = filter.effectiveKeyword();
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("description")), like),
                cb.like(cb.lower(root.get("fileName")), like),
                cb.like(cb.lower(root.get("emailEvent").get("name")), like)));
        }
        if (filter.getName() != null && !filter.getName().isBlank()) {
            spec = spec.and(EmailTemplateSpecification.nameLike(filter.getName()));
        }

        List<Long> eventIds = decodeAll(filter.allEventIds());
        if (eventIds != null) {
            spec = spec.and((root, query, cb) -> root.get("emailEvent").get("id").in(eventIds));
        }

        // contradictory pairs cancel to no constraint, as everywhere else
        boolean wantsOn = filter.hasStatus("enabled");
        boolean wantsOff = filter.hasStatus("disabled");
        if (wantsOn != wantsOff) {
            spec = spec.and(EmailTemplateSpecification.enabled(wantsOn));
        } else if (filter.getEnabled() != null) {
            spec = spec.and(EmailTemplateSpecification.enabled(filter.getEnabled()));
        }

        Specification<EmailTemplate> kind = null;
        if (filter.hasKind("sent")) kind = EmailTemplateSpecification.isDefault(true);
        if (filter.hasKind("original")) {
            Specification<EmailTemplate> extra = EmailTemplateSpecification.isSystemDefault(true);
            kind = kind == null ? extra : kind.or(extra);
        }
        if (filter.hasKind("custom")) {
            Specification<EmailTemplate> extra = EmailTemplateSpecification.isSystemDefault(false);
            kind = kind == null ? extra : kind.or(extra);
        }
        if (kind != null) spec = spec.and(kind);

        if (!filter.hasKind("sent") && filter.getIsDefault() != null) {
            spec = spec.and(EmailTemplateSpecification.isDefault(filter.getIsDefault()));
        }
        if (filter.getIsSystemDefault() != null && !filter.hasKind("original") && !filter.hasKind("custom")) {
            spec = spec.and(EmailTemplateSpecification.isSystemDefault(filter.getIsSystemDefault()));
        }

        return spec;
    }

    /**
     * Decodes the obfuscated event ids, or narrows to nothing if none of them read.
     *
     * An unreadable id must not quietly widen the list to every template — that is the
     * opposite of what was asked for.
     */
    private List<Long> decodeAll(List<String> obfuscated) {
        if (obfuscated == null || obfuscated.isEmpty()) return null;
        List<Long> ids = new ArrayList<>();
        for (String value : obfuscated) {
            try {
                ids.add(idObfuscator.decodeId(value));
            } catch (Exception e) {
                log.warn("Unreadable event id on the templates filter: {}", value);
            }
        }
        if (ids.isEmpty()) ids.add(-1L);
        return ids;
    }

    /**
     * The cards that head the list.
     *
     * "Not the one sent" is the counter worth having: several templates can exist for an
     * event while exactly one is used, so a carefully edited template that is not the
     * default has changed nothing at all.
     */
    private Map<String, Object> buildStats(Specification<EmailTemplate> spec) {
        return listStats.of(EmailTemplate.class, spec)
            .total()
            .count("enabled", EmailTemplateSpecification.enabled(true))
            .complement("disabled", "enabled")
            .count("sent", EmailTemplateSpecification.isDefault(true))
            .count("original", EmailTemplateSpecification.isSystemDefault(true))
            .count("custom", EmailTemplateSpecification.isSystemDefault(false))
            .build();
    }
}
