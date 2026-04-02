package com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Services;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.DTOs.SafariVehicleDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Entity.SafariVehicle;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Repository.SafariVehicleRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SafariVehicleGetService {

    private final SafariVehicleRepository safariVehicleRepository;
    private final SafariRepository safariRepository;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> getSafariVehicles(String safariIdObfuscated) {
        log.info("Fetching vehicles for safari: {}", safariIdObfuscated);
        try {
            Long safariId = idObfuscator.decodeId(safariIdObfuscated);
            Safari safari = safariRepository.findById(safariId).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            List<SafariVehicle> vehicles = safariVehicleRepository.findBySafariId(safariId);
            List<SafariVehicleDTO> dtos = vehicles.stream().map(this::convertToDTO).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(200, "Safari vehicles retrieved successfully", dtos));

        } catch (Exception e) {
            log.error("Error fetching safari vehicles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to retrieve safari vehicles", "SAFARI_VEHICLES_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> getSafariVehicleById(String safariIdObfuscated, String idObfuscated) {
        log.info("Fetching safari vehicle: {}", idObfuscated);
        try {
            Long safariId = idObfuscator.decodeId(safariIdObfuscated);
            Long id = idObfuscator.decodeId(idObfuscated);

            SafariVehicle safariVehicle = safariVehicleRepository.findById(id).orElse(null);
            if (safariVehicle == null || !safariVehicle.getSafari().getId().equals(safariId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Safari vehicle assignment not found", "SAFARI_VEHICLE_NOT_FOUND")
                );
            }

            SafariVehicleDTO dto = convertToDTO(safariVehicle);

            Long nextId = safariVehicleRepository.findNextIdBySafariId(safariId, id).orElse(null);
            Long previousId = safariVehicleRepository.findPreviousIdBySafariId(safariId, id).orElse(null);
            if (nextId == null) nextId = safariVehicleRepository.findFirstIdBySafariId(safariId).orElse(null);
            if (previousId == null) previousId = safariVehicleRepository.findLastIdBySafariId(safariId).orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("safariVehicle", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok(ApiResponse.success(200, "Safari vehicle retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching safari vehicle: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to retrieve safari vehicle", "SAFARI_VEHICLE_FETCH_FAILED")
            );
        }
    }

    public SafariVehicleDTO convertToDTO(SafariVehicle sv) {
        return SafariVehicleDTO.builder()
            .id(idObfuscator.encodeId(sv.getId()))
            .safariId(idObfuscator.encodeId(sv.getSafari().getId()))
            .safariName(sv.getSafari().getName())
            .safariCode(sv.getSafari().getCode())
            .safariStartDate(sv.getSafari().getStartDate())
            .safariEndDate(sv.getSafari().getEndDate())
            .vehicleId(idObfuscator.encodeId(sv.getVehicle().getId()))
            .vehicleName(sv.getVehicle().getName())
            .vehicleRegistrationNumber(sv.getVehicle().getRegistrationNumber())
            .vehicleTypeDisplayName(sv.getVehicle().getType() != null ? sv.getVehicle().getType().getDisplayName() : null)
            .vehicleCapacity(sv.getVehicle().getCapacity())
            .startDate(sv.getStartDate())
            .endDate(sv.getEndDate())
            .driverId(sv.getDriver() != null ? idObfuscator.encodeId(sv.getDriver().getId()) : null)
            .driverFullName(sv.getDriver() != null ? sv.getDriver().getFullName() : null)
            .driverPhone(sv.getDriver() != null ? sv.getDriver().getPhone() : null)
            .assignmentNotes(sv.getAssignmentNotes())
            .status(sv.getStatus())
            .statusDisplayName(sv.getStatus() != null ? sv.getStatus().getDisplayName() : null)
            .createdAt(sv.getCreatedAt())
            .updatedAt(sv.getUpdatedAt())
            .build();
    }
}
