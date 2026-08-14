package com.itineraryledger.kabengosafaris.Newsletter.Services;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Newsletter.DTOs.NewsletterSubscriptionDTO;
import com.itineraryledger.kabengosafaris.Newsletter.DTOs.NewsletterSubscriptionListItemDTO;
import com.itineraryledger.kabengosafaris.Newsletter.Entity.NewsletterSubscription;
import com.itineraryledger.kabengosafaris.Newsletter.Entity.SubscriptionStatus;
import com.itineraryledger.kabengosafaris.Newsletter.Repository.NewsletterSubscriptionRepository;
import com.itineraryledger.kabengosafaris.Newsletter.Specifications.NewsletterFilter;
import com.itineraryledger.kabengosafaris.Newsletter.Specifications.NewsletterSubscriptionSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(readOnly = true)
public class NewsletterGetService {

    private final NewsletterSubscriptionRepository repository;
    private final IdObfuscator idObfuscator;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "email", "name", "status", "source", "subscribedAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "subscribedAt";

    @Autowired
    public NewsletterGetService(
        NewsletterSubscriptionRepository repository,
        IdObfuscator idObfuscator,
        ListStats listStats,
        RecordNavigation recordNavigation
    ) {
        this.repository = repository;
        this.idObfuscator = idObfuscator;
        this.listStats = listStats;
        this.recordNavigation = recordNavigation;
    }

    public ResponseEntity<ApiResponse<?>> getSubscriptionById(String idObfuscated) {
        return getSubscriptionById(idObfuscated, null, null, null);
    }

