package com.itineraryledger.kabengosafaris.Vehicle.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Vehicle.Entity.Vehicle;
import com.itineraryledger.kabengosafaris.Vehicle.Repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DeleteVehicleService {

    private final VehicleRepository vehicleRepository;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> deleteVehicles(List<String> idObfuscatedList) {
        log.info("Deleting {} vehicles", idObfuscatedList.size());

        try {
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    ids.add(idObfuscator.decodeId(idObfuscated));
                } catch (Exception e) {
                    log.warn("Failed to decode vehicle ID: {}", idObfuscated, e);
                }
            }

            int deletedCount = 0;
            for (Long id : ids) {
                try {
                    Vehicle vehicle = vehicleRepository.findById(id).orElse(null);
                    if (vehicle == null) {
                        log.warn("Vehicle not found: {}", id);
                        continue;
                    }

                    deleteVehicle(id);
                    deletedCount++;
                    log.info("Vehicle deleted: {} (ID: {})", vehicle.getName(), id);

                } catch (Exception e) {
                    log.error("Error deleting vehicle: {}", id, e);
                }
            }

            return ResponseEntity.ok(
                ApiResponse.success(200, deletedCount + " vehicle(s) deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting vehicles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete vehicles", "VEHICLES_DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_VEHICLE", description = "Deleting vehicle", entityType = "Vehicle", entityIdParamName = "id")
    public void deleteVehicle(Long id) {
        vehicleRepository.deleteById(id);
    }
}
