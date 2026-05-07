package com.itineraryledger.kabengosafaris.Vendor.Services;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Vendor.DTOs.VendorDTO;
import com.itineraryledger.kabengosafaris.Vendor.Entity.Vendor;
import com.itineraryledger.kabengosafaris.Vendor.Enums.VendorType;
import com.itineraryledger.kabengosafaris.Vendor.Repository.VendorRepository;
import com.itineraryledger.kabengosafaris.Vendor.Specifications.VendorSpecification;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorGetService {

    private final VendorRepository vendorRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "code", "type", "city", "country", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    public ResponseEntity<ApiResponse<?>> getVendorById(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Vendor vendor = vendorRepository.findById(id).orElse(null);
            if (vendor == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Vendor not found", "VENDOR_NOT_FOUND"));
            }

            Long nextId = vendorRepository.findNextId(id).orElse(null);
            Long previousId = vendorRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = vendorRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = vendorRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("vendor", toDTO(vendor));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok(ApiResponse.success(200, "Vendor retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching vendor", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch vendor", "VENDOR_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getAllVendors(
            String name,
            String code,
            VendorType type,
            String city,
            String country,
            Boolean isActive,
            String keyword,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {
        try {
            int pageNo = page == null || page < 0 ? 0 : page;
            int pageSize = size == null || size <= 0 ? 10 : Math.min(size, 200);
            String resolvedSort = (sortBy != null && VALID_SORT_FIELDS.contains(sortBy))
                    ? sortBy : DEFAULT_SORT_FIELD;
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                    ? Sort.Direction.ASC : Sort.Direction.DESC;

            Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(direction, resolvedSort));

            Specification<Vendor> spec = Specification.unrestricted()
                    .and(VendorSpecification.byName(name))
                    .and(VendorSpecification.byCode(code))
                    .and(VendorSpecification.byType(type))
                    .and(VendorSpecification.byCity(city))
                    .and(VendorSpecification.byCountry(country))
                    .and(VendorSpecification.byIsActive(isActive))
                    .and(VendorSpecification.searchByKeyword(keyword));

            Page<Vendor> paged = vendorRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("vendors", paged.getContent().stream().map(this::toDTO).toList());
            response.put("currentPage", paged.getNumber());
            response.put("totalItems", paged.getTotalElements());
            response.put("totalPages", paged.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);

            return ResponseEntity.ok(ApiResponse.success(200, "Vendors retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing vendors", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list vendors", "VENDOR_LIST_FAILED"));
        }
    }

    public VendorDTO toDTO(Vendor v) {
        return VendorDTO.builder()
            .id(idObfuscator.encodeId(v.getId()))
            .code(v.getCode())
            .name(v.getName())
            .type(v.getType())
            .typeDisplayName(v.getType() != null ? v.getType().getDisplayName() : null)
            .contactPerson(v.getContactPerson())
            .email(v.getEmail())
            .phone(v.getPhone())
            .taxId(v.getTaxId())
            .address(v.getAddress())
            .city(v.getCity())
            .country(v.getCountry())
            .preferredCurrency(v.getPreferredCurrency())
            .paymentTerms(v.getPaymentTerms())
            .notes(v.getNotes())
            .isActive(v.getIsActive())
            .createdById(v.getCreatedBy() != null ? idObfuscator.encodeId(v.getCreatedBy().getId()) : null)
            .createdByName(v.getCreatedBy() != null ? v.getCreatedBy().getUsername() : null)
            .updatedById(v.getUpdatedBy() != null ? idObfuscator.encodeId(v.getUpdatedBy().getId()) : null)
            .updatedByName(v.getUpdatedBy() != null ? v.getUpdatedBy().getUsername() : null)
            .createdAt(v.getCreatedAt())
            .updatedAt(v.getUpdatedAt())
            .build();
    }
}
