package com.itineraryledger.kabengosafaris.Vendor.Services;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Vendor.DTOs.VendorDTO;
import com.itineraryledger.kabengosafaris.Vendor.Entity.Vendor;
import com.itineraryledger.kabengosafaris.Vendor.Enums.VendorType;
import com.itineraryledger.kabengosafaris.Vendor.Repository.VendorRepository;
import com.itineraryledger.kabengosafaris.Vendor.Specifications.VendorFilter;
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
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "code", "type", "city", "country", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    public ResponseEntity<ApiResponse<?>> getVendorById(String idObfuscated) {
        return getVendorById(idObfuscated, null, null, null);
    }

    /**
     * One vendor, plus where it sits in the set the caller was looking at.
     *
     * The filter and sort come from the list page, so prev/next walks the same
     * rows that were on screen — page into a park authority from a filtered list
     * and the arrows stay among park authorities.
     */
    public ResponseEntity<ApiResponse<?>> getVendorById(
            String idObfuscated,
            VendorFilter filter,
            String sortBy,
            String sortDirection
    ) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Vendor vendor = vendorRepository.findById(id).orElse(null);
            if (vendor == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Vendor not found", "VENDOR_NOT_FOUND"));
            }

            Specification<Vendor> navSpec = buildSpec(filter != null ? filter : new VendorFilter());
            String navSortBy = (sortBy != null && VALID_SORT_FIELDS.contains(sortBy))
                    ? sortBy : DEFAULT_SORT_FIELD;
            boolean ascending = "asc".equalsIgnoreCase(sortDirection);

            Map<String, Object> nav = recordNavigation.navigate(
                    Vendor.class, navSpec, navSortBy, ascending, id);
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("vendor", toDTO(vendor));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Vendor retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching vendor", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch vendor", "VENDOR_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getAllVendors(
            VendorFilter filter,
            Boolean includeStats,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {
        try {
            int pageNo = page == null || page < 0 ? 0 : page;
            // clamp: an unbounded size is a way to ask for the whole table by accident
            int pageSize = size == null || size <= 0 ? 10 : Math.min(size, 100);
            String resolvedSort = (sortBy != null && VALID_SORT_FIELDS.contains(sortBy))
                    ? sortBy : DEFAULT_SORT_FIELD;
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                    ? Sort.Direction.ASC : Sort.Direction.DESC;

            Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(direction, resolvedSort));

            Specification<Vendor> spec = buildSpec(filter != null ? filter : new VendorFilter());
            Page<Vendor> paged = vendorRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("vendors", paged.getContent().stream().map(this::toDTO).toList());
            response.put("currentPage", paged.getNumber());
            response.put("totalItems", paged.getTotalElements());
            response.put("totalPages", paged.getTotalPages());
            response.put("pageSize", paged.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", resolvedSort);
            response.put("currentSortDirection", direction.name().toLowerCase());
            /*
             * Counters for the WHOLE filtered set, from the same specification as
             * the rows, so a card and the table under it cannot disagree.
             */
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Vendors retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing vendors", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list vendors", "VENDOR_LIST_FAILED"));
        }
    }

    /**
     * ONE specification, shared by the rows, the counters and the record walk.
     *
     * Every dimension ORs inside itself and ANDs across — "lodge or park
     * authority, in Arusha, with no email" is one question.
     */
    private Specification<Vendor> buildSpec(VendorFilter filter) {
        Specification<Vendor> spec = Specification.<Vendor>unrestricted()
                .and(VendorSpecification.byName(filter.getName()))
                .and(VendorSpecification.byCode(filter.getCode()))
                .and(VendorSpecification.byTypes(filter.allTypes()))
                .and(VendorSpecification.byCities(filter.allCities()))
                .and(VendorSpecification.byCountries(filter.allCountries()))
                .and(VendorSpecification.byCurrencies(filter.getCurrencies()))
                .and(VendorSpecification.searchByKeyword(filter.getKeyword()))
                .and(VendorSpecification.createdAfter(filter.getCreatedAfter()));

        if (filter.getIsActive() != null) {
            spec = spec.and(VendorSpecification.byIsActive(filter.getIsActive()));
        }
        if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
            List<Boolean> states = filter.getStatuses().stream()
                    .map(state -> "active".equalsIgnoreCase(state) ? Boolean.TRUE
                            : "inactive".equalsIgnoreCase(state) ? Boolean.FALSE : null)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            // active AND inactive is every vendor: a contradiction cancels
            if (states.size() == 1) {
                spec = spec.and(VendorSpecification.byIsActive(states.get(0)));
            }
        }

        // the data-quality asks, OR'd: "show me what needs filling in"
        Specification<Vendor> quality = null;
        if (filter.wants("missingEmail")) quality = or(quality, VendorSpecification.missingEmail());
        if (filter.wants("missingPhone")) quality = or(quality, VendorSpecification.missingPhone());
        if (filter.wants("missingTaxId")) quality = or(quality, VendorSpecification.missingTaxId());
        if (quality != null) spec = spec.and(quality);

        return spec;
    }

    private Specification<Vendor> or(Specification<Vendor> spec, Specification<Vendor> extra) {
        return spec == null ? extra : spec.or(extra);
    }

    /**
     * The cards that head the list, every one of them reachable as a filter.
     *
     * No money here: what we owe a vendor lives on their expenses, per currency,
     * and a single figure across currencies would be an invented exchange rate.
     */
    private Map<String, Object> buildStats(Specification<Vendor> spec) {
        return listStats.of(Vendor.class, spec)
                .total()
                .count("active", VendorSpecification.byIsActive(true))
                .complement("inactive", "active")
                .breakdown("byType", VendorType.values(), VendorSpecification::byType)
                .count("missingEmail", VendorSpecification.missingEmail())
                .count("missingPhone", VendorSpecification.missingPhone())
                .count("missingTaxId", VendorSpecification.missingTaxId())
                .recency(VendorSpecification::createdAfter)
                .build();
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
