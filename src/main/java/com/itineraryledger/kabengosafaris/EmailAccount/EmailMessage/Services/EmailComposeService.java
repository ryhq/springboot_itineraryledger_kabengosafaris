package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailFolderRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailMessageRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.ComposeEmailDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailDeliveryStatus;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailFolder;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailFolderType;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailMessage;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountProvider;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.SendingMethod;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailContact.ContactSource;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailComposeService {

    private final EmailMessageRepository emailMessageRepository;
    private final EmailFolderRepository emailFolderRepository;
    private final EmailAccountRepository emailAccountRepository;
    private final EmailStorageService emailStorageService;
    private final EmailContactService emailContactService;
    private final ObjectMapper objectMapper;
    private final IdObfuscator idObfuscator;

    /**
     * Compose and send a new email
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> composeAndSend(String accountIdObfuscated, ComposeEmailDTO dto, List<MultipartFile> attachments) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            EmailAccount account = emailAccountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            if (Boolean.TRUE.equals(dto.getIsDraft())) {
                return saveDraft(account, dto, attachments);
            }

            return sendEmail(account, dto, attachments, null);
        } catch (Exception e) {
            log.error("Error composing email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to compose email", "COMPOSE_FAILED"));
        }
    }

    /**
     * Reply to an existing email
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> reply(String accountIdObfuscated, String messageIdObfuscated, ComposeEmailDTO dto, List<MultipartFile> attachments) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long originalMsgId = idObfuscator.decodeId(messageIdObfuscated);

            EmailAccount account = emailAccountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            EmailMessage original = emailMessageRepository.findById(originalMsgId).orElse(null);
            if (original == null || !original.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Original message not found", "MESSAGE_NOT_FOUND"));
            }

            // Set reply headers
            dto.setInReplyToMessageId(idObfuscator.encodeId(original.getId()));

            return sendEmail(account, dto, attachments, original);
        } catch (Exception e) {
            log.error("Error replying to email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reply to email", "REPLY_FAILED"));
        }
    }

    /**
     * Reply all to an existing email
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> replyAll(String accountIdObfuscated, String messageIdObfuscated, ComposeEmailDTO dto, List<MultipartFile> attachments) {
        // Same as reply — the frontend sets toAddresses + ccAddresses with all recipients
        return reply(accountIdObfuscated, messageIdObfuscated, dto, attachments);
    }

    /**
     * §10 — Quick reply. Builds a minimal ComposeEmailDTO from the
     * original message (To = sender, optional Cc = original to+cc list
     * minus this account when REPLY_ALL, subject prefixed with "Re:"
     * if missing) and routes through the standard reply flow.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> quickReply(
            String accountIdObfuscated,
            String messageIdObfuscated,
            com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.QuickReplyDTO dto) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long originalMsgId = idObfuscator.decodeId(messageIdObfuscated);

            EmailAccount account = emailAccountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            EmailMessage original = emailMessageRepository.findById(originalMsgId).orElse(null);
            if (original == null || !original.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Original message not found", "MESSAGE_NOT_FOUND"));
            }

            String subject = original.getSubject() == null ? "(no subject)" : original.getSubject();
            if (!subject.toLowerCase().startsWith("re:")) subject = "Re: " + subject;

            java.util.List<String> toList = original.getFromAddress() == null
                ? java.util.List.of()
                : java.util.List.of(original.getFromAddress());

            java.util.List<String> ccList = new java.util.ArrayList<>();
            if (dto.getReplyMode() == com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.QuickReplyDTO.ReplyMode.REPLY_ALL) {
                ccList.addAll(parseAddresses(original.getToAddresses()));
                ccList.addAll(parseAddresses(original.getCcAddresses()));
                // Strip the account's own address so we don't cc ourselves
                String self = account.getEmail();
                if (self != null) ccList.removeIf(addr -> addr.equalsIgnoreCase(self));
            }

            String body = dto.getBody();
            // Treat plain-text bodies as <pre>-style HTML so line breaks survive
            String htmlBody = body.contains("<") ? body
                : "<div style=\"white-space:pre-wrap\">" + escapeHtml(body) + "</div>";

            ComposeEmailDTO compose = ComposeEmailDTO.builder()
                .toAddresses(toList)
                .ccAddresses(ccList)
                .subject(subject)
                .htmlBody(htmlBody)
                .inReplyToMessageId(idObfuscator.encodeId(original.getId()))
                .build();

            return sendEmail(account, compose, null, original);
        } catch (Exception e) {
            log.error("Error quick-replying", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to quick-reply", "QUICK_REPLY_FAILED"));
        }
    }

    private java.util.List<String> parseAddresses(String raw) {
        if (raw == null || raw.isBlank()) return java.util.List.of();
        // Stored as JSON array — fall back to a permissive split if parse fails.
        try {
            com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
            return m.readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
        } catch (Exception ignored) {
            java.util.List<String> out = new java.util.ArrayList<>();
            for (String s : raw.replace("[", "").replace("]", "").replace("\"", "").split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) out.add(t);
            }
            return out;
        }
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Forward an email
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> forward(String accountIdObfuscated, String messageIdObfuscated, ComposeEmailDTO dto, List<MultipartFile> attachments) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long originalMsgId = idObfuscator.decodeId(messageIdObfuscated);

            EmailAccount account = emailAccountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            EmailMessage original = emailMessageRepository.findById(originalMsgId).orElse(null);
            if (original == null || !original.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Original message not found", "MESSAGE_NOT_FOUND"));
            }

            return sendEmail(account, dto, attachments, null);
        } catch (Exception e) {
            log.error("Error forwarding email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to forward email", "FORWARD_FAILED"));
        }
    }

    /**
     * Save a new draft
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> saveDraft(String accountIdObfuscated, ComposeEmailDTO dto, List<MultipartFile> attachments) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            EmailAccount account = emailAccountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            return saveDraft(account, dto, attachments);
        } catch (Exception e) {
            log.error("Error saving draft", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to save draft", "SAVE_DRAFT_FAILED"));
        }
    }

    /**
     * Update an existing draft
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> updateDraft(String accountIdObfuscated, String draftIdObfuscated, ComposeEmailDTO dto, List<MultipartFile> attachments) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long draftId = idObfuscator.decodeId(draftIdObfuscated);

            EmailAccount account = emailAccountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            EmailMessage draft = emailMessageRepository.findById(draftId).orElse(null);
            if (draft == null || !draft.getEmailAccount().getId().equals(accountId) || !draft.getIsDraft()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Draft not found", "DRAFT_NOT_FOUND"));
            }

            // Delete old .eml file
            emailStorageService.deleteEmlFile(accountId, draft.getStoragePath(), draft.getFileName());

            // Build new MimeMessage
            JavaMailSender mailSender = createMailSender(account);
            MimeMessage mimeMessage = buildMimeMessage(mailSender, account, dto, attachments, null);

            // Save new .eml
            String fileName = emailStorageService.generateEmlFileName(mimeMessage.getMessageID());
            emailStorageService.saveEmlFromMimeMessage(accountId, "drafts", fileName, mimeMessage);

            // Calculate file size
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mimeMessage.writeTo(baos);

            // Update DB record
            draft.setToAddresses(dto.getToAddresses() != null ? objectMapper.writeValueAsString(dto.getToAddresses()) : null);
            draft.setCcAddresses(dto.getCcAddresses() != null ? objectMapper.writeValueAsString(dto.getCcAddresses()) : null);
            draft.setBccAddresses(dto.getBccAddresses() != null ? objectMapper.writeValueAsString(dto.getBccAddresses()) : null);
            draft.setSubject(dto.getSubject());
            draft.setSnippet(extractSnippet(dto.getHtmlBody(), 200));
            draft.setFileName(fileName);
            draft.setFileSize((long) baos.size());
            draft.setMessageId(mimeMessage.getMessageID());
            draft.setHasAttachments(attachments != null && !attachments.isEmpty());
            draft.setAttachmentCount(attachments != null ? attachments.size() : 0);
            emailMessageRepository.save(draft);

            return ResponseEntity.ok(ApiResponse.success(200, "Draft updated successfully",
                idObfuscator.encodeId(draft.getId())));
        } catch (Exception e) {
            log.error("Error updating draft", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update draft", "UPDATE_DRAFT_FAILED"));
        }
    }

    /**
     * Send an existing draft
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> sendDraft(String accountIdObfuscated, String draftIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long draftId = idObfuscator.decodeId(draftIdObfuscated);

            EmailAccount account = emailAccountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            EmailMessage draft = emailMessageRepository.findById(draftId).orElse(null);
            if (draft == null || !draft.getEmailAccount().getId().equals(accountId) || !draft.getIsDraft()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Draft not found", "DRAFT_NOT_FOUND"));
            }

            boolean isResendApi = account.getProviderType() == EmailAccountProvider.RESEND
                    && account.getSendingMethod() == SendingMethod.API;

            if (isResendApi) {
                // For Resend API: reconstruct email from draft fields and send via API
                String apiKey = EncryptionUtil.decrypt(account.getApiKey());
                Resend resend = new Resend(apiKey);

                // Parse stored addresses
                List<String> toAddresses = draft.getToAddresses() != null
                        ? objectMapper.readValue(draft.getToAddresses(), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class))
                        : List.of();

                CreateEmailOptions.Builder builder = CreateEmailOptions.builder()
                        .from(account.getName() + " <" + account.getEmail() + ">")
                        .to(toAddresses)
                        .subject(draft.getSubject());

                // Read HTML body from .eml file
                byte[] emlBytes = emailStorageService.readEmlFile(accountId, draft.getStoragePath(), draft.getFileName());
                if (emlBytes != null) {
                    JavaMailSenderImpl tempSender = new JavaMailSenderImpl();
                    jakarta.mail.Session session = tempSender.getSession();
                    MimeMessage parsedMessage = new MimeMessage(session, new java.io.ByteArrayInputStream(emlBytes));
                    String htmlBody = extractHtmlFromMimeMessage(parsedMessage);
                    if (htmlBody != null) {
                        builder.html(htmlBody);
                        builder.text(HtmlToText.convert(htmlBody));
                    }
                }

                if (draft.getCcAddresses() != null) {
                    List<String> ccAddresses = objectMapper.readValue(draft.getCcAddresses(), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                    if (!ccAddresses.isEmpty()) builder.cc(ccAddresses);
                }
                if (draft.getBccAddresses() != null) {
                    List<String> bccAddresses = objectMapper.readValue(draft.getBccAddresses(), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                    if (!bccAddresses.isEmpty()) builder.bcc(bccAddresses);
                }

                resend.emails().send(builder.build());
            } else {
                // Send via SMTP
                byte[] emlBytes = emailStorageService.readEmlFile(accountId, draft.getStoragePath(), draft.getFileName());
                if (emlBytes == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResponse.error(500, "Draft .eml file not found", "DRAFT_FILE_MISSING"));
                }

                JavaMailSender mailSender = createMailSender(account);
                jakarta.mail.Session session = ((JavaMailSenderImpl) mailSender).getSession();
                MimeMessage mimeMessage = new MimeMessage(session, new java.io.ByteArrayInputStream(emlBytes));
                mailSender.send(mimeMessage);
            }

            // Move from drafts to sent
            EmailFolder sentFolder = emailFolderRepository
                .findByEmailAccountIdAndType(accountId, EmailFolderType.SENT).orElse(null);
            EmailFolder draftsFolder = draft.getFolder();

            if (sentFolder != null) {
                emailStorageService.moveEmlFile(accountId, "drafts", "sent", draft.getFileName());
                draft.setFolder(sentFolder);
                draft.setStoragePath("sent");
                emailFolderRepository.incrementMessageCount(draftsFolder.getId(), -1);
                emailFolderRepository.incrementMessageCount(sentFolder.getId(), 1);
            }

            draft.setIsDraft(false);
            draft.setSentAt(LocalDateTime.now());
            emailMessageRepository.save(draft);

            // Update account stats
            account.setEmailsSentCount((account.getEmailsSentCount() != null ? account.getEmailsSentCount() : 0L) + 1);
            emailAccountRepository.save(account);

            return ResponseEntity.ok(ApiResponse.success(200, "Draft sent successfully", null));
        } catch (Exception e) {
            log.error("Error sending draft", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to send draft", "SEND_DRAFT_FAILED"));
        }
    }

    // =====================================================================
    // Private helpers
    // =====================================================================

    private ResponseEntity<ApiResponse<?>> sendEmail(EmailAccount account, ComposeEmailDTO dto, List<MultipartFile> attachments, EmailMessage replyTo) {
        try {
            boolean isResendApi = account.getProviderType() == EmailAccountProvider.RESEND
                    && account.getSendingMethod() == SendingMethod.API;

            EmailMessage sentCopy = null;

            if (isResendApi) {
                // Send via Resend HTTP API
                sentCopy = sendViaResendApi(account, dto, attachments, replyTo);
            } else {
                // Send via SMTP (including Resend SMTP)
                JavaMailSender mailSender = createMailSender(account);
                MimeMessage mimeMessage = buildMimeMessage(mailSender, account, dto, attachments, replyTo);
                mailSender.send(mimeMessage);

                // Save .eml copy to SENT folder
                sentCopy = saveSentCopy(account, mimeMessage, dto, attachments, replyTo);
            }

            // Auto-harvest contacts from recipients
            emailContactService.harvestContacts(account, dto.getToAddresses(), ContactSource.SENT);
            emailContactService.harvestContacts(account, dto.getCcAddresses(), ContactSource.CC);

            // Update account stats
            account.setEmailsSentCount((account.getEmailsSentCount() != null ? account.getEmailsSentCount() : 0L) + 1);
            emailAccountRepository.save(account);

            log.info("Email sent successfully from {} to {}", account.getEmail(), dto.getToAddresses());

            /*
             * WHICH message went out, not merely that one did.
             *
             * This used to answer `data: null`, so a caller who needed to record what it had just
             * sent — an availability request naming its own thread — had nothing to record. The
             * fields are null only when the sent copy could not be filed, which is a warning about
             * bookkeeping rather than a failed send.
             */
            Map<String, Object> sent = new HashMap<>();
            sent.put("messageId", sentCopy != null ? idObfuscator.encodeId(sentCopy.getId()) : null);
            sent.put("rfcMessageId", sentCopy != null ? sentCopy.getMessageId() : null);
            sent.put("threadId", sentCopy != null ? sentCopy.getThreadId() : null);
            sent.put("folderId", sentCopy != null && sentCopy.getFolder() != null
                ? idObfuscator.encodeId(sentCopy.getFolder().getId()) : null);

            return ResponseEntity.ok(ApiResponse.success(200, "Email sent successfully", sent));
        } catch (Exception e) {
            log.error("Error sending email from account {}: {}", account.getEmail(), e.getMessage());
            account.setEmailsFailedCount((account.getEmailsFailedCount() != null ? account.getEmailsFailedCount() : 0L) + 1);
            emailAccountRepository.save(account);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to send email: " + e.getMessage(), "SEND_FAILED"));
        }
    }

    /**
     * Send email via Resend HTTP API with support for CC, BCC, and attachments
     */
    /** Returns the sent copy it filed, so this path can be recorded like the SMTP one. */
    private EmailMessage sendViaResendApi(EmailAccount account, ComposeEmailDTO dto, List<MultipartFile> attachments, EmailMessage replyTo) throws ResendException {
        String apiKey = EncryptionUtil.decrypt(account.getApiKey());
        Resend resend = new Resend(apiKey);

        CreateEmailOptions.Builder builder = CreateEmailOptions.builder()
                .from(account.getName() + " <" + account.getEmail() + ">")
                .to(dto.getToAddresses())
                .subject(dto.getSubject())
                .html(dto.getHtmlBody())
                // the text alternative, same as the SMTP path builds
                .text(HtmlToText.convert(dto.getHtmlBody()));

        if (dto.getCcAddresses() != null && !dto.getCcAddresses().isEmpty()) {
            builder.cc(dto.getCcAddresses());
        }
        if (dto.getBccAddresses() != null && !dto.getBccAddresses().isEmpty()) {
            builder.bcc(dto.getBccAddresses());
        }

        if (attachments != null && !attachments.isEmpty()) {
            List<Attachment> resendAttachments = new ArrayList<>(attachments.size());
            for (MultipartFile file : attachments) {
                if (file == null || file.isEmpty()) continue;
                try {
                    String fileName = file.getOriginalFilename() != null
                            ? file.getOriginalFilename()
                            : "attachment";
                    String base64Content = Base64.getEncoder().encodeToString(file.getBytes());
                    resendAttachments.add(Attachment.builder()
                            .fileName(fileName)
                            .content(base64Content)
                            .build());
                } catch (Exception e) {
                    log.warn("Skipping unreadable attachment '{}' for Resend send: {}",
                            file.getOriginalFilename(), e.getMessage());
                }
            }
            if (!resendAttachments.isEmpty()) {
                builder.attachments(resendAttachments);
            }
        }

        CreateEmailResponse response = resend.emails().send(builder.build());
        log.info("Email sent via Resend API from compose. Email ID: {}", response.getId());

        // Build a synthetic MimeMessage purely for storage so the sent-folder
        // read path (which parses .eml from disk) can show the full HTML body
        // — not just the 200-char snippet. The mail sender here is never used
        // to actually send anything; we only need its createMimeMessage() to
        // produce a Session-backed MimeMessage we can serialize.
        try {
            MimeMessage mimeMessage = buildMimeMessage(
                    new JavaMailSenderImpl(), account, dto, attachments, replyTo);
            return saveSentCopy(account, mimeMessage, dto, attachments, replyTo);
        } catch (Exception e) {
            log.warn("Failed to persist .eml for Resend API send from {}: {}",
                    account.getEmail(), e.getMessage());
            return null;
        }
    }

    private ResponseEntity<ApiResponse<?>> saveDraft(EmailAccount account, ComposeEmailDTO dto, List<MultipartFile> attachments) {
        try {
            Long accountId = account.getId();
            JavaMailSender mailSender = createMailSender(account);
            MimeMessage mimeMessage = buildMimeMessage(mailSender, account, dto, attachments, null);

            String fileName = emailStorageService.generateEmlFileName(mimeMessage.getMessageID());
            emailStorageService.saveEmlFromMimeMessage(accountId, "drafts", fileName, mimeMessage);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mimeMessage.writeTo(baos);

            EmailFolder draftsFolder = emailFolderRepository
                .findByEmailAccountIdAndType(accountId, EmailFolderType.DRAFTS).orElse(null);

            if (draftsFolder == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Drafts folder not found", "DRAFTS_FOLDER_MISSING"));
            }

            EmailMessage emailMessage = EmailMessage.builder()
                .emailAccount(account)
                .folder(draftsFolder)
                .messageId(mimeMessage.getMessageID())
                .fromAddress(account.getEmail())
                .fromName(account.getName())
                .toAddresses(dto.getToAddresses() != null ? objectMapper.writeValueAsString(dto.getToAddresses()) : null)
                .ccAddresses(dto.getCcAddresses() != null ? objectMapper.writeValueAsString(dto.getCcAddresses()) : null)
                .bccAddresses(dto.getBccAddresses() != null ? objectMapper.writeValueAsString(dto.getBccAddresses()) : null)
                .subject(dto.getSubject())
                .snippet(extractSnippet(dto.getHtmlBody(), 200))
                .isRead(true)
                .isDraft(true)
                .hasAttachments(attachments != null && !attachments.isEmpty())
                .attachmentCount(attachments != null ? attachments.size() : 0)
                .fileName(fileName)
                .fileSize((long) baos.size())
                .storagePath("drafts")
                .sentAt(LocalDateTime.now())
                .receivedAt(LocalDateTime.now())
                .build();

            emailMessage = emailMessageRepository.save(emailMessage);
            emailFolderRepository.incrementMessageCount(draftsFolder.getId(), 1);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Draft saved successfully", idObfuscator.encodeId(emailMessage.getId())));
        } catch (Exception e) {
            log.error("Error saving draft", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to save draft", "SAVE_DRAFT_FAILED"));
        }
    }

    /**
     * The sent copy, and WHICH copy it was.
     *
     * It used to return nothing, so the id of the message just sent was thrown away and the caller
     * could not point anything at it — no availability request, no follow-up, no "open the thread".
     * Null still means the copy could not be filed, which is a warning rather than a failed send.
     */
    private EmailMessage saveSentCopy(EmailAccount account, MimeMessage mimeMessage, ComposeEmailDTO dto, List<MultipartFile> attachments, EmailMessage replyTo) {
        try {
            Long accountId = account.getId();
            String fileName = emailStorageService.generateEmlFileName(mimeMessage.getMessageID());
            emailStorageService.saveEmlFromMimeMessage(accountId, "sent", fileName, mimeMessage);

            EmailFolder sentFolder = emailFolderRepository
                .findByEmailAccountIdAndType(accountId, EmailFolderType.SENT).orElse(null);

            if (sentFolder == null) {
                log.warn("No SENT folder found for account {}", account.getEmail());
                return null;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mimeMessage.writeTo(baos);

            // Determine threading
            String inReplyTo = null;
            String references = null;
            String threadId = mimeMessage.getMessageID();

            if (replyTo != null) {
                inReplyTo = replyTo.getMessageId();
                references = replyTo.getReferences() != null
                    ? replyTo.getReferences() + " " + replyTo.getMessageId()
                    : replyTo.getMessageId();
                threadId = replyTo.getThreadId();
            }

            EmailMessage emailMessage = EmailMessage.builder()
                .emailAccount(account)
                .folder(sentFolder)
                .messageId(mimeMessage.getMessageID())
                .inReplyTo(inReplyTo)
                .references(references)
                .threadId(threadId)
                .fromAddress(account.getEmail())
                .fromName(account.getName())
                .toAddresses(dto.getToAddresses() != null ? objectMapper.writeValueAsString(dto.getToAddresses()) : null)
                .ccAddresses(dto.getCcAddresses() != null ? objectMapper.writeValueAsString(dto.getCcAddresses()) : null)
                .bccAddresses(dto.getBccAddresses() != null ? objectMapper.writeValueAsString(dto.getBccAddresses()) : null)
                .subject(dto.getSubject())
                .snippet(extractSnippet(dto.getHtmlBody(), 200))
                .isRead(true)
                .isDraft(false)
                .hasAttachments(attachments != null && !attachments.isEmpty())
                .attachmentCount(attachments != null ? attachments.size() : 0)
                .fileName(fileName)
                .fileSize((long) baos.size())
                .storagePath("sent")
                .sentAt(LocalDateTime.now())
                .receivedAt(LocalDateTime.now())
                /*
                 * SENT, because that is what just happened: our server accepted it.
                 *
                 * Only the events path and the Resend webhook set this before, so a message written
                 * by hand sat in Sent with no state at all — indistinguishable from one that never
                 * left. DELIVERED is not ours to claim; the webhook upgrades it when the receiving
                 * server confirms, and that difference is exactly what somebody chasing a lodge needs.
                 */
                .deliveryStatus(EmailDeliveryStatus.SENT)
                .build();

            EmailMessage saved = emailMessageRepository.save(emailMessage);
            emailFolderRepository.incrementMessageCount(sentFolder.getId(), 1);
            return saved;
        } catch (Exception e) {
            log.warn("Failed to save sent copy for account {}: {}", account.getEmail(), e.getMessage());
            return null;
        }
    }

    private MimeMessage buildMimeMessage(JavaMailSender mailSender, EmailAccount account, ComposeEmailDTO dto, List<MultipartFile> attachments, EmailMessage replyTo) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        boolean hasAttachments = attachments != null && !attachments.isEmpty();
        /*
         * Always multipart, attachments or not: multipart is what lets the message carry BOTH a
         * text and an HTML part (see setText below). With NO_MULTIPART there is nowhere to put
         * the text half.
         */
        MimeMessageHelper helper = new MimeMessageHelper(
            mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

        helper.setFrom(new InternetAddress(account.getEmail(), account.getName()));
        helper.setTo(dto.getToAddresses().toArray(new String[0]));

        if (dto.getCcAddresses() != null && !dto.getCcAddresses().isEmpty()) {
            helper.setCc(dto.getCcAddresses().toArray(new String[0]));
        }
        if (dto.getBccAddresses() != null && !dto.getBccAddresses().isEmpty()) {
            helper.setBcc(dto.getBccAddresses().toArray(new String[0]));
        }

        helper.setSubject(dto.getSubject());
        /*
         * text first, HTML second — multipart/alternative, in the order the standard wants.
         *
         * Sending HTML alone (which is what this did) costs spam score and leaves anything
         * reading text with tag soup. The text half is derived from the same body, so the two
         * can never say different things.
         */
        helper.setText(HtmlToText.convert(dto.getHtmlBody()), dto.getHtmlBody());

        // Set Message-ID
        String hostPart = account.getSmtpHost() != null ? account.getSmtpHost() : "resend.dev";
        String messageId = "<" + UUID.randomUUID() + "@" + hostPart + ">";
        mimeMessage.setHeader("Message-ID", messageId);

        // Set threading headers if reply
        if (replyTo != null) {
            if (replyTo.getMessageId() != null) {
                mimeMessage.setHeader("In-Reply-To", replyTo.getMessageId());
                String refs = replyTo.getReferences() != null
                    ? replyTo.getReferences() + " " + replyTo.getMessageId()
                    : replyTo.getMessageId();
                mimeMessage.setHeader("References", refs);
            }
        }

        // Add attachments
        if (hasAttachments) {
            for (MultipartFile file : attachments) {
                String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment";
                helper.addAttachment(filename, file);
            }
        }

        return mimeMessage;
    }

    private JavaMailSender createMailSender(EmailAccount account) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        // Handle Resend SMTP: use API key as password, fixed host/username
        boolean isResendSmtp = account.getProviderType() == EmailAccountProvider.RESEND
                && account.getSendingMethod() == SendingMethod.SMTP;

        if (isResendSmtp) {
            sender.setHost(account.getSmtpHost() != null ? account.getSmtpHost() : "smtp.resend.com");
            sender.setPort(account.getSmtpPort() != null ? account.getSmtpPort() : 465);
            sender.setUsername("resend");
            sender.setPassword(EncryptionUtil.decrypt(account.getApiKey()));
        } else {
            sender.setHost(account.getSmtpHost());
            sender.setPort(account.getSmtpPort());
            sender.setUsername(account.getSmtpUsername());
            sender.setPassword(EncryptionUtil.decrypt(account.getSmtpPassword()));
        }

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(account.getUseTls()));
        props.put("mail.smtp.starttls.required", String.valueOf(account.getUseTls()));
        props.put("mail.smtp.ssl.enable", String.valueOf(account.getUseSsl()));

        if (Boolean.TRUE.equals(account.getUseSsl())) {
            props.put("mail.smtp.socketFactory.protocol", "SSLv23");
            props.put("mail.smtp.socketFactory.port", String.valueOf(account.getSmtpPort()));
        }

        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        return sender;
    }

    private String extractSnippet(String html, int maxLength) {
        if (html == null) return null;
        // Strip HTML tags for snippet
        String text = html.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    /**
     * Extract HTML content from a MimeMessage (for sending drafts via Resend API)
     */
    private String extractHtmlFromMimeMessage(MimeMessage message) {
        try {
            Object content = message.getContent();
            if (content instanceof String) {
                return (String) content;
            }
            if (content instanceof jakarta.mail.Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    jakarta.mail.BodyPart part = multipart.getBodyPart(i);
                    if (part.isMimeType("text/html")) {
                        return (String) part.getContent();
                    }
                }
                // Fallback to text/plain
                for (int i = 0; i < multipart.getCount(); i++) {
                    jakarta.mail.BodyPart part = multipart.getBodyPart(i);
                    if (part.isMimeType("text/plain")) {
                        return (String) part.getContent();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract HTML from MimeMessage: {}", e.getMessage());
        }
        return null;
    }
}
