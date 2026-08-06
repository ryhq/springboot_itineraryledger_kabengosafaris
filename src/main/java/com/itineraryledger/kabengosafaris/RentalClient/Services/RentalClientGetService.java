package com.itineraryledger.kabengosafaris.RentalClient.Services;

import com.itineraryledger.kabengosafaris.RentalClient.DTOs.RentalClientDTO;
import com.itineraryledger.kabengosafaris.RentalClient.DTOs.RentalClientListItemDTO;
import com.itineraryledger.kabengosafaris.RentalClient.Entity.RentalClient;
import com.itineraryledger.kabengosafaris.RentalClient.Enums.RentalClientType;
import com.itineraryledger.kabengosafaris.RentalClient.Repository.RentalClientRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RentalClientGetService {

    private final RentalClientRepository rentalClientRepository;
    private final IdObfuscator idObfuscator;

    // filter-aware prev/next + the N of M readout, and declarative counters
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "firstName", "lastName", "companyName", "clientType", "phone",
        "email", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    public ResponseEntity<ApiResponse<?>> getRentalClientById(String idObfuscated) {
        return getRentalClientById(idObfuscated, null, null, null, null, null, null, null, null, null);
    }

    /**
     * One client, plus where it sits in the set the caller was looking at — the
     * list's filters and sort decide the walk, not raw id order.
     */
    public ResponseEntity<ApiResponse<?>> getRentalClientById(
        String idObfuscated,
        RentalClientType clientType,
        String firstName,
        String lastName,
        String companyName,
        String phone,
        String email,
        Boolean isActive,
        String keyword,
        String sortBy
    ) {
        log.info("Fetching rental client with ID: {}", idObfuscated);
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            RentalClient client = rentalClientRepository.findById(id).orElse(null);

            if (client == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Rental client not found", "RENTAL_CLIENT_NOT_FOUND")
                );
            }

            RentalClientDTO dto = convertToDTO(client);
            /*
             * Prev/next walks the SAME filtered, sorted set the list showed; the old
             * pair walked raw id order over every client regardless of the filter.
             */
            Specification<RentalClient> navSpec = buildSpec(
                clientType, firstName, lastName, companyName, phone, email, isActive, keyword
            );
            String navSortBy = validateSortField(sortBy);
            if (navSortBy == null) navSortBy = "createdAt";
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                RentalClient.class, navSpec, navSortBy, false, id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("rentalClient", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Rental client retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching rental client: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to retrieve rental client", "RENTAL_CLIENT_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getAllRentalClients(
        RentalClientType clientType, String firstName, String lastName,
        String companyName, String phone, String email,
        Boolean isActive, String keyword, Boolean includeStats,
        Integer page, Integer size, String sortBy, String sortDirection
    ) {
        log.info("Fetching rental clients - page: {}, size: {}", page, size);
        try {
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort sort = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.by(validatedSortBy).descending()
                : Sort.by(validatedSortBy).ascending();

            PageRequest pageRequest = PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 10,
                sort
            );

            Specification<RentalClient> spec = buildSpec(
                clientType, firstName, lastName, companyName, phone, email, isActive, keyword
            );

            Page<RentalClient> clientPage = rentalClientRepository.findAll(spec, pageRequest);

            List<RentalClientDTO> dtos = clientPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("rentalClients", dtos);
            response.put("currentPage", clientPage.getNumber());
            response.put("totalItems", clientPage.getTotalElements());
            response.put("totalPages", clientPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            // counters share the rows' Specification, so cards and table agree
            if (includeStats == null || includeStats) {
                response.put("stats", computeStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Rental clients retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching rental clients", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list rental clients", "RENTAL_CLIENTS_LIST_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getRentalClientsList() {
        log.info("Fetching rental clients lightweight list");
        try {
            List<RentalClient> clients = rentalClientRepository.findAll(Sort.by(Sort.Direction.ASC, "firstName"));
            List<RentalClientListItemDTO> dtos = clients.stream()
                .map(c -> RentalClientListItemDTO.builder()
                    .id(idObfuscator.encodeId(c.getId()))
                    .displayName(c.getDisplayName())
                    .clientType(c.getClientType())
                    .clientTypeDisplayName(c.getClientType() != null ? c.getClientType().getDisplayName() : null)
                    .phone(c.getPhone())
                    .email(c.getEmail())
                    .isActive(c.getIsActive())
                    .build())
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(200, "Rental clients list retrieved successfully", dtos));

        } catch (Exception e) {
            log.error("Error fetching rental clients list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list rental clients", "RENTAL_CLIENTS_LIST_FAILED")
            );
        }
    }

    public RentalClientDTO convertToDTO(RentalClient client) {
        return RentalClientDTO.builder()
            .id(idObfuscator.encodeId(client.getId()))
            .clientType(client.getClientType())
            .clientTypeDisplayName(client.getClientType() != null ? client.getClientType().getDisplayName() : null)
            .displayName(client.getDisplayName())
            .firstName(client.getFirstName())
            .lastName(client.getLastName())
            .companyName(client.getCompanyName())
            .taxId(client.getTaxId())
            .phone(client.getPhone())
            .email(client.getEmail())
            .address(client.getAddress())
            .notes(client.getNotes())
            .isActive(client.getIsActive())
            .createdAt(client.getCreatedAt())
            .updatedAt(client.getUpdatedAt())
            .build();
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /** The ONE place a client filter is expressed — rows, counters and prev/next. */
    private Specification<RentalClient> buildSpec(
        RentalClientType clientType, String firstName, String lastName, String companyName,
        String phone, String email, Boolean isActive, String keyword
    ) {
        return Specification.<RentalClient>unrestricted()
            .and(RentalClientSpecification.hasClientType(clientType))
            .and(RentalClientSpecification.firstNameLike(firstName))
            .and(RentalClientSpecification.lastNameLike(lastName))
            .and(RentalClientSpecification.companyNameLike(companyName))
            .and(RentalClientSpecification.phoneLike(phone))
            .and(RentalClientSpecification.emailLike(email))
            .and(RentalClientSpecification.isActive(isActive))
            .and(RentalClientSpecification.keyword(keyword));
    }

    /** Counters built from the SAME Specification as the rows. */
    private Map<String, Object> computeStats(Specification<RentalClient> base) {
        return listStats.of(RentalClient.class, base)
            .total()
            .count("active", RentalClientSpecification.isActive(true))
            .complement("inactive", "active")
            .breakdown("byType", RentalClientType.values(), RentalClientSpecification::hasClientType)
            .recency(RentalClientSpecification::createdAfter)
            .build();
    }
}
