package com.itineraryledger.kabengosafaris.ContactMessage.Services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.ContactMessage.DTOs.ContactMessageDTO;
import com.itineraryledger.kabengosafaris.ContactMessage.DTOs.UpdateContactMessageDTO;
import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessage;
import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessageStatus;
import com.itineraryledger.kabengosafaris.ContactMessage.Repository.ContactMessageRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class ContactMessageUpdateService {

    private final ContactMessageRepository repository;
    private final IdObfuscator idObfuscator;
    private final ContactMessageGetService getService;

    @Autowired
    public ContactMessageUpdateService(
        ContactMessageRepository repository,
        IdObfuscator idObfuscator,
        ContactMessageGetService getService
    ) {
        this.repository = repository;
        this.idObfuscator = idObfuscator;
        this.getService = getService;
    }

    @AuditLogAnnotation(action = "UPDATE_CONTACT_MESSAGE", description = "Updating contact message", entityType = "ContactMessage", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> updateMessage(String idObfuscated, UpdateContactMessageDTO updateDTO) {
        log.info("Updating contact message with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode message ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid message ID", "INVALID_MESSAGE_ID")
                );
            }

            ContactMessage message = repository.findById(id).orElse(null);
            if (message == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Contact message not found", "MESSAGE_NOT_FOUND")
                );
            }

            if (updateDTO.getAdminNotes() != null) message.setAdminNotes(updateDTO.getAdminNotes());

            if (updateDTO.getStatus() != null) {
                ContactMessageStatus oldStatus = message.getStatus();
                // the status arrives as a String so a blank can CLEAR it; parse it once
                ContactMessageStatus requestedStatus = updateDTO.getStatus().isBlank()
                    ? null
                    : ContactMessageStatus.valueOf(updateDTO.getStatus().trim());
                message.setStatus(requestedStatus);

                if (requestedStatus == ContactMessageStatus.RESPONDED && oldStatus != ContactMessageStatus.RESPONDED && message.getRespondedAt() == null) {
                    message.setRespondedAt(LocalDateTime.now());
                }
            }

            message = repository.save(message);

            ContactMessageDTO dto = getService.convertToDTO(message);

            log.info("Contact message updated successfully: {}", message.getCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Contact message updated successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error updating contact message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update contact message", "MESSAGE_UPDATE_FAILED")
            );
        }
    }
}
