package com.itineraryledger.kabengosafaris.VehicleHire.Services.VehicleHireServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.VehicleHire.Repository.VehicleHireRepository;
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
public class DeleteVehicleHireService {

    private final VehicleHireRepository vehicleHireRepository;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> deleteVehicleHires(List<String> idObfuscatedList) {
        log.info("Deleting {} vehicle hires", idObfuscatedList.size());
        try {
            int deletedCount = 0;
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    if (vehicleHireRepository.existsById(id)) {
                        deleteVehicleHireInternal(id);
                        deletedCount++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to delete vehicle hire: {}", idObfuscated, e);
                }
            }
            return ResponseEntity.ok(ApiResponse.success(200, deletedCount + " vehicle hire(s) deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting vehicle hires", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete vehicle hires", "VEHICLE_HIRES_DELETE_FAILED"));
        }
    }

    @AuditLogAnnotation(action = "DELETE_VEHICLE_HIRE", description = "Deleting vehicle hire", entityType = "VehicleHire", entityIdParamName = "id")
    public void deleteVehicleHireInternal(Long id) {
        vehicleHireRepository.deleteById(id);
    }
}
