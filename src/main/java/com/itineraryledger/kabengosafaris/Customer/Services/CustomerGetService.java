package com.itineraryledger.kabengosafaris.Customer.Services;

import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerListItemDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerSource;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Customer.Specifications.CustomerSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CustomerGetService - Service for retrieving customers with filtering, pagination, and sorting
 *
 * NOTE: getCustomerByCode and searchCustomers have been removed.
 * Use getAllCustomers with the 'code' or 'keyword' filter instead.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerGetService {

    private final CustomerRepository customerRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "code", "firstName", "lastName", "companyName", "customerType", "nationality",
        "country", "city", "source", "isVip", "isBlacklisted", "isActive",
        "totalBookings", "totalSpent", "lastBookingDate", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    /**
     * Get a single customer by obfuscated ID
     */
    public ResponseEntity<ApiResponse<?>> getCustomerById(String idObfuscated) {
        log.info("Fetching customer with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode customer ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid customer ID", "INVALID_CUSTOMER_ID")
                );
            }

            Customer customer = customerRepository.findById(id).orElse(null);
            if (customer == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Customer not found", "CUSTOMER_NOT_FOUND")
                );
            }

            CustomerDTO customerDTO = convertToDTO(customer);

            // Circular navigation
            Long nextId = customerRepository.findNextId(id).orElse(null);
            Long previousId = customerRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = customerRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = customerRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("customer", customerDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Customer retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching customer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch customer", "CUSTOMER_FETCH_FAILED")
            );
        }
    }

    /**
     * Get all customers with pagination, sorting, and filtering
     *
     * NOTE: To filter by code, use the 'code' parameter.
     * To search by keyword, use the 'keyword' parameter.
     */
    public ResponseEntity<ApiResponse<?>> getAllCustomers(
            String name,
            String email,
            String phone,
            String code,
            CustomerType customerType,
            CustomerSource source,
            String nationality,
            String country,
            String city,
            Boolean isActive,
            Boolean isVip,
            Boolean isBlacklisted,
            Boolean hasBookings,
            BigDecimal minTotalSpent,
            BigDecimal maxTotalSpent,
            String keyword,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {
        log.info("Fetching all customers with filters");

        try {
            // Build specification
            Specification<Customer> spec = Specification.unrestricted();

            if (name != null && !name.isEmpty()) {
                spec = spec.and(CustomerSpecification.nameLike(name));
            }
            if (email != null && !email.isEmpty()) {
                spec = spec.and(CustomerSpecification.emailLike(email));
            }
            if (phone != null && !phone.isEmpty()) {
                spec = spec.and(CustomerSpecification.phoneLike(phone));
            }
            if (code != null && !code.isEmpty()) {
                spec = spec.and(CustomerSpecification.codeLike(code));
            }
            if (customerType != null) {
                spec = spec.and(CustomerSpecification.hasCustomerType(customerType));
            }
            if (source != null) {
                spec = spec.and(CustomerSpecification.hasSource(source));
            }
            if (nationality != null && !nationality.isEmpty()) {
                spec = spec.and(CustomerSpecification.nationalityLike(nationality));
            }
            if (country != null && !country.isEmpty()) {
                spec = spec.and(CustomerSpecification.countryLike(country));
            }
            if (city != null && !city.isEmpty()) {
                spec = spec.and(CustomerSpecification.cityLike(city));
            }
            if (isActive != null) {
                spec = spec.and(CustomerSpecification.isActive(isActive));
            }
            if (isVip != null) {
                spec = spec.and(CustomerSpecification.isVip(isVip));
            }
            if (isBlacklisted != null) {
                spec = spec.and(CustomerSpecification.isBlacklisted(isBlacklisted));
            }
            if (hasBookings != null) {
                if (hasBookings) {
                    spec = spec.and(CustomerSpecification.hasBookings());
                } else {
                    spec = spec.and(CustomerSpecification.hasNoBookings());
                }
            }
            if (minTotalSpent != null) {
                spec = spec.and(CustomerSpecification.minTotalSpent(minTotalSpent));
            }
            if (maxTotalSpent != null) {
                spec = spec.and(CustomerSpecification.maxTotalSpent(maxTotalSpent));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(CustomerSpecification.searchKeyword(keyword));
            }

            // Pagination
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            // Fetch customers
            Page<Customer> customerPage = customerRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<CustomerListItemDTO> customerDTOs = customerPage.getContent().stream()
                .map(this::convertToListItemDTO)
                .collect(Collectors.toList());

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("customers", customerDTOs);
            response.put("currentPage", customerPage.getNumber());
            response.put("totalItems", customerPage.getTotalElements());
            response.put("totalPages", customerPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Customers retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching customers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch customers", "CUSTOMERS_FETCH_FAILED")
            );
        }
    }

    /**
     * Convert Customer entity to CustomerDTO (full details)
     */
    private CustomerDTO convertToDTO(Customer customer) {
        // Get primary email and phone for display
        String primaryEmail = customer.getPrimaryEmail();
        String primaryPhone = customer.getPrimaryPhone();

        return CustomerDTO.builder()
            .id(idObfuscator.encodeId(customer.getId()))
            .code(customer.getCode())
            .customerType(customer.getCustomerType())
            .customerTypeDisplayName(customer.getCustomerType().getDisplayName())
            .customerTypeDescription(customer.getCustomerType().getDescription())
            .title(customer.getTitle())
            .salutation(customer.getSalutation())
            .firstName(customer.getFirstName())
            .lastName(customer.getLastName())
            .companyName(customer.getCompanyName())
            .displayName(customer.getDisplayName())
            .primaryEmail(primaryEmail)
            .primaryPhone(primaryPhone)
            .nationality(customer.getNationality())
            .residency(customer.getResidency())
            .passportNumber(customer.getPassportNumber())
            .passportExpiry(customer.getPassportExpiry())
            .dateOfBirth(customer.getDateOfBirth())
            .passportExpiringSoon(isPassportExpiringSoon(customer.getPassportExpiry()))
            .address(customer.getAddress())
            .city(customer.getCity())
            .state(customer.getState())
            .country(customer.getCountry())
            .postalCode(customer.getPostalCode())
            .fullAddress(buildFullAddress(customer))
            .preferredLanguage(customer.getPreferredLanguage())
            .preferredCurrency(customer.getPreferredCurrency())
            .source(customer.getSource())
            .sourceDisplayName(customer.getSource() != null ? customer.getSource().getDisplayName() : null)
            .referredBy(customer.getReferredBy())
            .dietaryRequirements(customer.getDietaryRequirements())
            .medicalConditions(customer.getMedicalConditions())
            .specialRequests(customer.getSpecialRequests())
            .interests(customer.getInterests())
            .internalNotes(customer.getInternalNotes())
            .isVip(customer.getIsVip())
            .isBlacklisted(customer.getIsBlacklisted())
            .blacklistReason(customer.getBlacklistReason())
            .totalBookings(customer.getTotalBookings())
            .totalSpent(customer.getTotalSpent())
            .lastBookingDate(customer.getLastBookingDate())
            .canBook(customer.canBook())
            .isActive(customer.getIsActive())
            .createdBy(customer.getCreatedBy())
            .createdAt(customer.getCreatedAt())
            .updatedAt(customer.getUpdatedAt())
            .emailCount(customer.getEmails() != null ? customer.getEmails().size() : 0)
            .phoneCount(customer.getPhones() != null ? customer.getPhones().size() : 0)
            .documentCount(customer.getDocuments() != null ? customer.getDocuments().size() : 0)
            .noteCount(customer.getNotes() != null ? customer.getNotes().size() : 0)
            .build();
    }

    /**
     * Convert Customer entity to CustomerListItemDTO (lightweight)
     */
    private CustomerListItemDTO convertToListItemDTO(Customer customer) {
        // Get primary email and phone for display
        String primaryEmail = customer.getPrimaryEmail();
        String primaryPhone = customer.getPrimaryPhone();

        return CustomerListItemDTO.builder()
            .id(idObfuscator.encodeId(customer.getId()))
            .code(customer.getCode())
            .customerType(customer.getCustomerType())
            .customerTypeDisplayName(customer.getCustomerType().getDisplayName())
            .displayName(customer.getDisplayName())
            .primaryEmail(primaryEmail)
            .primaryPhone(primaryPhone)
            .nationality(customer.getNationality())
            .country(customer.getCountry())
            .source(customer.getSource())
            .sourceDisplayName(customer.getSource() != null ? customer.getSource().getDisplayName() : null)
            .isVip(customer.getIsVip())
            .isBlacklisted(customer.getIsBlacklisted())
            .isActive(customer.getIsActive())
            .canBook(customer.canBook())
            .totalBookings(customer.getTotalBookings())
            .totalSpent(customer.getTotalSpent())
            .lastBookingDate(customer.getLastBookingDate())
            .createdAt(customer.getCreatedAt())
            .build();
    }

    private Boolean isPassportExpiringSoon(LocalDate passportExpiry) {
        if (passportExpiry == null) return null;
        return passportExpiry.isBefore(LocalDate.now().plusMonths(6));
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    private String buildFullAddress(Customer customer) {
        StringBuilder sb = new StringBuilder();
        if (customer.getAddress() != null && !customer.getAddress().isBlank()) {
            sb.append(customer.getAddress());
        }
        if (customer.getCity() != null && !customer.getCity().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(customer.getCity());
        }
        if (customer.getState() != null && !customer.getState().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(customer.getState());
        }
        if (customer.getPostalCode() != null && !customer.getPostalCode().isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(customer.getPostalCode());
        }
        if (customer.getCountry() != null && !customer.getCountry().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(customer.getCountry());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