    /**
     * One subscriber, plus where they sit in the set the caller was looking at.
     *
     * The filter and sort come from the list page, so paging through bounced addresses
     * stays among bounced addresses.
     */
    public ResponseEntity<ApiResponse<?>> getSubscriptionById(
        String idObfuscated,
        NewsletterFilter filter,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching newsletter subscription with ID: {}", idObfuscated);
        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode subscription ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid subscription ID", "INVALID_SUBSCRIPTION_ID")
                );
            }

            NewsletterSubscription subscription = repository.findById(id).orElse(null);
            if (subscription == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Subscription not found", "SUBSCRIPTION_NOT_FOUND")
                );
            }

            NewsletterSubscriptionDTO dto = convertToDTO(subscription);

            /*
             * The walk runs over the SAME specification the list used, in the same order.
             * It used to page over every subscriber by id, so the arrows left the filtered
             * set the moment anybody used a filter.
             */
            Specification<NewsletterSubscription> navSpec =
                buildSpec(filter != null ? filter : new NewsletterFilter());
            String navSortBy = validateSortField(sortBy) != null ? validateSortField(sortBy) : DEFAULT_SORT_FIELD;
            boolean ascending = "asc".equalsIgnoreCase(sortDirection);

            Map<String, Object> nav = recordNavigation.navigate(
                NewsletterSubscription.class, navSpec, navSortBy, ascending, id);
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("subscription", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok().body(ApiResponse.success(200, "Subscription retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch subscription", "SUBSCRIPTION_FETCH_FAILED")
            );
        }
    }

    /** Kept so any caller still passing loose parameters keeps working. */
    public ResponseEntity<ApiResponse<?>> listSubscriptions(
        Integer pageNumber, Integer pageSize, String sortBy, String sortDirection,
        SubscriptionStatus status, String email, String name, String source,
        LocalDateTime subscribedAfter, LocalDateTime subscribedBefore, String keyword
    ) {
        NewsletterFilter filter = new NewsletterFilter();
        filter.setStatus(status);
        filter.setEmail(email);
        filter.setName(name);
        filter.setSource(source);
        filter.setSubscribedAfter(subscribedAfter);
        filter.setSubscribedBefore(subscribedBefore);
        filter.setKeyword(keyword);
        return listSubscriptions(filter, null, pageNumber, pageSize, sortBy, sortDirection);
    }

    public ResponseEntity<ApiResponse<?>> listSubscriptions(
        NewsletterFilter filter,
        Boolean includeStats,
        Integer pageNumber, Integer pageSize, String sortBy, String sortDirection
    ) {
        log.info("Listing newsletter subscriptions with filters");
        NewsletterFilter active = filter != null ? filter : new NewsletterFilter();
        try {
            pageNumber = (pageNumber != null) ? pageNumber : 0;
            pageSize = (pageSize != null) ? pageSize : 20;
            sortDirection = (sortDirection != null && !sortDirection.isEmpty()) ? sortDirection : "desc";

            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(validatedSortBy).descending()
                : Sort.by(validatedSortBy).ascending();
            Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

            Specification<NewsletterSubscription> spec = buildSpec(active);

            Page<NewsletterSubscription> page = repository.findAll(spec, pageable);

            List<NewsletterSubscriptionListItemDTO> dtos = page.getContent().stream()
                .map(this::convertToListItemDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("subscriptions", dtos);
            response.put("currentPage", page.getNumber());
            response.put("totalItems", page.getTotalElements());
            response.put("totalPages", page.getTotalPages());
            response.put("pageSize", page.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection);
            /*
             * Counters for the WHOLE filtered set, from the same specification as the rows,
             * so a card and the table under it cannot disagree.
             */
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok().body(ApiResponse.success(200, "Subscriptions retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing subscriptions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list subscriptions", "SUBSCRIPTIONS_LIST_FAILED")
            );
        }
    }

    /** One specification, used by the rows, the cards and the record walk. */
    private Specification<NewsletterSubscription> buildSpec(NewsletterFilter filter) {
        Specification<NewsletterSubscription> spec = Specification.unrestricted();

        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            spec = spec.and(NewsletterSubscriptionSpecification.searchKeyword(filter.getKeyword()));
        }
        if (filter.getEmail() != null && !filter.getEmail().isBlank()) {
            spec = spec.and(NewsletterSubscriptionSpecification.byEmail(filter.getEmail()));
        }
        if (filter.getName() != null && !filter.getName().isBlank()) {
            spec = spec.and(NewsletterSubscriptionSpecification.byName(filter.getName()));
        }
        if (!filter.allStatuses().isEmpty()) {
            spec = spec.and(NewsletterSubscriptionSpecification.statusIn(filter.allStatuses()));
        }
        if (!filter.allSources().isEmpty()) {
            spec = spec.and(NewsletterSubscriptionSpecification.sourceIn(filter.allSources()));
        }
        if (filter.wants("noCustomer")) spec = spec.and(NewsletterSubscriptionSpecification.hasNoCustomer());
        if (filter.wants("noName")) spec = spec.and(NewsletterSubscriptionSpecification.hasNoName());
        if (filter.getSubscribedAfter() != null) {
            spec = spec.and(NewsletterSubscriptionSpecification.subscribedAfter(filter.getSubscribedAfter()));
        }
        if (filter.getSubscribedBefore() != null) {
            spec = spec.and(NewsletterSubscriptionSpecification.subscribedBefore(filter.getSubscribedBefore()));
        }
        return spec;
    }

    /**
     * The cards over the subscriber list.
     *
     * Bounced is the one worth acting on: those addresses cost money to mail and will never
     * arrive, and nothing else on the row says so.
     */
    private Map<String, Object> buildStats(Specification<NewsletterSubscription> spec) {
        return listStats.of(NewsletterSubscription.class, spec)
            .total()
            .breakdown("byStatus", SubscriptionStatus.values(),
                st -> NewsletterSubscriptionSpecification.statusIn(List.of(st)))
            .count("active", NewsletterSubscriptionSpecification.statusIn(List.of(SubscriptionStatus.ACTIVE)))
            .count("unsubscribed", NewsletterSubscriptionSpecification.statusIn(List.of(SubscriptionStatus.UNSUBSCRIBED)))
            .count("bounced", NewsletterSubscriptionSpecification.statusIn(List.of(SubscriptionStatus.BOUNCED)))
            .count("noCustomer", NewsletterSubscriptionSpecification.hasNoCustomer())
            .count("noName", NewsletterSubscriptionSpecification.hasNoName())
            /*
             * Recency is measured by when they signed up, not by a createdAt — that is the
             * date this table actually keeps, and the one anybody means by "new".
             */
            .window("newLast7Days", 7, NewsletterSubscriptionSpecification::subscribedAfter)
            .window("newLast30Days", 30, NewsletterSubscriptionSpecification::subscribedAfter)
            .build();
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    public NewsletterSubscriptionDTO convertToDTO(NewsletterSubscription subscription) {
        NewsletterSubscriptionDTO dto = NewsletterSubscriptionDTO.builder()
            .id(idObfuscator.encodeId(subscription.getId()))
            .email(subscription.getEmail())
            .name(subscription.getName())
            .preferredLocale(subscription.getPreferredLocale())
            .status(subscription.getStatus())
            .statusDisplayName(subscription.getStatus() != null ? subscription.getStatus().getDisplayName() : null)
            .source(subscription.getSource())
            .subscribedAt(subscription.getSubscribedAt())
            .updatedAt(subscription.getUpdatedAt())
            .unsubscribedAt(subscription.getUnsubscribedAt())
            .build();

        if (subscription.getCustomer() != null) {
            dto.setCustomerId(idObfuscator.encodeId(subscription.getCustomer().getId()));
            dto.setCustomerName(subscription.getCustomer().getFullName());
        }

        return dto;
    }

    private NewsletterSubscriptionListItemDTO convertToListItemDTO(NewsletterSubscription subscription) {
        NewsletterSubscriptionListItemDTO dto = NewsletterSubscriptionListItemDTO.builder()
            .id(idObfuscator.encodeId(subscription.getId()))
            .email(subscription.getEmail())
            .name(subscription.getName())
            .status(subscription.getStatus())
            .statusDisplayName(subscription.getStatus() != null ? subscription.getStatus().getDisplayName() : null)
            .source(subscription.getSource())
            .subscribedAt(subscription.getSubscribedAt())
            .build();

        if (subscription.getCustomer() != null) {
            dto.setCustomerName(subscription.getCustomer().getFullName());
        }

        return dto;
    }
}
