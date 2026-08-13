package com.itineraryledger.kabengosafaris.BookingInquiry.Services;

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

import com.itineraryledger.kabengosafaris.BookingInquiry.DTOs.BookingInquiryDTO;
import com.itineraryledger.kabengosafaris.BookingInquiry.DTOs.BookingInquiryListItemDTO;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.BookingInquiry;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.InquiryStatus;
import com.itineraryledger.kabengosafaris.BookingInquiry.Repository.BookingInquiryRepository;
import com.itineraryledger.kabengosafaris.BookingInquiry.Specifications.BookingInquiryFilter;
import com.itineraryledger.kabengosafaris.BookingInquiry.Specifications.BookingInquirySpecification;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripInterest;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(readOnly = true)
public class BookingInquiryGetService {

    private final BookingInquiryRepository repository;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "code", "firstName", "lastName", "email", "country", "status",
        "budgetCategory", "tripType", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public BookingInquiryGetService(
        BookingInquiryRepository repository,
        IdObfuscator idObfuscator,
        com.itineraryledger.kabengosafaris.Response.ListStats listStats,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.repository = repository;
        this.idObfuscator = idObfuscator;
        this.listStats = listStats;
        this.recordNavigation = recordNavigation;
    }

    public ResponseEntity<ApiResponse<?>> getInquiryById(String idObfuscated) {
        return getInquiryById(idObfuscated, null, null, null);
    }

