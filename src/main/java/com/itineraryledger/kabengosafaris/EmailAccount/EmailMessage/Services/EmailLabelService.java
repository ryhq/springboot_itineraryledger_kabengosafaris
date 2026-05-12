package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailLabelRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailMessageRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.ApplyLabelsDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.BulkApplyLabelsDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.CreateEmailLabelDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.EmailLabelDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.UpdateEmailLabelDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailLabel;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailLabelColor;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailMessage;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailLabelService {

    private final EmailLabelRepository emailLabelRepository;
    private final EmailMessageRepository emailMessageRepository;
    private final EmailAccountRepository emailAccountRepository;
    private final IdObfuscator idObfuscator;

    /** Seed the four canonical system labels on a new account. */
    @Transactional
    public void seedSystemLabels(EmailAccount account) {
        for (EmailLabelColor color : EmailLabelColor.values()) {
            String name = capitalise(color.name());
            if (emailLabelRepository.existsByEmailAccountIdAndName(account.getId(), name)) continue;
            emailLabelRepository.save(EmailLabel.builder()
                .emailAccount(account)
                .name(name)
                .color(color)
                .isSystem(true)
                .build());
        }
    }

    private String capitalise(String s) {
        return s.substring(0, 1) + s.substring(1).toLowerCase();
    }

    public ResponseEntity<ApiResponse<?>> getLabels(String accountIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            List<EmailLabel> labels = emailLabelRepository
                .findByEmailAccountIdOrderByIsSystemDescNameAsc(accountId);

            Map<Long, Long> counts = new HashMap<>();
            for (Object[] row : emailLabelRepository.countMessagesPerLabel(accountId)) {
                counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }

            List<EmailLabelDTO> dtos = labels.stream()
                .map(l -> toDTO(l, counts.getOrDefault(l.getId(), 0L)))
                .toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Labels retrieved", dtos));
        } catch (Exception e) {
            log.error("Error getting labels", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to get labels", "GET_LABELS_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> createLabel(String accountIdObfuscated, CreateEmailLabelDTO dto) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            EmailAccount account = emailAccountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }
            if (emailLabelRepository.existsByEmailAccountIdAndName(accountId, dto.getName().trim())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Label name already exists", "LABEL_NAME_TAKEN"));
            }
            EmailLabel saved = emailLabelRepository.save(EmailLabel.builder()
                .emailAccount(account)
                .name(dto.getName().trim())
                .color(dto.getColor())
                .isSystem(false)
                .build());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Label created", toDTO(saved, 0L)));
        } catch (Exception e) {
            log.error("Error creating label", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create label", "CREATE_LABEL_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> updateLabel(String accountIdObfuscated, String labelIdObfuscated, UpdateEmailLabelDTO dto) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long labelId = idObfuscator.decodeId(labelIdObfuscated);
            EmailLabel label = emailLabelRepository.findById(labelId).orElse(null);
            if (label == null || !label.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Label not found", "LABEL_NOT_FOUND"));
            }
            if (Boolean.TRUE.equals(label.getIsSystem())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "System labels cannot be renamed or recoloured", "LABEL_IS_SYSTEM"));
            }
            if (dto.getName() != null && !dto.getName().isBlank()) {
                String newName = dto.getName().trim();
                if (!newName.equals(label.getName())
                    && emailLabelRepository.existsByEmailAccountIdAndName(accountId, newName)) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Label name already exists", "LABEL_NAME_TAKEN"));
                }
                label.setName(newName);
            }
            if (dto.getColor() != null) label.setColor(dto.getColor());
            emailLabelRepository.save(label);
            return ResponseEntity.ok(ApiResponse.success(200, "Label updated", toDTO(label, null)));
        } catch (Exception e) {
            log.error("Error updating label", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update label", "UPDATE_LABEL_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteLabel(String accountIdObfuscated, String labelIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long labelId = idObfuscator.decodeId(labelIdObfuscated);
            EmailLabel label = emailLabelRepository.findById(labelId).orElse(null);
            if (label == null || !label.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Label not found", "LABEL_NOT_FOUND"));
            }
            if (Boolean.TRUE.equals(label.getIsSystem())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "System labels cannot be deleted", "LABEL_IS_SYSTEM"));
            }
            // Detach from all messages — orphanRemoval isn't on the inverse
            // side, so we wipe the join rows explicitly via the JPQL bridge.
            List<EmailMessage> tagged = emailMessageRepository.findAllByLabelsId(labelId);
            for (EmailMessage m : tagged) m.getLabels().removeIf(l -> l.getId().equals(labelId));
            emailMessageRepository.saveAll(tagged);
            emailLabelRepository.delete(label);
            return ResponseEntity.ok(ApiResponse.success(200, "Label deleted", null));
        } catch (Exception e) {
            log.error("Error deleting label", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete label", "DELETE_LABEL_FAILED"));
        }
    }

    /** Replace the entire label set on a single message. */
    @Transactional
    public ResponseEntity<ApiResponse<?>> applyToMessage(String accountIdObfuscated, String messageIdObfuscated, ApplyLabelsDTO dto) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long messageId = idObfuscator.decodeId(messageIdObfuscated);
            EmailMessage msg = emailMessageRepository.findById(messageId).orElse(null);
            if (msg == null || !msg.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Message not found", "MESSAGE_NOT_FOUND"));
            }
            Set<EmailLabel> next = new HashSet<>(resolveLabels(accountId, dto.getLabelIds()));
            msg.setLabels(next);
            emailMessageRepository.save(msg);
            return ResponseEntity.ok(ApiResponse.success(200, "Labels updated", null));
        } catch (Exception e) {
            log.error("Error applying labels", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to apply labels", "APPLY_LABELS_FAILED"));
        }
    }

    /** Add and/or remove labels across a batch of messages. */
    @Transactional
    public ResponseEntity<ApiResponse<?>> bulkApply(String accountIdObfuscated, BulkApplyLabelsDTO dto) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            if (dto.getMessageIds() == null || dto.getMessageIds().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "messageIds is required", "MISSING_MESSAGE_IDS"));
            }
            Set<EmailLabel> toAdd = new HashSet<>(resolveLabels(accountId, dto.getAdd()));
            Set<Long> toRemoveIds = (dto.getRemove() == null ? List.<String>of() : dto.getRemove())
                .stream().map(idObfuscator::decodeId).collect(Collectors.toSet());

            int updated = 0;
            for (String idObs : dto.getMessageIds()) {
                Long msgId = idObfuscator.decodeId(idObs);
                EmailMessage m = emailMessageRepository.findById(msgId).orElse(null);
                if (m == null || !m.getEmailAccount().getId().equals(accountId)) continue;
                if (!toAdd.isEmpty()) m.getLabels().addAll(toAdd);
                if (!toRemoveIds.isEmpty()) m.getLabels().removeIf(l -> toRemoveIds.contains(l.getId()));
                emailMessageRepository.save(m);
                updated++;
            }
            return ResponseEntity.ok(ApiResponse.success(200, updated + " messages updated", null));
        } catch (Exception e) {
            log.error("Error bulk applying labels", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to bulk apply labels", "BULK_APPLY_LABELS_FAILED"));
        }
    }

    private List<EmailLabel> resolveLabels(Long accountId, List<String> obfuscatedIds) {
        if (obfuscatedIds == null || obfuscatedIds.isEmpty()) return List.of();
        List<Long> ids = obfuscatedIds.stream().map(idObfuscator::decodeId).toList();
        return emailLabelRepository.findAllById(ids).stream()
            .filter(l -> l.getEmailAccount().getId().equals(accountId))
            .toList();
    }

    private EmailLabelDTO toDTO(EmailLabel l, Long count) {
        return EmailLabelDTO.builder()
            .id(idObfuscator.encodeId(l.getId()))
            .name(l.getName())
            .color(l.getColor())
            .isSystem(l.getIsSystem())
            .count(count)
            .build();
    }
}
