package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailFolderRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailMessageRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.SnoozeDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailMessage;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * §3 — Snooze. Hides a message from the inbox until snoozeUntil passes;
 * the scheduled wake job then clears the field and marks the message
 * unread so it re-surfaces with attention weight.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSnoozeService {

    private final EmailMessageRepository emailMessageRepository;
    private final EmailFolderRepository emailFolderRepository;
    private final IdObfuscator idObfuscator;

    @Transactional
    public ResponseEntity<ApiResponse<?>> snooze(String accountIdObfuscated, String messageIdObfuscated, SnoozeDTO dto) {
        try {
            if (dto.getSnoozeUntil() == null || !dto.getSnoozeUntil().isAfter(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "snoozeUntil must be in the future", "SNOOZE_INVALID_TIME"));
            }
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long messageId = idObfuscator.decodeId(messageIdObfuscated);
            EmailMessage msg = emailMessageRepository.findById(messageId).orElse(null);
            if (msg == null || !msg.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Message not found", "MESSAGE_NOT_FOUND"));
            }
            msg.setSnoozeUntil(dto.getSnoozeUntil());
            emailMessageRepository.save(msg);
            return ResponseEntity.ok(ApiResponse.success(200, "Snoozed", null));
        } catch (Exception e) {
            log.error("Error snoozing message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to snooze message", "SNOOZE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> unsnooze(String accountIdObfuscated, String messageIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long messageId = idObfuscator.decodeId(messageIdObfuscated);
            EmailMessage msg = emailMessageRepository.findById(messageId).orElse(null);
            if (msg == null || !msg.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Message not found", "MESSAGE_NOT_FOUND"));
            }
            msg.setSnoozeUntil(null);
            emailMessageRepository.save(msg);
            return ResponseEntity.ok(ApiResponse.success(200, "Unsnoozed", null));
        } catch (Exception e) {
            log.error("Error unsnoozing message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to unsnooze message", "UNSNOOZE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> snoozeBatch(String accountIdObfuscated, SnoozeDTO dto) {
        try {
            if (dto.getSnoozeUntil() == null || !dto.getSnoozeUntil().isAfter(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "snoozeUntil must be in the future", "SNOOZE_INVALID_TIME"));
            }
            if (dto.getMessageIds() == null || dto.getMessageIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "messageIds is required", "MISSING_MESSAGE_IDS"));
            }
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            int updated = 0;
            for (String idObs : dto.getMessageIds()) {
                Long msgId = idObfuscator.decodeId(idObs);
                EmailMessage m = emailMessageRepository.findById(msgId).orElse(null);
                if (m == null || !m.getEmailAccount().getId().equals(accountId)) continue;
                m.setSnoozeUntil(dto.getSnoozeUntil());
                emailMessageRepository.save(m);
                updated++;
            }
            return ResponseEntity.ok(ApiResponse.success(200, updated + " messages snoozed", null));
        } catch (Exception e) {
            log.error("Error batch snoozing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to batch snooze", "BATCH_SNOOZE_FAILED"));
        }
    }

    /**
     * Scheduled wake job. Runs every 5 minutes. Any message whose
     * snoozeUntil is in the past has its snooze field cleared and the
     * unread flag re-asserted so it re-surfaces in the inbox with
     * attention weight. Folder unread counters are kept in sync.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 30_000L)
    @Transactional
    public void wakeDueSnoozes() {
        List<EmailMessage> due = emailMessageRepository.findSnoozedDueBy(LocalDateTime.now());
        if (due.isEmpty()) return;
        for (EmailMessage m : due) {
            m.setSnoozeUntil(null);
            if (Boolean.FALSE.equals(m.getIsRead())) {
                // already unread, no counter change
            } else {
                m.setIsRead(false);
                emailFolderRepository.incrementUnreadCount(m.getFolder().getId(), 1);
            }
            emailMessageRepository.save(m);
        }
        log.info("Woke {} snoozed message(s)", due.size());
    }
}
