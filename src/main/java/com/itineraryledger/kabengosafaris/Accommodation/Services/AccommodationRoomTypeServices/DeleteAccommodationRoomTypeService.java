package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationRoomTypeServices;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomTypeRepository;
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
 * DeleteAccommodationRoomTypeService - Service for deleting accommodation room types
 */
@Service
@Slf4j
@Transactional
public class DeleteAccommodationRoomTypeService {

    private final AccommodationRoomTypeRepository roomTypeRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public DeleteAccommodationRoomTypeService(
        AccommodationRoomTypeRepository roomTypeRepository,
        IdObfuscator idObfuscator
    ) {
        this.roomTypeRepository = roomTypeRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete accommodation room types by list of IDs
     *
     * @param idObfuscatedList List of obfuscated room type IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteAccommodationRoomTypes(List<String> idObfuscatedList) {
        log.info("Deleting {} accommodation room types", idObfuscatedList.size());

        try {
            if (idObfuscatedList == null || idObfuscatedList.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No room type IDs provided",
                        "NO_IDS_PROVIDED"
                    )
                );
            }

            List<Long> roomTypeIds = new ArrayList<>();
            List<String> notFoundIds = new ArrayList<>();
            int deletedCount = 0;

            // Decode all IDs first
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long roomTypeId = idObfuscator.decodeId(idObfuscated);
                    roomTypeIds.add(roomTypeId);
                } catch (Exception e) {
                    log.warn("Failed to decode room type ID: {}", idObfuscated, e);
                    notFoundIds.add(idObfuscated);
                }
            }

            // Delete each room type
            for (Long roomTypeId : roomTypeIds) {
                AccommodationRoomType roomType = roomTypeRepository.findById(roomTypeId).orElse(null);
                if (roomType != null) {
                    // Use proxy to ensure audit logging works
                    DeleteAccommodationRoomTypeService proxy = (DeleteAccommodationRoomTypeService) AopContext.currentProxy();
                    proxy.deleteSingleRoomType(roomType);
                    deletedCount++;
                } else {
                    notFoundIds.add(idObfuscator.encodeId(roomTypeId));
                }
            }

            // Prepare response message
            String message = deletedCount + " room type(s) deleted successfully";
            if (!notFoundIds.isEmpty()) {
                message += ". " + notFoundIds.size() + " room type(s) not found";
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    message,
                    null
                )
            );

        } catch (Exception e) {
            log.error("Error deleting accommodation room types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete accommodation room types",
                    "ACCOMMODATION_ROOM_TYPE_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete a single room type (with audit logging)
     */
    @AuditLogAnnotation(
        action = "DELETE_ACCOMMODATION_ROOM_TYPE",
        description = "Deleting accommodation room type",
        entityType = "AccommodationRoomType"
    )
    public void deleteSingleRoomType(AccommodationRoomType roomType) {
        log.info("Deleting room type: {}", roomType.getName());
        roomTypeRepository.delete(roomType);
    }
}
