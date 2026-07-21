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
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Testimony.Entity.Testimony;
import com.itineraryledger.kabengosafaris.Testimony.Entity.TestimonyImage;
import com.itineraryledger.kabengosafaris.Testimony.Enums.TestimonySource;
import com.itineraryledger.kabengosafaris.Testimony.Repository.TestimonyRepository;
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
        PublicTranslationService publicTranslationService
    ) {
        this.testimonyRepository = testimonyRepository;
        this.idObfuscator = idObfuscator;
        this.storageService = storageService;
        this.publicTranslationService = publicTranslationService;
    }

    public ResponseEntity<ApiResponse<?>> getTestimonyById(String idObfuscated) {
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

            Long nextId = testimonyRepository.findNextId(id).orElse(null);
            Long previousId = testimonyRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = testimonyRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = testimonyRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("testimony", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

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

    public ResponseEntity<ApiResponse<?>> listTestimonies(
        Integer pageNumber, Integer pageSize, String sortBy, String sortDirection,
        String authorName, TestimonySource source, Integer rating, Integer minRating, Integer maxRating,
        Boolean isApproved, Boolean isFeatured, Boolean isVerifiedBooking, Boolean isActive,
        String sentimentTag, String customerId, String safariId, String keyword
    ) {
        log.info("Listing testimonies with filters");
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

            Specification<Testimony> spec = Specification.unrestricted();
            if (authorName != null && !authorName.isEmpty()) spec = spec.and(TestimonySpecification.byAuthorName(authorName));
            if (source != null) spec = spec.and(TestimonySpecification.bySource(source));
            if (rating != null) spec = spec.and(TestimonySpecification.byRating(rating));
            if (minRating != null) spec = spec.and(TestimonySpecification.byMinRating(minRating));
            if (maxRating != null) spec = spec.and(TestimonySpecification.byMaxRating(maxRating));
            if (isApproved != null) spec = spec.and(TestimonySpecification.isApproved(isApproved));
            if (isFeatured != null) spec = spec.and(TestimonySpecification.isFeatured(isFeatured));
            if (isVerifiedBooking != null) spec = spec.and(TestimonySpecification.isVerifiedBooking(isVerifiedBooking));
            if (isActive != null) spec = spec.and(TestimonySpecification.isActive(isActive));
            if (sentimentTag != null && !sentimentTag.isEmpty()) spec = spec.and(TestimonySpecification.bySentimentTag(sentimentTag));
            if (keyword != null && !keyword.isEmpty()) spec = spec.and(TestimonySpecification.searchKeyword(keyword));
            if (customerId != null && !customerId.isEmpty()) {
                try { spec = spec.and(TestimonySpecification.byCustomerId(idObfuscator.decodeId(customerId))); }
                catch (Exception e) { log.warn("Invalid customerId: {}", customerId); }
            }
            if (safariId != null && !safariId.isEmpty()) {
                try { spec = spec.and(TestimonySpecification.bySafariId(idObfuscator.decodeId(safariId))); }
                catch (Exception e) { log.warn("Invalid safariId: {}", safariId); }
            }

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

            return ResponseEntity.ok().body(ApiResponse.success(200, "Testimonies retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing testimonies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list testimonies", "TESTIMONIES_LIST_FAILED")
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
