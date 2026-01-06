package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomStandardServices;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomStandardRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * DeleteAccommodationRoomStandardService - Service for deleting accommodation room standards
 */
@Service
@Slf4j
@Transactional
public class DeleteAccommodationRoomStandardService {

    private final AccommodationRoomStandardRepository roomStandardRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public DeleteAccommodationRoomStandardService(
        AccommodationRoomStandardRepository roomStandardRepository,
        IdObfuscator idObfuscator
    ) {
        this.roomStandardRepository = roomStandardRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete accommodation room standards by list of IDs
     *
     * @param idObfuscatedList List of obfuscated room standard IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteAccommodationRoomStandards(List<String> idObfuscatedList) {
        log.info("Deleting {} accommodation room standards", idObfuscatedList.size());

        try {
            if (idObfuscatedList == null || idObfuscatedList.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No room standard IDs provided",
                        "NO_IDS_PROVIDED"
                    )
                );
            }

            List<Long> roomStandardIds = new ArrayList<>();
            List<String> notFoundIds = new ArrayList<>();
            int deletedCount = 0;

            // Decode all IDs first
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long roomStandardId = idObfuscator.decodeId(idObfuscated);
                    roomStandardIds.add(roomStandardId);
                } catch (Exception e) {
                    log.warn("Failed to decode room standard ID: {}", idObfuscated, e);
                    notFoundIds.add(idObfuscated);
                }
            }

            // Delete each room standard
            for (Long roomStandardId : roomStandardIds) {
                AccommodationRoomStandard roomStandard = roomStandardRepository.findById(roomStandardId).orElse(null);
                if (roomStandard != null) {
                    // Use proxy to ensure audit logging works
                    DeleteAccommodationRoomStandardService proxy = (DeleteAccommodationRoomStandardService) AopContext.currentProxy();
                    proxy.deleteSingleRoomStandard(roomStandard);
                    deletedCount++;
                } else {
                    notFoundIds.add(idObfuscator.encodeId(roomStandardId));
                }
            }

            // Prepare response message
            String message = deletedCount + " room standard(s) deleted successfully";
            if (!notFoundIds.isEmpty()) {
                message += ". " + notFoundIds.size() + " room standard(s) not found";
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    message,
                    null
                )
            );

        } catch (Exception e) {
            log.error("Error deleting accommodation room standards", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete accommodation room standards",
                    "ACCOMMODATION_ROOM_STANDARD_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete a single room standard (with audit logging)
     */
    @AuditLogAnnotation(
        action = "DELETE_ACCOMMODATION_ROOM_STANDARD",
        description = "Deleting accommodation room standard",
        entityType = "AccommodationRoomStandard"
    )
    public void deleteSingleRoomStandard(AccommodationRoomStandard roomStandard) {
        log.info("Deleting room standard: {}", roomStandard.getName());
        roomStandardRepository.delete(roomStandard);
    }
}
