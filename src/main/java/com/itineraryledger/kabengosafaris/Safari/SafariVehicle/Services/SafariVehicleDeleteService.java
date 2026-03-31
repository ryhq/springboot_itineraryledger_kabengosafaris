package com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Entity.SafariVehicle;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Repository.SafariVehicleRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SafariVehicleDeleteService {

    private final SafariVehicleRepository safariVehicleRepository;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> deleteSafariVehicle(String safariIdObfuscated, String idObfuscated) {
        log.info("Deleting safari vehicle: {}", idObfuscated);
        try {
            Long safariId = idObfuscator.decodeId(safariIdObfuscated);
            Long id = idObfuscator.decodeId(idObfuscated);

            SafariVehicle safariVehicle = safariVehicleRepository.findById(id).orElse(null);
            if (safariVehicle == null || !safariVehicle.getSafari().getId().equals(safariId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Safari vehicle assignment not found", "SAFARI_VEHICLE_NOT_FOUND")
                );
            }

            deleteSafariVehicleInternal(id);
            return ResponseEntity.ok(ApiResponse.success(200, "Safari vehicle assignment deleted successfully", null));

        } catch (Exception e) {
            log.error("Error deleting safari vehicle: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete safari vehicle", "SAFARI_VEHICLE_DELETE_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> deleteSafariVehicles(String safariIdObfuscated, List<String> idObfuscatedList) {
        log.info("Deleting {} safari vehicles", idObfuscatedList.size());
        try {
            Long safariId = idObfuscator.decodeId(safariIdObfuscated);
            int deletedCount = 0;

            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    SafariVehicle sv = safariVehicleRepository.findById(id).orElse(null);
                    if (sv != null && sv.getSafari().getId().equals(safariId)) {
                        deleteSafariVehicleInternal(id);
                        deletedCount++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to delete safari vehicle: {}", idObfuscated, e);
                }
            }

            return ResponseEntity.ok(
                ApiResponse.success(200, deletedCount + " safari vehicle assignment(s) deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting safari vehicles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete safari vehicles", "SAFARI_VEHICLES_DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_SAFARI_VEHICLE", description = "Deleting safari vehicle assignment", entityType = "SafariVehicle", entityIdParamName = "id")
    public void deleteSafariVehicleInternal(Long id) {
        safariVehicleRepository.deleteById(id);
    }
}
