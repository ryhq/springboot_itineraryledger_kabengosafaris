package com.itineraryledger.kabengosafaris.RentalClient.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.RentalClient.DTOs.CreateRentalClientDTO;
import com.itineraryledger.kabengosafaris.RentalClient.Entity.RentalClient;
import com.itineraryledger.kabengosafaris.RentalClient.Enums.RentalClientType;
import com.itineraryledger.kabengosafaris.RentalClient.Repository.RentalClientRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreateRentalClientService {

    private final RentalClientRepository rentalClientRepository;
    private final RentalClientGetService rentalClientGetService;

    @Transactional
    @AuditLogAnnotation(action = "CREATE_RENTAL_CLIENT", description = "Creating a new rental client", entityType = "RentalClient")
    public ResponseEntity<ApiResponse<?>> createRentalClient(CreateRentalClientDTO createDTO) {
        log.info("Creating rental client of type: {}", createDTO.getClientType());

        try {
            if (createDTO.getClientType() == RentalClientType.INDIVIDUAL) {
                if (createDTO.getFirstName() == null || createDTO.getFirstName().isBlank()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "First name is required for individual clients", "FIRST_NAME_REQUIRED")
                    );
                }
                if (createDTO.getLastName() == null || createDTO.getLastName().isBlank()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Last name is required for individual clients", "LAST_NAME_REQUIRED")
                    );
                }
            } else if (createDTO.getClientType() == RentalClientType.COMPANY) {
                if (createDTO.getCompanyName() == null || createDTO.getCompanyName().isBlank()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Company name is required for company clients", "COMPANY_NAME_REQUIRED")
                    );
                }
            }

            RentalClient client = RentalClient.builder()
                .clientType(createDTO.getClientType())
                .firstName(createDTO.getFirstName())
                .lastName(createDTO.getLastName())
                .companyName(createDTO.getCompanyName())
                .taxId(createDTO.getTaxId())
                .phone(createDTO.getPhone())
                .email(createDTO.getEmail())
                .address(createDTO.getAddress())
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .notes(createDTO.getNotes())
                .build();

            client = rentalClientRepository.save(client);
            log.info("Rental client created successfully: {} (ID: {})", client.getDisplayName(), client.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Rental client created successfully", rentalClientGetService.convertToDTO(client))
            );

        } catch (Exception e) {
            log.error("Error creating rental client", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create rental client", "RENTAL_CLIENT_CREATE_FAILED")
            );
        }
    }
}
