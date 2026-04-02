package com.itineraryledger.kabengosafaris.RentalClient.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.RentalClient.DTOs.UpdateRentalClientDTO;
import com.itineraryledger.kabengosafaris.RentalClient.Entity.RentalClient;
import com.itineraryledger.kabengosafaris.RentalClient.Enums.RentalClientType;
import com.itineraryledger.kabengosafaris.RentalClient.Repository.RentalClientRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateRentalClientService {

    private final RentalClientRepository rentalClientRepository;
    private final RentalClientGetService rentalClientGetService;
    private final IdObfuscator idObfuscator;

    @Transactional
    @AuditLogAnnotation(action = "UPDATE_RENTAL_CLIENT", description = "Updating rental client", entityType = "RentalClient")
    public ResponseEntity<ApiResponse<?>> updateRentalClient(String idObfuscated, UpdateRentalClientDTO updateDTO) {
        log.info("Updating rental client: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            RentalClient client = rentalClientRepository.findById(id).orElse(null);

            if (client == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Rental client not found", "RENTAL_CLIENT_NOT_FOUND")
                );
            }

            if (updateDTO.getClientType() != null) client.setClientType(updateDTO.getClientType());
            if (updateDTO.getFirstName() != null) client.setFirstName(updateDTO.getFirstName());
            if (updateDTO.getLastName() != null) client.setLastName(updateDTO.getLastName());
            if (updateDTO.getCompanyName() != null) client.setCompanyName(updateDTO.getCompanyName());
            if (updateDTO.getTaxId() != null) client.setTaxId(updateDTO.getTaxId());
            if (updateDTO.getPhone() != null) client.setPhone(updateDTO.getPhone());
            if (updateDTO.getEmail() != null) client.setEmail(updateDTO.getEmail());
            if (updateDTO.getAddress() != null) client.setAddress(updateDTO.getAddress());
            if (updateDTO.getIsActive() != null) client.setIsActive(updateDTO.getIsActive());
            if (updateDTO.getNotes() != null) client.setNotes(updateDTO.getNotes());

            // Validate type-specific required fields after updates
            if (client.getClientType() == RentalClientType.INDIVIDUAL) {
                if (client.getFirstName() == null || client.getFirstName().isBlank()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "First name is required for individual clients", "FIRST_NAME_REQUIRED")
                    );
                }
                if (client.getLastName() == null || client.getLastName().isBlank()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Last name is required for individual clients", "LAST_NAME_REQUIRED")
                    );
                }
            } else if (client.getClientType() == RentalClientType.COMPANY) {
                if (client.getCompanyName() == null || client.getCompanyName().isBlank()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Company name is required for company clients", "COMPANY_NAME_REQUIRED")
                    );
                }
            }

            client = rentalClientRepository.save(client);
            log.info("Rental client updated successfully: {}", client.getDisplayName());

            return ResponseEntity.ok(
                ApiResponse.success(200, "Rental client updated successfully", rentalClientGetService.convertToDTO(client))
            );

        } catch (Exception e) {
            log.error("Error updating rental client: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update rental client", "RENTAL_CLIENT_UPDATE_FAILED")
            );
        }
    }
}
