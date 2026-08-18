package com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity.AvailabilityRequest;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Repository.AvailabilityRequestRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The chase list: every ask across every trip, in the order somebody works through them.
 *
 * A safari's own tab answers "what have we asked about this trip". This answers the question a
 * morning actually starts with — "who owes us an answer, and who has answered and been ignored" —
 * which spans trips and therefore cannot live under one.
 *
 * Sorted by chase date ascending by default: the thing that has been waiting longest, first.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AvailabilityRequestListService {

    private final AvailabilityRequestRepository requestRepository;
    private final AvailabilityRequestService requestService;
    private final ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;
    private final IdObfuscator idObfuscator;

    private static final Set<String> VALID_SORT_FIELDS =
        Set.of("chaseDueAt", "sentAt", "repliedAt", "status", "createdAt");
    private static final String DEFAULT_SORT_FIELD = "chaseDueAt";

    public ResponseEntity<ApiResponse<?>> list(
        List<String> statuses,
        Boolean chaseDue,
        Boolean awaiting,
        Boolean repliedUndecided,
        String safariId,
        String accommodationId,
        LocalDateTime sentAfter,
        LocalDateTime sentBefore,
        String keyword,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        try {
            String resolvedSort = sortBy != null && VALID_SORT_FIELDS.contains(sortBy) ? sortBy : DEFAULT_SORT_FIELD;
            /*
             * Ascending by default, which is the opposite of every other list here — and right:
             * the oldest unanswered ask is the one to chase first.
             */
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            int pageSize = size == null || size <= 0 ? 25 : Math.min(size, 100);
            Pageable pageable = PageRequest.of(page == null || page < 0 ? 0 : page, pageSize,
                Sort.by(direction, resolvedSort));

            LocalDateTime now = LocalDateTime.now();
            Specification<AvailabilityRequest> spec = buildSpec(
                statuses, chaseDue, awaiting, repliedUndecided, safariId, accommodationId,
                sentAfter, sentBefore, keyword, now);

            Page<AvailabilityRequest> found = requestRepository.findAll(spec, pageable);

            List<Map<String, Object>> rows = new ArrayList<>();
            for (AvailabilityRequest request : found.getContent()) rows.add(requestService.toRow(request, now));

            Map<String, Object> response = new HashMap<>();
            response.put("availabilityRequests", rows);
            response.put("currentPage", found.getNumber());
            response.put("totalItems", found.getTotalElements());
            response.put("totalPages", found.getTotalPages());
            response.put("pageSize", found.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", resolvedSort);
            response.put("currentSortDirection", direction.name().toLowerCase());

            if (!Boolean.FALSE.equals(includeStats)) {
                /*
                 * Counters over the SAME specification as the rows, so the cards and the table can
                 * never disagree. Every one of them is reachable as a filter.
                 */
                response.put("stats", listStats.of(AvailabilityRequest.class, spec)
                    .total()
                    .count("chaseDue", AvailabilityRequestSpecification.chaseDue(now))
                    .count("awaitingReply", AvailabilityRequestSpecification.awaiting(now))
                    .count("repliedUndecided", AvailabilityRequestSpecification.repliedUndecided())
                    .count("confirmed", AvailabilityRequestSpecification.closedFor(
                        AvailabilityRequest.ClosedReason.CONFIRMED))
                    .count("declined", AvailabilityRequestSpecification.closedFor(
                        AvailabilityRequest.ClosedReason.DECLINED))
                    .count("superseded", AvailabilityRequestSpecification.closedFor(
                        AvailabilityRequest.ClosedReason.SUPERSEDED))
                    .count("sentLast7Days", AvailabilityRequestSpecification.sentAfter(now.minusDays(7)))
                    .build());
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Availability requests retrieved", response));
        } catch (Exception e) {
            log.error("Error listing availability requests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list availability requests", "AVAILABILITY_REQUEST_LIST_FAILED"));
        }
    }

    /**
     * One request, and where it sits in the set the list was showing.
     *
     * This was missing entirely: the record page asked for it and got a 500, which is the worst
     * kind of gap — a page that exists, a link that leads to it, and nothing behind it. The
     * filters travel with the request so the prev/next arrows walk the SAME set as the list, which
     * is the house rule; without them the arrows would wander into requests the list had excluded.
     */
    public ResponseEntity<ApiResponse<?>> getOne(
        String requestIdObfuscated,
        List<String> statuses,
        Boolean chaseDue,
        Boolean awaiting,
        Boolean repliedUndecided,
        String safariId,
        String accommodationId,
        LocalDateTime sentAfter,
        LocalDateTime sentBefore,
        String keyword,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long id = idObfuscator.decodeId(requestIdObfuscated);
            AvailabilityRequest request = id == null
                ? null : requestRepository.findById(id).orElse(null);
            if (request == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Availability request not found",
                        "AVAILABILITY_REQUEST_NOT_FOUND"));
            }

            LocalDateTime now = LocalDateTime.now();
            String resolvedSort = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Specification<AvailabilityRequest> spec = buildSpec(
                statuses, chaseDue, awaiting, repliedUndecided, safariId, accommodationId,
                sentAfter, sentBefore, keyword, now);

            Map<String, Object> nav = recordNavigation.navigate(
                AvailabilityRequest.class, spec, resolvedSort,
                !"desc".equalsIgnoreCase(sortDirection), id);
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("availabilityRequest", requestService.toRow(request, now));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));
            return ResponseEntity.ok(ApiResponse.success(200, "Availability request retrieved", response));
        } catch (Exception e) {
            log.error("Error fetching an availability request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch the availability request",
                    "AVAILABILITY_REQUEST_FETCH_FAILED"));
        }
    }

    /** ONE description of the filtered set, shared by the rows and the counters. */
    private Specification<AvailabilityRequest> buildSpec(
        List<String> statuses, Boolean chaseDue, Boolean awaiting, Boolean repliedUndecided,
        String safariIdObfuscated, String accommodationIdObfuscated,
        LocalDateTime sentAfter, LocalDateTime sentBefore, String keyword, LocalDateTime now
    ) {
        Specification<AvailabilityRequest> spec = Specification.unrestricted();

        if (statuses != null && !statuses.isEmpty()) {
            List<AvailabilityRequest.Status> parsed = new ArrayList<>();
            for (String value : statuses) {
                try {
                    parsed.add(AvailabilityRequest.Status.valueOf(value.trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    /* an unreadable status narrows nothing rather than erroring the whole list */
                }
            }
            Specification<AvailabilityRequest> byStatus = AvailabilityRequestSpecification.statusIn(parsed);
            if (byStatus != null) spec = spec.and(byStatus);
        }

        /*
         * The three working states are OR-ed together, because they are one dimension: a list of
         * "chase due or replied" is a sensible morning, and ANDing them would return nothing.
         */
        List<Specification<AvailabilityRequest>> states = new ArrayList<>();
        if (Boolean.TRUE.equals(chaseDue)) states.add(AvailabilityRequestSpecification.chaseDue(now));
        if (Boolean.TRUE.equals(awaiting)) states.add(AvailabilityRequestSpecification.awaiting(now));
        if (Boolean.TRUE.equals(repliedUndecided)) states.add(AvailabilityRequestSpecification.repliedUndecided());
        if (!states.isEmpty()) {
            Specification<AvailabilityRequest> any = states.get(0);
            for (int i = 1; i < states.size(); i++) any = any.or(states.get(i));
            spec = spec.and(any);
        }

        spec = spec.and(AvailabilityRequestSpecification.forSafari(decode(safariIdObfuscated)));
        spec = spec.and(AvailabilityRequestSpecification.forAccommodation(decode(accommodationIdObfuscated)));
        spec = spec.and(AvailabilityRequestSpecification.sentAfter(sentAfter));
        spec = spec.and(AvailabilityRequestSpecification.sentBefore(sentBefore));
        spec = spec.and(AvailabilityRequestSpecification.keyword(keyword));
        return spec;
    }

    private Long decode(String obfuscated) {
        if (obfuscated == null || obfuscated.isBlank()) return null;
        try {
            return idObfuscator.decodeId(obfuscated);
        } catch (Exception e) {
            return null;
        }
    }
}
