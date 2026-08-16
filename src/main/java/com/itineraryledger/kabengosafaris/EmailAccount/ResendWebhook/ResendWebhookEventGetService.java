package com.itineraryledger.kabengosafaris.EmailAccount.ResendWebhook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResendWebhookEventGetService {

    private final ResendWebhookEventRepository webhookEventRepository;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    private static final Set<String> VALID_SORT_FIELDS = Set.of(
            "receivedAt", "eventTimestamp", "eventType", "fromEmail", "toEmail", "subject"
    );

    public ResponseEntity<ApiResponse<?>> listEvents(
            Integer pageNumber, Integer pageSize, String sortBy, String sortDirection,
            String eventType, String fromEmail, String toEmail, String keyword) {
        return listEvents(pageNumber, pageSize, sortBy, sortDirection, eventType, fromEmail, toEmail,
                keyword, null);
    }

    public ResponseEntity<ApiResponse<?>> listEvents(
            Integer pageNumber, Integer pageSize, String sortBy, String sortDirection,
            String eventType, String fromEmail, String toEmail, String keyword, Boolean includeStats) {
        try {
            pageNumber = (pageNumber != null) ? pageNumber : 0;
            pageSize = (pageSize != null) ? pageSize : 25;
            sortDirection = (sortDirection != null && !sortDirection.isEmpty()) ? sortDirection : "desc";

            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null && sortBy != null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }
            if (validatedSortBy == null) validatedSortBy = "receivedAt";

            Sort sort = sortDirection.equalsIgnoreCase("desc")
                    ? Sort.by(validatedSortBy).descending()
                    : Sort.by(validatedSortBy).ascending();
            Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

            Specification<ResendWebhookEvent> spec = buildSpec(eventType, fromEmail, toEmail, keyword);

            Page<ResendWebhookEvent> page = webhookEventRepository.findAll(spec, pageable);

            List<ResendWebhookEventDTO> dtos = page.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("events", dtos);
            response.put("currentPage", page.getNumber());
            response.put("totalItems", page.getTotalElements());
            response.put("totalPages", page.getTotalPages());
            response.put("pageSize", page.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection.toLowerCase());
            /*
             * Counters over the WHOLE filtered set. Page scope answered "3 bounced" about the
             * twenty-five rows on screen out of 2,279 — true, and read by everybody as the
             * number of bounces there have ever been.
             */
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection);

            return ResponseEntity.ok(ApiResponse.success(200, "Webhook events retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing webhook events", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to list webhook events", "WEBHOOK_EVENTS_LIST_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getEvent(String obfuscatedId) {
        return getEvent(obfuscatedId, null, null, null, null, null, null);
    }

    /**
     * One event, plus where it sits in the set the caller was looking at.
     *
     * The filter comes from the list, so stepping through bounces stays among bounces. It
     * used to walk by timestamp across the whole table, with no position at all.
     */
    public ResponseEntity<ApiResponse<?>> getEvent(
        String obfuscatedId,
        String eventType,
        String fromEmail,
        String toEmail,
        String keyword,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long id = idObfuscator.decodeId(obfuscatedId);
            ResendWebhookEvent event = webhookEventRepository.findById(id).orElse(null);
            if (event == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Webhook event not found", "NOT_FOUND")
                );
            }

            ResendWebhookEventDTO dto = convertToDTO(event);

            // Circular navigation — find previous and next by receivedAt desc order
            Specification<ResendWebhookEvent> navSpec = buildSpec(eventType, fromEmail, toEmail, keyword);
            String navSortBy = validateSortField(sortBy) != null ? validateSortField(sortBy) : "receivedAt";
            Map<String, Object> nav = recordNavigation.navigate(
                ResendWebhookEvent.class, navSpec, navSortBy, "asc".equalsIgnoreCase(sortDirection), id);
            Long nextRaw = (Long) nav.get("nextRawId");
            Long previousRaw = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("event", dto);
            response.put("rawPayload", event.getRawPayload());
            response.put("previousId", previousRaw != null ? idObfuscator.encodeId(previousRaw) : null);
            response.put("nextId", nextRaw != null ? idObfuscator.encodeId(nextRaw) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Webhook event retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching webhook event: {}", obfuscatedId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch webhook event", "WEBHOOK_EVENT_FETCH_FAILED")
            );
        }
    }

    private ResendWebhookEventDTO convertToDTO(ResendWebhookEvent event) {
        return ResendWebhookEventDTO.builder()
                .id(idObfuscator.encodeId(event.getId()))
                .svixId(event.getSvixId())
                .eventType(event.getEventType())
                .emailId(event.getEmailId())
                .fromEmail(event.getFromEmail())
                .toEmail(event.getToEmail())
                .subject(event.getSubject())
                .eventTimestamp(event.getEventTimestamp())
                .receivedAt(event.getReceivedAt())
                .build();
    }

    /** ONE specification, shared by the rows, the counters and the record walk. */
    private Specification<ResendWebhookEvent> buildSpec(
        String eventType, String fromEmail, String toEmail, String keyword
    ) {
        Specification<ResendWebhookEvent> spec = Specification.unrestricted();
        if (eventType != null && !eventType.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("eventType"), eventType));
        }
        if (fromEmail != null && !fromEmail.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("fromEmail")), "%" + fromEmail.toLowerCase() + "%"));
        }
        if (toEmail != null && !toEmail.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.like(cb.lower(root.get("toEmail")), "%" + toEmail.toLowerCase() + "%"));
        }
        if (keyword != null && !keyword.isEmpty()) {
            String kw = "%" + keyword.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("fromEmail")), kw),
                cb.like(cb.lower(root.get("toEmail")), kw),
                cb.like(cb.lower(root.get("subject")), kw),
                cb.like(cb.lower(root.get("emailId")), kw),
                cb.like(cb.lower(root.get("eventType")), kw)));
        }
        return spec;
    }

    /** One counter per outcome, and the two that are bad news lead the cards. */
    private Map<String, Object> buildStats(Specification<ResendWebhookEvent> spec) {
        return listStats.of(ResendWebhookEvent.class, spec)
            .total()
            .count("bounced", ofType("email.bounced"))
            .count("complained", ofType("email.complained"))
            .count("delivered", ofType("email.delivered"))
            .count("sent", ofType("email.sent"))
            .count("opened", ofType("email.opened"))
            .count("clicked", ofType("email.clicked"))
            .count("delayed", ofType("email.delivery_delayed"))
            .build();
    }

    private Specification<ResendWebhookEvent> ofType(String type) {
        return (root, query, cb) -> cb.equal(root.get("eventType"), type);
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) return null;
        return VALID_SORT_FIELDS.contains(sortBy) ? sortBy : null;
    }
}
