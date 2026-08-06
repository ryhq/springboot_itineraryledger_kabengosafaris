package com.itineraryledger.kabengosafaris.VehicleHire.Services;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.VehicleHire.DTOs.VehicleHireDTO;
import com.itineraryledger.kabengosafaris.VehicleHire.Entity.VehicleHire;
import com.itineraryledger.kabengosafaris.VehicleHire.Enums.HireStatus;
import com.itineraryledger.kabengosafaris.VehicleHire.Enums.PaymentStatus;
import com.itineraryledger.kabengosafaris.VehicleHire.Repository.VehicleHireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleHireGetService {

    private final VehicleHireRepository vehicleHireRepository;
    private final IdObfuscator idObfuscator;

    // filter-aware prev/next + the N of M readout, and declarative counters
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "startDate", "endDate", "dailyRate", "totalAmount",
        "status", "paymentStatus", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    public ResponseEntity<ApiResponse<?>> getVehicleHireById(String idObfuscated) {
        return getVehicleHireById(idObfuscated, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * One hire, plus where it sits in the set the caller was looking at — the
     * list's filters and sort decide the walk, not raw id order.
     */
    public ResponseEntity<ApiResponse<?>> getVehicleHireById(
        String idObfuscated,
        String vehicleIdObfuscated,
        String rentalClientIdObfuscated,
        HireStatus status,
        PaymentStatus paymentStatus,
        LocalDate startDateAfter,
        LocalDate startDateBefore,
        LocalDate endDateAfter,
        LocalDate endDateBefore,
        String keyword,
        String sortBy
    ) {
        log.info("Fetching vehicle hire: {}", idObfuscated);
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            VehicleHire hire = vehicleHireRepository.findById(id).orElse(null);
            if (hire == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Vehicle hire not found", "VEHICLE_HIRE_NOT_FOUND"));
            }

            VehicleHireDTO dto = convertToDTO(hire);
            /*
             * Prev/next walks the SAME filtered, sorted set the list showed; the old
             * pair walked raw id order over every hire regardless of the filter.
             */
            Specification<VehicleHire> navSpec = buildSpec(
                decodeOrNull(vehicleIdObfuscated), decodeOrNull(rentalClientIdObfuscated), status, paymentStatus,
                startDateAfter, startDateBefore, endDateAfter, endDateBefore, keyword
            );
            String navSortBy = validateSortField(sortBy);
            if (navSortBy == null) navSortBy = "createdAt";
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                VehicleHire.class, navSpec, navSortBy, false, id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("vehicleHire", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Vehicle hire retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching vehicle hire: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to retrieve vehicle hire", "VEHICLE_HIRE_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getAllVehicleHires(
        String vehicleIdObfuscated, String rentalClientIdObfuscated, HireStatus status, PaymentStatus paymentStatus,
        LocalDate startDateAfter, LocalDate startDateBefore, LocalDate endDateAfter, LocalDate endDateBefore,
        String keyword, Boolean includeStats,
        Integer page, Integer size, String sortBy, String sortDirection
    ) {
        log.info("Fetching vehicle hires - page: {}, size: {}", page, size);
        try {
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD"));
            }

            Long vehicleId = null;
            if (vehicleIdObfuscated != null && !vehicleIdObfuscated.isEmpty()) {
                vehicleId = idObfuscator.decodeId(vehicleIdObfuscated);
            }
            Long rentalClientId = null;
            if (rentalClientIdObfuscated != null && !rentalClientIdObfuscated.isEmpty()) {
                rentalClientId = idObfuscator.decodeId(rentalClientIdObfuscated);
            }

            Sort sort = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.by(validatedSortBy).descending() : Sort.by(validatedSortBy).ascending();
            PageRequest pageRequest = PageRequest.of(page != null ? page : 0, size != null ? size : 10, sort);

            Specification<VehicleHire> spec = buildSpec(
                vehicleId, rentalClientId, status, paymentStatus,
                startDateAfter, startDateBefore, endDateAfter, endDateBefore, keyword
            );

            Page<VehicleHire> hirePage = vehicleHireRepository.findAll(spec, pageRequest);
            List<VehicleHireDTO> dtos = hirePage.getContent().stream().map(this::convertToDTO).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("vehicleHires", dtos);
            response.put("currentPage", hirePage.getNumber());
            response.put("totalItems", hirePage.getTotalElements());
            response.put("totalPages", hirePage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            // counters share the rows' Specification, so cards and table agree
            if (includeStats == null || includeStats) {
                response.put("stats", computeStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Vehicle hires retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching vehicle hires", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list vehicle hires", "VEHICLE_HIRES_LIST_FAILED"));
        }
    }

    public VehicleHireDTO convertToDTO(VehicleHire hire) {
        return VehicleHireDTO.builder()
            .id(idObfuscator.encodeId(hire.getId()))
            .vehicleId(idObfuscator.encodeId(hire.getVehicle().getId()))
            .vehicleName(hire.getVehicle().getName())
            .vehicleRegistrationNumber(hire.getVehicle().getRegistrationNumber())
            .vehicleTypeDisplayName(hire.getVehicle().getType() != null ? hire.getVehicle().getType().getDisplayName() : null)
            .rentalClientId(hire.getRentalClient() != null ? idObfuscator.encodeId(hire.getRentalClient().getId()) : null)
            .rentalClientName(hire.getRentalClient() != null ? hire.getRentalClient().getDisplayName() : null)
            .rentalClientPhone(hire.getRentalClient() != null ? hire.getRentalClient().getPhone() : null)
            .rentalClientEmail(hire.getRentalClient() != null ? hire.getRentalClient().getEmail() : null)
            .rentalClientType(hire.getRentalClient() != null ? hire.getRentalClient().getClientType().name() : null)
            .rentalClientTypeDisplayName(hire.getRentalClient() != null ? hire.getRentalClient().getClientType().getDisplayName() : null)
            .driverId(hire.getDriver() != null ? idObfuscator.encodeId(hire.getDriver().getId()) : null)
            .driverName(hire.getDriver() != null ? hire.getDriver().getFullName() : null)
            .driverPhone(hire.getDriver() != null ? hire.getDriver().getPhone() : null)
            .driverLicenseNumber(hire.getDriver() != null ? hire.getDriver().getLicenseNumber() : null)
            .startDate(hire.getStartDate())
            .endDate(hire.getEndDate())
            .pickupLocation(hire.getPickupLocation())
            .dropoffLocation(hire.getDropoffLocation())
            .dailyRate(hire.getDailyRate())
            .totalAmount(hire.getTotalAmount())
            .currency(hire.getCurrency())
            .status(hire.getStatus())
            .statusDisplayName(hire.getStatus() != null ? hire.getStatus().getDisplayName() : null)
            .paymentStatus(hire.getPaymentStatus())
            .paymentStatusDisplayName(hire.getPaymentStatus() != null ? hire.getPaymentStatus().getDisplayName() : null)
            .notes(hire.getNotes())
            .createdAt(hire.getCreatedAt())
            .updatedAt(hire.getUpdatedAt())
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
     * The ONE place a hire filter is expressed — rows, counters and prev/next all
     * build from it, so a card can never disagree with the table.
     */
    private Specification<VehicleHire> buildSpec(
        Long vehicleId, Long rentalClientId, HireStatus status, PaymentStatus paymentStatus,
        LocalDate startDateAfter, LocalDate startDateBefore,
        LocalDate endDateAfter, LocalDate endDateBefore, String keyword
    ) {
        return Specification.<VehicleHire>unrestricted()
            .and(VehicleHireSpecification.hasVehicleId(vehicleId))
            .and(VehicleHireSpecification.hasRentalClientId(rentalClientId))
            .and(VehicleHireSpecification.hasStatus(status))
            .and(VehicleHireSpecification.hasPaymentStatus(paymentStatus))
            .and(VehicleHireSpecification.startDateAfter(startDateAfter))
            .and(VehicleHireSpecification.startDateBefore(startDateBefore))
            .and(VehicleHireSpecification.endDateAfter(endDateAfter))
            .and(VehicleHireSpecification.endDateBefore(endDateBefore))
            .and(VehicleHireSpecification.keyword(keyword));
    }

    /** Decodes an obfuscated id, or null when absent or unreadable. */
    private Long decodeOrNull(String obfuscated) {
        if (obfuscated == null || obfuscated.isBlank()) return null;
        try {
            return idObfuscator.decodeId(obfuscated);
        } catch (Exception e) {
            log.warn("Unreadable id in filter: {}", obfuscated);
            return null;
        }
    }

    /** Counters built from the SAME Specification as the rows. */
    private Map<String, Object> computeStats(Specification<VehicleHire> base) {
        return listStats.of(VehicleHire.class, base)
            .total()
            .breakdown("byStatus", HireStatus.values(), VehicleHireSpecification::hasStatus)
            .breakdown("byPaymentStatus", PaymentStatus.values(), VehicleHireSpecification::hasPaymentStatus)
            .recency(VehicleHireSpecification::createdAfter)
            .build();
    }
}
