package com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyServices;

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

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Testimony.Entity.Testimony;
import com.itineraryledger.kabengosafaris.Testimony.Entity.TestimonyImage;
import com.itineraryledger.kabengosafaris.Testimony.Enums.TestimonySource;
import com.itineraryledger.kabengosafaris.Testimony.Repository.TestimonyRepository;
import com.itineraryledger.kabengosafaris.Testimony.Specifications.TestimonyFilter;
import com.itineraryledger.kabengosafaris.Testimony.Specifications.TestimonySpecification;
import com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyDTO;
import com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyListItemDTO;
import com.itineraryledger.kabengosafaris.Public.Services.PublicTranslationService;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyImageServices.TestimonyImageStorageService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(readOnly = true)
public class TestimonyGetService {

    private final TestimonyRepository testimonyRepository;
    private final IdObfuscator idObfuscator;
    private final TestimonyImageStorageService storageService;
    private final PublicTranslationService publicTranslationService;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "authorName", "rating", "source", "reviewDate", "isApproved", "isFeatured",
        "isVerifiedBooking", "isActive", "displayOrder", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public TestimonyGetService(
        TestimonyRepository testimonyRepository,
        IdObfuscator idObfuscator,
        TestimonyImageStorageService storageService,
        PublicTranslationService publicTranslationService,
        ListStats listStats,
        RecordNavigation recordNavigation
    ) {
        this.testimonyRepository = testimonyRepository;
        this.idObfuscator = idObfuscator;
        this.storageService = storageService;
        this.publicTranslationService = publicTranslationService;
        this.listStats = listStats;
        this.recordNavigation = recordNavigation;
    }

    public ResponseEntity<ApiResponse<?>> getTestimonyById(String idObfuscated) {
        return getTestimonyById(idObfuscated, null, null, null);
    }

