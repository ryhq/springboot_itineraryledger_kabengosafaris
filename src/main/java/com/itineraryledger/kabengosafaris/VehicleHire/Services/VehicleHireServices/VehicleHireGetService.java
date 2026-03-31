package com.itineraryledger.kabengosafaris.VehicleHire.Services.VehicleHireServices;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.VehicleHire.DTOs.VehicleHireDTOs.VehicleHireDTO;
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

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "clientName", "startDate", "endDate", "dailyRate", "totalAmount",
        "status", "paymentStatus", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    public ResponseEntity<ApiResponse<?>> getVehicleHireById(String idObfuscated) {
        log.info("Fetching vehicle hire: {}", idObfuscated);
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            VehicleHire hire = vehicleHireRepository.findById(id).orElse(null);
            if (hire == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Vehicle hire not found", "VEHICLE_HIRE_NOT_FOUND"));
            }

            VehicleHireDTO dto = convertToDTO(hire);
            Long nextId = vehicleHireRepository.findNextId(id).orElse(null);
            Long previousId = vehicleHireRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = vehicleHireRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = vehicleHireRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("vehicleHire", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok(ApiResponse.success(200, "Vehicle hire retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching vehicle hire: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to retrieve vehicle hire", "VEHICLE_HIRE_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getAllVehicleHires(
        String vehicleIdObfuscated, String clientName, HireStatus status, PaymentStatus paymentStatus,
        LocalDate startDateAfter, LocalDate startDateBefore, LocalDate endDateAfter, LocalDate endDateBefore,
        String keyword, Integer page, Integer size, String sortBy, String sortDirection
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

            Sort sort = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.by(validatedSortBy).descending() : Sort.by(validatedSortBy).ascending();
            PageRequest pageRequest = PageRequest.of(page != null ? page : 0, size != null ? size : 10, sort);

            Specification<VehicleHire> spec = Specification.where(VehicleHireSpecification.hasVehicleId(vehicleId))
                .and(VehicleHireSpecification.clientNameLike(clientName))
                .and(VehicleHireSpecification.hasStatus(status))
                .and(VehicleHireSpecification.hasPaymentStatus(paymentStatus))
                .and(VehicleHireSpecification.startDateAfter(startDateAfter))
                .and(VehicleHireSpecification.startDateBefore(startDateBefore))
                .and(VehicleHireSpecification.endDateAfter(endDateAfter))
                .and(VehicleHireSpecification.endDateBefore(endDateBefore))
                .and(VehicleHireSpecification.keyword(keyword));

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
            .clientName(hire.getClientName())
            .clientPhone(hire.getClientPhone())
            .clientEmail(hire.getClientEmail())
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
}
