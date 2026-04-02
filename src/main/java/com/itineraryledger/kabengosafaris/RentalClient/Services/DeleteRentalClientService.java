package com.itineraryledger.kabengosafaris.RentalClient.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.RentalClient.Entity.RentalClient;
import com.itineraryledger.kabengosafaris.RentalClient.Repository.RentalClientRepository;
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
public class DeleteRentalClientService {

    private final RentalClientRepository rentalClientRepository;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> deleteRentalClients(List<String> idObfuscatedList) {
        log.info("Deleting {} rental clients", idObfuscatedList.size());

        try {
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    ids.add(idObfuscator.decodeId(idObfuscated));
                } catch (Exception e) {
                    log.warn("Failed to decode rental client ID: {}", idObfuscated, e);
                }
            }

            int deletedCount = 0;
            for (Long id : ids) {
                try {
                    RentalClient client = rentalClientRepository.findById(id).orElse(null);
                    if (client == null) {
                        log.warn("Rental client not found: {}", id);
                        continue;
                    }

                    deleteRentalClient(id);
                    deletedCount++;
                    log.info("Rental client deleted: {} (ID: {})", client.getDisplayName(), id);

                } catch (Exception e) {
                    log.error("Error deleting rental client: {}", id, e);
                }
            }

            return ResponseEntity.ok(
                ApiResponse.success(200, deletedCount + " rental client(s) deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting rental clients", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete rental clients", "RENTAL_CLIENTS_DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_RENTAL_CLIENT", description = "Deleting rental client", entityType = "RentalClient", entityIdParamName = "id")
    public void deleteRentalClient(Long id) {
        rentalClientRepository.deleteById(id);
    }
}
