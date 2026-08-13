package com.itineraryledger.kabengosafaris.ContactMessage.Services;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
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
            List<String> deletedIds = new ArrayList<>();
            List<Map<String, Object>> skipped = new ArrayList<>();

            for (String obfuscated : idObfuscatedList) {
                Long id;
                try {
                    id = idObfuscator.decodeId(obfuscated);
                } catch (Exception e) {
                    skipped.add(skip(obfuscated, "Unreadable id"));
                    continue;
                }

                if (!repository.existsById(id)) {
                    skipped.add(skip(obfuscated, "No longer exists"));
                    continue;
                }

                try {
                    ((ContactMessageDeleteService) AopContext.currentProxy()).deleteMessage(id);
                    deletedIds.add(obfuscated);
                } catch (Exception e) {
                    log.error("Error deleting message: {}", id, e);
                    skipped.add(skip(obfuscated,
                        e.getMessage() != null ? e.getMessage() : "Could not be deleted"));
                }
            }

            Map<String, Object> report = new HashMap<>();
            report.put("deletedCount", deletedIds.size());
            report.put("deletedIds", deletedIds);
            report.put("skipped", skipped);

            String message = deletedIds.size()
                + (deletedIds.size() == 1 ? " message deleted" : " messages deleted")
                + (skipped.isEmpty() ? "" : ", " + skipped.size() + " skipped");

            return ResponseEntity.ok().body(ApiResponse.success(200, message, report));

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

    /**
     * Why one id did not go.
     *
     * A caller that asked about six needs to know which four survived and why —
     * a bare count leaves them guessing, and guessing about deletions is worse
     * than being told.
     */
    private Map<String, Object> skip(String id, String reason) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("id", id);
        entry.put("reason", reason);
        return entry;
    }
}
