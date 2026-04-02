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

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "firstName", "lastName", "companyName", "clientType", "phone",
        "email", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    public ResponseEntity<ApiResponse<?>> getRentalClientById(String idObfuscated) {
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

            Long nextId = rentalClientRepository.findNextId(id).orElse(null);
            Long previousId = rentalClientRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = rentalClientRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = rentalClientRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("rentalClient", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

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
        Boolean isActive, String keyword,
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

            Specification<RentalClient> spec = Specification.<RentalClient>unrestricted()
                .and(RentalClientSpecification.hasClientType(clientType))
                .and(RentalClientSpecification.firstNameLike(firstName))
                .and(RentalClientSpecification.lastNameLike(lastName))
                .and(RentalClientSpecification.companyNameLike(companyName))
                .and(RentalClientSpecification.phoneLike(phone))
                .and(RentalClientSpecification.emailLike(email))
                .and(RentalClientSpecification.isActive(isActive))
                .and(RentalClientSpecification.keyword(keyword));

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
}
