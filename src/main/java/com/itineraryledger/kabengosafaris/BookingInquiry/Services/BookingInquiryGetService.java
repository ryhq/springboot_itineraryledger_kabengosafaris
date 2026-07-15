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

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "code", "firstName", "lastName", "email", "country", "status",
        "budgetCategory", "tripType", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public BookingInquiryGetService(BookingInquiryRepository repository, IdObfuscator idObfuscator) {
        this.repository = repository;
        this.idObfuscator = idObfuscator;
    }

    public ResponseEntity<ApiResponse<?>> getInquiryById(String idObfuscated) {
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

            Long nextId = repository.findNextId(id).orElse(null);
            Long previousId = repository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = repository.findFirstId().orElse(null);
            if (previousId == null) previousId = repository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("inquiry", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(ApiResponse.success(200, "Booking inquiry retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching booking inquiry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch booking inquiry", "INQUIRY_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> listInquiries(
        Integer pageNumber, Integer pageSize, String sortBy, String sortDirection,
        InquiryStatus status, BudgetCategory budgetCategory, TripType tripType,
        String email, String country, LocalDateTime createdAfter, LocalDateTime createdBefore,
        String keyword
    ) {
        log.info("Listing booking inquiries with filters");
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

            Specification<BookingInquiry> spec = Specification.unrestricted();
            if (status != null) spec = spec.and(BookingInquirySpecification.byStatus(status));
            if (budgetCategory != null) spec = spec.and(BookingInquirySpecification.byBudgetCategory(budgetCategory));
            if (tripType != null) spec = spec.and(BookingInquirySpecification.byTripType(tripType));
            if (email != null && !email.isEmpty()) spec = spec.and(BookingInquirySpecification.byEmail(email));
            if (country != null && !country.isEmpty()) spec = spec.and(BookingInquirySpecification.byCountry(country));
            if (createdAfter != null) spec = spec.and(BookingInquirySpecification.createdAfter(createdAfter));
            if (createdBefore != null) spec = spec.and(BookingInquirySpecification.createdBefore(createdBefore));
            if (keyword != null && !keyword.isEmpty()) spec = spec.and(BookingInquirySpecification.searchKeyword(keyword));

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

            return ResponseEntity.ok().body(ApiResponse.success(200, "Booking inquiries retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing booking inquiries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list booking inquiries", "INQUIRIES_LIST_FAILED")
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
