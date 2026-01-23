package com.itineraryledger.kabengosafaris.Customer.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CreateCustomerDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository;
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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * CustomerCreateService - Service for creating customers
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CustomerCreateService {

    private final CustomerRepository customerRepository;
    private final CustomerEmailRepository customerEmailRepository;
    private final IdObfuscator idObfuscator;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9\\s\\-()]{6,50}$"
    );

    /**
     * Create a new customer
     *
     * @param createDTO The customer data
     * @return ResponseEntity with ApiResponse containing the created customer
     */
    @AuditLogAnnotation(action = "CREATE_CUSTOMER", description = "Creating a new customer", entityType = "Customer")
    public ResponseEntity<ApiResponse<?>> createCustomer(CreateCustomerDTO createDTO) {
        log.info("Creating new customer");

        try {
            // Validate customer type specific requirements
            String validationError = validateCustomerTypeRequirements(createDTO);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, validationError, "VALIDATION_ERROR")
                );
            }

            // Validate at least one email or phone is provided
            validationError = validateContactInformation(createDTO);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, validationError, "VALIDATION_ERROR")
                );
            }

            // Validate and check for duplicate emails
            if (createDTO.getEmails() != null && !createDTO.getEmails().isEmpty()) {
                for (CreateCustomerDTO.CustomerEmailDTO emailDTO : createDTO.getEmails()) {
                    // Validate email format
                    if (!EMAIL_PATTERN.matcher(emailDTO.getEmail()).matches()) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Invalid email format: " + emailDTO.getEmail(), "INVALID_EMAIL_FORMAT")
                        );
                    }
                    // Check for duplicate email
                    if (customerEmailRepository.existsByEmail(emailDTO.getEmail())) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(
                                400,
                                "Email '" + emailDTO.getEmail() + "' is already in use",
                                "EMAIL_EXISTS"
                            )
                        );
                    }
                }
            }

            // Validate phone formats
            if (createDTO.getPhones() != null && !createDTO.getPhones().isEmpty()) {
                for (CreateCustomerDTO.CustomerPhoneDTO phoneDTO : createDTO.getPhones()) {
                    if (!PHONE_PATTERN.matcher(phoneDTO.getPhoneNumber()).matches()) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Invalid phone number format: " + phoneDTO.getPhoneNumber(), "INVALID_PHONE_FORMAT")
                        );
                    }
                }
            }

            // Create customer entity (code will be set after first save)
            Customer customer = Customer.builder()
                .code("TEMP") // Temporary code, will be updated after save
                .customerType(createDTO.getCustomerType())
                .title(createDTO.getTitle())
                .firstName(createDTO.getFirstName())
                .lastName(createDTO.getLastName())
                .companyName(createDTO.getCompanyName())
                .nationality(createDTO.getNationality())
                .residency(createDTO.getResidency())
                .passportNumber(createDTO.getPassportNumber())
                .passportExpiry(createDTO.getPassportExpiry())
                .dateOfBirth(createDTO.getDateOfBirth())
                .address(createDTO.getAddress())
                .city(createDTO.getCity())
                .state(createDTO.getState())
                .country(createDTO.getCountry())
                .postalCode(createDTO.getPostalCode())
                .preferredLanguage(createDTO.getPreferredLanguage() != null ? createDTO.getPreferredLanguage() : "en")
                .preferredCurrency(createDTO.getPreferredCurrency() != null ? createDTO.getPreferredCurrency() : "USD")
                .source(createDTO.getSource())
                .referredBy(createDTO.getReferredBy())
                .dietaryRequirements(createDTO.getDietaryRequirements())
                .medicalConditions(createDTO.getMedicalConditions())
                .specialRequests(createDTO.getSpecialRequests())
                .interests(createDTO.getInterests())
                .internalNotes(createDTO.getInternalNotes())
                .isVip(createDTO.getIsVip() != null ? createDTO.getIsVip() : false)
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .build();

            // Save customer (first save to get ID)
            customer = customerRepository.save(customer);

            // Generate and set code after saving (requires ID)
            String code = customer.generateCode();
            customer.setCode(code);

            // Create and add emails
            if (createDTO.getEmails() != null && !createDTO.getEmails().isEmpty()) {
                boolean hasPrimary = createDTO.getEmails().stream()
                    .anyMatch(e -> Boolean.TRUE.equals(e.getIsPrimary()));

                for (int i = 0; i < createDTO.getEmails().size(); i++) {
                    CreateCustomerDTO.CustomerEmailDTO emailDTO = createDTO.getEmails().get(i);
                    CustomerEmail email = CustomerEmail.builder()
                        .email(emailDTO.getEmail())
                        .emailType(emailDTO.getEmailType() != null ? emailDTO.getEmailType() : CustomerEmail.EmailType.PERSONAL)
                        .isPrimary(Boolean.TRUE.equals(emailDTO.getIsPrimary()) || (!hasPrimary && i == 0))
                        .label(emailDTO.getLabel())
                        .isActive(true)
                        .build();
                    customer.addEmail(email);
                }
            }

            // Create and add phones
            if (createDTO.getPhones() != null && !createDTO.getPhones().isEmpty()) {
                boolean hasPrimary = createDTO.getPhones().stream()
                    .anyMatch(p -> Boolean.TRUE.equals(p.getIsPrimary()));

                for (int i = 0; i < createDTO.getPhones().size(); i++) {
                    CreateCustomerDTO.CustomerPhoneDTO phoneDTO = createDTO.getPhones().get(i);
                    CustomerPhone phone = CustomerPhone.builder()
                        .phoneNumber(phoneDTO.getPhoneNumber())
                        .countryCode(phoneDTO.getCountryCode())
                        .phoneType(phoneDTO.getPhoneType() != null ? phoneDTO.getPhoneType() : CustomerPhone.PhoneType.MOBILE)
                        .isPrimary(Boolean.TRUE.equals(phoneDTO.getIsPrimary()) || (!hasPrimary && i == 0))
                        .isWhatsApp(Boolean.TRUE.equals(phoneDTO.getIsWhatsApp()))
                        .label(phoneDTO.getLabel())
                        .isActive(true)
                        .build();
                    customer.addPhone(phone);
                }
            }

            customer = customerRepository.save(customer);

            // Convert to DTO
            CustomerDTO customerDTO = convertToDTO(customer);

            log.info("Customer created successfully: {} (code: {})", customer.getDisplayName(), customer.getCode());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Customer created successfully",
                    customerDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating customer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create customer",
                    "CUSTOMER_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Validate customer type specific requirements
     */
    private String validateCustomerTypeRequirements(CreateCustomerDTO dto) {
        if (dto.getCustomerType() == CustomerType.INDIVIDUAL) {
            if (dto.getFirstName() == null || dto.getFirstName().isBlank()) {
                return "First name is required for individual customers";
            }
            if (dto.getLastName() == null || dto.getLastName().isBlank()) {
                return "Last name is required for individual customers";
            }
        } else {
            // CORPORATE or TRAVEL_AGENT
            if (dto.getCompanyName() == null || dto.getCompanyName().isBlank()) {
                return "Company name is required for corporate customers and travel agents";
            }
        }
        return null;
    }

    /**
     * Validate that at least one email or phone is provided
     */
    private String validateContactInformation(CreateCustomerDTO dto) {
        boolean hasEmails = dto.getEmails() != null && !dto.getEmails().isEmpty();
        boolean hasPhones = dto.getPhones() != null && !dto.getPhones().isEmpty();

        if (!hasEmails && !hasPhones) {
            return "At least one email or phone is required";
        }

        // Check for duplicate emails in the request
        if (hasEmails) {
            Set<String> emailSet = new HashSet<>();
            for (CreateCustomerDTO.CustomerEmailDTO emailDTO : dto.getEmails()) {
                if (emailDTO.getEmail() != null) {
                    String normalizedEmail = emailDTO.getEmail().toLowerCase().trim();
                    if (!emailSet.add(normalizedEmail)) {
                        return "Duplicate email found in request: " + emailDTO.getEmail();
                    }
                }
            }
        }

        // Check for duplicate phones in the request
        if (hasPhones) {
            Set<String> phoneSet = new HashSet<>();
            for (CreateCustomerDTO.CustomerPhoneDTO phoneDTO : dto.getPhones()) {
                if (phoneDTO.getPhoneNumber() != null) {
                    String normalizedPhone = phoneDTO.getPhoneNumber().replaceAll("\\s+", "");
                    if (!phoneSet.add(normalizedPhone)) {
                        return "Duplicate phone number found in request: " + phoneDTO.getPhoneNumber();
                    }
                }
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
     * Check if passport is expiring within 6 months
     */
    private Boolean isPassportExpiringSoon(LocalDate passportExpiry) {
        if (passportExpiry == null) {
            return null;
        }
        return passportExpiry.isBefore(LocalDate.now().plusMonths(6));
    }

    /**
     * Build full address string
     */
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
