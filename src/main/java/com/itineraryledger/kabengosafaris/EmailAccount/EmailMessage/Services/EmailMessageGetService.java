package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailAttachmentRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailFolderRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailMessageRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.EmailAttachmentDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.EmailMessageDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.EmailMessageListDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailAttachment;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailMessage;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailMessageGetService {

    private final EmailMessageRepository emailMessageRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final EmailFolderRepository emailFolderRepository;
    private final EmailAccountRepository emailAccountRepository;
    private final EmailStorageService emailStorageService;
    private final IdObfuscator idObfuscator;
    private final MuteRuleService muteRuleService;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "sentAt", "receivedAt", "subject", "fromAddress", "isRead", "isStarred", "createdAt"
    );
    private static final String DEFAULT_SORT_FIELD = "sentAt";

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * Get paginated list of email messages with filtering
     */
    public ResponseEntity<?> getMessages(
        String accountIdObfuscated,
        int page,
        int size,
        String folderId,
        Boolean isRead,
        Boolean isStarred,
        Boolean isFlagged,
        Boolean hasAttachments,
        String search,
        String fromAddress,
        String subject,
        List<String> labelIds,
        LocalDateTime sentAfter,
        LocalDateTime sentBefore,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            if (!emailAccountRepository.existsById(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD"));
            }

            Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable paging = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

            // Build specification
            // §3 — hide snoozed messages from list queries. They re-surface
            // once EmailSnoozeService.wakeDueSnoozes clears the field.
            // §7 — also exclude anything matching an active mute rule;
            // those are reported separately via /folders/{id}/muted-summary.
            Specification<EmailMessage> spec = Specification.<EmailMessage>unrestricted()
                .and(EmailMessageSpecification.forAccount(accountId))
                .and(EmailMessageSpecification.notSnoozed())
                .and(EmailMessageSpecification.notMatchingMuteRules(muteRuleService.getActiveRules(accountId)));

            if (folderId != null && !folderId.isBlank()) {
                Long decodedFolderId = idObfuscator.decodeId(folderId);
                spec = spec.and(EmailMessageSpecification.inFolder(decodedFolderId));
            }
            if (isRead != null) {
                spec = spec.and(EmailMessageSpecification.isRead(isRead));
            }
            if (isStarred != null) {
                spec = spec.and(EmailMessageSpecification.isStarred(isStarred));
            }
            if (isFlagged != null) {
                spec = spec.and(EmailMessageSpecification.isFlagged(isFlagged));
            }
            if (hasAttachments != null) {
                spec = spec.and(EmailMessageSpecification.hasAttachments(hasAttachments));
            }
            if (labelIds != null && !labelIds.isEmpty()) {
                List<Long> decoded = labelIds.stream()
                    .map(idObfuscator::decodeId)
                    .filter(java.util.Objects::nonNull)
                    .toList();
                if (!decoded.isEmpty()) {
                    spec = spec.and(EmailMessageSpecification.hasAnyLabel(decoded));
                }
            }
            if (search != null && !search.isBlank()) {
                // §8 — parse operator syntax (from:, to:, label:, has:,
                // is:, before:, after:) out of the search bar input.
                SearchQueryParser.Parsed parsed = SearchQueryParser.parse(search);
                for (String f : parsed.getFrom()) spec = spec.and(EmailMessageSpecification.fromAddressLike(f));
                for (String t : parsed.getTo()) spec = spec.and(EmailMessageSpecification.toAddressLike(t));
                for (String s : parsed.getSubject()) spec = spec.and(EmailMessageSpecification.subjectLike(s));
                if (parsed.getHasAttachment() != null) spec = spec.and(EmailMessageSpecification.hasAttachments(parsed.getHasAttachment()));
                if (parsed.getIsUnread() != null) spec = spec.and(EmailMessageSpecification.isRead(!parsed.getIsUnread()));
                if (parsed.getIsStarred() != null) spec = spec.and(EmailMessageSpecification.isStarred(parsed.getIsStarred()));
                if (parsed.getIsFlagged() != null) spec = spec.and(EmailMessageSpecification.isFlagged(parsed.getIsFlagged()));
                if (parsed.getBefore() != null) spec = spec.and(EmailMessageSpecification.sentBefore(parsed.getBefore()));
                if (parsed.getAfter() != null) spec = spec.and(EmailMessageSpecification.sentAfter(parsed.getAfter()));
                if (!parsed.getLabel().isEmpty()) {
                    // label:foo resolves on label name substring — convert to ids and use hasAnyLabel.
                    // (Substring match against EmailLabel.name would need an extra spec; keep it
                    // simple here and let the explicit `labelIds` query param do exact filtering.)
                    spec = spec.and((root, query, cb) -> {
                        if (query != null) query.distinct(true);
                        var join = root.<EmailMessage, com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailLabel>join("labels");
                        var disjunction = cb.disjunction();
                        for (String name : parsed.getLabel()) {
                            disjunction = cb.or(disjunction,
                                cb.like(cb.lower(join.<String>get("name")), "%" + name.toLowerCase() + "%"));
                        }
                        return disjunction;
                    });
                }
                if (parsed.getFreeText() != null && !parsed.getFreeText().isBlank()) {
                    spec = spec.and(EmailMessageSpecification.searchAll(parsed.getFreeText()));
                }
            }
            if (fromAddress != null && !fromAddress.isBlank()) {
                spec = spec.and(EmailMessageSpecification.fromAddressLike(fromAddress));
            }
            if (subject != null && !subject.isBlank()) {
                spec = spec.and(EmailMessageSpecification.subjectLike(subject));
            }
            if (sentAfter != null) {
                spec = spec.and(EmailMessageSpecification.sentAfter(sentAfter));
            }
            if (sentBefore != null) {
                spec = spec.and(EmailMessageSpecification.sentBefore(sentBefore));
            }

            Page<EmailMessage> pagedMessages = emailMessageRepository.findAll(spec, paging);

            // §2 — batch the threadCount lookup for every threadId present in
            // the page so the list rows can render the count chip without N+1.
            List<String> threadIds = pagedMessages.getContent().stream()
                .map(EmailMessage::getThreadId)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();
            Map<String, Long> threadCounts = new HashMap<>();
            if (!threadIds.isEmpty()) {
                for (Object[] row : emailMessageRepository.countByThreadIds(accountId, threadIds)) {
                    threadCounts.put((String) row[0], ((Number) row[1]).longValue());
                }
            }

            List<EmailMessageListDTO> dtos = pagedMessages.getContent().stream()
                .map(m -> {
                    EmailMessageListDTO dto = toListDTO(m);
                    Long count = threadCounts.get(m.getThreadId());
                    if (count != null && count > 1) dto.setThreadCount(count.intValue());
                    return dto;
                })
                .toList();

            // §Change — surface the account's last successful sync so the
            // frontend sync pill can colour itself by freshness without an
            // extra request.
            //
            // lastFetchedAt is stored as a zone-less LocalDateTime. Convert
            // it to an Instant using the JVM default zone so Jackson emits
            // an ISO-8601 UTC string ("…Z") — the frontend's new Date(...)
            // then parses it correctly regardless of the user's timezone.
            LocalDateTime lastSyncLocal = emailAccountRepository.findById(accountId)
                .map(a -> a.getLastFetchedAt())
                .orElse(null);
            java.time.Instant lastSyncAt = lastSyncLocal == null
                ? null
                : lastSyncLocal.atZone(java.time.ZoneId.systemDefault()).toInstant();

            Map<String, Object> response = new HashMap<>();
            response.put("messages", dtos);
            response.put("currentPage", pagedMessages.getNumber());
            response.put("totalItems", pagedMessages.getTotalElements());
            response.put("totalPages", pagedMessages.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("lastSyncAt", lastSyncAt);

            return ResponseEntity.ok(ApiResponse.success(200, "Messages retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error getting messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to get messages", "GET_MESSAGES_FAILED"));
        }
    }

    /**
     * Get a single email message with full body parsed from .eml file
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> getMessage(String accountIdObfuscated, String messageIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long messageId = idObfuscator.decodeId(messageIdObfuscated);

            EmailMessage message = emailMessageRepository.findById(messageId).orElse(null);
            if (message == null || !message.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Message not found", "MESSAGE_NOT_FOUND"));
            }

            // Silently mark as read if unread
            if (!message.getIsRead()) {
                message.setIsRead(true);
                emailMessageRepository.save(message);
                emailFolderRepository.incrementUnreadCount(message.getFolder().getId(), -1);
            }

            // Parse HTML body from .eml file
            String htmlBody = parseHtmlBodyFromEml(accountId, message.getStoragePath(), message.getFileName());

            // Get attachments
            List<EmailAttachment> attachments = emailAttachmentRepository.findByEmailMessageId(messageId);
            List<EmailAttachmentDTO> attachmentDTOs = attachments.stream().map(this::toAttachmentDTO).toList();

            EmailMessageDTO dto = toFullDTO(message, htmlBody, attachmentDTOs);

            // Circular navigation within folder
            Long folderId = message.getFolder().getId();
            Long nextId = emailMessageRepository.findNextIdInFolder(accountId, folderId, messageId).orElse(null);
            Long previousId = emailMessageRepository.findPreviousIdInFolder(accountId, folderId, messageId).orElse(null);
            if (nextId == null) nextId = emailMessageRepository.findFirstIdInFolder(accountId, folderId).orElse(null);
            if (previousId == null) previousId = emailMessageRepository.findLastIdInFolder(accountId, folderId).orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("message", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok(ApiResponse.success(200, "Message retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error getting message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to get message", "GET_MESSAGE_FAILED"));
        }
    }

    /**
     * §2 — Get a thread by its threadId (RFC-2822 derived string), returning
     * thread-level metadata plus all messages oldest → newest. This is the
     * shape the inbox v2 reading pane expects.
     */
    public ResponseEntity<ApiResponse<?>> getThreadById(String accountIdObfuscated, String threadId) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            List<EmailMessage> messages = emailMessageRepository
                .findByEmailAccountIdAndThreadIdOrderBySentAtAsc(accountId, threadId);
            if (messages.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Thread not found", "THREAD_NOT_FOUND"));
            }

            EmailMessage latest = messages.get(messages.size() - 1);
            List<EmailMessageListDTO> messageDtos = messages.stream().map(this::toListDTO).toList();

            // Union of labels across the whole thread, deduplicated by id.
            Set<String> threadLabels = new HashSet<>();
            messages.forEach(m -> m.getLabels().forEach(l -> threadLabels.add(idObfuscator.encodeId(l.getId()))));

            // Distinct participants by email address.
            Map<String, Map<String, String>> participants = new HashMap<>();
            for (EmailMessage m : messages) {
                if (m.getFromAddress() == null) continue;
                participants.putIfAbsent(m.getFromAddress(), Map.of(
                    "name", m.getFromName() == null ? m.getFromAddress() : m.getFromName(),
                    "email", m.getFromAddress()
                ));
            }

            Map<String, Object> thread = new HashMap<>();
            thread.put("id", threadId);
            thread.put("subject", latest.getSubject());
            thread.put("participants", participants.values());
            thread.put("labels", threadLabels);
            thread.put("messages", messageDtos);
            thread.put("messageCount", messages.size());

            Map<String, Object> response = new HashMap<>();
            response.put("thread", thread);

            return ResponseEntity.ok(ApiResponse.success(200, "Thread retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error getting thread by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to get thread", "GET_THREAD_FAILED"));
        }
    }

    /**
     * Get thread (all messages with the same threadId) — legacy entry point
     * that takes a message id and resolves to its thread.
     */
    public ResponseEntity<ApiResponse<?>> getThread(String accountIdObfuscated, String messageIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long messageId = idObfuscator.decodeId(messageIdObfuscated);

            EmailMessage message = emailMessageRepository.findById(messageId).orElse(null);
            if (message == null || !message.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Message not found", "MESSAGE_NOT_FOUND"));
            }

            List<EmailMessage> threadMessages = emailMessageRepository
                .findByEmailAccountIdAndThreadIdOrderBySentAtAsc(accountId, message.getThreadId());

            List<EmailMessageListDTO> dtos = threadMessages.stream().map(this::toListDTO).toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Thread retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error getting thread", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to get thread", "GET_THREAD_FAILED"));
        }
    }

    /**
     * Get raw .eml file bytes for download
     */
    public byte[] getRawEml(String accountIdObfuscated, String messageIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long messageId = idObfuscator.decodeId(messageIdObfuscated);

            EmailMessage message = emailMessageRepository.findById(messageId).orElse(null);
            if (message == null || !message.getEmailAccount().getId().equals(accountId)) {
                return null;
            }

            return emailStorageService.readEmlFile(accountId, message.getStoragePath(), message.getFileName());
        } catch (Exception e) {
            log.error("Error getting raw eml", e);
            return null;
        }
    }

    /**
     * Get attachment bytes for download
     */
    public byte[] getAttachmentBytes(String accountIdObfuscated, String messageIdObfuscated, String attachmentIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long messageId = idObfuscator.decodeId(messageIdObfuscated);
            Long attachmentId = idObfuscator.decodeId(attachmentIdObfuscated);

            EmailMessage message = emailMessageRepository.findById(messageId).orElse(null);
            if (message == null || !message.getEmailAccount().getId().equals(accountId)) {
                return null;
            }

            EmailAttachment attachment = emailAttachmentRepository.findById(attachmentId).orElse(null);
            if (attachment == null || !attachment.getEmailMessage().getId().equals(messageId)) {
                return null;
            }

            return emailStorageService.readAttachment(accountId, attachment.getFileName());
        } catch (Exception e) {
            log.error("Error getting attachment", e);
            return null;
        }
    }

    /**
     * Get attachment metadata
     */
    public EmailAttachment getAttachmentEntity(String accountIdObfuscated, String messageIdObfuscated, String attachmentIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long messageId = idObfuscator.decodeId(messageIdObfuscated);
            Long attachmentId = idObfuscator.decodeId(attachmentIdObfuscated);

            EmailMessage message = emailMessageRepository.findById(messageId).orElse(null);
            if (message == null || !message.getEmailAccount().getId().equals(accountId)) {
                return null;
            }

            EmailAttachment attachment = emailAttachmentRepository.findById(attachmentId).orElse(null);
            if (attachment == null || !attachment.getEmailMessage().getId().equals(messageId)) {
                return null;
            }

            return attachment;
        } catch (Exception e) {
            log.error("Error getting attachment entity", e);
            return null;
        }
    }

    // =====================================================================
    // Private helpers
    // =====================================================================

    private String parseHtmlBodyFromEml(Long accountId, String storagePath, String fileName) {
        try {
            byte[] emlBytes = emailStorageService.readEmlFile(accountId, storagePath, fileName);
            if (emlBytes == null) return null;

            Session session = Session.getInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session, new ByteArrayInputStream(emlBytes));

            Object content = mimeMessage.getContent();
            if (content instanceof String text) {
                // Single-part message
                if (mimeMessage.isMimeType("text/html")) {
                    return text;
                }
                return "<pre>" + text + "</pre>";
            }
            if (content instanceof MimeMultipart multipart) {
                return extractHtmlFromMultipart(multipart);
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to parse HTML body from .eml: {}", e.getMessage());
            return null;
        }
    }

    private String extractHtmlFromMultipart(MimeMultipart multipart) throws Exception {
        String htmlContent = null;
        String plainContent = null;

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.isMimeType("text/html")) {
                htmlContent = (String) part.getContent();
            } else if (part.isMimeType("text/plain") && plainContent == null) {
                plainContent = (String) part.getContent();
            } else if (part.getContent() instanceof MimeMultipart nested) {
                String nestedHtml = extractHtmlFromMultipart(nested);
                if (nestedHtml != null) htmlContent = nestedHtml;
            }
        }

        if (htmlContent != null) return htmlContent;
        if (plainContent != null) return "<pre>" + plainContent + "</pre>";
        return null;
    }

    private EmailMessageListDTO toListDTO(EmailMessage message) {
        return EmailMessageListDTO.builder()
            .id(idObfuscator.encodeId(message.getId()))
            .fromAddress(message.getFromAddress())
            .fromName(message.getFromName())
            .toAddresses(message.getToAddresses())
            .subject(message.getSubject())
            .snippet(message.getSnippet())
            .isRead(message.getIsRead())
            .isStarred(message.getIsStarred())
            .isFlagged(message.getIsFlagged())
            .isDraft(message.getIsDraft())
            .hasAttachments(message.getHasAttachments())
            .attachmentCount(message.getAttachmentCount())
            .sentAt(message.getSentAt())
            .snoozeUntil(message.getSnoozeUntil())
            .threadId(message.getThreadId())
            .labels(message.getLabels() == null ? java.util.List.of()
                : message.getLabels().stream()
                    .map(l -> idObfuscator.encodeId(l.getId()))
                    .toList())
            // threadCount populated in §2.
            .build();
    }

    private EmailMessageDTO toFullDTO(EmailMessage message, String htmlBody, List<EmailAttachmentDTO> attachments) {
        return EmailMessageDTO.builder()
            .id(idObfuscator.encodeId(message.getId()))
            .folderId(idObfuscator.encodeId(message.getFolder().getId()))
            .folderName(message.getFolder().getName())
            .messageId(message.getMessageId())
            .inReplyTo(message.getInReplyTo())
            .threadId(message.getThreadId())
            .fromAddress(message.getFromAddress())
            .fromName(message.getFromName())
            .toAddresses(message.getToAddresses())
            .ccAddresses(message.getCcAddresses())
            .bccAddresses(message.getBccAddresses())
            .subject(message.getSubject())
            .snippet(message.getSnippet())
            .htmlBody(htmlBody)
            .isRead(message.getIsRead())
            .isStarred(message.getIsStarred())
            .isFlagged(message.getIsFlagged())
            .isDraft(message.getIsDraft())
            .hasAttachments(message.getHasAttachments())
            .attachmentCount(message.getAttachmentCount())
            .fileSize(message.getFileSize())
            .sentAt(message.getSentAt())
            .receivedAt(message.getReceivedAt())
            .snoozeUntil(message.getSnoozeUntil())
            .labels(message.getLabels() == null ? java.util.List.of()
                : message.getLabels().stream()
                    .map(l -> idObfuscator.encodeId(l.getId()))
                    .toList())
            .attachments(attachments)
            .createdAt(message.getCreatedAt())
            .updatedAt(message.getUpdatedAt())
            .build();
    }

    private EmailAttachmentDTO toAttachmentDTO(EmailAttachment attachment) {
        return EmailAttachmentDTO.builder()
            .id(idObfuscator.encodeId(attachment.getId()))
            .fileName(attachment.getFileName())
            .originalFileName(attachment.getOriginalFileName())
            .mimeType(attachment.getMimeType())
            .fileSize(attachment.getFileSize())
            .isInline(attachment.getIsInline())
            .build();
    }
}
