package com.itineraryledger.kabengosafaris.Vendor.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import com.itineraryledger.kabengosafaris.Vendor.DTOs.CreateVendorDTO;
import com.itineraryledger.kabengosafaris.Vendor.Entity.Vendor;
import com.itineraryledger.kabengosafaris.Vendor.Repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class VendorCreateService {

    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;
    private final VendorGetService vendorGetService;

    @AuditLogAnnotation(
        action = "CREATE_VENDOR",
        entityType = "VENDOR",
        description = "Create a new vendor"
    )
    public ResponseEntity<ApiResponse<?>> createVendor(CreateVendorDTO dto) {
        try {
            if (dto.getName() != null && vendorRepository.existsByNameIgnoreCase(dto.getName().trim())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse.error(409, "A vendor with this name already exists", "VENDOR_NAME_EXISTS"));
            }

            User currentUser = getCurrentUser();

            Vendor vendor = Vendor.builder()
                .code("TEMP")
                .name(dto.getName().trim())
                .type(dto.getType())
                .contactPerson(trimToNull(dto.getContactPerson()))
                .email(trimToNull(dto.getEmail()))
                .phone(trimToNull(dto.getPhone()))
                .taxId(trimToNull(dto.getTaxId()))
                .address(trimToNull(dto.getAddress()))
                .city(trimToNull(dto.getCity()))
                .country(trimToNull(dto.getCountry()))
                .preferredCurrency(dto.getPreferredCurrency() != null
                        ? dto.getPreferredCurrency().toUpperCase().trim() : "TZS")
                .paymentTerms(trimToNull(dto.getPaymentTerms()))
                .notes(trimToNull(dto.getNotes()))
                .isActive(dto.getIsActive() == null || dto.getIsActive())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

            vendor = vendorRepository.save(vendor);
            vendor.setCode(vendor.generateCode());
            vendor = vendorRepository.save(vendor);

            log.info("Vendor created: {} (code={})", vendor.getName(), vendor.getCode());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Vendor created successfully",
                        vendorGetService.toDTO(vendor)));
        } catch (Exception e) {
            log.error("Error creating vendor", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create vendor: " + e.getMessage(), "VENDOR_CREATE_FAILED"));
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user) {
                return userRepository.findById(user.getId()).orElse(null);
            }
        }
        return null;
    }
}
