package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationBoardTypeServices;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationBoardTypeRepository;
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
 * DeleteAccommodationBoardTypeService - Service for deleting accommodation board types
 */
@Service
@Slf4j
@Transactional
public class DeleteAccommodationBoardTypeService {

    private final AccommodationBoardTypeRepository boardTypeRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public DeleteAccommodationBoardTypeService(
        AccommodationBoardTypeRepository boardTypeRepository,
        IdObfuscator idObfuscator
    ) {
        this.boardTypeRepository = boardTypeRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete accommodation board types by list of IDs
     *
     * @param idObfuscatedList List of obfuscated board type IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteAccommodationBoardTypes(List<String> idObfuscatedList) {
        log.info("Deleting {} accommodation board types", idObfuscatedList.size());

        try {
            if (idObfuscatedList == null || idObfuscatedList.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No board type IDs provided",
                        "NO_IDS_PROVIDED"
                    )
                );
            }

            List<Long> boardTypeIds = new ArrayList<>();
            List<String> notFoundIds = new ArrayList<>();
            int deletedCount = 0;

            // Decode all IDs first
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long boardTypeId = idObfuscator.decodeId(idObfuscated);
                    boardTypeIds.add(boardTypeId);
                } catch (Exception e) {
                    log.warn("Failed to decode board type ID: {}", idObfuscated, e);
                    notFoundIds.add(idObfuscated);
                }
            }

            // Delete each board type
            for (Long boardTypeId : boardTypeIds) {
                AccommodationBoardType boardType = boardTypeRepository.findById(boardTypeId).orElse(null);
                if (boardType != null) {
                    // Use proxy to ensure audit logging works
                    DeleteAccommodationBoardTypeService proxy = (DeleteAccommodationBoardTypeService) AopContext.currentProxy();
                    proxy.deleteSingleBoardType(boardType);
                    deletedCount++;
                } else {
                    notFoundIds.add(idObfuscator.encodeId(boardTypeId));
                }
            }

            // Prepare response message
            String message = deletedCount + " board type(s) deleted successfully";
            if (!notFoundIds.isEmpty()) {
                message += ". " + notFoundIds.size() + " board type(s) not found";
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    message,
                    null
                )
            );

        } catch (Exception e) {
            log.error("Error deleting accommodation board types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete accommodation board types",
                    "ACCOMMODATION_BOARD_TYPE_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete a single board type (with audit logging)
     */
    @AuditLogAnnotation(
        action = "DELETE_ACCOMMODATION_BOARD_TYPE",
        description = "Deleting accommodation board type",
        entityType = "AccommodationBoardType"
    )
    public void deleteSingleBoardType(AccommodationBoardType boardType) {
        log.info("Deleting board type: {}", boardType.getName());
        boardTypeRepository.delete(boardType);
    }
}
