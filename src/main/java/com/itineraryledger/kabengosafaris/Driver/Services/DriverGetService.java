package com.itineraryledger.kabengosafaris.Driver.Services;

import com.itineraryledger.kabengosafaris.Driver.DTOs.DriverDTO;
import com.itineraryledger.kabengosafaris.Driver.DTOs.DriverListItemDTO;
import com.itineraryledger.kabengosafaris.Driver.Entity.Driver;
import com.itineraryledger.kabengosafaris.Driver.Enums.DriverStatus;
import com.itineraryledger.kabengosafaris.Driver.Repository.DriverRepository;
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
public class DriverGetService {

    private final DriverRepository driverRepository;

    // filter-aware prev/next + the N of M readout, and declarative counters
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "firstName", "lastName", "phone", "licenseNumber", "status",
        "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    public ResponseEntity<ApiResponse<?>> getDriverById(String idObfuscated) {
        return getDriverById(idObfuscated, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * One driver, plus where it sits in the set the caller was looking at.
     *
     * The list's filters and sort arrive here because paging out of a filtered list
     * must stay inside that filter, and the N of M readout must count the same set.
     */
    public ResponseEntity<ApiResponse<?>> getDriverById(
        String idObfuscated,
        String firstName, String lastName, String phone,
        DriverStatus status, Boolean isActive,
        Boolean licenseExpired, Boolean talaExpired, Boolean tourGuideIdExpired,
        String keyword, String sortBy, String sortDirection
    ) {
        log.info("Fetching driver with ID: {}", idObfuscated);
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Driver driver = driverRepository.findById(id).orElse(null);

            if (driver == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Driver not found", "DRIVER_NOT_FOUND")
                );
            }

            DriverDTO dto = convertToDTO(driver);

            /*
             * Prev/next walks the SAME filtered, sorted set the list showed; the old
             * pair walked raw id order over every driver regardless of the filter.
             */
            Specification<Driver> navSpec = buildSpec(
                firstName, lastName, phone, status, isActive,
                licenseExpired, talaExpired, tourGuideIdExpired, keyword
            );
            String navSortBy = validateSortField(sortBy);
            if (navSortBy == null) navSortBy = DEFAULT_SORT_FIELD;
            boolean navAscending = sortDirection != null && sortDirection.equalsIgnoreCase("asc");
            Map<String, Object> nav = recordNavigation.navigate(
                Driver.class, navSpec, navSortBy, navAscending, id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("driver", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Driver retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching driver: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to retrieve driver", "DRIVER_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getAllDrivers(
        String firstName, String lastName, String phone,
        DriverStatus status, Boolean isActive,
        Boolean licenseExpired, Boolean talaExpired, Boolean tourGuideIdExpired,
        String keyword,
        Boolean includeStats,
        Integer page, Integer size, String sortBy, String sortDirection
    ) {
        log.info("Fetching drivers - page: {}, size: {}", page, size);
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

            Specification<Driver> spec = buildSpec(
                firstName, lastName, phone, status, isActive,
                licenseExpired, talaExpired, tourGuideIdExpired, keyword
            );

            Page<Driver> driverPage = driverRepository.findAll(spec, pageRequest);

            List<DriverDTO> dtos = driverPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("drivers", dtos);
            response.put("currentPage", driverPage.getNumber());
            response.put("totalItems", driverPage.getTotalElements());
            response.put("totalPages", driverPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            // counters share the rows' Specification, so cards and table agree
            if (includeStats == null || includeStats) {
                response.put("stats", computeStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Drivers retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching drivers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list drivers", "DRIVERS_LIST_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getDriversList() {
        log.info("Fetching drivers lightweight list");
        try {
            List<Driver> drivers = driverRepository.findAll(Sort.by(Sort.Direction.ASC, "firstName"));
            List<DriverListItemDTO> dtos = drivers.stream()
                .map(d -> DriverListItemDTO.builder()
                    .id(idObfuscator.encodeId(d.getId()))
                    .fullName(d.getFullName())
                    .phone(d.getPhone())
                    .licenseNumber(d.getLicenseNumber())
                    .status(d.getStatus())
                    .statusDisplayName(d.getStatus() != null ? d.getStatus().getDisplayName() : null)
                    .isActive(d.getIsActive())
                    .build())
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(200, "Drivers list retrieved successfully", dtos));

        } catch (Exception e) {
            log.error("Error fetching drivers list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list drivers", "DRIVERS_LIST_FAILED")
            );
        }
    }

    public DriverDTO convertToDTO(Driver driver) {
        return DriverDTO.builder()
            .id(idObfuscator.encodeId(driver.getId()))
            .firstName(driver.getFirstName())
            .lastName(driver.getLastName())
            .fullName(driver.getFullName())
            .phone(driver.getPhone())
            .email(driver.getEmail())
            .licenseNumber(driver.getLicenseNumber())
            .licenseExpiryDate(driver.getLicenseExpiryDate())
            .licenseClass(driver.getLicenseClass())
            .talaLicenseNumber(driver.getTalaLicenseNumber())
            .talaExpiryDate(driver.getTalaExpiryDate())
            .tourGuideId(driver.getTourGuideId())
            .tourGuideIdExpiryDate(driver.getTourGuideIdExpiryDate())
            .status(driver.getStatus())
            .statusDisplayName(driver.getStatus() != null ? driver.getStatus().getDisplayName() : null)
            .notes(driver.getNotes())
            .isActive(driver.getIsActive())
            .createdAt(driver.getCreatedAt())
            .updatedAt(driver.getUpdatedAt())
            .build();
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * The ONE place a driver filter is expressed — rows, counters and prev/next all
     * build from this, so a card can never disagree with the table and the arrows
     * can never walk a different set from the one on screen.
     */
    private Specification<Driver> buildSpec(
        String firstName, String lastName, String phone,
        DriverStatus status, Boolean isActive,
        Boolean licenseExpired, Boolean talaExpired, Boolean tourGuideIdExpired,
        String keyword
    ) {
        return Specification.<Driver>unrestricted()
            .and(DriverSpecification.firstNameLike(firstName))
            .and(DriverSpecification.lastNameLike(lastName))
            .and(DriverSpecification.phoneLike(phone))
            .and(DriverSpecification.hasStatus(status))
            .and(DriverSpecification.isActive(isActive))
            .and(DriverSpecification.licenseExpired(licenseExpired))
            .and(DriverSpecification.talaExpired(talaExpired))
            .and(DriverSpecification.tourGuideIdExpired(tourGuideIdExpired))
            .and(DriverSpecification.keyword(keyword));
    }

    /** Counters built from the SAME Specification as the rows. */
    private Map<String, Object> computeStats(Specification<Driver> base) {
        return listStats.of(Driver.class, base)
            .total()
            .count("active", DriverSpecification.isActive(true))
            .complement("inactive", "active")
            // the papers that stop a driver working: every one of these is actionable
            .count("licenseExpired", DriverSpecification.licenseExpired(true))
            .count("talaExpired", DriverSpecification.talaExpired(true))
            .count("tourGuideIdExpired", DriverSpecification.tourGuideIdExpired(true))
            .breakdown("byStatus", DriverStatus.values(), DriverSpecification::hasStatus)
            .recency(DriverSpecification::createdAfter)
            .build();
    }
}
