package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicTestimonyDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Testimony.Entity.Testimony;
import com.itineraryledger.kabengosafaris.Testimony.Entity.TestimonyImage;
import com.itineraryledger.kabengosafaris.Testimony.Enums.TestimonySource;
import com.itineraryledger.kabengosafaris.Testimony.Repository.TestimonyRepository;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyImageServices.TestimonyImageStorageService;
import com.itineraryledger.kabengosafaris.Testimony.Specifications.TestimonySpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PublicTestimonyService {

    private final TestimonyRepository testimonyRepository;
    private final SafariRepository safariRepository;
    private final CustomerEmailRepository customerEmailRepository;
    private final IdObfuscator idObfuscator;
    private final TestimonyImageStorageService storageService;
    private final PublicTranslationService publicTranslationService;

    // ── Public query methods ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getPublicTestimonies(String lang) {
        log.info("Fetching public approved testimonies with lang: {}", lang);
        try {
            List<Testimony> testimonies = testimonyRepository.findByIsApprovedTrueAndIsActiveTrueOrderByDisplayOrderAsc();
            List<PublicTestimonyDTO> dtos = testimonies.stream()
                .map(this::convertToPublicDTO)
                .collect(Collectors.toList());
            publicTranslationService.translateDtoList(dtos, lang);
            return ResponseEntity.ok(ApiResponse.success(200, "Testimonies retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error fetching public testimonies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch testimonies", "TESTIMONIES_FETCH_FAILED"));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getPublicTestimoniesPaginated(Integer page, Integer size, String lang) {
        log.info("Fetching public testimonies paginated with lang: {} - page: {}, size: {}", lang, page, size);
        try {
            page = page != null ? page : 0;
            size = size != null ? size : 3;

            Specification<Testimony> spec = TestimonySpecification.isApproved(true)
                .and(TestimonySpecification.isActive(true));

            Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending());
            Page<Testimony> testimonyPage = testimonyRepository.findAll(spec, pageable);

            List<PublicTestimonyDTO> dtos = testimonyPage.getContent().stream()
                .map(this::convertToPublicDTO)
                .collect(Collectors.toList());

            publicTranslationService.translateDtoList(dtos, lang);

            Map<String, Object> response = new HashMap<>();
            response.put("testimonies", dtos);
            response.put("currentPage", testimonyPage.getNumber());
            response.put("totalItems", testimonyPage.getTotalElements());
            response.put("totalPages", testimonyPage.getTotalPages());
            response.put("pageSize", testimonyPage.getSize());

            return ResponseEntity.ok(ApiResponse.success(200, "Testimonies retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching paginated public testimonies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch testimonies", "TESTIMONIES_FETCH_FAILED"));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getFeaturedTestimonies(String lang) {
        log.info("Fetching featured testimonies with lang: {}", lang);
        try {
            List<Testimony> testimonies = testimonyRepository
                .findByIsFeaturedTrueAndIsApprovedTrueAndIsActiveTrueOrderByDisplayOrderAsc();
            List<PublicTestimonyDTO> dtos = testimonies.stream()
                .map(this::convertToPublicDTO)
                .collect(Collectors.toList());
            publicTranslationService.translateDtoList(dtos, lang);
            return ResponseEntity.ok(ApiResponse.success(200, "Featured testimonies retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error fetching featured testimonies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch featured testimonies", "FEATURED_TESTIMONIES_FETCH_FAILED"));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getTestimonySummary() {
        log.info("Fetching public testimony rating summary");
        try {
            long reviewCount = testimonyRepository.countByIsApprovedTrueAndIsActiveTrueAndRatingNotNull();
            Double rawAvg = testimonyRepository.averageApprovedActiveRating();
            double averageRating = (rawAvg != null && reviewCount > 0)
                ? Math.round(rawAvg * 10.0) / 10.0
                : 0.0;

            Map<String, Object> summary = new HashMap<>();
            summary.put("averageRating", averageRating);
            summary.put("reviewCount", reviewCount);
            summary.put("bestRating", 5);
            summary.put("worstRating", 1);

            return ResponseEntity.ok(ApiResponse.success(200, "Testimony summary retrieved successfully", summary));
        } catch (Exception e) {
            log.error("Error fetching testimony summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch testimony summary", "TESTIMONY_SUMMARY_FAILED"));
        }
    }

    // ── Public testimony submission ──────────────────────────────────

    @Transactional
    public ResponseEntity<ApiResponse<?>> submitPublicTestimony(PublicTestimonyRequest request) {
        log.info("Public testimony submission from: {}", request.authorName());

        try {
            Testimony testimony = Testimony.builder()
                .authorName(request.authorName())
                .authorTitle(request.authorTitle())
                .authorCountry(request.authorCountry())
                .authorEmail(request.authorEmail())
                .message(request.message())
                .rating(request.rating())
                .source(TestimonySource.WEBSITE)
                .reviewDate(LocalDate.now())
                .isVerifiedBooking(false)
                .isApproved(false)
                .isFeatured(false)
                .isActive(false)
                .displayOrder(0)
                .build();

            // Lookup customer by email and link if found
            if (request.authorEmail() != null && !request.authorEmail().isBlank()) {
                try {
                    customerEmailRepository.findByEmail(request.authorEmail().trim())
                        .map(CustomerEmail::getCustomer)
                        .ifPresent(customer -> {
                            testimony.setCustomer(customer);
                            testimony.setIsVerifiedBooking(true);
                            log.info("Linked public testimony to customer via email: {}", request.authorEmail());
                        });
                } catch (Exception e) {
                    log.warn("Error looking up customer by email: {}", request.authorEmail(), e);
                }
            }

            if (request.safariId() != null && !request.safariId().isBlank()) {
                try {
                    Long safariId = idObfuscator.decodeId(request.safariId());
                    Safari safari = safariRepository.findById(safariId).orElse(null);
                    if (safari != null) {
                        testimony.setSafari(safari);
                    }
                } catch (Exception e) {
                    log.warn("Invalid safari ID in public testimony: {}", request.safariId());
                }
            }

            testimonyRepository.save(testimony);

            log.info("Public testimony submitted successfully from: {}", request.authorName());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Thank you for sharing your experience! Your testimonial will be reviewed shortly.",
                    Map.of("submitted", true))
            );

        } catch (Exception e) {
            log.error("Error submitting public testimony", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to submit testimonial. Please try again.", "TESTIMONY_SUBMIT_FAILED")
            );
        }
    }

    // ── DTO conversion ───────────────────────────────────────────────

    public PublicTestimonyDTO convertToPublicDTO(Testimony testimony) {
        String primaryImageUrl = null;
        TestimonyImage primaryImage = testimony.getPrimaryImage();
        if (primaryImage != null && primaryImage.getFileName() != null) {
            primaryImageUrl = storageService.constructFileImageUrl(primaryImage.getFileName());
        } else if (testimony.getImages() != null && !testimony.getImages().isEmpty()) {
            TestimonyImage first = testimony.getActiveImages().stream().findFirst().orElse(null);
            if (first != null && first.getFileName() != null) {
                primaryImageUrl = storageService.constructFileImageUrl(first.getFileName());
            }
        }

        PublicTestimonyDTO dto = PublicTestimonyDTO.builder()
            .authorName(testimony.getAuthorName())
            .authorTitle(testimony.getAuthorTitle())
            .authorCountry(testimony.getAuthorCountry())
            .reviewTitle(testimony.getReviewTitle())
            .message(testimony.getMessage())
            .rating(testimony.getRating())
            .reviewDate(testimony.getReviewDate())
            .sourceDisplayName(testimony.getSource() != null ? testimony.getSource().getDisplayName() : null)
            .sourceUrl(testimony.getSourceUrl())
            .isVerifiedBooking(testimony.getIsVerifiedBooking())
            .primaryImageUrl(primaryImageUrl)
            .imageCount((long) testimony.getImages().size())
            .build();

        if (testimony.getSafari() != null) {
            dto.setSafariName(testimony.getSafari().getName());
        }

        return dto;
    }

    public record PublicTestimonyRequest(
        String authorName,
        String authorTitle,
        String authorCountry,
        String authorEmail,
        String message,
        Integer rating,
        String safariId
    ) {}
}