    /**
     * One inquiry, plus where it sits in the set the caller was looking at.
     *
     * Paging out of an "unanswered" list must stay among unanswered ones — arrows
     * that traverse a different set from the one on screen are worse than none.
     */
    public ResponseEntity<ApiResponse<?>> getInquiryById(
        String idObfuscated,
        BookingInquiryFilter filter,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching booking inquiry with ID: {}", idObfuscated);
        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode inquiry ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid inquiry ID", "INVALID_INQUIRY_ID")
                );
            }

            BookingInquiry inquiry = repository.findById(id).orElse(null);
            if (inquiry == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Booking inquiry not found", "INQUIRY_NOT_FOUND")
                );
            }

            BookingInquiryDTO dto = convertToDTO(inquiry);

            Specification<BookingInquiry> navSpec = buildSpec(
                filter != null ? filter : new BookingInquiryFilter());
            String navSortBy = validateSortField(sortBy) != null
                ? validateSortField(sortBy) : "createdAt";
            Map<String, Object> nav = recordNavigation.navigate(
                BookingInquiry.class, navSpec, navSortBy, "asc".equalsIgnoreCase(sortDirection), id);
            Long navNext = (Long) nav.get("nextRawId");
            Long navPrevious = (Long) nav.get("previousRawId");

            Long nextId = repository.findNextId(id).orElse(null);
            Long previousId = repository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = repository.findFirstId().orElse(null);
            if (previousId == null) previousId = repository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("inquiry", dto);
            response.put("nextId", navNext != null ? idObfuscator.encodeId(navNext) : null);
            response.put("previousId", navPrevious != null ? idObfuscator.encodeId(navPrevious) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok().body(ApiResponse.success(200, "Booking inquiry retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching booking inquiry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch booking inquiry", "INQUIRY_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> listInquiries(
        BookingInquiryFilter filter,
        Boolean includeStats,
        Integer pageNumber, Integer pageSize, String sortBy, String sortDirection
    ) {
        log.info("Listing booking inquiries with filters");
        try {
            pageNumber = (pageNumber != null) ? pageNumber : 0;
            // clamp: an unbounded size is a way to ask for the whole table by accident
            pageSize = (pageSize != null && pageSize > 0) ? Math.min(pageSize, 100) : 20;
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

            Specification<BookingInquiry> spec = buildSpec(
                filter != null ? filter : new BookingInquiryFilter());

            Page<BookingInquiry> page = repository.findAll(spec, pageable);

            List<BookingInquiryListItemDTO> dtos = page.getContent().stream()
                .map(this::convertToListItemDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("inquiries", dtos);
            response.put("currentPage", page.getNumber());
            response.put("totalItems", page.getTotalElements());
            response.put("totalPages", page.getTotalPages());
            response.put("pageSize", page.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection);
            /*
             * Counters for the WHOLE filtered set, from the same specification as
             * the rows, so a card and the table under it cannot disagree.
             */
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok().body(ApiResponse.success(200, "Booking inquiries retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing booking inquiries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list booking inquiries", "INQUIRIES_LIST_FAILED")
            );
        }
    }

    /**
     * ONE specification, shared by the rows, the counters and the record walk.
     *
     * Every dimension ORs inside itself and ANDs across — "luxury or mid-range,
     * from Germany, still unanswered" is one question.
     */
    private Specification<BookingInquiry> buildSpec(BookingInquiryFilter filter) {
        Specification<BookingInquiry> spec = Specification.<BookingInquiry>unrestricted()
            .and(BookingInquirySpecification.byStatuses(filter.allStatuses()))
            .and(BookingInquirySpecification.byBudgetCategories(filter.allBudgetCategories()))
            .and(BookingInquirySpecification.byTripTypes(filter.allTripTypes()))
            .and(BookingInquirySpecification.byCountries(filter.allCountries()))
            .and(BookingInquirySpecification.startingAfter(filter.getStartingAfter()))
            .and(BookingInquirySpecification.startingBefore(filter.getStartingBefore()));

        if (filter.getEmail() != null && !filter.getEmail().isEmpty()) {
            spec = spec.and(BookingInquirySpecification.byEmail(filter.getEmail()));
        }
        if (filter.getCreatedAfter() != null) {
            spec = spec.and(BookingInquirySpecification.createdAfter(filter.getCreatedAfter()));
        }
        if (filter.getCreatedBefore() != null) {
            spec = spec.and(BookingInquirySpecification.createdBefore(filter.getCreatedBefore()));
        }
        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            spec = spec.and(BookingInquirySpecification.searchKeyword(filter.getKeyword()));
        }
        if (filter.getItineraryId() != null && !filter.getItineraryId().isBlank()) {
            spec = spec.and(BookingInquirySpecification.byItineraryId(decode(filter.getItineraryId())));
        }
        if (filter.getCustomerId() != null && !filter.getCustomerId().isBlank()) {
            spec = spec.and(BookingInquirySpecification.byCustomerId(decode(filter.getCustomerId())));
        }

        // the work queues, OR'd: "show me what needs doing"
        Specification<BookingInquiry> queue = null;
        if (filter.wants("unanswered")) queue = or(queue, BookingInquirySpecification.unanswered());
        if (filter.wants("stale")) queue = or(queue, BookingInquirySpecification.staleFor(3));
        if (filter.wants("noPhone")) queue = or(queue, BookingInquirySpecification.missingPhone());
        if (filter.wants("travellingSoon")) queue = or(queue, BookingInquirySpecification.travellingWithin(30));
        if (filter.wants("converted")) queue = or(queue, BookingInquirySpecification.converted(true));
        if (queue != null) spec = spec.and(queue);

        return spec;
    }

    private Specification<BookingInquiry> or(
            Specification<BookingInquiry> spec, Specification<BookingInquiry> extra) {
        return spec == null ? extra : spec.or(extra);
    }

    private Long decode(String obfuscated) {
        try {
            return idObfuscator.decodeId(obfuscated);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * The cards that head the list, every one of them reachable as a filter.
     *
     * They are all about what needs doing, because that is what an inquiry list
     * is for: somebody asked for a safari and the only question is whether we
     * have answered.
     */
    private Map<String, Object> buildStats(Specification<BookingInquiry> spec) {
        return listStats.of(BookingInquiry.class, spec)
            .total()
            .breakdown("byStatus", InquiryStatus.values(), BookingInquirySpecification::byStatus)
            .count("unanswered", BookingInquirySpecification.unanswered())
            .count("stale", BookingInquirySpecification.staleFor(3))
            .count("travellingSoon", BookingInquirySpecification.travellingWithin(30))
            .count("noPhone", BookingInquirySpecification.missingPhone())
            .count("converted", BookingInquirySpecification.converted(true))
            .recency(BookingInquirySpecification::createdAfter)
            .build();
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    public BookingInquiryDTO convertToDTO(BookingInquiry inquiry) {
        BookingInquiryDTO dto = BookingInquiryDTO.builder()
            .id(idObfuscator.encodeId(inquiry.getId()))
            .code(inquiry.getCode())
            .firstName(inquiry.getFirstName())
            .lastName(inquiry.getLastName())
            .displayName(inquiry.getDisplayName())
            .email(inquiry.getEmail())
            .phone(inquiry.getPhone())
            .country(inquiry.getCountry())
            .adults(inquiry.getAdults())
            .children(inquiry.getChildren())
            .totalTravelers(inquiry.getTotalTravelers())
            .preferredStartDate(inquiry.getPreferredStartDate())
            .preferredEndDate(inquiry.getPreferredEndDate())
            .budgetCategory(inquiry.getBudgetCategory())
            .budgetCategoryDisplayName(inquiry.getBudgetCategory() != null ? inquiry.getBudgetCategory().getDisplayName() : null)
            .tripType(inquiry.getTripType())
            .tripTypeDisplayName(inquiry.getTripType() != null ? inquiry.getTripType().getDisplayName() : null)
            .interests(inquiry.getInterests())
            .interestDisplayNames(inquiry.getInterests() != null
                ? inquiry.getInterests().stream().map(TripInterest::getDisplayName).collect(java.util.stream.Collectors.toList())
                : java.util.List.of())
            .preferredDurationDays(inquiry.getPreferredDurationDays())
            .destinationParkIds(inquiry.getDestinationParks() != null
                ? inquiry.getDestinationParks().stream().map(p -> idObfuscator.encodeId(p.getId())).collect(java.util.stream.Collectors.toList())
                : java.util.List.of())
            .destinationParkNames(inquiry.getDestinationParks() != null
                ? inquiry.getDestinationParks().stream().map(com.itineraryledger.kabengosafaris.Park.Park::getName).collect(java.util.stream.Collectors.toList())
                : java.util.List.of())
            .specialRequests(inquiry.getSpecialRequests())
            .message(inquiry.getMessage())
            .status(inquiry.getStatus())
            .statusDisplayName(inquiry.getStatus() != null ? inquiry.getStatus().getDisplayName() : null)
            .source(inquiry.getSource())
            .preferredLocale(inquiry.getPreferredLocale())
            .itineraryName(inquiry.getItineraryName())
            .adminNotes(inquiry.getAdminNotes())
            .createdAt(inquiry.getCreatedAt())
            .updatedAt(inquiry.getUpdatedAt())
            .contactedAt(inquiry.getContactedAt())
            .convertedAt(inquiry.getConvertedAt())
            .build();

        if (inquiry.getItinerary() != null) {
            dto.setItineraryId(idObfuscator.encodeId(inquiry.getItinerary().getId()));
        }
        if (inquiry.getCustomer() != null) {
            dto.setCustomerId(idObfuscator.encodeId(inquiry.getCustomer().getId()));
            dto.setCustomerName(inquiry.getCustomer().getFullName());
        }

        return dto;
    }

    private BookingInquiryListItemDTO convertToListItemDTO(BookingInquiry inquiry) {
        BookingInquiryListItemDTO dto = BookingInquiryListItemDTO.builder()
            .id(idObfuscator.encodeId(inquiry.getId()))
            .code(inquiry.getCode())
            .displayName(inquiry.getDisplayName())
            .email(inquiry.getEmail())
            .country(inquiry.getCountry())
            .totalTravelers(inquiry.getTotalTravelers())
            .budgetCategoryDisplayName(inquiry.getBudgetCategory() != null ? inquiry.getBudgetCategory().getDisplayName() : null)
            .tripTypeDisplayName(inquiry.getTripType() != null ? inquiry.getTripType().getDisplayName() : null)
            .status(inquiry.getStatus())
            .statusDisplayName(inquiry.getStatus() != null ? inquiry.getStatus().getDisplayName() : null)
            .itineraryName(inquiry.getItineraryName())
            .createdAt(inquiry.getCreatedAt())
            .build();

        return dto;
    }
}
