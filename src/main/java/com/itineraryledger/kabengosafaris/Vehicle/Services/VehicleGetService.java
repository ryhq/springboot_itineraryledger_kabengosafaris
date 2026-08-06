package com.itineraryledger.kabengosafaris.Vehicle.Services;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs.VehicleDTO;
import com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs.VehicleListItemDTO;
import com.itineraryledger.kabengosafaris.Vehicle.Entity.Vehicle;
import com.itineraryledger.kabengosafaris.Vehicle.Enums.FuelType;
import com.itineraryledger.kabengosafaris.Vehicle.Enums.VehicleType;
import com.itineraryledger.kabengosafaris.Vehicle.Repository.VehicleRepository;
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
public class VehicleGetService {

    private final VehicleRepository vehicleRepository;
    private final IdObfuscator idObfuscator;

    // filter-aware prev/next + the N of M readout, and declarative counters
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "registrationNumber", "type", "make", "model", "year",
        "capacity", "fuelType", "mileage", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    public ResponseEntity<ApiResponse<?>> getVehicleById(String idObfuscated) {
        return getVehicleById(idObfuscated, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * One vehicle, plus where it sits in the set the caller was looking at.
     *
     * The list's filters and sort arrive here because paging out of a filtered list
     * must stay inside that filter, and the N of M readout must count the same set.
     */
    public ResponseEntity<ApiResponse<?>> getVehicleById(
        String idObfuscated,
        String name, String registrationNumber, VehicleType type,
        String make, String model, Integer year, FuelType fuelType,
        Integer minCapacity, Integer maxCapacity, Boolean isActive,
        Boolean insuranceExpired, Boolean inspectionExpired, String keyword,
        String sortBy, String sortDirection
    ) {
        log.info("Fetching vehicle with ID: {}", idObfuscated);
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Vehicle vehicle = vehicleRepository.findById(id).orElse(null);

            if (vehicle == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Vehicle not found", "VEHICLE_NOT_FOUND")
                );
            }

            VehicleDTO dto = convertToDTO(vehicle);

            /*
             * Prev/next walks the SAME filtered, sorted set the list showed. The old
             * findNextId/findPreviousId pair walked raw id order over every vehicle,
             * so paging out of a filtered list escaped the filter silently.
             */
            Specification<Vehicle> navSpec = buildSpec(
                name, registrationNumber, type, make, model, year, fuelType,
                minCapacity, maxCapacity, isActive, insuranceExpired, inspectionExpired, keyword
            );
            String navSortBy = validateSortField(sortBy);
            if (navSortBy == null) navSortBy = DEFAULT_SORT_FIELD;
            boolean navAscending = sortDirection != null && sortDirection.equalsIgnoreCase("asc");
            Map<String, Object> nav = recordNavigation.navigate(
                Vehicle.class, navSpec, navSortBy, navAscending, id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("vehicle", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Vehicle retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching vehicle: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to retrieve vehicle", "VEHICLE_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getAllVehicles(
        String name, String registrationNumber, VehicleType type,
        String make, String model, Integer year, FuelType fuelType,
        Integer minCapacity, Integer maxCapacity, Boolean isActive,
        Boolean insuranceExpired, Boolean inspectionExpired,
        String keyword,
        Boolean includeStats,
        Integer page, Integer size, String sortBy, String sortDirection
    ) {
        log.info("Fetching vehicles - page: {}, size: {}", page, size);
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

            Specification<Vehicle> spec = buildSpec(
                name, registrationNumber, type, make, model, year, fuelType,
                minCapacity, maxCapacity, isActive, insuranceExpired, inspectionExpired, keyword
            );

            Page<Vehicle> vehiclePage = vehicleRepository.findAll(spec, pageRequest);

            List<VehicleDTO> dtos = vehiclePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("vehicles", dtos);
            response.put("currentPage", vehiclePage.getNumber());
            response.put("totalItems", vehiclePage.getTotalElements());
            response.put("totalPages", vehiclePage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            // counters share the rows' Specification, so cards and table agree
            if (includeStats == null || includeStats) {
                response.put("stats", computeStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Vehicles retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching vehicles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list vehicles", "VEHICLES_LIST_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getVehiclesList() {
        log.info("Fetching vehicles lightweight list");
        try {
            List<Vehicle> vehicles = vehicleRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
            List<VehicleListItemDTO> dtos = vehicles.stream()
                .map(v -> VehicleListItemDTO.builder()
                    .id(idObfuscator.encodeId(v.getId()))
                    .name(v.getName())
                    .registrationNumber(v.getRegistrationNumber())
                    .type(v.getType())
                    .typeDisplayName(v.getType() != null ? v.getType().getDisplayName() : null)
                    .capacity(v.getCapacity())
                    .isActive(v.getIsActive())
                    .build())
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(200, "Vehicles list retrieved successfully", dtos));

        } catch (Exception e) {
            log.error("Error fetching vehicles list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list vehicles", "VEHICLES_LIST_FAILED")
            );
        }
    }

    public VehicleDTO convertToDTO(Vehicle vehicle) {
        return VehicleDTO.builder()
            .id(idObfuscator.encodeId(vehicle.getId()))
            .name(vehicle.getName())
            .registrationNumber(vehicle.getRegistrationNumber())
            .type(vehicle.getType())
            .typeDisplayName(vehicle.getType() != null ? vehicle.getType().getDisplayName() : null)
            .typeDescription(vehicle.getType() != null ? vehicle.getType().getDescription() : null)
            .make(vehicle.getMake())
            .model(vehicle.getModel())
            .year(vehicle.getYear())
            .color(vehicle.getColor())
            .capacity(vehicle.getCapacity())
            .fuelType(vehicle.getFuelType())
            .fuelTypeDisplayName(vehicle.getFuelType() != null ? vehicle.getFuelType().getDisplayName() : null)
            .mileage(vehicle.getMileage())
            .insuranceExpiryDate(vehicle.getInsuranceExpiryDate())
            .inspectionExpiryDate(vehicle.getInspectionExpiryDate())
            .isActive(vehicle.getIsActive())
            .notes(vehicle.getNotes())
            .createdAt(vehicle.getCreatedAt())
            .updatedAt(vehicle.getUpdatedAt())
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
     * The ONE place a vehicle filter is expressed — rows, counters and prev/next all
     * build from this, so a card can never disagree with the table and the arrows
     * can never walk a different set from the one on screen.
     */
    private Specification<Vehicle> buildSpec(
        String name, String registrationNumber, VehicleType type,
        String make, String model, Integer year, FuelType fuelType,
        Integer minCapacity, Integer maxCapacity, Boolean isActive,
        Boolean insuranceExpired, Boolean inspectionExpired, String keyword
    ) {
        return Specification.<Vehicle>unrestricted()
            .and(VehicleSpecification.nameLike(name))
            .and(VehicleSpecification.registrationNumberLike(registrationNumber))
            .and(VehicleSpecification.hasType(type))
            .and(VehicleSpecification.makeLike(make))
            .and(VehicleSpecification.modelLike(model))
            .and(VehicleSpecification.hasYear(year))
            .and(VehicleSpecification.hasFuelType(fuelType))
            .and(VehicleSpecification.minCapacity(minCapacity))
            .and(VehicleSpecification.maxCapacity(maxCapacity))
            .and(VehicleSpecification.isActive(isActive))
            .and(VehicleSpecification.insuranceExpired(insuranceExpired))
            .and(VehicleSpecification.inspectionExpired(inspectionExpired))
            .and(VehicleSpecification.keyword(keyword));
    }

    /** Counters built from the SAME Specification as the rows. */
    private Map<String, Object> computeStats(Specification<Vehicle> base) {
        return listStats.of(Vehicle.class, base)
            .total()
            .count("active", VehicleSpecification.isActive(true))
            .complement("inactive", "active")
            // actionable, not decorative: these are the vehicles that cannot go out
            .count("insuranceExpired", VehicleSpecification.insuranceExpired(true))
            .count("inspectionExpired", VehicleSpecification.inspectionExpired(true))
            .breakdown("byType", VehicleType.values(), VehicleSpecification::hasType)
            .recency(VehicleSpecification::createdAfter)
            .build();
    }
}
