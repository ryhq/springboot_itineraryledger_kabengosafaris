package com.itineraryledger.kabengosafaris.Driver.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Driver.Entity.Driver;
import com.itineraryledger.kabengosafaris.Driver.Repository.DriverRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
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
public class DeleteDriverService {

    private final DriverRepository driverRepository;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> deleteDrivers(List<String> idObfuscatedList) {
        log.info("Deleting {} drivers", idObfuscatedList.size());

        try {
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    ids.add(idObfuscator.decodeId(idObfuscated));
                } catch (Exception e) {
                    log.warn("Failed to decode driver ID: {}", idObfuscated, e);
                }
            }

            int deletedCount = 0;
            for (Long id : ids) {
                try {
                    Driver driver = driverRepository.findById(id).orElse(null);
                    if (driver == null) {
                        log.warn("Driver not found: {}", id);
                        continue;
                    }

                    deleteDriver(id);
                    deletedCount++;
                    log.info("Driver deleted: {} (ID: {})", driver.getFullName(), id);

                } catch (Exception e) {
                    log.error("Error deleting driver: {}", id, e);
                }
            }

            return ResponseEntity.ok(
                ApiResponse.success(200, deletedCount + " driver(s) deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting drivers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete drivers", "DRIVERS_DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_DRIVER", description = "Deleting driver", entityType = "Driver", entityIdParamName = "id")
    public void deleteDriver(Long id) {
        driverRepository.deleteById(id);
    }
}
