package com.itineraryledger.kabengosafaris.ContactMessage.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.ContactMessage.Repository.ContactMessageRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class ContactMessageDeleteService {

    private final ContactMessageRepository repository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ContactMessageDeleteService(ContactMessageRepository repository, IdObfuscator idObfuscator) {
        this.repository = repository;
        this.idObfuscator = idObfuscator;
    }

    public ResponseEntity<ApiResponse<?>> deleteMessages(List<String> idObfuscatedList) {
        log.info("Deleting {} contact messages", idObfuscatedList.size());

        try {
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    ids.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                }
            }

            int deletedCount = 0;
            for (Long id : ids) {
                try {
                    if (repository.existsById(id)) {
                        ((ContactMessageDeleteService) AopContext.currentProxy()).deleteMessage(id);
                        deletedCount++;
                        log.info("Contact message deleted successfully: {}", id);
                    } else {
                        log.warn("Contact message not found: {}", id);
                    }
                } catch (Exception e) {
                    log.error("Error deleting contact message: {}", id, e);
                }
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, deletedCount + " message(s) deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting contact messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete contact messages", "MESSAGES_DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_CONTACT_MESSAGE", description = "Deleting contact message", entityType = "ContactMessage", entityIdParamName = "id")
    public void deleteMessage(Long id) {
        repository.deleteById(id);
    }
}
