package com.itineraryledger.kabengosafaris.Vendor.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import com.itineraryledger.kabengosafaris.Vendor.DTOs.UpdateVendorDTO;
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
public class VendorUpdateService {

    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;
    private final VendorGetService vendorGetService;

    @AuditLogAnnotation(
        action = "UPDATE_VENDOR",
        entityType = "VENDOR",
        entityIdParamName = "idObfuscated",
        description = "Update an existing vendor"
    )
    public ResponseEntity<ApiResponse<?>> updateVendor(String idObfuscated, UpdateVendorDTO dto) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Vendor vendor = vendorRepository.findById(id).orElse(null);
            if (vendor == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Vendor not found", "VENDOR_NOT_FOUND"));
            }

            if (dto.getName() != null) {
                String newName = dto.getName().trim();
                if (vendorRepository.existsByNameIgnoreCaseAndIdNot(newName, id)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        ApiResponse.error(409, "A vendor with this name already exists",
                            "VENDOR_NAME_EXISTS"));
                }
                vendor.setName(newName);
            }
            if (dto.getType() != null) vendor.setType(dto.getType().isBlank() ? null : com.itineraryledger.kabengosafaris.Vendor.Enums.VendorType.valueOf(dto.getType().trim()));
            if (dto.getContactPerson() != null) vendor.setContactPerson(trimToNull(dto.getContactPerson()));
            if (dto.getEmail() != null) vendor.setEmail(trimToNull(dto.getEmail()));
            if (dto.getPhone() != null) vendor.setPhone(trimToNull(dto.getPhone()));
            if (dto.getTaxId() != null) vendor.setTaxId(trimToNull(dto.getTaxId()));
            if (dto.getAddress() != null) vendor.setAddress(trimToNull(dto.getAddress()));
            if (dto.getCity() != null) vendor.setCity(trimToNull(dto.getCity()));
            if (dto.getCountry() != null) vendor.setCountry(trimToNull(dto.getCountry()));
            if (dto.getPreferredCurrency() != null) {
                vendor.setPreferredCurrency(dto.getPreferredCurrency().toUpperCase().trim());
            }
            if (dto.getPaymentTerms() != null) vendor.setPaymentTerms(trimToNull(dto.getPaymentTerms()));
            if (dto.getNotes() != null) vendor.setNotes(trimToNull(dto.getNotes()));
            if (dto.getIsActive() != null) vendor.setIsActive(dto.getIsActive());

            vendor.setUpdatedBy(getCurrentUser());
            vendor = vendorRepository.save(vendor);

            return ResponseEntity.ok(ApiResponse.success(200, "Vendor updated successfully",
                    vendorGetService.toDTO(vendor)));
        } catch (Exception e) {
            log.error("Error updating vendor", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update vendor: " + e.getMessage(),
                    "VENDOR_UPDATE_FAILED"));
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
