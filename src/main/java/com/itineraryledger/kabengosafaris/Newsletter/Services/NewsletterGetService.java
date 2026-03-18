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
import com.itineraryledger.kabengosafaris.Newsletter.Specifications.NewsletterSubscriptionSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(readOnly = true)
public class NewsletterGetService {

    private final NewsletterSubscriptionRepository repository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "email", "name", "status", "source", "subscribedAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "subscribedAt";

    @Autowired
    public NewsletterGetService(NewsletterSubscriptionRepository repository, IdObfuscator idObfuscator) {
        this.repository = repository;
        this.idObfuscator = idObfuscator;
    }

    public ResponseEntity<ApiResponse<?>> getSubscriptionById(String idObfuscated) {
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

            Long nextId = repository.findNextId(id).orElse(null);
            Long previousId = repository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = repository.findFirstId().orElse(null);
            if (previousId == null) previousId = repository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("subscription", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(ApiResponse.success(200, "Subscription retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch subscription", "SUBSCRIPTION_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> listSubscriptions(
        Integer pageNumber, Integer pageSize, String sortBy, String sortDirection,
        SubscriptionStatus status, String email, String name, String source,
        LocalDateTime subscribedAfter, LocalDateTime subscribedBefore, String keyword
    ) {
        log.info("Listing newsletter subscriptions with filters");
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

            Specification<NewsletterSubscription> spec = Specification.unrestricted();
            if (status != null) spec = spec.and(NewsletterSubscriptionSpecification.byStatus(status));
            if (email != null && !email.isEmpty()) spec = spec.and(NewsletterSubscriptionSpecification.byEmail(email));
            if (name != null && !name.isEmpty()) spec = spec.and(NewsletterSubscriptionSpecification.byName(name));
            if (source != null && !source.isEmpty()) spec = spec.and(NewsletterSubscriptionSpecification.bySource(source));
            if (subscribedAfter != null) spec = spec.and(NewsletterSubscriptionSpecification.subscribedAfter(subscribedAfter));
            if (subscribedBefore != null) spec = spec.and(NewsletterSubscriptionSpecification.subscribedBefore(subscribedBefore));
            if (keyword != null && !keyword.isEmpty()) spec = spec.and(NewsletterSubscriptionSpecification.searchKeyword(keyword));

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

            return ResponseEntity.ok().body(ApiResponse.success(200, "Subscriptions retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing subscriptions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list subscriptions", "SUBSCRIPTIONS_LIST_FAILED")
            );
        }
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
