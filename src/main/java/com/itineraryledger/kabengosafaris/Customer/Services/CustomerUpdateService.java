package com.itineraryledger.kabengosafaris.Customer.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.UpdateCustomerDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * CustomerUpdateService - Service for updating customers
 *
 * NOTE: Emails and phones are NOT updated through this service.
 * They must be updated using their respective services (CustomerEmailService, CustomerPhoneService).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CustomerUpdateService {

    private final CustomerRepository customerRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Update an existing customer
     */
    @AuditLogAnnotation(action = "UPDATE_CUSTOMER", description = "Updating a customer", entityType = "Customer")
    public ResponseEntity<ApiResponse<?>> updateCustomer(String idObfuscated, UpdateCustomerDTO updateDTO) {
        log.info("Updating customer with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode customer ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid customer ID", "INVALID_CUSTOMER_ID")
                );
            }

            // Find customer
            Customer customer = customerRepository.findById(id).orElse(null);
            if (customer == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Customer not found", "CUSTOMER_NOT_FOUND")
                );
            }

            // Update fields if provided
            if (updateDTO.getCustomerType() != null) {
                customer.setCustomerType(updateDTO.getCustomerType());
            }
            if (updateDTO.getTitle() != null) {
                customer.setTitle(updateDTO.getTitle());
            }
            if (updateDTO.getSalutation() != null) {
                customer.setSalutation(updateDTO.getSalutation());
            }
            if (updateDTO.getFirstName() != null) {
                customer.setFirstName(updateDTO.getFirstName());
            }
            if (updateDTO.getLastName() != null) {
                customer.setLastName(updateDTO.getLastName());
            }
            if (updateDTO.getCompanyName() != null) {
                customer.setCompanyName(updateDTO.getCompanyName());
            }
            if (updateDTO.getNationality() != null) {
                customer.setNationality(updateDTO.getNationality());
            }
            if (updateDTO.getResidency() != null) {
                customer.setResidency(updateDTO.getResidency());
            }
            if (updateDTO.getPassportNumber() != null) {
                customer.setPassportNumber(updateDTO.getPassportNumber());
            }
            if (updateDTO.getPassportExpiry() != null) {
                customer.setPassportExpiry(updateDTO.getPassportExpiry());
            }
            if (updateDTO.getDateOfBirth() != null) {
                customer.setDateOfBirth(updateDTO.getDateOfBirth());
            }
            if (updateDTO.getAddress() != null) {
                customer.setAddress(updateDTO.getAddress());
            }
            if (updateDTO.getCity() != null) {
                customer.setCity(updateDTO.getCity());
            }
            if (updateDTO.getState() != null) {
                customer.setState(updateDTO.getState());
            }
            if (updateDTO.getCountry() != null) {
                customer.setCountry(updateDTO.getCountry());
            }
            if (updateDTO.getPostalCode() != null) {
                customer.setPostalCode(updateDTO.getPostalCode());
            }
            if (updateDTO.getPreferredLanguage() != null) {
                customer.setPreferredLanguage(updateDTO.getPreferredLanguage());
            }
            if (updateDTO.getPreferredCurrency() != null) {
                customer.setPreferredCurrency(updateDTO.getPreferredCurrency());
            }
            if (updateDTO.getSource() != null) {
                customer.setSource(updateDTO.getSource());
            }
            if (updateDTO.getReferredBy() != null) {
                customer.setReferredBy(updateDTO.getReferredBy());
            }
            if (updateDTO.getDietaryRequirements() != null) {
                customer.setDietaryRequirements(updateDTO.getDietaryRequirements());
            }
            if (updateDTO.getMedicalConditions() != null) {
                customer.setMedicalConditions(updateDTO.getMedicalConditions());
            }
            if (updateDTO.getSpecialRequests() != null) {
                customer.setSpecialRequests(updateDTO.getSpecialRequests());
            }
            if (updateDTO.getInterests() != null) {
                customer.setInterests(updateDTO.getInterests());
            }
            if (updateDTO.getInternalNotes() != null) {
                customer.setInternalNotes(updateDTO.getInternalNotes());
            }
            if (updateDTO.getIsVip() != null) {
                customer.setIsVip(updateDTO.getIsVip());
            }
            if (updateDTO.getIsBlacklisted() != null) {
                customer.setIsBlacklisted(updateDTO.getIsBlacklisted());
            }
            if (updateDTO.getBlacklistReason() != null) {
                customer.setBlacklistReason(updateDTO.getBlacklistReason());
            }
            if (updateDTO.getIsActive() != null) {
                customer.setIsActive(updateDTO.getIsActive());
            }

            // Validate customer type requirements after update
            String validationError = validateCustomerTypeRequirements(customer);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, validationError, "VALIDATION_ERROR")
                );
            }

            // Save customer
            customer = customerRepository.save(customer);

            // Convert to DTO
            CustomerDTO customerDTO = convertToDTO(customer);

            log.info("Customer updated successfully: {}", customer.getCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Customer updated successfully", customerDTO)
            );

        } catch (Exception e) {
            log.error("Error updating customer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update customer", "CUSTOMER_UPDATE_FAILED")
            );
        }
    }

    /**
     * Validate customer type specific requirements
     */
    private String validateCustomerTypeRequirements(Customer customer) {
        if (customer.getCustomerType() == CustomerType.INDIVIDUAL) {
            if (customer.getFirstName() == null || customer.getFirstName().isBlank()) {
                return "First name is required for individual customers";
            }
            if (customer.getLastName() == null || customer.getLastName().isBlank()) {
                return "Last name is required for individual customers";
            }
        } else {
            if (customer.getCompanyName() == null || customer.getCompanyName().isBlank()) {
                return "Company name is required for corporate customers and travel agents";
            }
        }
        return null;
    }

    /**
     * Convert Customer entity to CustomerDTO
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

    private Boolean isPassportExpiringSoon(LocalDate passportExpiry) {
        if (passportExpiry == null) return null;
        return passportExpiry.isBefore(LocalDate.now().plusMonths(6));
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