    /**
     * One review, plus where it sits in the set the caller was looking at.
     *
     * The filter and sort come from the list page, so working through the pending queue
     * with the arrows stays inside the pending queue.
     */
    public ResponseEntity<ApiResponse<?>> getTestimonyById(
        String idObfuscated,
        TestimonyFilter filter,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching testimony with ID: {}", idObfuscated);
        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode testimony ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid testimony ID", "INVALID_TESTIMONY_ID")
                );
            }

            Testimony testimony = testimonyRepository.findById(id).orElse(null);
            if (testimony == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Testimony not found", "TESTIMONY_NOT_FOUND")
                );
            }

            TestimonyDTO dto = convertToDTO(testimony);

            /*
             * The walk runs over the SAME specification the list used, in the same order.
             * It used to page over every review by id, so the arrows left the filtered set
             * the moment anybody used a filter.
             */
            Specification<Testimony> navSpec = buildSpec(filter != null ? filter : new TestimonyFilter());
            String navSortBy = validateSortField(sortBy) != null ? validateSortField(sortBy) : DEFAULT_SORT_FIELD;
            boolean ascending = "asc".equalsIgnoreCase(sortDirection);

            Map<String, Object> nav = recordNavigation.navigate(
                Testimony.class, navSpec, navSortBy, ascending, id);
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("testimony", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok().body(ApiResponse.success(200, "Testimony retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching testimony", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch testimony", "TESTIMONY_FETCH_FAILED")
            );
        }
    }

    public long getApprovedActiveCount() {
        return testimonyRepository.countByIsApprovedTrueAndIsActiveTrue();
    }

    public ResponseEntity<ApiResponse<?>> getPublicTestimonies() {
        log.info("Fetching public approved testimonies");
        try {
            List<Testimony> testimonies = testimonyRepository.findByIsApprovedTrueAndIsActiveTrueOrderByDisplayOrderAsc();
            List<TestimonyDTO> dtos = testimonies.stream().map(this::convertToDTO).collect(Collectors.toList());
            return ResponseEntity.ok().body(ApiResponse.success(200, "Testimonies retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error fetching public testimonies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch testimonies", "TESTIMONIES_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getPublicTestimonies(String lang) {
        log.info("Fetching public approved testimonies with lang: {}", lang);
        try {
            List<Testimony> testimonies = testimonyRepository.findByIsApprovedTrueAndIsActiveTrueOrderByDisplayOrderAsc();
            List<TestimonyDTO> dtos = testimonies.stream().map(this::convertToDTO).collect(Collectors.toList());
            publicTranslationService.translateDtoList(dtos, lang);
            return ResponseEntity.ok().body(ApiResponse.success(200, "Testimonies retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error fetching public testimonies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch testimonies", "TESTIMONIES_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getFeaturedTestimonies() {
        log.info("Fetching featured testimonies");
        try {
            List<Testimony> testimonies = testimonyRepository.findByIsFeaturedTrueAndIsApprovedTrueAndIsActiveTrueOrderByDisplayOrderAsc();
            List<TestimonyDTO> dtos = testimonies.stream().map(this::convertToDTO).collect(Collectors.toList());
            return ResponseEntity.ok().body(ApiResponse.success(200, "Featured testimonies retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error fetching featured testimonies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch featured testimonies", "FEATURED_TESTIMONIES_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getFeaturedTestimonies(String lang) {
        log.info("Fetching featured testimonies with lang: {}", lang);
        try {
            List<Testimony> testimonies = testimonyRepository.findByIsFeaturedTrueAndIsApprovedTrueAndIsActiveTrueOrderByDisplayOrderAsc();
            List<TestimonyDTO> dtos = testimonies.stream().map(this::convertToDTO).collect(Collectors.toList());
            publicTranslationService.translateDtoList(dtos, lang);
            return ResponseEntity.ok().body(ApiResponse.success(200, "Featured testimonies retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error fetching featured testimonies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch featured testimonies", "FEATURED_TESTIMONIES_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getPublicTestimoniesPaginated(Integer page, Integer size) {
        log.info("Fetching public testimonies paginated - page: {}, size: {}", page, size);
        try {
            page = page != null ? page : 0;
            size = size != null ? size : 3;

            Specification<Testimony> spec = TestimonySpecification.isApproved(true)
                .and(TestimonySpecification.isActive(true));

            Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending());
            Page<Testimony> testimonyPage = testimonyRepository.findAll(spec, pageable);

            List<TestimonyDTO> dtos = testimonyPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("testimonies", dtos);
            response.put("currentPage", testimonyPage.getNumber());
            response.put("totalItems", testimonyPage.getTotalElements());
            response.put("totalPages", testimonyPage.getTotalPages());
            response.put("pageSize", testimonyPage.getSize());

            return ResponseEntity.ok().body(ApiResponse.success(200, "Testimonies retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching paginated public testimonies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch testimonies", "TESTIMONIES_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getPublicTestimoniesPaginated(Integer page, Integer size, String lang) {
        log.info("Fetching public testimonies paginated with lang: {} - page: {}, size: {}", lang, page, size);
        try {
            page = page != null ? page : 0;
            size = size != null ? size : 3;

            Specification<Testimony> spec = TestimonySpecification.isApproved(true)
                .and(TestimonySpecification.isActive(true));

            Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending());
            Page<Testimony> testimonyPage = testimonyRepository.findAll(spec, pageable);

            List<TestimonyDTO> dtos = testimonyPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            publicTranslationService.translateDtoList(dtos, lang);

            Map<String, Object> response = new HashMap<>();
            response.put("testimonies", dtos);
            response.put("currentPage", testimonyPage.getNumber());
            response.put("totalItems", testimonyPage.getTotalElements());
            response.put("totalPages", testimonyPage.getTotalPages());
            response.put("pageSize", testimonyPage.getSize());

            return ResponseEntity.ok().body(ApiResponse.success(200, "Testimonies retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching paginated public testimonies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch testimonies", "TESTIMONIES_FETCH_FAILED")
            );
        }
    }

    /** Kept so any caller still passing loose parameters keeps working. */
    public ResponseEntity<ApiResponse<?>> listTestimonies(
        Integer pageNumber, Integer pageSize, String sortBy, String sortDirection,
        String authorName, TestimonySource source, Integer rating, Integer minRating, Integer maxRating,
        Boolean isApproved, Boolean isFeatured, Boolean isVerifiedBooking, Boolean isActive,
        String sentimentTag, String customerId, String safariId, String keyword
    ) {
        TestimonyFilter filter = new TestimonyFilter();
        filter.setAuthorName(authorName);
        filter.setSource(source);
        filter.setRating(rating);
        filter.setMinRating(minRating);
        filter.setMaxRating(maxRating);
        filter.setIsApproved(isApproved);
        filter.setIsFeatured(isFeatured);
        filter.setIsVerifiedBooking(isVerifiedBooking);
        filter.setIsActive(isActive);
        filter.setSentimentTag(sentimentTag);
        filter.setCustomerId(customerId);
        filter.setSafariId(safariId);
        filter.setKeyword(keyword);
        return listTestimonies(filter, null, pageNumber, pageSize, sortBy, sortDirection);
    }

    public ResponseEntity<ApiResponse<?>> listTestimonies(
        TestimonyFilter filter,
        Boolean includeStats,
        Integer pageNumber, Integer pageSize, String sortBy, String sortDirection
    ) {
        log.info("Listing testimonies with filters");
        TestimonyFilter active = filter != null ? filter : new TestimonyFilter();
        try {
            pageNumber = (pageNumber != null) ? pageNumber : 0;
            pageSize = (pageSize != null) ? pageSize : 20;
            sortDirection = (sortDirection != null && !sortDirection.isEmpty()) ? sortDirection : "desc";

            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(validatedSortBy).descending()
                : Sort.by(validatedSortBy).ascending();
            Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

            Specification<Testimony> spec = buildSpec(active);

            Page<Testimony> testimonyPage = testimonyRepository.findAll(spec, pageable);

            List<TestimonyListItemDTO> dtos = testimonyPage.getContent().stream()
                .map(this::convertToListItemDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("testimonies", dtos);
            response.put("currentPage", testimonyPage.getNumber());
            response.put("totalItems", testimonyPage.getTotalElements());
            response.put("totalPages", testimonyPage.getTotalPages());
            response.put("pageSize", testimonyPage.getSize());
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

            return ResponseEntity.ok().body(ApiResponse.success(200, "Testimonies retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing testimonies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list testimonies", "TESTIMONIES_LIST_FAILED")
            );
        }
    }

    /**
     * One specification, used by the rows, the cards and the record walk.
     *
     * OR inside a dimension, AND across dimensions — so "Google or TripAdvisor, pending,
     * one or two stars" reads the way somebody means it.
     */
    private Specification<Testimony> buildSpec(TestimonyFilter filter) {
        Specification<Testimony> spec = Specification.unrestricted();

        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            spec = spec.and(TestimonySpecification.searchKeyword(filter.getKeyword()));
        }
        if (filter.getAuthorName() != null && !filter.getAuthorName().isBlank()) {
            spec = spec.and(TestimonySpecification.byAuthorName(filter.getAuthorName()));
        }
        if (!filter.allSources().isEmpty()) {
            spec = spec.and(TestimonySpecification.sourceIn(filter.allSources()));
        }
        if (!filter.allRatings().isEmpty()) {
            spec = spec.and(TestimonySpecification.ratingIn(filter.allRatings()));
        }
        if (filter.getMinRating() != null) spec = spec.and(TestimonySpecification.byMinRating(filter.getMinRating()));
        if (filter.getMaxRating() != null) spec = spec.and(TestimonySpecification.byMaxRating(filter.getMaxRating()));

        Boolean approved = filter.resolvedApproved();
        if (approved != null) spec = spec.and(TestimonySpecification.isApproved(approved));
        Boolean active = filter.resolvedActive();
        if (active != null) spec = spec.and(TestimonySpecification.isActive(active));

        // flags are additive: asking for featured AND verified means both
        if (filter.hasFlag("featured") || Boolean.TRUE.equals(filter.getIsFeatured())) {
            spec = spec.and(TestimonySpecification.isFeatured(true));
        } else if (Boolean.FALSE.equals(filter.getIsFeatured())) {
            spec = spec.and(TestimonySpecification.isFeatured(false));
        }
        if (filter.hasFlag("verified") || Boolean.TRUE.equals(filter.getIsVerifiedBooking())) {
            spec = spec.and(TestimonySpecification.isVerifiedBooking(true));
        } else if (Boolean.FALSE.equals(filter.getIsVerifiedBooking())) {
            spec = spec.and(TestimonySpecification.isVerifiedBooking(false));
        }
        if (filter.hasFlag("answered")) spec = spec.and(TestimonySpecification.hasAdminResponse());

        if (filter.wants("unanswered")) spec = spec.and(TestimonySpecification.hasNoAdminResponse());
        if (filter.wants("unpublishedPraise")) spec = spec.and(TestimonySpecification.isUnpublishedPraise());
        if (filter.wants("critical")) spec = spec.and(TestimonySpecification.isCritical());

        if (filter.getSentimentTag() != null && !filter.getSentimentTag().isBlank()) {
            spec = spec.and(TestimonySpecification.bySentimentTag(filter.getSentimentTag()));
        }
        if (filter.getCreatedAfter() != null) spec = spec.and(TestimonySpecification.createdAfter(filter.getCreatedAfter()));
        if (filter.getCreatedBefore() != null) spec = spec.and(TestimonySpecification.createdBefore(filter.getCreatedBefore()));

        // an id that will not decode is a filter nobody asked for, not a 500
        if (filter.getCustomerId() != null && !filter.getCustomerId().isBlank()) {
            try { spec = spec.and(TestimonySpecification.byCustomerId(idObfuscator.decodeId(filter.getCustomerId()))); }
            catch (Exception e) { log.warn("Invalid customerId: {}", filter.getCustomerId()); }
        }
        if (filter.getSafariId() != null && !filter.getSafariId().isBlank()) {
            try { spec = spec.and(TestimonySpecification.bySafariId(idObfuscator.decodeId(filter.getSafariId()))); }
            catch (Exception e) { log.warn("Invalid safariId: {}", filter.getSafariId()); }
        }
        return spec;
    }

    /**
     * The cards over the review list.
     *
     * Two of these are work queues rather than statistics: praise waiting on approval is
     * money left on the table, and an unanswered complaint is what a prospective customer
     * reads next.
     */
    private Map<String, Object> buildStats(Specification<Testimony> spec) {
        return listStats.of(Testimony.class, spec)
            .total()
            .count("approved", TestimonySpecification.isApproved(true))
            .complement("pending", "approved")
            .count("featured", TestimonySpecification.isFeatured(true))
            .count("verified", TestimonySpecification.isVerifiedBooking(true))
            .count("active", TestimonySpecification.isActive(true))
            .complement("inactive", "active")
            .count("unanswered", TestimonySpecification.hasNoAdminResponse())
            .count("unpublishedPraise", TestimonySpecification.isUnpublishedPraise())
            .count("critical", TestimonySpecification.isCritical())
            .breakdown("bySource", TestimonySource.values(), src -> TestimonySpecification.sourceIn(List.of(src)))
            .breakdown("byRating", new Integer[] {1, 2, 3, 4, 5}, r -> TestimonySpecification.ratingIn(List.of(r)))
            .recency(TestimonySpecification::createdAfter)
            .build();
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    public TestimonyDTO convertToDTO(Testimony testimony) {
        TestimonyDTO dto = TestimonyDTO.builder()
            .id(idObfuscator.encodeId(testimony.getId()))
            .authorName(testimony.getAuthorName())
            .authorTitle(testimony.getAuthorTitle())
            .authorCountry(testimony.getAuthorCountry())
            .authorEmail(testimony.getAuthorEmail())
            .reviewTitle(testimony.getReviewTitle())
            .message(testimony.getMessage())
            .rating(testimony.getRating())
            .adminResponse(testimony.getAdminResponse())
            .adminResponseDate(testimony.getAdminResponseDate())
            .reviewDate(testimony.getReviewDate())
            .source(testimony.getSource())
            .sourceUrl(testimony.getSourceUrl())
            .sourceDisplayName(testimony.getSource() != null ? testimony.getSource().getDisplayName() : null)
            .isVerifiedBooking(testimony.getIsVerifiedBooking())
            .isApproved(testimony.getIsApproved())
            .isFeatured(testimony.getIsFeatured())
            .isActive(testimony.getIsActive())
            .displayOrder(testimony.getDisplayOrder())
            .sentimentTags(testimony.getSentimentTags())
            .imageCount((long) testimony.getImages().size())
            .createdAt(testimony.getCreatedAt())
            .updatedAt(testimony.getUpdatedAt())
            .build();

        if (testimony.getCustomer() != null) {
            dto.setCustomerId(idObfuscator.encodeId(testimony.getCustomer().getId()));
            dto.setCustomerName(testimony.getCustomer().getFullName());
        }
        if (testimony.getSafari() != null) {
            dto.setSafariId(idObfuscator.encodeId(testimony.getSafari().getId()));
            dto.setSafariName(testimony.getSafari().getName());
        }
        if (testimony.getCreatedBy() != null) {
            dto.setCreatedById(idObfuscator.encodeId(testimony.getCreatedBy().getId()));
            dto.setCreatedByName(testimony.getCreatedBy().getFirstName() + " " + testimony.getCreatedBy().getLastName());
        }
        if (testimony.getUpdatedBy() != null) {
            dto.setUpdatedById(idObfuscator.encodeId(testimony.getUpdatedBy().getId()));
            dto.setUpdatedByName(testimony.getUpdatedBy().getFirstName() + " " + testimony.getUpdatedBy().getLastName());
        }

        TestimonyImage primaryImage = testimony.getPrimaryImage();
        if (primaryImage != null && primaryImage.getFileName() != null) {
            dto.setPrimaryImageUrl(storageService.constructImageUrl(idObfuscator.encodeId(primaryImage.getId())));
        }

        return dto;
    }

    private TestimonyListItemDTO convertToListItemDTO(Testimony testimony) {
        TestimonyListItemDTO dto = TestimonyListItemDTO.builder()
            .id(idObfuscator.encodeId(testimony.getId()))
            .authorName(testimony.getAuthorName())
            .authorTitle(testimony.getAuthorTitle())
            .authorCountry(testimony.getAuthorCountry())
            .authorEmail(testimony.getAuthorEmail())
            .reviewTitle(testimony.getReviewTitle())
            .message(testimony.getMessage())
            .rating(testimony.getRating())
            .source(testimony.getSource())
            .sourceUrl(testimony.getSourceUrl())
            .sourceDisplayName(testimony.getSource() != null ? testimony.getSource().getDisplayName() : null)
            .isApproved(testimony.getIsApproved())
            .isFeatured(testimony.getIsFeatured())
            .isVerifiedBooking(testimony.getIsVerifiedBooking())
            .isActive(testimony.getIsActive())
            .displayOrder(testimony.getDisplayOrder())
            .reviewDate(testimony.getReviewDate())
            .imageCount((long) testimony.getImages().size())
            .createdAt(testimony.getCreatedAt())
            .build();

        if (testimony.getCustomer() != null) {
            dto.setCustomerName(testimony.getCustomer().getFirstName() + " " + testimony.getCustomer().getLastName());
        }
        if (testimony.getSafari() != null) {
            dto.setSafariName(testimony.getSafari().getName());
        }

        TestimonyImage primaryImage = testimony.getPrimaryImage();
        if (primaryImage != null && primaryImage.getFileName() != null) {
            dto.setPrimaryImageUrl(storageService.constructImageUrl(idObfuscator.encodeId(primaryImage.getId())));
        }

        return dto;
    }
}
